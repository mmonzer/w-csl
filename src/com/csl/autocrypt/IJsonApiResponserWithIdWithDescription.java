package com.csl.autocrypt;

import com.ucsl.json.Json;
import main.services.JsonApiResponse;

public interface IJsonApiResponserWithIdWithDescription {
    public JsonApiResponse apply(int id, String name, String description, Json callback);
}
