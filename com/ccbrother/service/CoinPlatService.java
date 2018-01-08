package com.hykj.ccbrother.service;

import com.hykj.ccbrother.base.BaseService;
import com.hykj.ccbrother.mapper.CoinPlatMapper;
import com.hykj.ccbrother.model.CoinPlatModel;
import com.hykj.ccbrother.service.plat.PlatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

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

    @Override
    public CoinPlatModel change(CoinPlatModel model, Map condition) {
        model.setPlatName(tradingPlatformService.getById(model.getPlatId()).getName());
        return super.change(model, condition);
    }

    public void nextDay(){
        mapper.nextDay();
    }
}