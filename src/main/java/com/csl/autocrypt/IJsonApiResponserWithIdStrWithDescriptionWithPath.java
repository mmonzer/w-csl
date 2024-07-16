package com.csl.autocrypt;

import com.ucsl.json.Json;
import main.services.JsonApiResponse;

public interface IJsonApiResponserWithIdStrWithDescriptionWithPath {
    public JsonApiResponse apply(String id, String name, String description, String path, Json callback);
}
