package com.gjx.util;

import java.util.regex.Pattern;

/**
 * 验证工具类
 */
public class ValidationUtil {
    
    /**
     * 手机号码正则表达式
     * 匹配中国大陆11位手机号码
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    
    /**
     * 验证手机号码格式
     * @param phone 手机号码
     * @return 是否有效
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone).matches();
    }
}