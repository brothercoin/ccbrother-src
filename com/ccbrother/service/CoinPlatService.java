package com.hykj.ccbrother.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.hykj.ccbrother.base.BaseService;
import com.hykj.ccbrother.config.Config;
import com.hykj.ccbrother.mapper.CoinPlatMapper;
import com.hykj.ccbrother.model.CoinPlatModel;
import com.hykj.ccbrother.model.UserExchangeRateModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 各个平台下币种交易最新行情
 *
 * @author innel
 * @email 2638086622@qq.com
 * @date 2017-11-11 14:07:01
 */
@Service
public class CoinPlatService extends BaseService<CoinPlatModel, CoinPlatMapper> {

    @Autowired
    TradingPlatformService tradingPlatformService;

    @Autowired
    UserExchangeRateService userExchangeRateService;

    @Override
    public CoinPlatModel change(CoinPlatModel model, Map condition) {
        model.setPlatName(tradingPlatformService.getById(model.getPlatId()).getName());
        return super.change(model, condition);
    }

    public PageInfo<CoinPlatModel> getCommendList(Map map, int page) {
        PageHelper.startPage(page, Config.PageSize);
        List<CoinPlatModel> models = mapper.getCommendList(map);
        PageInfo<CoinPlatModel> p = new PageInfo<>(models);
        BigDecimal d1 = getD1(map);
        if (d1 != null) {
            models = models.stream().map(model -> {
                return doUserEx(model, d1);
            }).collect(Collectors.toList());
            p.setList(models);

        }
        return p;
    }

    public List<CoinPlatModel> getCommendList(Map map) {
        List<CoinPlatModel> models = mapper.getCommendList(map);
        BigDecimal d1 = getD1(map);
        if (d1 != null) {
            models = models.stream().map(model -> doUserEx(model, d1)).collect(Collectors.toList());
        }
        return models;
    }


    /**
     * 计算自定义费率比例
     *
     * @return
     */
    private BigDecimal getD1(Map map) {
        if (map.get("userId") == null) {
            return null;
        }
        Map params = new HashMap();
        params.put("userId", map.get("userId"));
        params.put("type", 1);
        List<UserExchangeRateModel> eList = userExchangeRateService.getList(params);
        UserExchangeRateModel cnyRateModel = eList.get(0);
        BigDecimal rateCnyUsd;
        if (cnyRateModel.getUseSelf() == null || cnyRateModel.getUseSelf() == 0) {
            return null;
        }
        rateCnyUsd = cnyRateModel.getRateUsd();
        BigDecimal std = cnyRateModel.getRateUsdStd();
        BigDecimal d1 = rateCnyUsd.divide(std, 4);//比例
        return d1;
    }

    /**
     * 人民币价格再计算
     * @param model
     * @param d1
     * @return
     */
    private CoinPlatModel doUserEx(CoinPlatModel model, BigDecimal d1) {
        if (model.getCnyPrice() != null) {
            model.setCnyPrice(d1.multiply(model.getCnyPrice()));
        }

        if (model.getSellPriceCny() != null) {
            model.setSellPriceCny(d1.multiply(model.getSellPriceCny()));
        }

        if (model.getBuyPriceCny() != null) {
            model.setBuyPriceCny(d1.multiply(model.getBuyPriceCny()));
        }
        return model;
    }

    public void nextDay() {
        mapper.nextDay();
    }


}