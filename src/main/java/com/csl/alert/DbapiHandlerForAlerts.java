package com.csl.alert;

import com.csl.core.CSLContext;
import com.csl.core.Config;
import com.csl.intercom.dbapi.DbapiHandler;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage HTTP communications with DB-API.
 * Provides an interface for retrieving the devices, connections and so on,
 * and to send information to it (CPE Items, a Scan's status, ...).
 */
public class DbapiHandlerForAlerts extends DbapiHandler {
    private static final Logger logger = LoggerFactory.getLogger(DbapiHandlerForAlerts.class);

    public DbapiHandlerForAlerts() {
        this("CSLAlerts", CSLContext.getInstance().getConfig());
    }

    public DbapiHandlerForAlerts(String moduleName, Config config) {
        super(moduleName, config);
    }

    /**
     * Insert a new alert into DB
     *
     * @param alert alert to insert
     * @return the content of the response
     */
    public String insertAlert(AlertDescriptor alert) {
        String res = null;
        Request request = createRequest("POST", "/alerts", null, alert.toJson());
        try {
            ContentResponse response = request.send();
            res = new String(response.getContent());
        } catch (Exception e) {
            logger.error("Could not send alerts to DB-API.", e);
        }
        return res;
    }
}
