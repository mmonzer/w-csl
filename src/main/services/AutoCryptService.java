package main.services;

import com.csl.autocrypt.AutoCrypt;
import com.csl.intercom.jsoncmd.JsonCmdHelp;
import com.ucsl.interfaces.IJsonCmd;
import com.ucsl.interfaces.IJsonCmdHelp;
import com.ucsl.json.Json;
import main.services.endpoints.AutoCryptEndpoints;

/**
 * OCSP : Online Certificate Status Protocol
 * Role :
 * Path :
 * Issuer :
 */
public class AutoCryptService extends Service {
    private AutoCrypt manager;

    /**
     * Default constructor of the Suricata service.
     */
    public AutoCryptService() {
        this("autocrypt",
                "Service for managing the different certificates",
                "auto_crypt");
    }

    /**
     * Generic constructor of the Suricata service.
     */
    public AutoCryptService(String name, String description, String configFileSectionName) {
        super(name, description, configFileSectionName);
        manager = new AutoCrypt();
    }

    /**
     * Initialization of the TAPs commands
     *
     * @param config     the configuration section of the configuration file
     * @param configFile the CSL directory
     * @return true if the initialization happened with no problems, false otherwise.
     */
    @Override
    public boolean init(Json config, String configFile) {
        manager.setIp(config.at("ip").asString());
        manager.setPort(config.at("port").asInteger());
        manager.reinitApiHandler();

        addCmd("command_to_change",
                new IJsonCmd() {
                    @Override
                    public Json exec(Json params) {
                        Json payload = Json.object();
                        payload.at("cmd", "command_to_change");
                        payload.at("params", params);
                        return manager.sendCmdPost("/config", payload.toString()).toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Dummy api point for module")
                        .setParam("No params", "", IJsonCmdHelp.JSON)
                        .setResult("Dummy result", IJsonCmdHelp.JSON)
                        .setStatus(IJsonCmdHelp.STATUS_OK)
        );

        // Connexion
        addCmd(AutoCryptEndpoints.SET_IP, this::changeIp);
        addCmd(AutoCryptEndpoints.SET_PORT, this::changePort);
        // issuer-controller
        addCmd(AutoCryptEndpoints.GET_ISSUERS, this::getIssuers);
        addCmd(AutoCryptEndpoints.GET_ISSUER_INFO, this::getIssuerInfo);
        addCmd(AutoCryptEndpoints.UPDATE_ISSUER_INFO, this::updateIssuerInfo);
        addCmd(AutoCryptEndpoints.DELETE_ISSUER_INFO, this::deleteIssuer);
        addCmd(AutoCryptEndpoints.IMPORT_CERTIFICATE, this::importCertificate);
        // role-controller
        addCmd(AutoCryptEndpoints.GET_ROLES, this::getRoles);
        addCmd(AutoCryptEndpoints.CREATE_ROLE, this::createRole);
        addCmd(AutoCryptEndpoints.GET_ROLE, this::getRole);
        addCmd(AutoCryptEndpoints.DELETE_ROLE, this::deleteRole);
        addCmd(AutoCryptEndpoints.UPDATE_ROLE, this::updateRole);
        // miscellaneous-controller
        addCmd(AutoCryptEndpoints.ACTIVATE_OCSP, this::activateOCSP);
        // certificate-controller
        addCmd(AutoCryptEndpoints.GENERATE_CERTIFICATE, this::generateCertificate);
        addCmd(AutoCryptEndpoints.GET_CERTIFICATES, this::getCertificates);
        addCmd(AutoCryptEndpoints.GET_CERTIFICATE_INFO, this::getCertificateInfo);
        addCmd(AutoCryptEndpoints.REVOKE_CERTIFICATE, this::revokeCertificate);
        // ca-controller
        addCmd(AutoCryptEndpoints.GET_CA, this::generateRootCA);
        addCmd(AutoCryptEndpoints.GENERATE_INTERMEDIATE_CA, this::generateIntermediateCA);

        return true;
    }

    /**
     * Method that changes the ip to connect the autocrypt module
     * @param params parameters with the ip
     */
    public Json changeIp(Json params) {
        if (!params.has("ip") || !params.get("ip").isString()) {
            return JsonApiResponse.error("IP is missing from params").toJson();
        }

        manager.setIp(params.get("ip").asString());
        manager.reinitApiHandler();

        return JsonApiResponse.success().toJson();
    }

    /**
     * Method that changes the port to connect the autocrypt module
     * @param params parameters with the port
     */
    public Json changePort(Json params) {
        if (!params.has("port") || !params.get("port").isNumber()) {
            return JsonApiResponse.error("Port is missing from params").toJson();
        }

        manager.setPort(params.get("port").asInteger());
        manager.reinitApiHandler();

        return JsonApiResponse.success().toJson();
    }

    /**
     * Method that gets the list of issuers
     * @param params parameters with the path
     */
    public Json getIssuers(Json params) {
        return manager.sendCmdGet(AutoCryptEndpoints.ISSUER_URI, params).toJson();
    }

    /**
     * Method that recovers the information of the given issuer
     * @param params parameters with the path and the issuer id
     */
    public Json getIssuerInfo(Json params) {
        if (!params.has("path") || !params.get("path").isString()) {
            return JsonApiResponse.error("Path is missing from params").toJson();
        }
        if (!params.has("issuerId") || !params.get("issuerId").isString()) {
            return JsonApiResponse.error("issuerId is missing from params").toJson();
        }

        return manager.sendCmdGet(AutoCryptEndpoints.ISSUER_URI_+params.get("issuerId").asString(), params).toJson();
    }

    /**
     * Method that updates the information of the given issuer
     * @param params parameters with the path and the issuer id
     */
    public Json updateIssuerInfo(Json params) {
        if (!params.has("path") || !params.get("path").isString()) {
            return JsonApiResponse.error("Path is missing from params").toJson();
        }
        if (!params.has("issuerId") || !params.get("issuerId").isString()) {
            return JsonApiResponse.error("issuerId is missing from params").toJson();
        }
        // TODO: any parameters needed in the body?

        // TODO : path and issuerId are params, but not body
        return manager.sendCmdPut(AutoCryptEndpoints.ISSUER_URI_+params.get("issuerId").asString(), params).toJson();
    }

    /**
     * Method that deletes the given issuer
     * @param params parameters with the path and the issuer id
     */
    public Json deleteIssuer(Json params) {
        if (!params.has("path") || !params.get("path").isString()) {
            return JsonApiResponse.error("Path is missing from params").toJson();
        }
        if (!params.has("issuerId") || !params.get("issuerId").isString()) {
            return JsonApiResponse.error("issuerId is missing from params").toJson();
        }

        return manager.sendCmdDelete(AutoCryptEndpoints.ISSUER_URI_+params.get("issuerId").asString(), params).toJson();
    }

    /**
     * Method that imports a new certificate
     * @param params parameters with the path and the file
     */
    public Json importCertificate(Json params) {
        if (!params.has("path") || !params.get("path").isString()) {
            return JsonApiResponse.error("Path is missing from params").toJson();
        }
        if (!params.has("file") || !params.get("file").isString()) {
            return JsonApiResponse.error("File was not correctly uploaded").toJson();
        }

        // TODO : path is params, but not body
        return manager.sendCmdPost(AutoCryptEndpoints.ISSUER_URI_IMPORT, params).toJson();
    }

    /**
     * Gets the list of roles
     * @param params parameters with the path
     */
    public Json getRoles(Json params) {
        return manager.sendCmdGet(AutoCryptEndpoints.ROLE_URI, params).toJson();
    }

    /**
     * Creates a new role
     * @param params parameters with the path
     */
    public Json createRole(Json params) {
        if (!params.has("path") || !params.get("path").isString()) {
            return JsonApiResponse.error("Path is missing from params").toJson();
        }
        if (!params.has("name") || !params.get("name").isString()) {
            return JsonApiResponse.error("name is missing from params").toJson();
        }

        // TODO : path is params, but not body
        return manager.sendCmdPost(AutoCryptEndpoints.ROLE_URI, params).toJson();
    }

    /**
     * Gets the information of the given role
     * @param params parameters with the path and name of role
     */
    public Json getRole(Json params) {
        if (!params.has("path") || !params.get("path").isString()) {
            return JsonApiResponse.error("Path is missing from params").toJson();
        }
        if (!params.has("name") || !params.get("name").isString()) {
            return JsonApiResponse.error("name is missing from params").toJson();
        }

        return manager.sendCmdGet(AutoCryptEndpoints.ROLE_URI_+params.get("name").asString(), params).toJson();
    }

    /**
     * Deletes the given role
     * @param params parameters with the path and name of role
     */
    public Json deleteRole(Json params) {
        if (!params.has("path") || !params.get("path").isString()) {
            return JsonApiResponse.error("Path is missing from params").toJson();
        }
        if (!params.has("name") || !params.get("name").isString()) {
            return JsonApiResponse.error("name is missing from params").toJson();
        }

        return manager.sendCmdDelete(AutoCryptEndpoints.ROLE_URI_+params.get("name").asString(), params).toJson();
    }

    /**
     * Updates the information of the given role
     * @param params parameters with the path and name of role, others?
     */
    public Json updateRole(Json params) {
        if (!params.has("path") || !params.get("path").isString()) {
            return JsonApiResponse.error("Path is missing from params").toJson();
        }
        if (!params.has("name") || !params.get("name").isString()) {
            return JsonApiResponse.error("name is missing from params").toJson();
        }
        // TODO: any parameters needed in the body?

        // TODO : path is a param, not body
        return manager.sendCmdPut(AutoCryptEndpoints.ROLE_URI_+params.get("name").asString(), params).toJson();
    }

    /**
     * Activates the Online Certificate Status Protocol (OCSP)
     * @param params parameters with the path and name of role, others?
     */
    public Json activateOCSP(Json params) {
        if (!params.has("path") || !params.get("path").isString()) {
            return JsonApiResponse.error("Path is missing from params").toJson();
        }
        if (!params.has("ocspServers") || !params.get("ocspServers").isString()) {
            return JsonApiResponse.error("ocspServers is missing from params").toJson();
        }
        // TODO: any parameters needed in the body?

        // TODO : path and ocspServers are params, not body
        return manager.sendCmdPost(AutoCryptEndpoints.MISC_URI_ACTIVATE_OCSP, params).toJson();
    }

    /**
     * Generates a certificates at the given path and role
     * @param params parameters with the path and role
     */
    public Json generateCertificate(Json params) {
        if (!params.has("path") || !params.get("path").isString()) {
            return JsonApiResponse.error("Path is missing from params").toJson();
        }
        if (!params.has("role") || !params.get("role").isString()) {
            return JsonApiResponse.error("role is missing from params").toJson();
        }

        // TODO : path is params, not body
        return manager.sendCmdPost(AutoCryptEndpoints.CERT_URI_ISSUE, params).toJson();
    }

    /**
     * Gives the list of certificates
     * @param params parameters with the path
     */
    public Json getCertificates(Json params) {
        return manager.sendCmdGet(AutoCryptEndpoints.CERT_URI, params).toJson();
    }

    /**
     * Gives the information of the given certificate
     * @param params parameters with the serialNumber
     */
    public Json getCertificateInfo(Json params) {
        if (!params.has("serialNumber") || !params.get("serialNumber").isString()) {
            return JsonApiResponse.error("serialNumber is missing from params").toJson();
        }

        return manager.sendCmdGet(AutoCryptEndpoints.CERT_URI_+params.get("serialNumber").asString(), Json.object()).toJson();
    }

    /**
     * Revokes the given certificate
     * @param params parameters with the path
     */
    public Json revokeCertificate(Json params) {
        if (!params.has("path") || !params.get("path").isString()) {
            return JsonApiResponse.error("path is missing from params").toJson();
        }
        // TODO: any parameters needed in the body?

        return manager.sendCmdDelete(AutoCryptEndpoints.CERT_URI_REVOKE, Json.object()).toJson();
    }

    /**
     * Generate root CA
     * @param params parameters with commonName, ttl, and optionally path
     */
    public Json generateRootCA(Json params) {
        if (!params.has("commonName") || !params.get("commonName").isString()) {
            return JsonApiResponse.error("commonName is missing from params").toJson();
        }
        if (!params.has("ttl") || !params.get("ttl").isString()) {
            return JsonApiResponse.error("ttl is missing from params").toJson();
        }
        // TODO: any parameters needed in the body?

        // TODO : commonName and ttl are params, not body
        return manager.sendCmdPost(AutoCryptEndpoints.CA_URI_GENERATE_ROOT, params).toJson();
    }

    /**
     * Generate intermediate CA
     * @param params parameters with commonName, ttl, and optionally path
     */
    public Json generateIntermediateCA(Json params) {
        if (!params.has("commonName") || !params.get("commonName").isString()) {
            return JsonApiResponse.error("commonName is missing from params").toJson();
        }
        if (!params.has("ttl") || !params.get("ttl").isString()) {
            return JsonApiResponse.error("ttl is missing from params").toJson();
        }
        if (!params.has("path") || !params.get("path").isString()) {
            return JsonApiResponse.error("path is missing from params").toJson();
        }
        // TODO: any parameters needed in the body?

        // TODO : path, commonName and ttl are params, not body
        return manager.sendCmdPost(AutoCryptEndpoints.CA_URI_GENERATE_INTER, params).toJson();
    }

    // TODO : methods
    // TODO : endpoints
    // TODO : variables : params and body
    // TODO : documentation: endpoint and javadoc
}
