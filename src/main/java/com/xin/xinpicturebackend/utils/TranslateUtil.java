package com.xin.xinpicturebackend.utils;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;

import java.util.List;
import java.util.Map;

/**
 * 使用有道翻译
 */
public class TranslateUtil {

    private static final String url = "https://dict.youdao.com/suggest?num=5&ver=3.0&doctype=json&cache=false&le=en&q=";

    public static String ChToEn(String word) {
        String res = HttpUtil.get(url + word);
        Map bean = JSONUtil.toBean(res, Map.class);
        //bean.get("data").get("entries").get(0).get("explain")
        Map data = (Map)bean.get("data");
        List<Map> entries = JSONUtil.toList((JSONArray) data.get("entries"), Map.class);
        return  ((String) entries.get(0).get("explain")).trim().split(";")[0];
    }

}
