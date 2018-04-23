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
import java.util.Date;
import java.util.List;

@Service
public class CoincheckService implements  PlatService {

    private static final Logger logger = LoggerFactory.getLogger(CoincheckService.class);

    @Override
    public CoinPlatModel getTicker(CoinPlatModel coinPlatModel) {

        String symbol = coinPlatModel.getSymbol();
        String url = "https://coincheck.com/api/ticker?pair=";

        String r = HttpUtil.get(url + symbol, null);
        logger.debug(r);
        CoinPlatModel newCoinPlat = new CoinPlatModel();
        newCoinPlat.setId(coinPlatModel.getId());
        JSONObject apiBack = JSON.parseObject(r);

        newCoinPlat.setTradingTime(new Date());
        if(apiBack.getString("bid")!=null){
            newCoinPlat.setBuy(new BigDecimal(apiBack.getString("bid")));
        }
        if(apiBack.getString("last")!=null) {
            newCoinPlat.setLast(new BigDecimal(apiBack.getString("last")));
        }
        if(apiBack.getString("ask")!=null) {
            newCoinPlat.setSell(new BigDecimal(apiBack.getString("ask")));
        }
        if(apiBack.getString("volume")!=null) {
            newCoinPlat.setVol(new BigDecimal(apiBack.getString("volume")));
        }
        if(apiBack.getString("high")!=null) {
            newCoinPlat.setHigh(new BigDecimal(apiBack.getString("high")));
        }
        if(apiBack.getString("low")!=null) {
            newCoinPlat.setLow(new BigDecimal(apiBack.getString("low")));
        }
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
