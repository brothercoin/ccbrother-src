package com.hykj.ccbrother.service;

import com.hykj.ccbrother.base.BaseService;
import com.hykj.ccbrother.mapper.TradingPlatformMapper;
import com.hykj.ccbrother.model.TradingPlatformModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 交易所表
 *
 * @author innel
 * @email 2638086622@qq.com
 * @date 2017-11-17 15:44:22
 */
@Service
public class TradingPlatformService extends BaseService<TradingPlatformModel, TradingPlatformMapper> {


    public  List<TradingPlatformModel> getListOverview(Map map){
        return mapper.getListOverview(map);
    }

    public  List<TradingPlatformModel> getListOverview2(Map map){
        return mapper.getListOverview2(map);
    }
}