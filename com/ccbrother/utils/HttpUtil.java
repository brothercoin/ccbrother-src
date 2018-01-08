package com.hykj.ccbrother.utils;

import com.hykj.ccbrother.base.HttpException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.log4j.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

public class HttpUtil {

    static boolean isProxy = false;
    static String proxyHost = "178.32.213.128";
    static int proxyPort = 80;


    private static Logger logger = Logger.getLogger(HttpUtil.class);

    public static String post(String url, Map<String, String> param) {

        List<NameValuePair> nvp = new ArrayList<NameValuePair>();
        for (Map.Entry<String, String> entry : param.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            nvp.add(new BasicNameValuePair(key, value));
        }
        UrlEncodedFormEntity params = null;
        try {
            params = new UrlEncodedFormEntity(nvp);
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage(), e);
        }
        params.setContentType("application/x-www-form-urlencoded");//新添加的

        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(params);
        return http(httpPost);


    }

    public static String post(String url, String content) {

        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(new StringEntity(content, "UTF-8"));
        return http(httpPost);

    }




    public static String get(String url, Map<String, Object> params) {
        URI uri = buildURI(url, params);
        if (uri == null) {
            return "";
        }
        HttpGet httpGet = new HttpGet(uri);
        return http(httpGet);
    }


    private static URI buildURI(String url, Map<String, Object> params) {
        try {
            URIBuilder uriBuilder = new URIBuilder(url);
            if (params != null) {
                for (String key : params.keySet()) {
                    if (params.get(key) != null) {
                        uriBuilder.addParameter(key, params.get(key).toString());
                    }
                }
            }
            return uriBuilder.build();
        } catch (Exception ex) {
            logger.error("build URI error!", ex);
        }
        return null;
    }


    /**
     * 将url参数转换成map
     *
     * @param param aa=11&bb=22&cc=33
     * @return
     */
    public static Map<String, String> getUrlParams(String param) {
        Map<String, String> map = new HashMap<String, String>(0);
        if (StringUtils.isEmpty(param)) {
            return map;
        }
        String[] params = param.split("&");
        for (int i = 0; i < params.length; i++) {
            String[] p = params[i].split("=");
            if (p.length == 2) {
                map.put(p[0], p[1]);
            }
        }
        return map;
    }

    /**
     * 将map转换成url key并按字母排列
     *
     * @param map
     * @return
     */
    public static String getUrlParamsByMap(Map<String, Object> map) {
        if (map == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            sb.append(entry.getKey() + "=" + entry.getValue());
            sb.append("&");
        }
        String s = sb.toString();
        if (s.endsWith("&")) {
            s = org.apache.commons.lang.StringUtils.substringBeforeLast(s, "&");
        }
        return s;
    }

    /**
     * @Title: sortMap
     * @Description: 对集合内的数据按key的字母顺序做排序
     */
    public static String sortMap(final Map<String, Object> map) {
        final List<Map.Entry<String, Object>> infos = new ArrayList<Map.Entry<String, Object>>(map.entrySet());

        // 重写集合的排序方法：按字母顺序
        Collections.sort(infos, new Comparator<Map.Entry<String, Object>>() {
            @Override
            public int compare(final Map.Entry<String, Object> o1, final Map.Entry<String, Object> o2) {
                return (o1.getKey().toString().compareTo(o2.getKey()));
            }
        });

        StringBuffer sb = new StringBuffer();
        for (final Map.Entry<String, Object> m : infos) {
            sb.append(m.getKey() + "=" + m.getValue().toString());
            sb.append("&");
        }
        String s = sb.toString();
        if (s.endsWith("&")) {
            s = org.apache.commons.lang.StringUtils.substringBeforeLast(s, "&");
        }
        return s;
    }


    public static String http(HttpUriRequest request) {
        RequestConfig defaultRequestConfig;
        RequestConfig.Builder configBuilder = RequestConfig.custom()
                .setSocketTimeout(15000)
                .setConnectTimeout(15000)
                .setConnectionRequestTimeout(15000)
                .setCookieSpec(CookieSpecs.STANDARD);
        if (isProxy) {
            HttpHost proxy = new HttpHost(proxyHost, proxyPort);
            configBuilder .setProxy(proxy);
        }
        defaultRequestConfig = configBuilder.build();

        CloseableHttpClient httpclient = HttpClients.custom()
                .setSSLSocketFactory(createSSLConnSocketFactory())
                .setDefaultRequestConfig(defaultRequestConfig)
                .setDefaultRequestConfig(RequestConfig.custom()
                       .build())
                .build();


        CloseableHttpResponse response = null;
        BufferedInputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;
        try {
            
            response = httpclient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                logger.error(request.getURI() + " HttpStatus" + statusCode);
            }
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                outputStream = new ByteArrayOutputStream(512);
                inputStream = new BufferedInputStream(entity.getContent());
                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                }

                String str = new String(outputStream.toByteArray(), "UTF-8");
                return str;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new HttpException("send post error!" + request.getURI());
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    logger.error("close response error!" + request.getURI());
                }
            }

            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    logger.error("close inputStream error!" + request.getURI());
                }
            }

            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    logger.error("close outputStream error!" + request.getURI());
                }
            }

            try {
                httpclient.close();
            } catch (IOException e) {
                logger.error("close httpclient error!" + request.getURI());
            }
        }
        return null;
    }


    public static String get(String url, Map params, Map<String, String> header) {
        URI uri = buildURI(url, params);
        if (uri == null) {
            return "";
        }
        HttpGet httpGet = new HttpGet(uri);

        for (Map.Entry<String, String> entry : header.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            httpGet.addHeader(key, value);
        }
        return http(httpGet);
    }




    /**
     * 创建SSL安全连接
     *
     * @return
     */
    private static SSLConnectionSocketFactory createSSLConnSocketFactory() {
        SSLConnectionSocketFactory sslsf = null;
        try {
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {

                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            }).build();
            sslsf = new SSLConnectionSocketFactory(sslContext, new X509HostnameVerifier() {

                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }

                @Override
                public void verify(String host, SSLSocket ssl) throws IOException {
                }

                @Override
                public void verify(String host, X509Certificate cert) throws SSLException {
                }

                @Override
                public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {
                }
            });
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        return sslsf;
    }

    public static String post(String url, Map<String, String> param, Map<String, String> header) {

        List<NameValuePair> nvp = new ArrayList<NameValuePair>();
        for (Map.Entry<String, String> entry : param.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            nvp.add(new BasicNameValuePair(key, value));
        }
        UrlEncodedFormEntity postEntity = null;
        try {
            postEntity = new UrlEncodedFormEntity(nvp);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
        postEntity.setContentType("application/x-www-form-urlencoded");//新添加的
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(postEntity);
        for (Map.Entry<String, String> entry : header.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            httpPost.addHeader(key, value);
        }
        return http(httpPost);

    }


    public static String post(String url, String param, Map<String, String> header) {


        StringEntity httpEntity = new StringEntity(param, "UTF-8");
       // httpEntity.setContentType("application/x-www-form-urlencoded");//新添加的
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(httpEntity);
        for (Map.Entry<String, String> entry : header.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            httpPost.addHeader(key, value);
        }
        return http(httpPost);
    }


    public static String delete(String url, Map<String, String> header) {

        HttpDelete httpDelete = new HttpDelete(url);
        for (Map.Entry<String, String> entry : header.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            httpDelete.addHeader(key, value);
        }
        return http(httpDelete);

    }
}
