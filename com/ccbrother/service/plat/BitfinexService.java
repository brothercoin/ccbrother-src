/*
 * Copyright 2017-2101 Innel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hykj.ccbrother.service.plat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hykj.ccbrother.apimodel.CoinInfo;
import com.hykj.ccbrother.apimodel.OrderInfo;
import com.hykj.ccbrother.apimodel.UserInfo;
import com.hykj.ccbrother.base.AppBack;
import com.hykj.ccbrother.model.CoinPlatModel;
import com.hykj.ccbrother.utils.Base64Util;
import com.hykj.ccbrother.utils.HttpUtil;
import com.hykj.ccbrother.utils.MD5;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.*;

@Service
public class BitfinexService implements PlatService {

    private static final Logger logger = LoggerFactory.getLogger(BitfinexService.class);

    public CoinPlatModel getTicker(CoinPlatModel coinPlatModel) {

        String symbol = coinPlatModel.getSymbol();
        String url = "https://api.bitfinex.com/v1/pubticker/";

        String r = HttpUtil.get(url + symbol, null);
        //logger.debug(r);
        CoinPlatModel newCoinPlat = new CoinPlatModel();
        newCoinPlat.setId(coinPlatModel.getId());
        JSONObject apiBack = JSON.parseObject(r);
        if(apiBack.getString("bid")==null){
            return null;
        }
        if(apiBack.getString("timestamp").split(".").length > 0){
        	 String time = apiBack.getString("timestamp").split(".")[0];
             long timestamp = Long.parseLong(time);
             Date date = new Date(timestamp * 1000);
             newCoinPlat.setTradingTime(date);
        }
        newCoinPlat.setBuy(new BigDecimal(apiBack.getString("bid")));
        newCoinPlat.setHigh(new BigDecimal(apiBack.getString("high")));
        newCoinPlat.setLast(new BigDecimal(apiBack.getString("last_price")));
        newCoinPlat.setLow(new BigDecimal(apiBack.getString("low")));
        newCoinPlat.setSell(new BigDecimal(apiBack.getString("ask")));
        newCoinPlat.setVol(new BigDecimal(apiBack.getString("volume")));
        return newCoinPlat;
    }

    @Override
    public List<CoinPlatModel> getAllTicker(List<CoinPlatModel> list) {
        return null;
    }


    public AppBack trade(String apiKey,
                         String secret,
                         String symbol,
                         int type,
                         BigDecimal price,
                         BigDecimal amount) {
        String url = "https://api.bitfinex.com/v1/order/new";

        JSONObject payload = new JSONObject();
        payload.put("request", "/v1/order/new");
        payload.put("nonce", System.currentTimeMillis() + "");
        payload.put("symbol", symbol);
        payload.put("price", price.toString());
        payload.put("amount", amount.toString());
        payload.put("type", "limit");
        if (type == 1) {
            payload.put("side", "buy");
        }
        if (type == 2) {
            payload.put("side", "sell");
        }


        String payload_base64 = Base64Util.encode(payload.toJSONString().getBytes());
        String sign = createSignature(payload_base64, secret);
        Map<String, String> header = new HashMap<>();
        header.put("X-BFX-APIKEY", apiKey);
        header.put("X-BFX-PAYLOAD", payload_base64);
        header.put("X-BFX-SIGNATURE", sign);
        logger.info("header " + JSON.toJSONString(header));

        logger.info("payload " + JSON.toJSONString(payload));

        String r = HttpUtil.post(url, new HashMap<>(), header);
        logger.info("trade " + r);
        JSONObject apiBack = JSONObject.parseObject(r);
        if (apiBack.getString("message") != null) {
            return new AppBack(-1, apiBack.getString("message"));
        }
        String orderId=apiBack.getString("order_id");
        return new AppBack().add("orderId",orderId);
    }

    public UserInfo getUserInfo(String apiKey,
                                String secret,
                                int platId) {

        String url = "https://api.bitfinex.com/v1/balances";

        JSONObject payload = new JSONObject();
        payload.put("request", "/v1/balances");
        payload.put("nonce", System.currentTimeMillis() + "");

        String payload_base64 = Base64Util.encode(payload.toJSONString().getBytes());

        String sign = createSignature(payload_base64, secret);


        Map<String, String> header = new HashMap<>();
        header.put("X-BFX-APIKEY", apiKey);
        header.put("X-BFX-PAYLOAD", payload_base64);
        header.put("X-BFX-SIGNATURE", sign);
        logger.info("header " + JSON.toJSONString(header));


        String r = HttpUtil.post(url, new HashMap<>(), header);
        logger.info("getUserInfo " + r);
        
        JSONArray data = JSON.parseArray(r);
        UserInfo userInfo = new UserInfo();
        if(r.contains("error")){
        	return userInfo;
        }

        for (int i = 0; i < data.size(); i++) {
            JSONObject coin = data.getJSONObject(i);


            if ("trading".equals(coin.getString("type"))) {
                CoinInfo coinInfo = new CoinInfo();
                coinInfo.setName(coin.getString("currency"));
                coinInfo.setAmount(coin.getBigDecimal("available"));
                coinInfo.setPlatId(platId);
                if (0 != coinInfo.getAmount().compareTo(BigDecimal.ZERO)) {
                    userInfo.getFreeCoinList().add(coinInfo);
                }

                coinInfo = new CoinInfo();
                coinInfo.setName(coin.getString("currency"));
                coinInfo.setAmount(coin.getBigDecimal("amount").subtract(coin.getBigDecimal("available")));
                coinInfo.setPlatId(platId);
                if (0 != coinInfo.getAmount().compareTo(BigDecimal.ZERO)) {
                    userInfo.getFreezedCoinList().add(coinInfo);
                }
            }


        }
        userInfo.setPlatId(platId);
        return userInfo;
    }


    /**
     * "id": 5632146016,
     * "cid": 19066481125,
     * "cid_date": "2017-12-01",
     * "gid": null,
     * "symbol": "dshusd",
     * "exchange": "bitfinex",
     * "price": "1.0",
     * "avg_execution_price": "0.0",
     * "side": "buy",
     * "type": "limit",
     * "timestamp": "1512105467.0",
     * "is_live": true,
     * "is_cancelled": false,
     * "is_hidden": false,
     * "oco_order": null,
     * "was_forced": false,
     * "original_amount": "0.05",
     * "remaining_amount": "0.05",
     * "executed_amount": "0.0",
     * "src": "api"
     *
     * @param apiKey
     * @param secret
     * @param coinPlatId
     * @param symbol
     * @return
     */
    public List<OrderInfo> getOrderInfo(String apiKey,
                                        String secret,
                                        Integer coinPlatId,//选填
                                        String symbol) {//选填

        String url = "https://api.bitfinex.com/v1/orders";
        JSONObject payload = new JSONObject();
        payload.put("request", "/v1/orders");
        payload.put("nonce", System.currentTimeMillis() + "");

        String payload_base64 = Base64Util.encode(payload.toJSONString().getBytes());
        String sign = createSignature(payload_base64, secret);

        Map<String, String> header = new HashMap<>();
        header.put("X-BFX-APIKEY", apiKey);
        header.put("X-BFX-PAYLOAD", payload_base64);
        header.put("X-BFX-SIGNATURE", sign);
        logger.info("header " + JSON.toJSONString(header));
        String r = HttpUtil.post(url, new HashMap<>(), header);
        logger.info("getOrderInfo " + r);
        
        JSONArray apiBack = JSON.parseArray(r);
        List<OrderInfo>  orderList = new ArrayList<>();
        if(r.contains("error")){
        	return orderList;
        }
        
        for (int i = 0; i < apiBack.size(); i++) {
            JSONObject order = apiBack.getJSONObject(i);
            if (!symbol.equalsIgnoreCase(order.getString("symbol"))) {
                continue;
            }
            OrderInfo orderInfo = new OrderInfo();
            BigDecimal amount = order.getBigDecimal("original_amount");
            orderInfo.setAmount(amount);
            orderInfo.setCreateDate(new Date(order.getBigDecimal("timestamp").longValue()));
            orderInfo.setCoinPlatId(coinPlatId);
            orderInfo.setSymbol(symbol);
            if ("buy".equals(order.getString("side"))) {
                orderInfo.setType(1);
            } else if ("sell".equals(order.getString("side"))) {
                orderInfo.setType(2);
            }
            orderInfo.setSymbol(order.getString("symbol"));
            orderInfo.setPrice(order.getBigDecimal("price"));
            BigDecimal dealAmount = order.getBigDecimal("executed_amount");
            orderInfo.setDealAmount(dealAmount);
            if(order.getBoolean("is_cancelled")){
            	orderInfo.setStatus(-1);
            } else if(order.getBoolean("is_live")){
            	orderInfo.setStatus(0);
            } else if (amount.compareTo(dealAmount) == 0){
            	orderInfo.setStatus(2);
            }
            
            orderInfo.setOrderId(order.getString("id"));
            orderList.add(orderInfo);
        }
        return orderList;
    }

    public AppBack cancelOrder(String apiKey,
                               String secret,
                               String orderId,
                               String symbol) {
        String url = "https://api.bitfinex.com/v1/order/cancel";

        JSONObject payload = new JSONObject();
        payload.put("request", "/v1/order/cancel");
        payload.put("nonce", System.currentTimeMillis() + "");
        payload.put("order_id", Long.parseLong(orderId));


        String payload_base64 = Base64Util.encode(payload.toJSONString().getBytes());
        String sign = createSignature(payload_base64, secret);
        Map<String, String> header = new HashMap<>();
        header.put("X-BFX-APIKEY", apiKey);
        header.put("X-BFX-PAYLOAD", payload_base64);
        header.put("X-BFX-SIGNATURE", sign);

        String r = HttpUtil.post(url, new HashMap<>(), header);
        logger.info("cancelOrder " + r);
        JSONObject apiBack = JSONObject.parseObject(r);
        if (apiBack.getString("message") != null) {
            return new AppBack(-1, apiBack.getString("message"));
        }
        return new AppBack(apiBack);
    }


    public static final java.lang.String HMAC_SHA256_ALGORITHM = "HmacSHA384";
    public static final java.lang.String UNICODE_CODE = "UTF-8";

    private static String buildQueryString(Map<String, String> args) {
        StringBuilder result = new StringBuilder();
        for (String hashKey : args.keySet()) {
            if (result.length() > 0) result.append('&');
            try {
                result.append(URLEncoder.encode(hashKey, "UTF-8"))
                        .append("=").append(URLEncoder.encode(args.get(hashKey), "UTF-8"));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return result.toString();
    }

    private static String createSignature(String paramData, String plainSecretKey) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(plainSecretKey.getBytes(UNICODE_CODE), HMAC_SHA256_ALGORITHM);
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            mac.init(secretKey);
            byte[] hmacData = mac.doFinal(paramData.getBytes(UNICODE_CODE));
            return byteArrayToHexString(hmacData);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String byteArrayToHexString(byte[] bytes) {
        final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

}
