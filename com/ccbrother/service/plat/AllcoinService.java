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
import com.hykj.ccbrother.utils.MD5;
import com.hykj.ccbrother.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
/**
 * 此交易所限制5分钟只能3000条数据，过多会被拉黑1小时
 * @author 封君
 *
 */
@Service
public class AllcoinService implements PlatService {

    private static final Logger logger = LoggerFactory.getLogger(AllcoinService.class);

    public CoinPlatModel getTicker(CoinPlatModel coinPlatModel) {

        String symbol = coinPlatModel.getSymbol();
        //String url = "https://api.allcoin.com/api/v1/ticker?symbol=";
        String url = "https://api.allcoin.com/api/v1/ticker?symbol=";

        String r = HttpUtil.get(url + symbol, null);
        logger.debug(r);
        CoinPlatModel newCoinPlat = new CoinPlatModel();
        newCoinPlat.setId(coinPlatModel.getId());

        JSONObject apiBack = JSON.parseObject(r);

        //时间戳转化为Sting或Date
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Long time = new Long(apiBack.getString("date"));
        String d = format.format(time * 1000);
        Date date = null;
        try {
            date = format.parse(d);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        newCoinPlat.setTradingTime(date);
        JSONObject ticker = apiBack.getJSONObject("ticker");
        newCoinPlat.setBuy(new BigDecimal(ticker.getString("buy")));
        newCoinPlat.setHigh(new BigDecimal(ticker.getString("high")));
        newCoinPlat.setLast(new BigDecimal(ticker.getString("last")));
        newCoinPlat.setLow(new BigDecimal(ticker.getString("low")));
        newCoinPlat.setSell(new BigDecimal(ticker.getString("sell")));
        newCoinPlat.setVol(new BigDecimal(ticker.getString("vol")));
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
        String url = "https://api.allcoin.com/api/v1/trade";
        Map params = new HashMap();
        params.put("api_key", apiKey);
        params.put("symbol", symbol);
        switch (type) {
            case 1:
                params.put("type", "buy");
                break;
            case 2:
                params.put("type", "sell");
                break;
        }

        params.put("price", price);
        params.put("amount", amount);

        String s = HttpUtil.sortMap(params);
        String s1 = s + "&secret_key=" + secret;
        String sign = MD5.sign(s1, "UTF-8").toUpperCase();
        s += "&sign=" + sign;
        Map<String, String> map = HttpUtil.getUrlParams(s);
        String r = HttpUtil.post(url, map);
        JSONObject apiBack = JSON.parseObject(r);
        boolean result = apiBack.getBoolean("result");
        String orderId=apiBack.getString("order_id");
        if (!result) {
            String error_code = apiBack.getString("error_code");
            return new AppBack(-1, "交易错误 错误编码: " + error_code);
        }

        logger.info(r);
        return new AppBack().add("orderId",orderId);
    }

    public UserInfo getUserInfo(String apiKey,
                                String secret,
                                int platId) {

        String url = "https://api.allcoin.com/api/v1/userinfo";
        Map params = new HashMap();
        params.put("api_key", apiKey);
        String s = HttpUtil.sortMap(params);
        String s1 = s + "&secret_key=" + secret;
        String sign = MD5.sign(s1, "UTF-8").toUpperCase();
        s += "&sign=" + sign;
        Map<String, String> map = HttpUtil.getUrlParams(s);
        String r = HttpUtil.post(url, map);
        logger.info("getuSerInfo:" + r);
        JSONObject apiBack = JSON.parseObject(r);
        UserInfo userInfo = new UserInfo();
        if (!apiBack.getBoolean("result")) {
            logger.debug("获取失败 " + r);
            return userInfo;
        }
        JSONObject info = apiBack.getJSONObject("info");
        JSONObject funds = info.getJSONObject("funds");
        Map free = funds.getJSONObject("free");
        Iterator<Map.Entry<String, String>> it = free.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            CoinInfo coinInfo = new CoinInfo();
            coinInfo.setName(entry.getKey());
            coinInfo.setAmount(new BigDecimal(entry.getValue()));
            coinInfo.setPlatId(platId);
            if (0 != coinInfo.getAmount().compareTo(BigDecimal.ZERO)) {
                userInfo.getFreeCoinList().add(coinInfo);
            }
        }
        Map freezed = funds.getJSONObject("freezed");
        it = freezed.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            CoinInfo coinInfo = new CoinInfo();
            coinInfo.setName(entry.getKey());
            coinInfo.setAmount(new BigDecimal(entry.getValue()));
            coinInfo.setPlatId(platId);
            if (0 != coinInfo.getAmount().compareTo(BigDecimal.ZERO)) {
                userInfo.getFreezedCoinList().add(coinInfo);
            }
        }
        userInfo.setPlatId(platId);
        return userInfo;
    }


    public List<OrderInfo> getOrderInfo(String apiKey,
                                        String secret,
                                        Integer coinPlatId,//选填
                                        String symbol) {//选填

        String url = "https://api.allcoin.com/api/v1/order_info";
        Map params = new HashMap();
        params.put("api_key", apiKey);
        if (!StringUtil.isEmptyString(symbol)) {
            params.put("symbol", symbol);
        }
        params.put("order_id", -1);
        String s = HttpUtil.sortMap(params);
        String s1 = s + "&secret_key=" + secret;
        String sign = MD5.sign(s1, "UTF-8").toUpperCase();
        s += "&sign=" + sign;
        logger.debug(s);
        Map<String, String> map = HttpUtil.getUrlParams(s);
        String r = HttpUtil.post(url, map);
        logger.debug("获取结果 " + r);
        
        JSONObject apiBack = JSON.parseObject(r);
        List orderList = new ArrayList();
        if (!apiBack.getBoolean("result")) {
            return orderList;
        }
        JSONArray orders = apiBack.getJSONArray("order");
        for (int i = 0; i < orders.size(); i++) {
            JSONObject order = orders.getJSONObject(i);
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setAmount(order.getBigDecimal("amount"));
            orderInfo.setCreateDate(new Date(order.getLong("create_data")));
            orderInfo.setCoinPlatId(coinPlatId);
            orderInfo.setSymbol(symbol);
            if ("buy".equals(order.getString("type"))) {
                orderInfo.setType(1);
            } else if ("sell".equals(order.getString("type"))) {
                orderInfo.setType(2);
            }
            orderInfo.setSymbol(order.getString("symbol"));
            orderInfo.setPrice(order.getBigDecimal("price"));
            orderInfo.setStatus(order.getInteger("status"));
            orderInfo.setDealAmount(order.getBigDecimal("deal_amount"));
            orderInfo.setOrderId(order.getString("order_id"));
            orderList.add(orderInfo);

        }
        return orderList;
    }

    public AppBack cancelOrder(String apiKey,
                               String secret,
                               String orderId,
                               String symbol) {
        String url = "https://api.allcoin.com/api/v1/cancel_order";
        Map params = new HashMap();
        params.put("api_key", apiKey);
        params.put("symbol", symbol);
        params.put("order_id", orderId);

        String s = HttpUtil.sortMap(params);
        String s1 = s + "&secret_key=" + secret;
        String sign = MD5.sign(s1, "UTF-8").toUpperCase();
        s += "&sign=" + sign;
        Map<String, String> map = HttpUtil.getUrlParams(s);
        String r = HttpUtil.post(url, map);
        JSONObject apiBack = JSON.parseObject(r);
        boolean result = apiBack.getBoolean("result");
        logger.debug("返回结果 " + r);
        if (!result) {
            String error_code = apiBack.getString("error_code");
            return new AppBack(-1, "错误 错误编码: " + error_code);
        }

        return new AppBack();
    }

}
