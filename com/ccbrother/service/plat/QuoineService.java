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
import java.util.*;

@Service
public class QuoineService implements  PlatService {

    private static final Logger logger = LoggerFactory.getLogger(QuoineService.class);

    @Override
    public CoinPlatModel getTicker(CoinPlatModel coinPlatModel) {
        return null;
    }

    @Override
    public List<CoinPlatModel> getAllTicker(List<CoinPlatModel> list) {
        String url = "https://api.quoine.com/products";


        String r = HttpUtil.get(url , null);
        logger.debug("getAllTicker"+r);
        JSONArray result = JSON.parseArray(r);
        List newList=new ArrayList<CoinPlatModel>();
        Map<String,CoinPlatModel> tempMap=new HashMap<>();
        for(int i=0;i<result.size();i++) {
            JSONObject coinPlatJson=result.getJSONObject(i);
            CoinPlatModel newCoinPlat = new CoinPlatModel();
            newCoinPlat.setBuy(coinPlatJson.getBigDecimal("market_bid"));
            newCoinPlat.setHigh(coinPlatJson.getBigDecimal("high_market_ask"));
            newCoinPlat.setLow(coinPlatJson.getBigDecimal("low_market_bid"));
            newCoinPlat.setSell(coinPlatJson.getBigDecimal("market_ask"));
            newCoinPlat.setVol(coinPlatJson.getBigDecimal("volume_24h"));
            newCoinPlat.setLast(coinPlatJson.getBigDecimal("last_traded_price"));
            newCoinPlat.setTradingTime(new Date());
            tempMap.put(coinPlatJson.getString("currency_pair_code"),newCoinPlat);
        }
        for (int i=0;i<list.size();i++){
            CoinPlatModel coinPlatModel=tempMap.get(list.get(i).getSymbol());
            if(coinPlatModel!=null) {
                coinPlatModel.setId(list.get(i).getId());
                newList.add(coinPlatModel);
            }
        }
        return newList;
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
