package com.csl.autocrypt;

import com.ucsl.json.Json;

import java.util.Arrays;
import java.util.HashMap;

import static com.csl.autocrypt.enums.AutocryptConstants.*;
import static com.csl.autocrypt.outils.JsonHelper.jsonListToStringListAtJson;

public class ConvertDapiVault {
    static final HashMap<String, String> relationDbApiToVault = new HashMap<>();
    static final HashMap<String, String> relationVaultToDbApi = new HashMap<>();
    static final String[] fieldsListToString = {Common.CRL_DISTRIBUTION_POINTS, Issuer.OCSP_SERVERS};

    // region Init forward relation : dbapi->vault
    static {
        relationDbApiToVault.put(Common.ORGANIZATION_UNIT, Common.OU);
        relationDbApiToVault.put(Common.STATE, Common.PROVINCE);
        relationDbApiToVault.put(Common.CRL_DISTRIBUTION_POINTS, Common.CRL_DISTRIBUTION_POINTS);
        relationDbApiToVault.put(Issuer.OCSP_SERVERS, Issuer.OCSP_SERVERS);
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

    public static void transformKeysFromDbapiToVault(Json obj, String... keys) {
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

    public static void transformKeysFromVaultToDbapi(Json obj, String... keys) {
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
