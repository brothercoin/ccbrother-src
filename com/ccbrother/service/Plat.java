package com.hykj.ccbrother.service;

import com.hykj.ccbrother.apimodel.OrderInfo;
import com.hykj.ccbrother.apimodel.UserInfo;
import com.hykj.ccbrother.base.AppBack;
import com.hykj.ccbrother.model.CoinPlatModel;
import com.hykj.ccbrother.model.HedgingModel;
import com.hykj.ccbrother.model.UserConfigPlatCoinModel;
import org.springframework.scheduling.annotation.Async;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Future;

public interface Plat {
    /**
     * 交易
     *
     * @param userId
     * @param coinPlatId
     * @param type
     * @param price
     * @param amount
     * @return
     */
     AppBack trade(Integer userId,
                         Integer coinPlatId,
                         Integer type,
                         BigDecimal price,
                         BigDecimal amount);//1buy 2sell

    /**
     * 更新最新行情到数据库
     *
     * @param coinPlatModel
     */
    void getTicker(CoinPlatModel coinPlatModel);

    /**
     * 批量更新最新行情到数据库
     *
     * @param coinPlatModelList
     */
    void getAllTicker(List<CoinPlatModel> coinPlatModelList);


    /**
     * 获取用户资料
     *
     * @param userId
     * @param platId
     * @return
     */

    Future<UserInfo> getUserInfo(Integer userId,
                                 Integer platId);

    Future<List<OrderInfo>> getOrderInfo(Integer userId,
                                         Integer coinPlatId);

    AppBack cancelOrder(Integer userId,
                        Integer coinPlatId,
                        String orderId);


    void hedging(HedgingModel hedgingModel);

     UserConfigPlatCoinModel getUserCoin(Integer userId,
                                               Integer platId,
                                               Integer coinId,
                                               boolean autoFlash);

    void getHedgingOrder(HedgingModel hedgingModel);
}
