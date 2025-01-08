package com.csl.util;

import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;

import java.time.Instant;
import java.time.ZoneOffset;

public class EveMessageUtill {
    private EveMessageUtill(){}


    public static Json reformatTimeStamp(Json jsonObject) {

        String timeStamp = JsonUtil.getStringFromJson(jsonObject,"timestamp", "");
        if (!timeStamp.isEmpty()) {
            jsonObject.set("timestamp0", timeStamp);
            jsonObject.set("timestamp", convertTimestamp(timeStamp));
        }
        return jsonObject;
    }

    private static long convertTimestamp(String timeStamp) {

        int n= timeStamp.indexOf('+');
        if (n<0) n=timeStamp.indexOf('-');

        String sz="";

        if (n>=0) {
            sz=timeStamp.substring(n);
            timeStamp=timeStamp.substring(0,n);
        }

        if (!timeStamp.endsWith("Z")) timeStamp=timeStamp+"Z";

        Instant instant = Instant.parse(timeStamp);

        long offset=0;

        if (!sz.isEmpty()) {
            ZoneOffset zoneOffSet= ZoneOffset.of(sz);
            offset=zoneOffSet.getTotalSeconds();
        }

        return instant.toEpochMilli()-offset*1000;

    }

}
