package com.iotlyca.util;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by guangyang on 17/5/7.
 */

public class HttpParse {
    private static Map<String, Map<String,String>> summaryDic = new HashMap<>();
    private static String mData;

    public HttpParse() {
        summaryDic = new HashMap<>();
    }

    public static Map<String, Map<String,String>> updateSummary(String data) {
        mData = data;
        Map<String, String> topMap = jsonParse(data);
        for (Map.Entry<String, String> entry : topMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            Map<String, String> newMap = jsonParse(value);
            summaryDic.put(key, newMap);
        }
        return summaryDic;
    }

    public static Map<String, String> jsonParse(String data) {
        Map<String, String> map = new HashMap<>();

        try {
            JSONObject objs = new JSONObject(data);
            Iterator iterator = objs.keys();
            String key = "";
            while (iterator.hasNext()) {
                key = (String) iterator.next();
                String result = objs.getString(key);
                map.put(key, result);
                Log.v("http", key);
            }
        } catch (JSONException e) {
            Log.v("json","jsonexception");
        }

        return map;
    }

    public static Map<String, Map<String,String>> getSummaryDic() {
        return summaryDic;
    }

}
