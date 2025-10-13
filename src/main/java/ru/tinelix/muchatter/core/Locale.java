package ru.tinelix.muchatter.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;

import org.json.JSONArray;
import org.json.JSONObject;

public class Locale {

    public static String translate(String langCode, String resId) {

        if(!langCode.contains("_"))
            langCode += "_Int";

        String localeFileName = String.format("locales/%s.json", langCode);

        try {
            File f = new File(localeFileName);

            if (!f.exists()) return String.format("[%s|str|%s]", langCode, resId);

            InputStream is = new FileInputStream(localeFileName);
            String jsonTxt = IOUtils.toString(is, "UTF-8");

            JSONObject json = new JSONObject(jsonTxt);

            if(json.has("muchatter_l10n_strings")) {
                JSONObject strings = (JSONObject) json.get("muchatter_l10n_strings");

                if(strings.has(resId) && !strings.isNull(resId)) {
                    return (String) strings.get(resId);
                }
            }
        } catch(IOException ex) {}

        return String.format("[%s|str|%s]", langCode, resId);
    }

    public static String translate(String langCode, String resId, List<Object> args) {

        if(!langCode.contains("_"))
            langCode += "_Int";

        String localeFileName = String.format("locales/%s.json", langCode);

        try {
            File f = new File(localeFileName);

            if (!f.exists()) return String.format("[%s|str|%s]", langCode, resId);

            InputStream is = new FileInputStream(localeFileName);
            String jsonTxt = IOUtils.toString(is, "UTF-8");

            JSONObject json = new JSONObject(jsonTxt);

            if(json.has("muchatter_l10n_strings")) {
                JSONObject strings = (JSONObject) json.get("muchatter_l10n_strings");

                if(strings.has(resId) && !strings.isNull(resId)) {
                    return String.format((String) strings.get(resId), args.toArray());
                }
            }
        } catch(IOException ex) {
            ex.printStackTrace();
        }

        return String.format("[%s|str|%s]", langCode, resId);
    }

    public static String translate(String langCode, String resId, int index) {
        if(!langCode.contains("_"))
            langCode += "_Int";

        String localeFileName = String.format("locales/%s.json", langCode);

        try {
            File f = new File(localeFileName);

            if (!f.exists()) return String.format("[%s|arr[%d]|%s]", langCode, index, resId);

            InputStream is = new FileInputStream(localeFileName);
            String jsonTxt = IOUtils.toString(is, "UTF-8");

            JSONObject json = new JSONObject(jsonTxt);

            if(json.has("muchatter_l10n_arrays")) {
                JSONObject arrays =  (JSONObject) json.get("muchatter_l10n_arrays");

                if(arrays.has(resId) && !arrays.isNull(resId)) {
                    JSONArray array = (JSONArray) arrays.get(resId);

                    return (String) array.get(index);
                }
            }
        } catch(IOException ex) {}

        return String.format("[%s|arr[%d]|%s]", langCode, index, resId);
    }

    public static String translate(String langCode, String resId, int index, List<Object> args) {
        if(!langCode.contains("_"))
            langCode += "_Int";

        String localeFileName = String.format("locales/%s.json", langCode);

        try {
        File f = new File(localeFileName);

            if (!f.exists()) return String.format("[%s|arr[%d]|%s]", langCode, index, resId);

            InputStream is = new FileInputStream(localeFileName);
            String jsonTxt = IOUtils.toString(is, "UTF-8");

            JSONObject json = new JSONObject(jsonTxt);

            if(json.has("muchatter_l10n_arrays")) {
                JSONObject arrays =  (JSONObject) json.get("muchatter_l10n_arrays");

                if(arrays.has(resId) && !arrays.isNull(resId)) {
                    JSONArray array = (JSONArray) arrays.get(resId);
                    return String.format((String) array.get(index), args.toArray());
                }
            }

        } catch(IOException ex) {}

        return String.format("[%s|arr[%d]|%s]", langCode, index, resId);
    }

    public static int getLocaleArrayLength(String langCode, String resId) {
        if(!langCode.contains("_"))
            langCode += "_Int";

        String localeFileName = String.format("locales/%s.json", langCode);

        try {
        File f = new File(localeFileName);

            if (!f.exists()) return 0;

            InputStream is = new FileInputStream(localeFileName);
            String jsonTxt = IOUtils.toString(is, "UTF-8");

            JSONObject json = new JSONObject(jsonTxt);

            if(json.has("muchatter_l10n_arrays")) {
                JSONObject arrays =  (JSONObject) json.get("muchatter_l10n_arrays");

                if(arrays.has(resId) && !arrays.isNull(resId)) {
                    JSONArray array = (JSONArray) arrays.get(resId);
                    return array.length();
                }
            }

        } catch(IOException ex) {}

        return 0;
    }
}
