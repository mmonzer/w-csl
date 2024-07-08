package com.csl.autocrypt;

import com.ucsl.json.Json;

import java.util.HashMap;

import static com.csl.autocrypt.enums.AutocryptConstants.*;

public class ConvertDapiVault {
    static final HashMap<String, String> relationDbApiToVault = new HashMap<>();
    static final HashMap<String, String> relationVaultToDbApi = new HashMap<>();

    // region Init forward relation : dbapi->vault
    static {
        relationDbApiToVault.put(ORGANIZATION, OU);
        relationDbApiToVault.put(STATE, PROVINCE);
    }
    // endregion Init forward relation

    // region Init reverse relation
    static {
        for (String key: relationDbApiToVault.keySet()) {
            String value = relationDbApiToVault.get(key);
            if (relationVaultToDbApi.containsKey(value)) { throw new IllegalArgumentException("Wrong keys at ConvertDbapiVault");}
            relationVaultToDbApi.put(value, key);
        }
    }
    // endregion Init reverse relation

    public static void transformToVault(Json obj, String... keys) {
        if (obj!=null) {
            for (String key : keys) {
                if (obj.has(key)) {
                    Json val = obj.get(key);
                    obj.set(relationDbApiToVault.get(key), val);
                    obj.delAt(key);
                }
            }
        }
    }

    public static void transformToDbapi(Json obj, String... keys) {
        if (obj!=null) {
            for (String key : keys) {
                if (obj.has(key)) {
                    Json val = obj.get(key);
                    obj.set(relationVaultToDbApi.get(key), val);
                    obj.delAt(key);
                }
            }
        }
    }
}
