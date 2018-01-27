package com.hykj.ccbrother.service.plat;

import com.hykj.ccbrother.apimodel.OrderInfo;
import com.hykj.ccbrother.apimodel.UserInfo;
import com.hykj.ccbrother.base.AppBack;
import com.hykj.ccbrother.model.CoinPlatModel;

import java.math.BigDecimal;
import java.util.List;

public interface PlatService {

    CoinPlatModel getTicker(CoinPlatModel coinPlatModel);

    List<CoinPlatModel> getAllTicker(List<CoinPlatModel> list);

    AppBack trade(String apiKey,
                  String secret,
                  String symbol,
                  int type,
                  BigDecimal price,
                  BigDecimal amount);

    UserInfo getUserInfo(String apiKey,
                         String secret,
                         int platId);

    List<OrderInfo> getOrderInfo(String apiKey,
                                 String secret,
                                 Integer coinPlatId,//选填
                                 String symbol);

     AppBack cancelOrder(String apiKey,
                               String secret,
                               String orderId,
                               String symbol);


}
