package com.hykj.ccbrother.utils;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * Created by Administrator on 2017/7/5 0005.
 */
public class Util {
    /**
     * 产生随机数
     *
     * @param type
     * @return
     */
    public static String code(String type) {
        SimpleDateFormat sdf = new SimpleDateFormat("yy-M-d mm:ss:SSS");
        Random random = new Random();
        String time = sdf.format(new Date());
        String time1 = time.replaceAll(" ", "-").replaceAll("-", "").replaceAll(":", "");
        int a = random.nextInt(90) + 10;
        String orderNo = time1 + a;
        return type + orderNo;
    }

    public static void returnJson(HttpServletResponse response, String json) throws Exception {
        PrintWriter writer = null;
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=utf-8");
        try {
            writer = response.getWriter();
            writer.print(json);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null)
                writer.close();
        }
    }

    /**
     * 判断时间是不是今天
     *
     * @param date
     * @return 是返回true，不是返回false
     */
    public static boolean isNow(Date date) {

        if (date == null) {
            return false;
        }
        //当前时间
        Date now = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMdd");
        //获取今天的日期
        String nowDay = sf.format(now);

        System.out.println(nowDay);
        //对比的时间
        String day = sf.format(date);
        System.out.println(day);
        return day.equals(nowDay);

    }

    /**
     * 程序中访问http数据接口
     */
    public static String getURLContent(String urlStr) {
        /** 网络的url地址 */
        URL url = null;
        /** http连接 */
        HttpURLConnection httpConn = null;
        /**//** 输入流 */
        BufferedReader in = null;
        StringBuffer sb = new StringBuffer();
        try {
            url = new URL(urlStr);
            in = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
            String str = null;
            while ((str = in.readLine()) != null) {
                sb.append(str);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        String result = sb.toString();
        System.out.println(result);
        return result;
    }


}
