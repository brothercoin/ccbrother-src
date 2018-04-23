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
public class BitzService implements PlatService {
    private static final Logger logger = LoggerFactory.getLogger(BitzService.class);
    @Override
    public CoinPlatModel getTicker(CoinPlatModel coinPlatModel) {
        return null;
    }

    @Override
    public List<CoinPlatModel> getAllTicker(List<CoinPlatModel> list) {
        String url = "https://www.bit-z.com/api_v1/tickerall";
        String r = HttpUtil.get(url , null);
        List newList=new ArrayList<CoinPlatModel>();
        JSONObject apiBack = JSON.parseObject(r);
        JSONObject result = apiBack.getJSONObject("data");
        String symbol = null;
        Integer id = null;
        for (int i=0;i<list.size();i++) {
            CoinPlatModel coinPlatModel=new CoinPlatModel();
            symbol = list.get(i).getSymbol();
            id = list.get(i).getId();
            JSONObject coinJson = result.getJSONObject(symbol);
            coinPlatModel.setId(id);
            coinPlatModel.setBuy(coinJson.getBigDecimal("buy"));
            coinPlatModel.setHigh(coinJson.getBigDecimal("high"));
            coinPlatModel.setLow(coinJson.getBigDecimal("low"));
            coinPlatModel.setSell(coinJson.getBigDecimal("sell"));
            coinPlatModel.setVol(coinJson.getBigDecimal("vol"));
            coinPlatModel.setLast(coinJson.getBigDecimal("last"));
            coinPlatModel.setTradingTime(new Date());
            newList.add(coinPlatModel);
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
