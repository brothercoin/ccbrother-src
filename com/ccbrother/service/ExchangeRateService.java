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
import com.alibaba.fastjson.JSONObject;
import com.hykj.ccbrother.base.BaseService;
import com.hykj.ccbrother.mapper.ExchangeRateMapper;
import com.hykj.ccbrother.model.CoinPlatModel;
import com.hykj.ccbrother.model.ExchangeRateModel;
import com.hykj.ccbrother.utils.HttpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * 汇率表
 *
 * @author innel
 * @email 2638086622@qq.com
 * @date 2018-01-25 09:25:35
 */
@Service
public class ExchangeRateService extends BaseService<ExchangeRateModel, ExchangeRateMapper> {

    //      1005 日元
//         1004 韩元
    public static final int usdCoinId = 1006;
    public static final int usdtCoinId = 1002;
    public static final int cnyCoinId = 1001;
    public static final int jpyCoinId = 1005;
    public static final int krwCoinId = 1004;


    private static BigDecimal cnyUsdRate;
    private static Map<Integer, BigDecimal> rateListUsd = new Hashtable<>();//本地内存储存汇率表  要求线程安全

    @Autowired
    private CoinPlatService coinPlatService;

    @Autowired
    private CoinService coinService;

    private static String baseUrl = "https://sapi.k780.com";
    private static final String APP = "finance.rate";
    private static final String FORMAT = "json";
    private static final String APPKEY = "31404";
    private static final String SIGN = "1fdc0eb4296cb9322cfe62f25857e662";

    //获取外界人民币/美元  美元/日元的汇率  每小时限制50次
    public void getNetworkRate() {
        Map conditon = new HashMap();
        conditon.put("type", 1);
        conditon.put("status", 0);
        List<ExchangeRateModel> list = getList(conditon);
        System.out.println(list.size());
        for (ExchangeRateModel exchangeRateModel : list) {
            System.out.println(coinService.getById(exchangeRateModel.getTradeCoinId()).getShortName().toUpperCase());
            String scur = coinService.getById(exchangeRateModel.getTradeCoinId()).getShortName().toUpperCase();//获取左侧货币名字
            String tcur = coinService.getById(exchangeRateModel.getBaseCoinId()).getShortName().toUpperCase();//获取右侧名字
            String reString = sendUrl(scur, tcur);
            logger.info("获取外界汇率" + reString);
            JSONObject result = JSON.parseObject(reString);
            if ("1".equals(result.getString("success"))) {
                ExchangeRateModel newEx = new ExchangeRateModel();
                newEx.setRate(result.getJSONObject("result").getBigDecimal("rate"));
                newEx.setUpdateTime(result.getJSONObject("result").getDate("update"));
                newEx.setId(exchangeRateModel.getId());
                update(newEx);
            }
        }
    }

    //发送相应的汇率对网址 
    private String sendUrl(String scur, String tcur) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("scur", scur);//原币种
        params.put("tcur", tcur);//目标币种
        params.put("appkey", APPKEY);
        params.put("sign", SIGN);
        params.put("format", FORMAT);
        params.put("app", APP);
        String result = HttpUtil.post(baseUrl, params);
        return result;
    }


    //获取基准货币对美元的价格
    public BigDecimal getRateUsd(Integer baseCoinId) {
        if (rateListUsd.size() == 0) {
            rateListUsd.put(-1, BigDecimal.ZERO);
            updateRate();
            return getRateUsd(baseCoinId);
        }

        if (baseCoinId == null) {
            return null;
        }

        if (baseCoinId == usdCoinId || baseCoinId == usdtCoinId) {
            return BigDecimal.ONE;
        }
        return rateListUsd.get(baseCoinId);
    }

    //获取基准货币对美元的价格
    public BigDecimal getRateCny(Integer baseCoinId) {
        BigDecimal rateUsd = getRateUsd(baseCoinId);
        if (rateUsd == null || cnyUsdRate == null) {
            return null;
        }
        return rateUsd.multiply(cnyUsdRate);
    }

    //更新内存基准货币汇率
    public synchronized void updateRate() {
        logger.info("updateRate");
        setCoinRate();
        logger.info("updateRate2 ");
        getNetworkRate();
        logger.info("getNetworkRate ");
        List<ExchangeRateModel> list = getListLite(new HashMap());
        for (ExchangeRateModel exchangeRateModel : list) {
            Integer baseCoinId = exchangeRateModel.getTradeCoinId();
            rateListUsd.put(baseCoinId, exchangeRateModel.getRate());
        }
        logger.info("updateRate2.5 ");
        ExchangeRateModel cny = getById(1);
        cnyUsdRate = BigDecimal.ONE.divide(cny.getRate(), 4);
        logger.info("updateRate3 " + JSON.toJSONString(cny));
    }

    public void setCoinRate() { //把货币汇率存入汇率表
        Map conditon = new HashMap();
        conditon.put("baseCoinId", usdCoinId);
        conditon.put("status", 0);
        conditon.put("type", 2);
        List<ExchangeRateModel> list = getListLite(conditon);
        for (ExchangeRateModel exchangeRateModel : list) {
            logger.info("setCoinRate " + JSON.toJSONString(list));
            CoinPlatModel coinPlat = coinPlatService.getById(exchangeRateModel.getCoinPlatId());
            if (coinPlat != null) {
                ExchangeRateModel newEx = new ExchangeRateModel();
                newEx.setRate(coinPlat.getLast());
                newEx.setId(exchangeRateModel.getId());
                update(newEx);
            }
        }
        logger.info("setCoinRate over");

    }

    //获取某个交易下某种货币价值多少美元
    public BigDecimal getUsdPrice(Integer coinId, Integer platId) {
        Map conditon = new HashMap();
        conditon.put("coinId", coinId);
        conditon.put("platId", platId);
        conditon.put("hasUsePrice", 1);
        List<CoinPlatModel> coinList = coinPlatService.getListLite(conditon);
        if (coinList.size() > 0) {
            return coinList.get(0).getUsdPrice();
        }
        conditon = new HashMap();
        conditon.put("coinId", coinId);
        conditon.put("hasUsePrice", 1);
        coinList = coinPlatService.getListLite(conditon);
        if (coinList.size() > 0) {
            return coinList.get(0).getUsdPrice();
        }
        return BigDecimal.ZERO;


    }

}