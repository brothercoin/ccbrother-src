package com.hykj.ccbrother.utils;


public abstract class StringUtils {
    private StringUtils() {
    }

    public static boolean isEmpty(String value) {
        int strLen;
        if(value != null && (strLen = value.length()) != 0) {
            for(int i = 0; i < strLen; ++i) {
                if(!Character.isWhitespace(value.charAt(i))) {
                    return false;
                }
            }

            return true;
        } else {
            return true;
        }
    }

    public static boolean isNumeric(Object obj) {
        if(obj == null) {
            return false;
        } else {
            char[] chars = obj.toString().toCharArray();
            int length = chars.length;
            if(length < 1) {
                return false;
            } else {
                int i = 0;
                if(length > 1 && chars[0] == 45) {
                    i = 1;
                }

                while(i < length) {
                    if(!Character.isDigit(chars[i])) {
                        return false;
                    }

                    ++i;
                }

                return true;
            }
        }
    }

    public static boolean areNotEmpty(String... values) {
        boolean result = true;
        if(values != null && values.length != 0) {
            String[] arr$ = values;
            int len$ = values.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                String value = arr$[i$];
                result &= !isEmpty(value);
            }
        } else {
            result = false;
        }

        return result;
    }

    public static String unicodeToChinese(String unicode) {
        StringBuilder out = new StringBuilder();
        if(!isEmpty(unicode)) {
            for(int i = 0; i < unicode.length(); ++i) {
                out.append(unicode.charAt(i));
            }
        }

        return out.toString();
    }

    public static String stripNonValidXMLCharacters(String input) {
        if(input != null && !"".equals(input)) {
            StringBuilder out = new StringBuilder();

            for(int i = 0; i < input.length(); ++i) {
                char current = input.charAt(i);
                if(current == 9 || current == 10 || current == 13 || current >= 32 && current <= '\ud7ff' || current >= '\ue000' && current <= '�' || current >= 65536 && current <= 1114111) {
                    out.append(current);
                }
            }

            return out.toString();
        } else {
            return "";
        }
    }
}