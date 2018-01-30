package com.hykj.ccbrother.service.plat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hykj.ccbrother.apimodel.CoinInfo;
import com.hykj.ccbrother.apimodel.OrderInfo;
import com.hykj.ccbrother.apimodel.UserInfo;
import com.hykj.ccbrother.base.AppBack;
import com.hykj.ccbrother.model.CoinPlatModel;
import com.hykj.ccbrother.utils.HttpUtil;
import com.okcoin.rest.MD5Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class QuadrigacxService implements  PlatService {

    private static final Logger logger = LoggerFactory.getLogger(QuadrigacxService.class);

    @Override
    public CoinPlatModel getTicker(CoinPlatModel coinPlatModel) {
    	String url = "https://api.quadrigacx.com/v2/ticker?book=";
    	String symbol = coinPlatModel.getSymbol();
        logger.info("getTicker1"+url);
    	String r = HttpUtil.get(url + symbol, null);
    	logger.info("getTicker2"+r);
    	JSONObject apiBack = JSON.parseObject(r);
    	CoinPlatModel newCoinPlat = new CoinPlatModel();
    	newCoinPlat.setId(coinPlatModel.getId());
    	newCoinPlat.setBuy(apiBack.getBigDecimal("bid"));
    	newCoinPlat.setSell(apiBack.getBigDecimal("ask"));
    	newCoinPlat.setVol(apiBack.getBigDecimal("volume"));
    	newCoinPlat.setLow(apiBack.getBigDecimal("low"));
    	newCoinPlat.setHigh(apiBack.getBigDecimal("high"));
    	newCoinPlat.setLast(apiBack.getBigDecimal("last"));
        return newCoinPlat;
    }

    @Override
    public List<CoinPlatModel> getAllTicker(List<CoinPlatModel> list) {
        String url = "https://api.quadrigacx.com/public/info";
        String r = HttpUtil.get(url , null);
        logger.debug("getAllTicker"+r);
        JSONObject result = JSON.parseObject(r);
        List newList=new ArrayList<CoinPlatModel>();
        for(int i=0;i<list.size();i++) {
            JSONObject coinPlatJson=result.getJSONObject(list.get(i).getSymbol());
            CoinPlatModel newCoinPlat = new CoinPlatModel();
            newCoinPlat.setId(list.get(i).getId());
            newCoinPlat.setBuy(coinPlatJson.getBigDecimal("buy"));
            newCoinPlat.setSell(coinPlatJson.getBigDecimal("sell"));
            newCoinPlat.setVol(coinPlatJson.getBigDecimal("volume"));
            newCoinPlat.setLast(coinPlatJson.getBigDecimal("rate"));
            newCoinPlat.setTradingTime(new Date());
            newList.add(newCoinPlat);
        }
        return newList;
    }

    @Override
    public AppBack trade(String apiKey, String secret, String symbol, int type, BigDecimal price, BigDecimal amount) {
        String url=null;
        if(type==1){
             url = "https://api.quadrigacx.com/v2/buy";
        }
        if(type==2){
            url = "https://api.quadrigacx.com/v2/sell";
        }

        String[] temp = apiKey.split(",");
        String client=temp[0];
        apiKey=temp[1];
        // 构造参数签名
        String nonce = System.currentTimeMillis()+"";
        Map<String, String> params = new HashMap<String, String>();
        params.put("key", apiKey);
        params.put("nonce", nonce);
        String signature = hmac_sha1(nonce +client +apiKey, secret); // Hashing it
        params.put("signature", signature);
        params.put("amount", amount.toString());
        params.put("price", price.toString());
        params.put("book", symbol);
        logger.info(JSON.toJSONString(params));
        String r = HttpUtil.post(url, params);
        logger.info(r);

        JSONObject apiBack = JSON.parseObject(r);
        JSONObject error=apiBack.getJSONObject("error");
        if(error!=null){
            String msg=error.getString("message");
            return new AppBack(-1,msg,msg);
        }
        return new AppBack().add("orderId", apiBack.getString("id"));
    }

    @Override
    public UserInfo getUserInfo(String apiKey, String secret, int platId) {
        String url = "https://api.quadrigacx.com/v2/balance";

        String[] temp = apiKey.split(",");
        String client=temp[0];
        apiKey=temp[1];
        // 构造参数签名
        String nonce = System.currentTimeMillis()+"";
        Map<String, String> params = new HashMap<String, String>();
        params.put("key", apiKey);
        params.put("nonce", nonce);
        String signature = hmac_sha1(nonce +client +apiKey, secret); // Hashing it
        params.put("signature", signature);
        logger.info(JSON.toJSONString(params));
        String r = HttpUtil.post(url, params);
        logger.info(r);
        JSONObject apiBack = JSON.parseObject(r);
        JSONObject error=apiBack.getJSONObject("error");
        UserInfo userInfo = new UserInfo();
        if(error!=null){
            String msg=error.getString("message");
            return userInfo;
        }

        Iterator<Map.Entry<String, Object>> it = apiBack.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            logger.info("key= " + entry.getKey() + " and value= " + entry.getValue());

            temp=entry.getKey().split("_");
            if(temp.length<2){
                continue;
            }
            String name=temp[0];
            CoinInfo coinInfo = new CoinInfo();
            coinInfo.setName(name);
            coinInfo.setAmount(new BigDecimal(entry.getValue().toString()));
            coinInfo.setPlatId(platId);
            if (0 != coinInfo.getAmount().compareTo(BigDecimal.ZERO)) {
                if("available".equals(temp[1])){
                    userInfo.getFreeCoinList().add(coinInfo);
                }

                if("reserved".equals(temp[1])){
                    userInfo.getFreezedCoinList().add(coinInfo);
                }
            }
        }
        return userInfo;

    }

    @Override
    public List<OrderInfo> getOrderInfo(String apiKey, String secret, Integer coinPlatId, String symbol) {
        String url = "https://api.quadrigacx.com/v2/open_orders";

        String[] temp = apiKey.split(",");
        String client=temp[0];
        apiKey=temp[1];
        // 构造参数签名
        String nonce = System.currentTimeMillis()+"";
        Map<String, String> params = new HashMap<String, String>();
        params.put("key", apiKey);
        params.put("nonce", nonce);
        String signature = hmac_sha1(nonce +client +apiKey, secret); // Hashing it
        params.put("signature", signature);
        params.put("book", symbol);
        logger.info(JSON.toJSONString(params));
        String r = HttpUtil.post(url, params);
        logger.info("获取结果 " + r);
        JSONArray orders = JSON.parseArray(r);
        List orderList = new ArrayList();
        if(orders == null && orders.size() == 0){
        	return orderList;
        }

        for (int i = 0; i < orders.size(); i++) {
            JSONObject order = orders.getJSONObject(i);
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setAmount(order.getBigDecimal("amount"));

            SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");//小写的mm表示的是分钟
            String dstr=order.getString("datetime");

            try {
                orderInfo.setCreateDate(sdf.parse(dstr));
            } catch (ParseException e) {
                logger.error(e.getMessage(),e);
            }
            orderInfo.setCoinPlatId(coinPlatId);
            orderInfo.setSymbol(symbol);
            if ("0".equals(order.getString("type"))) {
                orderInfo.setType(1);
            } else if ("1".equals(order.getString("type"))) {
                orderInfo.setType(2);
            }
            orderInfo.setSymbol(order.getString("symbol"));
            orderInfo.setPrice(order.getBigDecimal("price"));

            orderInfo.setStatus(order.getInteger("status"));
            orderInfo.setOrderId(order.getString("id"));
            orderList.add(orderInfo);

        }
        return orderList;

    }

    @Override
    public AppBack cancelOrder(String apiKey, String secret, String orderId, String symbol) {
        String url = "https://api.quadrigacx.com/v2/cancel_order";

        String[] temp = apiKey.split(",");
        String client=temp[0];
        apiKey=temp[1];
        // 构造参数签名
        String nonce = System.currentTimeMillis()+"";
        Map<String, String> params = new HashMap<String, String>();
        params.put("key", apiKey);
        params.put("nonce", nonce);
        String signature = hmac_sha1(nonce +client +apiKey, secret); // Hashing it
        params.put("signature", signature);
        params.put("id", orderId);
        logger.info(JSON.toJSONString(params));
        String r = HttpUtil.post(url, params);
        logger.info("获取结果 " + r);
        if("true".equals(r)){
            return new AppBack();
        }
        if("\"true\"".equals(r)){
            return new AppBack();
        }

        JSONObject apiBack = JSON.parseObject(r);
        JSONObject error=apiBack.getJSONObject("error");
        if(error!=null){
            String msg=error.getString("message");
            return new AppBack(-1,msg,msg);
        }
        return new AppBack();
    }


      private String hmac_sha1(String value, String key) {
        try {
            // Get an hmac_sha1 key from the raw key bytes
            byte[] keyBytes = key.getBytes();
            SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA256");

            // Get an hmac_sha1 Mac instance and initialize with the signing key
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);

            // Compute the hmac on input data bytes
            byte[] rawHmac = mac.doFinal(value.getBytes());

            // Convert raw bytes to Hex
            //   return new String(rawHmac);
            String hexBytes = byte2hex(rawHmac);
            return hexBytes;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

     private String byte2hex(final byte[] b) throws UnsupportedEncodingException {
        String hs = "";
        String stmp = "";
        for (int n = 0; n < b.length; n++) {
            stmp = (java.lang.Integer.toHexString(b[n] & 0xFF));

            if (stmp.length() == 1) {
                hs = hs + "0" + stmp;
            } else hs = hs + stmp;
        }
        return hs;
    }


}
