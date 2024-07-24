package com.csl.autocrypt;

import main.services.JsonApiResponse;

@FunctionalInterface
public interface IJsonApeResponseToJsonApiResponse {
    JsonApiResponse apply(JsonApiResponse callback);
}
