package com.csl.intercom.cslscan.models.scans;

import com.ucsl.json.Json;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ExternalGeneratedConnectionRelatesDevice {
    // Getter and Setter for uuid
    private String uuid;
    // Getter and Setter for name
    private String name;
    // Getter and Setter for username
    private String username;
    // Getter and Setter for password
    private String password;
    // Getter and Setter for portNumber
    private int portNumber;
    // Getter and Setter for vendor
    private String vendor;


    public Json serializeForDbapi() {
        return Json.object(
                "name", name,
                "username", username,
                "password", password,
                "portNumber", portNumber,
                "vendor", vendor
        );
    }
}