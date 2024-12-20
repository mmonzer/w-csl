package com.csl.intercom.jsoncmd;

import com.csl.util.JCmd;
import com.ucsl.json.Json;
import com.ucsl.json.JsonUtil;

import java.util.List;
import java.util.Map.Entry;

/**
 * Helper class that builds the apihelp HTML page.
 */
public class ApiGetHelp {

    public static final String STATUS = "status";
    public static final String TD_FIN = "</td>";
    public static final String TR_FIN = "</tr>";
    public static final String PRINT = "print";
    public static final String TD_INIT = "<td>";
    public static final String TR_INIT = "<tr>";

    /**
     * Method that builds the body of HTML page from the api data.
     *
     * @param apiNames        list of names of the api endpoints
     * @param apiDescriptions list of descriptions of the api endpoints
     * @param params          parameters passed through the request
     * @return the HTML page in f{@link String} format.
     */
    public String getHelp(List<String> apiNames, List<String> apiDescriptions, Json params) {

        StringBuilder stringBuilder = new StringBuilder();
        String api = JsonUtil.getStringFromJson(params, "api", "");
        for (int i = 0; i < apiNames.size(); i++) {
            if (!api.isEmpty()) {
                if (api.compareTo(apiNames.get(i)) == 0) {

                    stringBuilder.append(getHelp(apiNames.get(i), apiDescriptions.get(i), params));
                }
            } else {
                if (!params.has("all")) params.set("all", "");
                stringBuilder.append(getHelp(apiNames.get(i), apiDescriptions.get(i), params));
            }
        }


        stringBuilder = new StringBuilder(generatePage("<table class=\"helptable\">" + stringBuilder + "</table>", params));
        return stringBuilder.toString();
    }

    /**
     * Method that builds the HTML page from the api data.
     *
     * @param sbody  body of the HTML page
     * @param params parameters passed through the request
     * @return the HTML page in f{@link String} format.
     */
    private String generatePage(String sbody, Json params) {

        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "<style>\n" +
                getStyle(params) +

                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                sbody +
                "</body>\n" +
                "</html>\n" ;
    }

    /**
     * Method that builds the part of an API endpoint.
     *
     * @param apiName        name of the api endpoint
     * @param apiDescription description of the api endpoint
     * @param params         parameters passed through the request
     * @return the HTML page in f{@link String} format.
     */
    public String getHelp(String apiName, String apiDescription, Json params) {

        String styletr = " style =\"width:100 mm\" ";
        String size = "3";
        if (params.has(STATUS)) size = "4";

        StringBuilder stringBuildPage;
        stringBuildPage = new StringBuilder("<tr width=\"100mm\"><td colspan=\"" + size + "\" class=\"apiname\">" + apiName + TD_FIN + TR_FIN);
        stringBuildPage.append("<tr width=\"100mm\"><td colspan=\"").append(size).append("\" class=\"apidescription\">").append(apiDescription).append(TD_FIN + TR_FIN);

        if (params.has("api") && params.has(JCmd.CMD) && params.has("url")) {

            String urlMain = params.get("url").asString() + "?ex";
            if (params.has(PRINT)) urlMain = urlMain + "&print";
            if (!params.has("all")) urlMain = urlMain + "&api=" + params.get("api").asString();

            stringBuildPage.append("<tr><td colspan=\"").append(size).append("\" >").append("<a href=\"").append(urlMain).append(" \" target=\"_blank\" >main page</a>").append(TD_FIN + TR_FIN);
        }


        Json helpInfoAsJson = getHelpInfoAsJson(apiName, params);

        String cmd = JsonUtil.getStringFromJson(params, JCmd.CMD, "");

        if (!helpInfoAsJson.isArray()) {
            if (helpInfoAsJson.has("error")) {
                stringBuildPage.append("<tr width=\"100mm\"><td colspan=\"\"+size+\"\" >").append("Not available" + TD_FIN + TR_FIN);
            } else {
                stringBuildPage.append("<tr><td colspan=\"\"+size+\"\" >").append("Not available" + TD_FIN + TR_FIN);
            }
        } else {

            stringBuildPage.append("<tr ").append(styletr).append(">").append("    <th >Cmd</th>\n").append(
                    //"    <th>Description</th>\n" +
                    "    <th>Params</th>\n").append("    <th>Result</th>\n");
            if (params.has(STATUS)) stringBuildPage.append("    <th>Status</th>\n");

            stringBuildPage.append("  </tr>");

            for (Json jrow : helpInfoAsJson.asJsonList()) {
                addApiCommandToHelp(apiName, params, jrow, cmd, stringBuildPage);
            }
        }
        return stringBuildPage.toString();
    }

    private void addApiCommandToHelp(String apiName, Json params, Json apiCommand, String requestedCommand, StringBuilder stringBuildPage) {
        boolean okcmd = true;
        boolean full = false;
        String url = "";
        String jcmd = apiCommand.get(JCmd.CMD).asString();
        if (params.has("api") && params.has(JCmd.CMD)) {
            full = true;
        } else {
            if (params.has("url")) url = params.get("url").asString();
            url = url + "?api=" + apiName + "&cmd=" + jcmd + "&ex";
            if (params.has("all")) url = url + "&all";
            if (params.has(PRINT)) url = url + "&print";
        }
        if (!requestedCommand.isEmpty()) okcmd = requestedCommand.compareTo(jcmd) == 0;

        String row = "";
        row = row + "<td><b>" + apiCommand.get(JCmd.CMD).asString() + "</b><br><i>" + apiCommand.get("desc").asString() + TD_FIN;
        if (apiCommand.has(JCmd.PARAMETERS)) {
            StringBuilder stringBuilder = new StringBuilder();
            for (Entry<String, Json> entry : apiCommand.get(JCmd.PARAMETERS).asJsonMap().entrySet()) {
                stringBuilder.append("&bull; ").append(entry.getKey()).append(" : ").append(entry.getValue().asString()).append("<br>");
            }
            row = row + TD_INIT + stringBuilder + TD_FIN;
        } else {
            row = row + TD_INIT + TD_FIN;
        }
        if (apiCommand.has("result")) {
            row = row + TD_INIT + apiCommand.get("result").asString() + TD_FIN;
        } else {
            row = row + TD_INIT + TD_FIN;
        }
        if (apiCommand.has(STATUS)) {
            row = row + TD_INIT + apiCommand.get(STATUS).asString() + TD_FIN;
        }

        if (okcmd) {
            stringBuildPage.append(TR_INIT).append(row).append(TR_FIN);
            row = "";
            String exstyle = "style=\" font-family: 'Courier New', monospace; font-size: small; \"";
            String ex_params = "";
            if (apiCommand.has("ex_params")) ex_params = formatJson(apiCommand.get("ex_params"), true, url);
            String ex_result = "";
            if (apiCommand.has("ex_result")) ex_result = formatJson(apiCommand.get("ex_result"), full, url);

            boolean empty = ex_params.isEmpty();

            if (empty) {
                row = row + "<td style=\"text-align: right; font-size: small;\"> " + TD_FIN;
            } else {
                row = row + "<td style=\"text-align: right; font-size: small;\"><i>Example " + TD_FIN;
            }
            row = row + "<td colspan=\"1 \" " + exstyle + ">" + ex_params + TD_FIN;
            row = row + "<td colspan=\"1 \" " + exstyle + ">" + ex_result + TD_FIN;

            if (apiCommand.has(STATUS)) {
                row = row + TD_INIT + TD_FIN;
            }
            stringBuildPage.append(TR_INIT).append(row).append(TR_FIN);
        }
    }

    private String formatJson(Json j, boolean full, String url) {

        boolean addlink = false;
        String s = JsonUtil.prettyPrint(j);

        StringBuilder z = new StringBuilder();
        String[] lines = s.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (!full) {
                if (i < 8) z.append(lines[i]).append("<br>");
                else addlink = true;
            } else
                z.append(lines[i]).append("<br>");
        }
        if (addlink) z.append("<a href=\"").append(url).append(" \" target=\"_blank\" >More ...</a>");
        return z.toString();
    }

    private Json getHelpInfoAsJson(String apiName, Json jparams) {
        jparams.set("user", "user1");
        return JServiceLoader.getCSLInterModuleCommunicationManager().executeCommand(apiName, Json.object().set(JCmd.CMD, "help").set(JCmd.PARAMETERS, jparams));
    }

    /**
     * Creates the style with a width depending on the parameters of the request.
     *
     * @param params parameters passed through the request
     * @return the style in {@link String} format.
     */
    private String getStyle(Json params) {
        return getStyle(params.has(PRINT));
    }

    /**
     * Creates the style for the apihelp endpoint HTML webpage.
     *
     * @param fixedWidth if the width is fixed or not
     * @return the style in format {@link String}.
     */
    private String getStyle(boolean fixedWidth) {

        String sw = "width: 100%;";
        if (fixedWidth) sw = "width:170mm; table-layout:fixed";

        return ".helptable {\n" +
                        "  font-family: Arial, Helvetica, sans-serif; " + //font-size: 80%;\n" +
                        "  border-collapse: collapse;\n" +
                        //"  width: 100%;\n" +
                        sw +
                        "}\n" +
                        "\n" +
                        ".helptable td, .helptable th {\n" +
                        "  border: 1px solid #ddd;\n" +
                        "  padding: 8px;vertical-align: top;" +
                        "}\n" +
                        "\n" +
                        ".helptable tr:nth-child(odd){background-color: #f2f2f2;}\n" +
                        "\n" +
                        ".helptable tr:hover {background-color: #ddd;}\n" +
                        ".helptable td.apiname {\n" +
                        "  border: 1px solid #ddd;\n" +
                        "  border-top: 6px solid #000;\n" +
                        "  padding: 8px;\n" +
                        "  background-color: #90D26D;\n" +
                        "  font-size: 150%; text-align:center;" +
                        "}\n" +
                        ".helptable td.apidescription {\n" +
                        "  border: 1px solid #ddd;\n" +
                        "  padding: 8px;\n" +
                        "  background-color: #D9EDBF;\n" +
                        "  font-size: 100%; text-align:center;" +
                        "}\n" +
                        ".helptable th {\n" +
                        "  padding-top: 12px;\n" +
                        "  padding-bottom: 12px;\n" +
                        "  text-align: left;\n" +
                        "  background-color:  #2C7865;\n" +
                        "  color: white;\n" +
                        "}";
    }
}
