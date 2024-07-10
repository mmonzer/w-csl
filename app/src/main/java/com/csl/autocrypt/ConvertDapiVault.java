package com.csl.autocrypt;

import com.ucsl.json.Json;

import java.util.Arrays;
import java.util.HashMap;

import static com.csl.autocrypt.enums.AutocryptConstants.*;
import static com.csl.autocrypt.outils.JsonHelper.jsonListToStringListAtJson;

public class ConvertDapiVault {
    static final HashMap<String, String> relationDbApiToVault = new HashMap<>();
    static final HashMap<String, String> relationVaultToDbApi = new HashMap<>();
    static final String[] fieldsListToString = {CRL_DISTRIBUTION_POINTS, OCSP_SERVERS};

    // region Init forward relation : dbapi->vault
    static {
        relationDbApiToVault.put(ORGANIZATION_UNIT, OU);
        relationDbApiToVault.put(STATE, PROVINCE);
        relationDbApiToVault.put(CRL_DISTRIBUTION_POINTS, CRL_DISTRIBUTION_POINTS);
        relationDbApiToVault.put(OCSP_SERVERS, OCSP_SERVERS);
    }
    // endregion Init forward relation

    // region Init reverse relation
    static {
        for (String key : relationDbApiToVault.keySet()) {
            String value = relationDbApiToVault.get(key);
            if (relationVaultToDbApi.containsKey(value)) {
                throw new RuntimeException("Wrong keys at ConvertDbapiVault");
            }
            relationVaultToDbApi.put(value, key);
        }
    }
    // endregion Init reverse relation

    public static void transformToVault(Json obj, String... keys) {
        if (obj != null) {
            for (String key : keys) {
                if (obj.has(key) && relationDbApiToVault.containsKey(key)) {
                    Json val = obj.get(key);
                    obj.set(relationDbApiToVault.get(key), val);
                    obj.delAt(key);
                }
            }
        }
    }

    public static void transformToDbapi(Json obj, String... keys) {
        if (obj != null) {
            for (String key : keys) {
                if (obj.has(key) && relationVaultToDbApi.containsKey(key)) {
                    if (Arrays.asList(fieldsListToString).contains(key)) {
                        jsonListToStringListAtJson(obj, key);
                    }
                    Json val = obj.get(key);
                    obj.delAt(key);
                    obj.set(relationVaultToDbApi.get(key), val);
                }
            }
        }
    }
}
