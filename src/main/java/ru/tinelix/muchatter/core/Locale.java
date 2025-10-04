package ru.tinelix.muchatter.core;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;

public class Locale {

    public static String translate(String langCode, String resId) {
        String localeFileName = String.format("../locale/%s.json", langCode);

        Object obj = new JSONParser().parse(
            new FileReader(localeFileName)
        );

        JSONObject json = (JSONObject) obj;


        if(json.has("muchatter_l10n_strings")) {
            JSONObject strings = json.getJsonObject("muchatter_l10n_strings");

            if(strings.has(resId) && !strings.isNull(resId)) {
                return (String) strings.get(resId);
            }
        }

        return null;
    }

    public static String translate(String language, String resId, List<Object> args) {
        String localeFileName = String.format("../locale/%s.json", langCode);

        Object obj = new JSONParser().parse(
            new FileReader(localeFileName)
        );

        JSONObject json = (JSONObject) obj;


        if(json.has("muchatter_l10n_strings")) {
            JSONObject strings = json.getJsonObject("muchatter_l10n_strings");

            if(strings.has(resId) && !strings.isNull(resId)) {
                return (String) String.format(strings.get(resId), args.toArray());
            }
        }

        return null;
    }

    public static String translate(String language, String resId, int index) {
        String localeFileName = String.format("../locale/%s.json", langCode);

        Object obj = new JSONParser().parse(
            new FileReader(localeFileName)
        );

        JSONObject json = (JSONObject) obj;

        if(json.has("muchatter_l10n_arrays")) {
            JSONObject arrays = json.getJsonObject("muchatter_l10n_arrays");

            if(arrays.has(resId) && !arrays.isNull(resId)) {
                JSONArray array = arrays.getJsonArray(resId);

                return (String) String.format((String) array.get(index), args.toArray());
            }
        }

        return null;
    }

    public static String translate(String language, String resId, int index, List<Object> args) {
        String localeFileName = String.format("../locale/%s.json", langCode);

        Object obj = new JSONParser().parse(
            new FileReader(localeFileName)
        );

        JSONObject json = (JSONObject) obj;

        if(json.has("muchatter_l10n_arrays")) {
            JSONObject arrays = json.getJsonObject("muchatter_l10n_arrays");

            if(arrays.has(resId) && !arrays.isNull(resId)) {
                JSONArray array = arrays.get(resId);

                return String.format((String) array.get(index), args.toArray());
            }
        }

        return null;
    }
}
