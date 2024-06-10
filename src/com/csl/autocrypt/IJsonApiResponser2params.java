package com.csl.autocrypt;

import com.ucsl.json.Json;
import main.services.JsonApiResponse;

public interface IJsonApiResponser2params {
    public JsonApiResponse apply(String id, Json callback);
}
