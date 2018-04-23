package com.hykj.ccbrother.service.plat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hykj.ccbrother.apimodel.OrderInfo;
import com.hykj.ccbrother.apimodel.UserInfo;
import com.hykj.ccbrother.base.AppBack;
import com.hykj.ccbrother.model.CoinPlatModel;
import com.hykj.ccbrother.utils.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class BitstampService implements  PlatService {

    private static final Logger logger = LoggerFactory.getLogger(BitstampService.class);

    @Override
    public CoinPlatModel getTicker(CoinPlatModel coinPlatModel) {

        String symbol = coinPlatModel.getSymbol();
        String url = "https://www.bitstamp.net/api/v2/ticker/";

        String r = HttpUtil.get(url + symbol, null);
        logger.debug(r);
        CoinPlatModel newCoinPlat = new CoinPlatModel();
        newCoinPlat.setId(coinPlatModel.getId());
        JSONObject apiBack = JSON.parseObject(r);

        //时间戳转化为Sting或Date
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Long time = new Long(apiBack.getString("timestamp"));
        String d = format.format(time * 1000);
        Date date = null;
        try {
            date = format.parse(d);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        newCoinPlat.setTradingTime(date);
        newCoinPlat.setBuy(new BigDecimal(apiBack.getString("bid")));
        newCoinPlat.setHigh(new BigDecimal(apiBack.getString("high")));
        newCoinPlat.setLast(new BigDecimal(apiBack.getString("last")));
        newCoinPlat.setLow(new BigDecimal(apiBack.getString("low")));
        newCoinPlat.setSell(new BigDecimal(apiBack.getString("ask")));
        newCoinPlat.setVol(new BigDecimal(apiBack.getString("volume")));
        return newCoinPlat;
    }

    @Override
    public List<CoinPlatModel> getAllTicker(List<CoinPlatModel> list) {

        return null;
    }

    @Override
    public AppBack trade(String apiKey, String secret, String symbol, int type, BigDecimal price, BigDecimal amount) {
        return null;
    }

    @Override
    public UserInfo getUserInfo(String apiKey, String secret, int platId) {
        return null;
    }

    @Override
    public List<OrderInfo> getOrderInfo(String apiKey, String secret, Integer coinPlatId, String symbol) {
        return null;
    }

    @Override
    public AppBack cancelOrder(String apiKey, String secret, String orderId, String symbol) {
        return null;
    }
}
