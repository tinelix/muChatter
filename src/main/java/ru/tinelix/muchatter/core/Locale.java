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
        String localeFileName = String.format("../locale/%s.json", langCode);

        try {
            File f = new File("file.json");

            if (!f.exists()) return String.format("[%s|%s]", langCode, resId);

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

        return String.format("[%s|%s]", langCode, resId);
    }

    public static String translate(String langCode, String resId, List<Object> args) {
        String localeFileName = String.format("../locale/%s.json", langCode);

        try {
            File f = new File("file.json");

            if (!f.exists()) return String.format("[%s|%s]", langCode, resId);

            InputStream is = new FileInputStream(localeFileName);
            String jsonTxt = IOUtils.toString(is, "UTF-8");

            JSONObject json = new JSONObject(jsonTxt);

            if(json.has("muchatter_l10n_strings")) {
                JSONObject strings = (JSONObject) json.get("muchatter_l10n_strings");

                if(strings.has(resId) && !strings.isNull(resId)) {
                    return String.format((String) strings.get(resId), args.toArray());
                }
            }
        } catch(IOException ex) {}

        return String.format("[%s|%s]", langCode, resId);
    }

    public static String translate(String langCode, String resId, int index) {
        String localeFileName = String.format("../locale/%s.json", langCode);

        try {
            File f = new File("file.json");

            if (!f.exists()) return String.format("[%s|%s]", langCode, resId);

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

        return String.format("[%s|%s]", langCode, resId);
    }

    public static String translate(String langCode, String resId, int index, List<Object> args) {
        String localeFileName = String.format("../locale/%s.json", langCode);

        try {
        File f = new File("file.json");

            if (!f.exists()) return String.format("[%s|%s]", langCode, resId);

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

            if(json.has("muchatter_l10n_arrays")) {
                JSONObject arrays = (JSONObject) json.get("muchatter_l10n_arrays");

                if(arrays.has(resId) && !arrays.isNull(resId)) {
                    JSONArray array = (JSONArray) arrays.get(resId);

                    return String.format((String) array.get(index), args.toArray());
                }
            }

        } catch(IOException ex) {}

        return String.format("[%s|%s]", langCode, resId);
    }
}
