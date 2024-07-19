package com.csl.autocrypt;

import com.ucsl.json.Json;
import main.services.JsonApiResponse;

public interface IJsonApiResponserWithIdStrWithDescription {
    public JsonApiResponse apply(String id, String name, String description, Json callback);
}
