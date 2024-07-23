package com.csl.autocrypt.services;

import com.ucsl.json.Json;
import main.services.JsonApiResponse;

@FunctionalInterface
public interface IVoidToJsonApiResponse {
    JsonApiResponse apply();
}
