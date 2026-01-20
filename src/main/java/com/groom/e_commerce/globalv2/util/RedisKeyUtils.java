package com.groom.e_commerce.globalv2.util;

public class RedisKeyUtils {
    private static final String SEPARATOR = ":";

    // MSA 분리 시 이 부분을 각 서비스명으로 고정
    public static String getCartKey(String suffix) {
        return "cart" + SEPARATOR + suffix;
    }

    public static String getProductKey(String suffix) {
        return "product" + SEPARATOR + suffix;
    }
}
