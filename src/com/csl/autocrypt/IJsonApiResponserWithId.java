package com.csl.autocrypt;

import com.ucsl.json.Json;
import main.services.JsonApiResponse;

public interface IJsonApiResponserWithId {
    public JsonApiResponse apply(int id, Json callback);
}
