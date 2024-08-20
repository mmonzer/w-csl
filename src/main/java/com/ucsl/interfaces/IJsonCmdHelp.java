package com.ucsl.interfaces;

import com.ucsl.json.Json;

public interface IJsonCmdHelp {

    String INT = "int";
    String LONG = "long";
    String STR = "string";
    String JSON = "json";
    String BOOL = "boolean";

    String STATUS_TODO = "TODO";
    String STATUS_OK = "OK";

    IJsonCmdHelp setName(String s);

    IJsonCmdHelp setDesc(String s);

    IJsonCmdHelp setParam(String name, String desc, String type);

    IJsonCmdHelp setResult(String s, String type);

    boolean isHidden();

    void setHidden(boolean hidden);

    Json toJson(Json mode);

    IJsonCmdHelp hide();

    IJsonCmdHelp setStatus(String s);

    IJsonCmdHelp setHelpProvider(ICmdHelpProvider h);
}