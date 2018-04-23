package com.hykj.ccbrother.service.plat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hykj.ccbrother.apimodel.CoinInfo;
import com.hykj.ccbrother.apimodel.OrderInfo;
import com.hykj.ccbrother.apimodel.UserInfo;
import com.hykj.ccbrother.base.AppBack;
import com.hykj.ccbrother.base.MsgException;
import com.hykj.ccbrother.model.CoinPlatModel;
import com.hykj.ccbrother.utils.HttpUtil;
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
public class BittrexService implements PlatService {

    private static final Logger logger = LoggerFactory.getLogger(BittrexService.class);

    @Override
    public CoinPlatModel getTicker(CoinPlatModel coinPlatModel) {
        return null;
    }

    @Override
    public List<CoinPlatModel> getAllTicker(List<CoinPlatModel> list) {
        String url = "https://bittrex.com/api/v1.1/public/getmarketsummaries";
        String r = HttpUtil.get(url, null);
        logger.debug("getAllTicker" + r);
        JSONObject apiBack = JSON.parseObject(r);
        JSONArray result = apiBack.getJSONArray("result");
        List newList = new ArrayList<CoinPlatModel>();
        Map<String, CoinPlatModel> tempMap = new HashMap<>();
        for (int i = 0; i < result.size(); i++) {

            JSONObject coinPlatJson = result.getJSONObject(i);
            CoinPlatModel newCoinPlat = new CoinPlatModel();
            newCoinPlat.setBuy(coinPlatJson.getBigDecimal("Bid"));
            newCoinPlat.setHigh(coinPlatJson.getBigDecimal("High"));
            newCoinPlat.setLow(coinPlatJson.getBigDecimal("Low"));
            newCoinPlat.setSell(coinPlatJson.getBigDecimal("Ask"));
            newCoinPlat.setVol(coinPlatJson.getBigDecimal("Volume"));
            newCoinPlat.setLast(coinPlatJson.getBigDecimal("Last"));
            newCoinPlat.setTradingTime(new Date());
            tempMap.put(coinPlatJson.getString("MarketName"), newCoinPlat);
        }
        for (int i = 0; i < list.size(); i++) {
            CoinPlatModel coinPlatModel = tempMap.get(list.get(i).getSymbol());
            if (coinPlatModel != null) {
                coinPlatModel.setId(list.get(i).getId());
                newList.add(coinPlatModel);
            }
        }
        logger.debug("newList" + JSON.toJSONString(newList));
        return newList;
    }

    @Override
    public AppBack trade(String apiKey, String secret, String symbol, int type, BigDecimal price, BigDecimal amount) {
        String url=null;
        if(type==1){
            url = "https://bittrex.com/api/v1.1/market/buylimit?apikey=" + apiKey;
        }
        if(type==2){
            url = "https://bittrex.com/api/v1.1/market/selllimit?apikey=" + apiKey;
        }
        url += "&market=" + symbol;
        url += "&quantity=" + amount;
        url += "&rate=" + price;
        String nonce = System.currentTimeMillis() + "";
        url += "&nonce=" + nonce;
        String sign = hmac_sha1(url, secret); // Hashing it
        Map<String, String> params = new HashMap<String, String>();
        params.put("apisign", sign);
        String r = HttpUtil.get(url, null, params);
        logger.info("trade " + r);

        JSONObject apiBack = JSON.parseObject(r);
        Boolean success = apiBack.getBoolean("success");
        if (!success) {
            throw new MsgException(apiBack.getString("message"));
        }

        String uuid=apiBack.getJSONObject("result").getString("uuid");

        return new AppBack().add("orderId",uuid);
    }

    @Override
    public UserInfo getUserInfo(String apiKey, String secret, int platId) {
        String url = "https://bittrex.com/api/v1.1/account/getbalances?apikey=" + apiKey;
        String nonce = System.currentTimeMillis() + "";
        url += "&nonce=" + nonce;
        String sign = hmac_sha1(url, secret); // Hashing it
        Map<String, String> params = new HashMap<String, String>();
        params.put("apisign", sign);
        logger.info(JSON.toJSONString(params));
        String r = HttpUtil.get(url, null, params);
        logger.info("getUserInfo " + r);
        UserInfo userInfo = new UserInfo();
        JSONObject apiBack = JSON.parseObject(r);
        Boolean success = apiBack.getBoolean("success");
        if (!success) {
            return userInfo;
        }
        JSONArray result = apiBack.getJSONArray("result");
        for (int i = 0; i <result.size();i++){
            CoinInfo coinInfo = new CoinInfo();
            coinInfo.setName(result.getJSONObject(i).getString("Currency"));
            BigDecimal balance=result.getJSONObject(i).getBigDecimal("Balance");
            BigDecimal available=result.getJSONObject(i).getBigDecimal("Available");
            coinInfo.setAmount(available);
            BigDecimal freezed=balance.subtract(available);

            if( available.compareTo(BigDecimal.ZERO)>0) {
                userInfo.getFreeCoinList().add(coinInfo);
            }

            if( freezed.compareTo(BigDecimal.ZERO)>0) {
                CoinInfo fCoin = new CoinInfo();
                fCoin.setName(result.getJSONObject(i).getString("Currency"));
                fCoin.setAmount(freezed);
                userInfo.getFreezedCoinList().add(fCoin);
            }
        }
        userInfo.setPlatId(platId);
        return userInfo;
    }

    @Override
    public List<OrderInfo> getOrderInfo(String apiKey, String secret, Integer coinPlatId, String symbol) {


        String url = "https://bittrex.com/api/v1.1/market/getopenorders?apikey=" + apiKey;

        url += "&market=" + symbol;
        String nonce = System.currentTimeMillis() + "";
        url += "&nonce=" + nonce;
        String sign = hmac_sha1(url, secret); // Hashing it
        Map<String, String> params = new HashMap<String, String>();
        params.put("apisign", sign);
        String r = HttpUtil.get(url, null, params);
        logger.info("getOrderInfo " + r);
        JSONObject apiBack = JSON.parseObject(r);
        Boolean success = apiBack.getBoolean("success");
        JSONArray orders = apiBack.getJSONArray("result");
        
        List orderList = new ArrayList();
        if (!success) {
            return orderList;
        }
       
        for (int i = 0; i < orders.size(); i++) {
            JSONObject order = orders.getJSONObject(i);
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setAmount(order.getBigDecimal("Quantity"));
            SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");//小写的mm表示的是分钟2017-05-12T17:17:57.437Z
            String dstr=order.getString("Opened");

            try {
                orderInfo.setCreateDate(sdf.parse(dstr));
            } catch (ParseException e) {
                logger.error(e.getMessage(),e);
            }
            orderInfo.setCoinPlatId(coinPlatId);
            orderInfo.setSymbol(symbol);
            if ("LIMIT_BUY".equals(order.getString("OrderType"))) {
                orderInfo.setType(1);
            } else if ("LIMIT_SELL".equals(order.getString("OrderType"))) {
                orderInfo.setType(2);
            }
            orderInfo.setPrice(order.getBigDecimal("Limit"));
            BigDecimal Quantity=order.getBigDecimal("Quantity");
            BigDecimal QuantityRemaining=order.getBigDecimal("QuantityRemaining");
            orderInfo.setDealAmount(Quantity.subtract(QuantityRemaining));

            if(Quantity.compareTo(QuantityRemaining)==0){
                orderInfo.setStatus(0);
            }else if (QuantityRemaining.compareTo(BigDecimal.ZERO)==0){
                orderInfo.setStatus(2);
            }else {
                orderInfo.setStatus(1);//TODO 未考虑撤单等情况
            }
            orderInfo.setOrderId(order.getString("OrderUuid"));
            orderList.add(orderInfo);

        }
        return orderList;
    }

    //
    @Override
    public AppBack cancelOrder(String apiKey, String secret, String orderId, String symbol) {
        String url = "https://bittrex.com/api/v1.1/market/cancel?apikey=" + apiKey;

        url+="&uuid="+orderId;
        String nonce = System.currentTimeMillis() + "";
        url += "&nonce=" + nonce;
        String sign = hmac_sha1(url, secret); // Hashing it
        Map<String, String> params = new HashMap<String, String>();
        params.put("apisign", sign);
        String r = HttpUtil.get(url, null, params);
        logger.info("cancelOrder " + r);
        UserInfo userInfo = new UserInfo();
        JSONObject apiBack = JSON.parseObject(r);
        Boolean success = apiBack.getBoolean("success");
        if (!success) {
            return new AppBack(-1,apiBack.getString("message"));
        }
        return new AppBack();
    }


    private String hmac_sha1(String value, String key) {
        try {
            // Get an hmac_sha1 key from the raw key bytes
            byte[] keyBytes = key.getBytes();
            SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA512");

            // Get an hmac_sha1 Mac instance and initialize with the signing key
            Mac mac = Mac.getInstance("HmacSHA512");
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
