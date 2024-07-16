package com.csl.autocrypt;

import com.ucsl.json.Json;
import main.services.JsonApiResponse;

public interface IJsonApiResponserWithDescription {
    public JsonApiResponse apply(String name, String description, Json callback);
}
