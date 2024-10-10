package main.services;

import com.csl.autocrypt.AutoCrypt;
import com.csl.core.CSLContext;
import com.csl.core.Config;
import com.csl.intercom.status.IStatusProvider;
import com.csl.logger.LoggerInterfaces;
import com.csl.util.ThreadUtils;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;
import lombok.Getter;
import main.services.endpoints.AutoCryptEndpoints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.csl.autocrypt.ConvertDapiVault.transformKeysFromDbapiToVault;
import static com.csl.autocrypt.outils.JsonHelper.*;
import static com.csl.autocrypt.enums.AutocryptConstants.*;

/**
 * Autocrypt service: managing certificates
 */
public class AutoCryptService extends Service implements IStatusProvider {
    @Getter
    private final AutoCrypt autocrypt;
    private boolean isRemote = false;
    private static final Logger logger = LoggerFactory.getLogger(AutoCryptService.class);
    private final int syncFrequency;

    /**
     * Default constructor of the AutoCrypt service (not remote)
     */
    public AutoCryptService() {
        this(false);
    }

    /**
     * Default constructor of the AutoCrypt service.
     */
    public AutoCryptService(boolean isRemote) {
        this("autocrypt", "Service for managing the different certificates", "auto_crypt", isRemote);
    }

    /**
     * Generic constructor of the AutoCrypt service.
     */
    public AutoCryptService(String name, String description, String configFileSectionName, boolean isRemote) {
        super(name, description, configFileSectionName);
        this.isRemote = isRemote;
        autocrypt = new AutoCrypt(name);
        syncFrequency = Config.instance.Autocrypt.getSyncFrequency();
    }

    /**
     * Initialization of the TAPs commands
     *
     * @return true if the initialization happened with no problems, false otherwise.
     */
    @Override
    public boolean init() {
        if (!isRemote) {
            autocrypt.reinitApiHandlers();
            autocrypt.syncAll();
        }

        CSLContext.instance.getStatusNotifier().registerStatusProvider(name, this);

        createEndpoints();

        //launchAutoSync();

        logger.info("Service autocrypt initialized.");
        return true;
    }

    /**
     * Creates the endpoints of this service
     */
    private void createEndpoints() {
        addCmd("upload_file", this::testUploadFiles);
        // Connexion
        addCmd(AutoCryptEndpoints.GET_IP, this::getIp);
        addCmd(AutoCryptEndpoints.SET_IP, this::changeIp);
        addCmd(AutoCryptEndpoints.GET_PORT, this::getPort);
        addCmd(AutoCryptEndpoints.SET_PORT, this::changePort);
        // issuer-controller
        addCmd(AutoCryptEndpoints.GET_ISSUERS, this::getIssuers);
        addCmd(AutoCryptEndpoints.GET_ISSUER_INFO, this::getIssuerInfo);
        addCmd(AutoCryptEndpoints.UPDATE_ISSUER_INFO, this::updateIssuerInfo);
        addCmd(AutoCryptEndpoints.DELETE_ISSUER_INFO, this::deleteIssuer);
        addCmd(AutoCryptEndpoints.IMPORT_ISSUER_INTERMEDIATE, this::importIssuerIntermediate);
        addCmd(AutoCryptEndpoints.IMPORT_ISSUER_ROOT, this::importIssuerRoot);
        addCmd(AutoCryptEndpoints.EXPORT_ISSUER, this::exportIssuer);
        // role-controller
        addCmd(AutoCryptEndpoints.GET_ROLES, this::getRoles);
        addCmd(AutoCryptEndpoints.CREATE_ROLE, this::createRole);
        addCmd(AutoCryptEndpoints.GET_ROLE, this::getRole);
        addCmd(AutoCryptEndpoints.DELETE_ROLE, this::deleteRole);
        addCmd(AutoCryptEndpoints.UPDATE_ROLE, this::updateRole);
        // miscellaneous-controller
        addCmd(AutoCryptEndpoints.ACTIVATE_OCSP, this::activateOCSP);
        addCmd(AutoCryptEndpoints.IS_ALIVE, this::getStatus);
        // certificate-controller
        addCmd(AutoCryptEndpoints.VALIDATE_TEMPLATE_CERTIFICATE, this::validateTemplate);
        addCmd(AutoCryptEndpoints.GENERATE_CERTIFICATE, this::generateCertificate);
        addCmd(AutoCryptEndpoints.GET_CERTIFICATES, this::getCertificates);
        addCmd(AutoCryptEndpoints.GET_CERTIFICATE_INFO, this::getCertificateInfo);
        addCmd(AutoCryptEndpoints.GET_CERTIFICATE, this::getCertificate);
        addCmd(AutoCryptEndpoints.DOWNLOAD_CERTIFICATE, this::downloadCertificate);
        addCmd(AutoCryptEndpoints.REVOKE_CERTIFICATE, this::revokeCertificate);
        addCmd(AutoCryptEndpoints.DEPLOY_CAMERA_CERTIFICATE, this::deployCertificate);
        // ca-controller
        addCmd(AutoCryptEndpoints.GENERATE_ROOT_CA, this::generateRootCA);
        addCmd(AutoCryptEndpoints.GENERATE_INTERMEDIATE_CA, this::generateIntermediateCA);
    }

    // region endpoint methods

    /**
     * Method that changes the ip to connect the autocrypt module
     *
     * @param body parameters with the ip
     */
    public Json changeIp(Json body) {
        autocrypt.setModuleIp(getValueString(body, Common.IP));
        autocrypt.reinitApiHandlers();

        return JsonApiResponse.success().toJson();
    }

    /**
     * Gets the ip to connect the autocrypt module
     *
     * @param body parameters with the ip
     */
    public Json getIp(Json body) {
        Json response = Json.object();
        response.at(Common.IP, autocrypt.getModuleIp());
        return JsonApiResponse.result(response).toJson();
    }

    /**
     * Dummy file upload commands
     * @param body body of the requests with params
     * @param files uploaded files
     * @return
     */
    public Json testUploadFiles(Json body, Json files) {
        Json response = Json.object();
        response.at(Common.IP, autocrypt.getModuleIp());
        return JsonApiResponse.result(response).toJson();
    }

    /**
     * Method that changes the port to connect the autocrypt module
     *
     * @param body parameters with the port
     */
    public Json changePort(Json body) {
        autocrypt.setModulePort(getValueInteger(body, Common.PORT));
        autocrypt.reinitApiHandlers();

        return JsonApiResponse.success().toJson();
    }

    /**
     * Gets the port to connect the autocrypt module
     *
     * @param body parameters with the port
     */
    public Json getPort(Json body) {
        Json response = Json.object();
        response.at(Common.PORT, autocrypt.getModulePort());
        return JsonApiResponse.result(response).toJson();
    }

    /**
     * Method that gets the list of issuers
     *
     * @param body parameters with the path
     */
    public Json getIssuers(Json body) {
        return autocrypt.getIssuers(body).toJson();
    }

    /**
     * Method that recovers the information of the given issuer
     *
     * @param body parameters with the path and the issuer id
     */
    public Json getIssuerInfo(Json body) {

        // region -- Verify required body keys and extract key values

        getValueString(body, Common.PATH);
        String issuerRef = extractValueString(body, Issuer.ISSUER_REF);

        // endregion -- Verify required body keys and extract key values

        return autocrypt.getIssuerInfo(issuerRef, body).toJson();
    }

    /**
     * Updates the information of the given issuer
     *
     * @param body parameters with the path and the issuer id
     */
    public Json updateIssuerInfo(Json body) {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        transferValueString(body, params, Common.PATH);
        String issuerRef = extractValueString(body, Issuer.ISSUER_REF);
        Json bodyBase = Json.read(body.toString());

        // endregion -- Verify required body keys and extract key values

        return autocrypt.updateIssuerInfo(issuerRef, params, bodyBase).toJson();
    }

    /**
     * Deletes the given issuer
     *
     * @param body parameters with the path and the issuer id
     */
    public Json deleteIssuer(Json body) {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        String issuerRef = extractValueString(body, Issuer.ISSUER_REF);
        transferValueString(body, params, Common.PATH);

        // endregion -- Verify required body keys and extract key values

        return autocrypt.deleteIssuer(issuerRef, body, params).toJson();
    }

    /**
     * Imports a new certificate
     *
     * @param body parameters with the name and the file
     * @param path path for the new issuer
     */
    public JsonApiResponse importIssuer(Json body, String path, boolean isRoot) {

        // region -- Verify required body keys and extract key values

        String name = getValueString(body, Common.NAME);
        Json params = Json.object();
        params.set( Common.PATH, path);
        if (!body.has(Common.FILE) || !body.get(Common.FILE).isString()) {
            return JsonApiResponse.error("File was not correctly uploaded");
        }
        String file = body.get(Common.FILE).asString();

        // endregion -- Verify required body keys and extract key values

        return autocrypt.importIssuer(name, params, file, isRoot);
    }

    /**
     * Imports a new certificate
     *
     * @param body parameters with the path and the file
     */
    public Json importIssuerIntermediate(Json body) {
        return importIssuer(body, extractValueString(body, Common.PATH), false).toJson();
    }

    /**
     * Imports a new certificate
     *
     * @param body parameters with the path and the file
     */
    public Json importIssuerRoot(Json body) {
        return importIssuer(body, Common.PKI, true).toJson();
    }

    /**
     * Exports the given certificate at the given path
     *
     * @param body parameters with the path and the issuer_ref
     */
    public Json exportIssuer(Json body) {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        String issuerRef = getValueString(body, Issuer.ISSUER_REF);
        transferValueString(body, params, Common.PATH);

        // endregion -- Verify required body keys and extract key values

        return autocrypt.exportIssuer(issuerRef, params).toJson();
    }

    /**
     * Gets the list of roles
     *
     * @param body parameters with the path
     */
    public Json getRoles(Json body) {
        return autocrypt.getRoles(body).toJson();
    }

    /**
     * Creates a new role
     *
     * @param body parameters with the path
     */
    public Json createRole(Json body) {

        // region -- Verify required body keys and extract key values

        if (body.has(Role.KEY_TYPE) && body.get(Role.KEY_TYPE).isString()) {
            body.set(Role.KEY_TYPE, body.get(Role.KEY_TYPE).asString().toLowerCase());
        }

        Json params = Json.object();
        getValueString(body, Common.NAME);
        getValueString(body, Issuer.ISSUER_REF);
        transferValueString(body, params, Common.PATH);

        transformKeysFromDbapiToVault(body, Common.ORGANIZATION_UNIT, Common.STATE);

        // endregion -- Verify required body keys and extract key values

        return autocrypt.createRole(name, params, body).toJson();
    }

    /**
     * Gets the information of the given role
     *
     * @param body parameters with the path and name of role
     */
    public Json getRole(Json body) {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        String name = getValueString(body, Common.NAME);
        transferValueString(body, params, Common.PATH);

        // endregion -- Verify required body keys and extract key values

        return autocrypt.getRole(name, params).toJson();
    }

    /**
     * Deletes the given role
     *
     * @param body parameters with the path and name of role
     */
    public Json deleteRole(Json body) {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        String name = extractValueString(body, Common.NAME);
        drop(body, Common.ID);
        transferValueString(body, params, Common.PATH);

        // endregion -- Verify required body keys and extract key values

        return autocrypt.deleteRole(name, body, params).toJson();
    }

    /**
     * Updates the information of the given role
     *
     * @param body parameters with the path and name of role, others?
     */
    public Json updateRole(Json body) {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        String name = extractValueString(body, Common.NAME);
        transferValueString(body, params, Common.PATH);
        transformKeysFromDbapiToVault(body, Common.ORGANIZATION_UNIT, Common.STATE);

        // endregion -- Verify required body keys and extract key values

        return autocrypt.updateRole(name, params, body).toJson();
    }

    /**
     * Activates the Online Certificate Status Protocol (OCSP)
     *
     * @param body parameters with the path and name of role, others?
     */
    public Json activateOCSP(Json body) {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        transferValueString(body, params, Common.PATH);
        transferValueString(body, params, Issuer.OCSP_SERVERS);

        // endregion -- Verify required body keys and extract key values

        return autocrypt.activateOCSP(body, params).toJson();
    }

    /**
     * Validates the template of a certificate
     *
     * @param body parameters with the path, name and issuer_ref
     */
    public Json validateTemplate(Json body) {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        transferValueString(body, params, Common.PATH);
        getValueString(body, Issuer.ISSUER_REF);
        getValueString(body, Common.NAME);

        // endregion -- Verify required body keys and extract key values

        return autocrypt.validateTemplate(body, params).toJson();
    }

    /**
     * Generates a certificates at the given path and role
     *
     * @param body parameters with the path and role
     */
    public Json generateCertificate(Json body) {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        transferValueString(body, params, Common.PATH);
        String roleName = extractValueString(body, Role.VAULT_ROLE_NAME);
        body.set(Role.ROLE_NAME, roleName);
        getValueString(body, Common.TTL);
//        body.set(Common.NAME, getValueString(body, Role.ROLE_NAME));

        // endregion -- Verify required body keys and extract key values

        return autocrypt.generateCertificate(params, body).toJson();
    }

    /**
     * Gives the list of certificates
     *
     * @param body parameters with the path
     */
    public Json getCertificates(Json body) {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        transferValueString(body, params, Common.PATH);

        // endregion -- Verify required body keys and extract key values

        return autocrypt.getCertificates(params).toJson();
    }

    /**
     * Gives the information of the given certificate
     *
     * @param body parameters with the serialNumber
     */
    public Json getCertificateInfo(Json body) {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        transferValueString(body, params, Common.PATH);
        String serialNumber = transferValueString(body, params, Certificate.SERIAL_NUMBER);
        return autocrypt.getCertificateInfo(serialNumber, params).toJson();
    }

    /**
     * Downloads the certificate
     *
     * @param body parameters with the serialNumber
     */
    public Json getCertificate(Json body) throws IllegalArgumentException {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        transferValueString(body, params, Common.PATH);
        String serialNumber = transferValueString(body, params, Certificate.SERIAL_NUMBER);
        boolean withPrivateKey = JsonUtil.getBooleanFromJson(body, Certificate.WITH_PRIVATE_KEY, false);

        // endregion -- Verify required body keys and extract key values

        return autocrypt.getCertificate(serialNumber, withPrivateKey, params).toJson();
    }

    /**
     * Downloads the certificate
     *
     * @param body parameters with the serialNumber
     */
    public Json downloadCertificate(Json body) throws IllegalArgumentException {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        transferValueString(body, params, Common.PATH);
        String serialNumber = transferValueString(body, params, Certificate.SERIAL_NUMBER);

        // endregion -- Verify required body keys and extract key values

        return autocrypt.downloadCertificate(serialNumber, params).getResult();
    }

    /**
     * Revokes the given certificate
     *
     * @param body parameters with the path
     */
    public Json revokeCertificate(Json body) throws IllegalArgumentException {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        transferValueString(body, params, Common.PATH);
//        getValueString(body, TTL);
        String serialNumber = transferValueString(body, params, Certificate.SERIAL_NUMBER);

        // endregion -- Verify required body keys and extract key values

        return autocrypt.revokeCertificate(serialNumber, params).toJson();
    }

    /**
     * Deploys the given certificate to the given device
     *
     * @param body parameters with the path
     */
    public Json deployCertificate(Json body) throws IllegalArgumentException {

        // region -- Verify required body keys and extract key values

        Json params = Json.object();
        params.set(Common.PATH, extractValueString(body, Device.CERTIFICATE_PATH));
        getValueString(body, Device.VENDOR);
        getValueString(body, Device.CERTIFICATE_SERIAL_NUMBER);
        getValueString(body, Device.IP);
        getValueString(body, Device.USERNAME);
        getValueString(body, Device.PASSWORD);

        // endregion -- Verify required body keys and extract key values

        return autocrypt.deployCertificate(body, params).toJson();
    }

    /**
     * Generate root CA
     *
     * @param body parameters with commonName, ttl, and optionally path
     */
    public Json generateRootCA(Json body) throws IllegalArgumentException {
        return autocrypt.generateRootCA(null, body).toJson();
    }

    /**
     * Generate intermediate CA
     *
     * @param body parameters with commonName, ttl, and optionally path
     */
    public Json generateIntermediateCA(Json body) throws IllegalArgumentException {
        return autocrypt.generateIntermediateCA(null, body).toJson();
    }

    /**
     * Verifies if the module api is reachable
     *
     * @param body default body of the POST request
     * @return whether it is reachable
     */
    public Json getStatus(Json body) {
        return JsonApiResponse.result(autocrypt.getStatus()).toJson();
    }

    // endregion endpoint methods

    /**
     * Verifies if the module api is reachable
     *
     * @return whether it is reachable
     */
    @Override
    public Json getStatus() {
        return autocrypt.getStatus();
    }

    /**
     * Synchronize Dbapi with Autocrypt automatically every 300s by default
     */
    private void launchAutoSync() {
        ScheduledExecutorService synchronizationSchedule = Executors.newScheduledThreadPool(1);
        ThreadUtils.uncorrelatedSingleThreadScheduledAtFixedRate(
                synchronizationSchedule,
                autocrypt::syncAll,
                syncFrequency, syncFrequency, TimeUnit.SECONDS,
                "autocrypt sync", LoggerInterfaces.CSL_CLIENT);
    }

}
