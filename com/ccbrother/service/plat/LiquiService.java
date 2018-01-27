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
public class LiquiService implements  PlatService {

    private static final Logger logger = LoggerFactory.getLogger(LiquiService.class);

    @Override
    public CoinPlatModel getTicker(CoinPlatModel coinPlatModel) {
        return null;
    }

    @Override
    public List<CoinPlatModel> getAllTicker(List<CoinPlatModel> list) {
        String url = "https://api.liqui.io/api/3/ticker/";
        StringBuilder symbol= new StringBuilder();
        for(int i=0;i<list.size();i++){
            if(i!=0){
                symbol.append("-");
            }
            symbol.append(list.get(i).getSymbol());
        }

        logger.debug(url+symbol);
        String r = HttpUtil.get(url+symbol , null);
        logger.debug("getAllTicker"+r);
        JSONObject apiBack = JSON.parseObject(r);
        List newList=new ArrayList<CoinPlatModel>();
        for (int i=0;i<list.size();i++){
            JSONObject coinPlatJson=apiBack.getJSONObject(list.get(i).getSymbol());
            CoinPlatModel newCoinPlat = new CoinPlatModel();
            newCoinPlat.setId(list.get(i).getId());
            newCoinPlat.setBuy(coinPlatJson.getBigDecimal("buy"));
            newCoinPlat.setHigh(coinPlatJson.getBigDecimal("high"));
            newCoinPlat.setLow(coinPlatJson.getBigDecimal("low"));
            newCoinPlat.setSell(coinPlatJson.getBigDecimal("sell"));
            newCoinPlat.setVol(coinPlatJson.getBigDecimal("vol"));
            newCoinPlat.setLast(coinPlatJson.getBigDecimal("last"));
            newCoinPlat.setTradingTime(new Date(coinPlatJson.getLong("updated")*1000));
            newList.add(newCoinPlat);
        }
        logger.debug("newList"+JSON.toJSONString(newList));
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
