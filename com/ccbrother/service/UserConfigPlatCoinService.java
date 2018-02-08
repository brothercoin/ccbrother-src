/*
 * Copyright 2017-2101 Innel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hykj.ccbrother.service;

import com.alibaba.fastjson.JSON;
import com.hykj.ccbrother.apimodel.CoinInfo;
import com.hykj.ccbrother.base.BaseService;
import com.hykj.ccbrother.mapper.UserConfigPlatCoinMapper;
import com.hykj.ccbrother.model.CoinModel;
import com.hykj.ccbrother.model.TradingPlatformModel;
import com.hykj.ccbrother.model.UserConfigPlatCoinModel;
import com.hykj.ccbrother.model.UserExchangeRateModel;
import javafx.application.Platform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户在各个平台下的钱，缓存，配置
 *
 * @author innel
 * @email 2638086622@qq.com
 * @date 2017-12-11 09:19:09
 */
@Service
public class UserConfigPlatCoinService extends BaseService<UserConfigPlatCoinModel, UserConfigPlatCoinMapper> {

    @Autowired
    CoinService coinService;

    @Autowired
    TradingPlatformService tradingPlatformService;

    @Autowired
    ExchangeRateService exchangeRateService;

    @Autowired
    UserExchangeRateService userExchangeRateService;


    @Override
    protected UserConfigPlatCoinModel change(UserConfigPlatCoinModel model, Map condition) {
        if (condition.get("backList")!=null){
            TradingPlatformModel tradingPlatformModel=tradingPlatformService.getById(model.getPlatId());
            if(tradingPlatformModel!=null){
                model.setPlatName(tradingPlatformModel.getName());
            }
        }
        if(condition.get("needPrice")!=null){
            BigDecimal usdPrice=exchangeRateService.getUsdPrice(model.getCoinId(),model.getPlatId());
            BigDecimal cnyPrice;
            Map params = new HashMap();
            params.put("userId", model.getUserId());
            params.put("type", 1);
            params.put("useSelf",1);
            List<UserExchangeRateModel> eList = userExchangeRateService.getList(params);
            if(eList.size()>0){
                cnyPrice=usdPrice.divide(eList.get(0).getRateUsd(),6);
            }else {
                cnyPrice=usdPrice.divide(exchangeRateService.getRateUsd(ExchangeRateService.usdCoinId));
            }
            if(model.getFreeAmount()!=null){
                model.setFreeUsdPrice(model.getFreeAmount().multiply(usdPrice));
                model.setFreeCnyPrice(model.getFreeAmount().multiply(cnyPrice));
            }else {
                model.setFreeUsdPrice(BigDecimal.ZERO);
                model.setFreeCnyPrice(BigDecimal.ZERO);
            }

            if(model.getFreezeAmount()!=null){
                model.setFreezeUsdPrice(model.getFreezeAmount().multiply(usdPrice));
                model.setFreezeCnyPrice(model.getFreezeAmount().multiply(cnyPrice));
            }else {
                model.setFreezeUsdPrice(BigDecimal.ZERO);
                model.setFreezeCnyPrice(BigDecimal.ZERO);
            }

        }
        return super.change(model, condition);
    }

    public void createAndUpdate(CoinInfo freeCoin,
                                Integer userId,
                                Integer platId,
                                int coinType) {//1 可用 0冻结
        //赋值 coin的Id TODO 算法不准确
    	Map params = new HashMap();
    	params.put("shortName", freeCoin.getName());
    	List<CoinModel> coinList = coinService.getListLite(params);
        if (coinList.size() >0) {
            freeCoin.setCoinId(coinList.get(0).getId());
        } else {
            logger.error("出现了一种不能识别的货币 " + JSON.toJSONString(freeCoin));
            //TODO 直接创建 相应交易所应该也有交易对要更新
            CoinModel coinModel=new CoinModel();
            coinModel.setShortName(freeCoin.getName());
            coinModel.setName(freeCoin.getName());
            coinService.create(coinModel);
            List<CoinModel> list = coinService.getList(params);
            //freeCoin.setCoinId(coinModel.getId());//这种方式好像无法获取id
            freeCoin.setCoinId(list.get(0).getId());
        }
        params.clear();
        params.put("userId", userId);
        params.put("platId", platId);
        params.put("coinId", freeCoin.getCoinId());
        List<UserConfigPlatCoinModel> cList = getListLite(params);
        UserConfigPlatCoinModel userConfigPlatCoinModel = new UserConfigPlatCoinModel();
        userConfigPlatCoinModel.setUpdateTime(new Date());
        if (coinType == 1) {
            userConfigPlatCoinModel.setFreeAmount(freeCoin.getAmount());
        } else {
            userConfigPlatCoinModel.setFreezeAmount(freeCoin.getAmount());
        }

        if (cList.size() == 0) {//没有用户余额记录，创建
            userConfigPlatCoinModel.setUserId(userId);
            userConfigPlatCoinModel.setPlatId(platId);
            String name = freeCoin.getName();
            userConfigPlatCoinModel.setCoinSymbol(name);
            userConfigPlatCoinModel.setCoinId(freeCoin.getCoinId());
            userConfigPlatCoinModel.setMinHold(BigDecimal.ZERO);
            userConfigPlatCoinModel.setMaxHold(new BigDecimal("9999999999999"));
            create(userConfigPlatCoinModel);
        } else {
            logger.info("config update");
            userConfigPlatCoinModel.setUpdateTime(new Date());
            userConfigPlatCoinModel.setId(cList.get(0).getId());
            update(userConfigPlatCoinModel);
        }
    }

}