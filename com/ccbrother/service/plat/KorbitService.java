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
public class KorbitService implements  PlatService {

    private static final Logger logger = LoggerFactory.getLogger(KorbitService.class);

    @Override
    public CoinPlatModel getTicker(CoinPlatModel coinPlatModel) {

        String symbol = coinPlatModel.getSymbol();
        String url = "https://api.korbit.co.kr/v1/ticker/detailed?currency_pair=";

        String r = HttpUtil.get(url + symbol, null);
        logger.debug(r);
        CoinPlatModel newCoinPlat = new CoinPlatModel();
        newCoinPlat.setId(coinPlatModel.getId());
        JSONObject apiBack = JSON.parseObject(r);

        newCoinPlat.setTradingTime(new Date(apiBack.getLong("timestamp")));
        newCoinPlat.setBuy(new BigDecimal(apiBack.getString("bid")));
        newCoinPlat.setLast(new BigDecimal(apiBack.getString("last")));
        newCoinPlat.setSell(new BigDecimal(apiBack.getString("ask")));
        newCoinPlat.setVol(new BigDecimal(apiBack.getString("volume")));
        newCoinPlat.setHigh(new BigDecimal(apiBack.getString("high")));
        newCoinPlat.setLow(new BigDecimal(apiBack.getString("low")));
        newCoinPlat.setIncrease(new BigDecimal(apiBack.getString("changePercent")));
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
