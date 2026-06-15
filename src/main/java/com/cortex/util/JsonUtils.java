package com.cortex.util;

/**
 * JSON 工具类：LLM 返回的 JSON 清理
 */
public class JsonUtils {

    /** 去除 LLM 返回文本中的 markdown 代码块标记（```json ... ```） */
    public static String cleanJson(String raw) {
        if (raw == null) {
            return "{}";
        }
        raw = raw.trim();
        if (raw.startsWith("```")) {
            int start = raw.indexOf('\n');
            int end = raw.lastIndexOf("```");
            if (start > 0 && end > start) {
                raw = raw.substring(start, end).trim();
            }
        }
        return raw;
    }
}
