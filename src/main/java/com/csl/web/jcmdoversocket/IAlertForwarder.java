package com.csl.web.jcmdoversocket;

import com.ucsl.json.Json;

public interface IAlertForwarder {
    void sendAlert(Json alert);
}
