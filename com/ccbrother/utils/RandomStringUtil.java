package com.hykj.ccbrother.utils;

import java.util.Random;

public class RandomStringUtil {

    public static final String ALLCHAR   = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String ALLNUMBER = "0123456789";

    /**
     * 返回一个定长的随机字符串(只包含大小写字母、数字)
     * 
     * @param length 随机字符串长度
     * @return 随机字符串
     */
    public static String generateString(int length) {
        StringBuffer sb = new StringBuffer();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(ALLCHAR.charAt(random.nextInt(ALLCHAR.length())));
        }
        return sb.toString();
    }

    /**
     * 生成随机数字
     * 
     * @param length
     * @return
     */
    public static String generateNumberString(int length) {
        StringBuffer sb = new StringBuffer();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(ALLNUMBER.charAt(random.nextInt(ALLNUMBER.length())));
        }
        return sb.toString();
    }
}
