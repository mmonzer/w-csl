package com.csl.intercom.status;

import com.ucsl.interfaces.ICSLService;
import com.ucsl.json.Json;

public interface IStatusProvider {
    public Json getStatus();
}
