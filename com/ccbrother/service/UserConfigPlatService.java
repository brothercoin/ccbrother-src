package com.hykj.ccbrother.service;

import com.hykj.ccbrother.base.BaseService;
import com.hykj.ccbrother.base.MsgException;
import com.hykj.ccbrother.mapper.UserConfigPlatMapper;
import com.hykj.ccbrother.model.CoinPlatModel;
import com.hykj.ccbrother.model.UserConfigPlatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 用户对各个交易平台的配置与授权
 *
 * @author innel
 * @email 2638086622@qq.com
 * @date 2017-11-08 08:55:32
 */
@Service
public class UserConfigPlatService extends BaseService<UserConfigPlatModel, UserConfigPlatMapper> {

    @Autowired
    TradingPlatformService tradingPlatformService;

    public BigDecimal getBuyRate(UserConfigPlatModel userConfigPlatModel,
                                 int platId) {
        if (userConfigPlatModel == null || userConfigPlatModel.getBRateRatio() == null) {
            return tradingPlatformService.getById(platId).getBuyRate();
        }
        return userConfigPlatModel.getBRateRatio();
    }

    public BigDecimal getSellRate(UserConfigPlatModel userConfigPlatModel,
                                  int platId) {
        if (userConfigPlatModel == null || userConfigPlatModel.getSRateRatio() == null) {
            return tradingPlatformService.getById(platId).getSellRate();
        }
        return userConfigPlatModel.getSRateRatio();
    }

    /**
     * 获取单位 买卖货币的手续费
     *
     * @param UserConfigPlatBuyL
     * @param UserConfigPlatSellL
     * @param coinPlatBuyL
     * @param coinPlatSellL
     * @param maxAmounL
     * @return
     */
    public BigDecimal getFeePrice(UserConfigPlatModel UserConfigPlatBuyL,
                                  UserConfigPlatModel UserConfigPlatSellL,
                                  CoinPlatModel coinPlatBuyL,
                                  CoinPlatModel coinPlatSellL,
                                  BigDecimal maxAmounL) { //计算等值差价

        BigDecimal feePrice;//手续费
        //左边市场买入手续费
        feePrice= getFeePriceBuy(UserConfigPlatBuyL,coinPlatBuyL,maxAmounL);
        feePrice=feePrice.add( getFeePriceSell(UserConfigPlatSellL,coinPlatSellL,maxAmounL));
        return feePrice;
    }

    //计算每单位手续费买入
    public BigDecimal getFeePriceBuy(UserConfigPlatModel userConfigPlat,
                                  CoinPlatModel coinPlat,
                                  BigDecimal amount) { //计算等值差价
        //左边市场买入手续费
        if (userConfigPlat.getBRateType() == null) {
            BigDecimal rate = getBuyRate(null, userConfigPlat.getPlatId());
            return coinPlat.getLast().multiply(rate);
        }
        if (userConfigPlat.getBRateType() == 1) {
            return coinPlat.getLast().multiply(userConfigPlat.getBRateRatio());
        }
        if (userConfigPlat.getBRateType() == 2) {
            return userConfigPlat.getBRateFix().divide(amount, 10);
        }
        throw new MsgException("数据异常");
    }

    //计算每单位手续费卖出
    public BigDecimal getFeePriceSell(UserConfigPlatModel userConfigPlat,
                                     CoinPlatModel coinPlat,
                                     BigDecimal amount) { //计算等值差价
        //左边市场买入手续费
        if (userConfigPlat.getSRateType() == null) {
            BigDecimal rate = getSellRate(null, userConfigPlat.getPlatId());
            return coinPlat.getLast().multiply(rate);
        }
        if (userConfigPlat.getSRateType() == 1) {
            return coinPlat.getLast().multiply(userConfigPlat.getSRateRatio());
        }
        if (userConfigPlat.getSRateType() == 2) {
            return userConfigPlat.getSRateFix().divide(amount, 10);
        }
        throw new MsgException("数据异常");
    }


}