package com.csl.autocrypt;

import com.ucsl.json.Json;
import main.services.JsonApiResponse;

public interface IJsonApiResponser {
    public JsonApiResponse apply(Json callback);
}
