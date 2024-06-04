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
                    public Json exec(Json body) {
                        Json payload = Json.object();
                        payload.at("cmd", "command_to_change");
                        payload.at("body", body);
                        return manager.sendCmdPost("/config", payload.toString()).toJson();
                    }
                },
                new JsonCmdHelp().setDesc("Dummy api point for module")
                        .setParam("No body", "", IJsonCmdHelp.JSON)
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
        addCmd(AutoCryptEndpoints.GENERATE_ROOT_CA, this::generateRootCA);
        addCmd(AutoCryptEndpoints.GENERATE_INTERMEDIATE_CA, this::generateIntermediateCA);

        return true;
    }

    /**
     * Method that changes the ip to connect the autocrypt module
     *
     * @param body parameters with the ip
     */
    public Json changeIp(Json body) {
        if (!body.has("ip") || !body.get("ip").isString()) {
            return errorVariableNotFound("ip");
        }

        manager.setIp(body.get("ip").asString());
        manager.reinitApiHandler();

        return JsonApiResponse.success().toJson();
    }

    /**
     * Method that changes the port to connect the autocrypt module
     *
     * @param body parameters with the port
     */
    public Json changePort(Json body) {
        if (!body.has("port") || !body.get("port").isNumber()) {
            return errorVariableNotFound("port");
        }

        manager.setPort(body.get("port").asInteger());
        manager.reinitApiHandler();

        return JsonApiResponse.success().toJson();
    }

    /**
     * Method that gets the list of issuers
     *
     * @param body parameters with the path
     */
    public Json getIssuers(Json body) {
        return manager.sendCmdGet(
                AutoCryptEndpoints.ISSUER_URI,
                body
        ).toJson();
    }

    /**
     * Method that recovers the information of the given issuer
     *
     * @param body parameters with the path and the issuer id
     */
    public Json getIssuerInfo(Json body) {
        // Check params
        if (!body.has("path") || !body.get("path").isString()) {
            return errorVariableNotFound("path");
        }
        if (!body.has("issuerId") || !body.get("issuerId").isString()) {
            return errorVariableNotFound("issuerId");
        }
        String issuerId = body.get("issuerId").asString();
        body.delAt("issuerId");

        return manager.sendCmdGet(
                AutoCryptEndpoints.ISSUER_URI_ + issuerId,
                body
        ).toJson();
    }

    /**
     * Method that updates the information of the given issuer
     *
     * @param body parameters with the path and the issuer id
     */
    public Json updateIssuerInfo(Json body) {
        Json params = Json.object();
        // Check params
        if (!body.has("path") || !body.get("path").isString()) {
            return errorVariableNotFound("path");
        }
        params.at("path", body.get("path").asString());
        body.delAt("path");
        if (!body.has("issuerId") || !body.get("issuerId").isString()) {
            return errorVariableNotFound("issuerId");
        }
        String issuerId = body.get("issuerId").asString();
        body.delAt("issuerId");
        // Check body
        // TODO: any parameters needed in the body?

        return manager.sendCmdPut(
                AutoCryptEndpoints.ISSUER_URI_ + issuerId,
                body,
                params
        ).toJson();
    }

    /**
     * Method that deletes the given issuer
     *
     * @param body parameters with the path and the issuer id
     */
    public Json deleteIssuer(Json body) {
        Json params = Json.object();
        // Check params
        if (!body.has("path") || !body.get("path").isString()) {
            return errorVariableNotFound("path");
        }
        params.at("path", body.get("path").asString());
        body.delAt("path");
        if (!body.has("issuerId") || !body.get("issuerId").isString()) {
            return errorVariableNotFound("issuerId");
        }
        String issuerId = body.get("issuerId").asString();
        body.delAt("issuerId");

        return manager.sendCmdDelete(
                AutoCryptEndpoints.ISSUER_URI_ + issuerId,
                body,
                params
        ).toJson();
    }

    /**
     * Method that imports a new certificate
     *
     * @param body parameters with the path and the file
     */
    public Json importCertificate(Json body) {
        Json params = Json.object();
        // Check params
        if (!body.has("path") || !body.get("path").isString()) {
            return errorVariableNotFound("path");
        }
        params.at("path", body.get("path"));
        body.delAt("path");
        // Check file (body)
        if (!body.has("file") || !body.get("file").isString()) {
            return JsonApiResponse.error("File was not correctly uploaded").toJson();
        }

        return manager.sendCmdPost(
                AutoCryptEndpoints.ISSUER_URI_IMPORT,
                body,
                params
        ).toJson();
    }

    /**
     * Gets the list of roles
     *
     * @param body parameters with the path
     */
    public Json getRoles(Json body) {
        return manager.sendCmdGet(
                AutoCryptEndpoints.ROLE_URI,
                body
        ).toJson();
    }

    /**
     * Creates a new role
     *
     * @param body parameters with the path
     */
    public Json createRole(Json body) {
        Json params = Json.object();
        // Check params
        if (!body.has("path") || !body.get("path").isString()) {
            return errorVariableNotFound("path");
        }
        params.at("path", body.get("path"));
        body.delAt("path");
        // Check body
        if (!body.has("name") || !body.get("name").isString()) {
            return errorVariableNotFound("name");
        }

        return manager.sendCmdPost(
                AutoCryptEndpoints.ROLE_URI,
                body,
                params
        ).toJson();
    }

    /**
     * Gets the information of the given role
     *
     * @param body parameters with the path and name of role
     */
    public Json getRole(Json body) {
        Json params = Json.object();
        // Check params
        if (!body.has("path") || !body.get("path").isString()) {
            return errorVariableNotFound("path");
        }
        params.at("path", body.get("path").asString());
        if (!body.has("name") || !body.get("name").isString()) {
            return errorVariableNotFound("name");
        }
        String name = body.get("name").asString();
        body.delAt("name");

        return manager.sendCmdGet(
                AutoCryptEndpoints.ROLE_URI_ + name,
                params
        ).toJson();
    }

    /**
     * Deletes the given role
     *
     * @param body parameters with the path and name of role
     */
    public Json deleteRole(Json body) {
        Json params = Json.object();
        // Check params
        if (!body.has("path") || !body.get("path").isString()) {
            return errorVariableNotFound("path");
        }
        params.at("path", body.get("path"));
        body.delAt("path");
        if (!body.has("name") || !body.get("name").isString()) {
            return errorVariableNotFound("name");
        }
        String name = body.get("name").asString();
        body.delAt("name");

        return manager.sendCmdDelete(
                AutoCryptEndpoints.ROLE_URI_ + name,
                body,
                params
        ).toJson();
    }

    /**
     * Updates the information of the given role
     *
     * @param body parameters with the path and name of role, others?
     */
    public Json updateRole(Json body) {
        Json params = Json.object();
        // Check params
        if (!body.has("path") || !body.get("path").isString()) {
            return errorVariableNotFound("path");
        }
        params.at("path", body.get("path"));
        body.delAt("path");
        if (!body.has("name") || !body.get("name").isString()) {
            return errorVariableNotFound("name");
        }
        String name = body.get("name").asString();
        body.delAt("name");
        // TODO: any parameters needed in the body?

        return manager.sendCmdPut(
                AutoCryptEndpoints.ROLE_URI_ + name,
                body,
                params
        ).toJson();
    }

    /**
     * Activates the Online Certificate Status Protocol (OCSP)
     *
     * @param body parameters with the path and name of role, others?
     */
    public Json activateOCSP(Json body) {
        Json params = Json.object();
        // Check params
        if (!body.has("path") || !body.get("path").isString()) {
            return errorVariableNotFound("path");
        }
        params.at("path", body.get("path"));
        body.delAt("path");
        if (!body.has("ocspServers") || !body.get("ocspServers").isString()) {
            return errorVariableNotFound("ocspServers");
        }
        params.at("ocspServers", body.get("ocspServers"));
        body.delAt("ocspServers");

        return manager.sendCmdPost(
                AutoCryptEndpoints.MISC_URI_ACTIVATE_OCSP,
                body,
                params
        ).toJson();
    }

    /**
     * Generates a certificates at the given path and role
     *
     * @param body parameters with the path and role
     */
    public Json generateCertificate(Json body) {
        Json params = Json.object();
        // Check params
        if (!body.has("path") || !body.get("path").isString()) {
            return errorVariableNotFound("path");
        }
        params.at("path", body.get("path"));
        body.delAt("path");
        // Check body
        if (!body.has("role_name") || !body.get("role_name").isString()) {
            return errorVariableNotFound("role_name");
        }

        return manager.sendCmdPost(
                AutoCryptEndpoints.CERT_URI_ISSUE,
                body,
                params
        ).toJson();
    }

    /**
     * Gives the list of certificates
     *
     * @param body parameters with the path
     */
    public Json getCertificates(Json body) {
        return manager.sendCmdGet(
                AutoCryptEndpoints.CERT_URI,
                body
        ).toJson();
    }

    /**
     * Gives the information of the given certificate
     *
     * @param body parameters with the serialNumber
     */
    public Json getCertificateInfo(Json body) {
        Json params = Json.object();
        // Check params
        if (!body.has("path") || !body.get("path").isString()) {
            return errorVariableNotFound("path");
        }
        params.at("path", body.get("path").asString());
        body.delAt("path");
        if (!body.has("serialNumber") || !body.get("serialNumber").isString()) {
            return errorVariableNotFound("serialNumber");
        }
        String serialNumber = body.get("serialNumber").asString();
        body.delAt("serialNumber");

        return manager.sendCmdGet(
                AutoCryptEndpoints.CERT_URI_ + serialNumber,
                params
        ).toJson();
    }

    /**
     * Revokes the given certificate
     *
     * @param body parameters with the path
     */
    public Json revokeCertificate(Json body) {
        // check params
        Json params = Json.object();
        if (!body.has("path") || !body.get("path").isString()) {
            return errorVariableNotFound("path");
        }
        params.at("path", body.get("path"));
        body.delAt("path");
        if (!body.has("serialNumber") || !body.get("serialNumber").isString()) {
            return errorVariableNotFound("serialNumber");
        }
        String serialNumber = body.get("serialNumber").asString();
        body.delAt("serialNumber");

        return manager.sendCmdDelete(
                AutoCryptEndpoints.CERT_URI_REVOKE+"/"+serialNumber,
                params
        ).toJson();
    }

    /**
     * Generate root CA
     *
     * @param body parameters with commonName, ttl, and optionally path
     */
    public Json generateRootCA(Json body) {
        // check params
        Json params = Json.object();
        if (!body.has("commonName") || !body.get("commonName").isString()) {
            return errorVariableNotFound("commonName");
        }
        params.at("commonName", body.get("commonName"));
        body.delAt("commonName");
        if (!body.has("ttl") || !body.get("ttl").isString()) {
            return errorVariableNotFound("ttl");
        }
        params.at("ttl", body.get("ttl"));
        body.delAt("ttl");

        return manager.sendCmdPost(
                AutoCryptEndpoints.CA_URI_GENERATE_ROOT,
                body,
                params
        ).toJson();
    }

    /**
     * Generate intermediate CA
     *
     * @param body parameters with commonName, ttl, and optionally path
     */
    public Json generateIntermediateCA(Json body) {
        Json params = Json.object();
        // check params
        if (!body.has("path") || !body.get("path").isString()) {
            return errorVariableNotFound("path");
        }
        params.at("path", body.get("path"));
        body.delAt("path");
        // check body
        if (!body.has("type") || !body.get("type").isString()) {
            return errorVariableNotFound("type");
        }
        if (!body.has("commonName") || !body.get("commonName").isString()) {
            return errorVariableNotFound("commonName");
        }
        if (!body.has("ttl") || !body.get("ttl").isString()) {
            return errorVariableNotFound("ttl");
        }

        return manager.sendCmdPost(
                AutoCryptEndpoints.CA_URI_GENERATE_INTER,
                body,
                params
        ).toJson();
    }
}
