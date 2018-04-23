package com.hykj.ccbrother.service.plat;

import com.alibaba.fastjson.JSON;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class CoinoneService implements  PlatService {

    private static final Logger logger = LoggerFactory.getLogger(CoinoneService.class);

    @Override
    public CoinPlatModel getTicker(CoinPlatModel coinPlatModel) {
        return null;
    }

    @Override
    public List<CoinPlatModel> getAllTicker(List<CoinPlatModel> list) {
        String url = "https://api.coinone.co.kr/ticker?currency=all";
        String r = HttpUtil.get(url , null);
        logger.debug("getAllTicker"+r);
        JSONObject result = JSON.parseObject(r);
        List newList=new ArrayList<CoinPlatModel>();
        Date time=new Date(result.getLong("timestamp")*1000);
        for(int i=0;i<list.size();i++) {
            JSONObject coinPlatJson=result.getJSONObject(list.get(i).getSymbol());
            CoinPlatModel newCoinPlat = new CoinPlatModel();
            newCoinPlat.setId(list.get(i).getId());
            newCoinPlat.setHigh(coinPlatJson.getBigDecimal("high"));
            newCoinPlat.setLow(coinPlatJson.getBigDecimal("low"));
            newCoinPlat.setVol(coinPlatJson.getBigDecimal("volume"));
            newCoinPlat.setLast(coinPlatJson.getBigDecimal("last"));
            newCoinPlat.setTradingTime(time);
            newList.add(newCoinPlat);
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
