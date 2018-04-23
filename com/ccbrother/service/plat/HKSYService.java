package com.hykj.ccbrother.service.plat;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hykj.ccbrother.apimodel.OrderInfo;
import com.hykj.ccbrother.apimodel.UserInfo;
import com.hykj.ccbrother.base.AppBack;
import com.hykj.ccbrother.model.CoinPlatModel;
import com.hykj.ccbrother.utils.HttpUtil;

@Service
public class HKSYService implements PlatService {
	private static final Logger logger = LoggerFactory
			.getLogger(HKSYService.class);

	@Override
	public CoinPlatModel getTicker(CoinPlatModel coinPlatModel) {
		String symbol = coinPlatModel.getSymbol();//symbol是xxx-xxx格式，前者是要查的，后者是要支付的
		String url = "http://openapi.hksy.com/app/coinMarket/v1/selectCoinMarketbyCoinName";
		Map map = new HashMap<>();
		map.put("coinName", symbol.split("_")[0]);//设置要查询的币种
		map.put("payCoinName", symbol.split("_")[1]);//设置要支付的币种（价格单位币种）
		String r = HttpUtil.get(url, map);
		logger.debug(r);
		CoinPlatModel newCoinPlat = new CoinPlatModel();
		newCoinPlat.setId(coinPlatModel.getId());
		JSONObject apiBack = JSON.parseObject(r);
		newCoinPlat.setTradingTime(new Date());
		JSONObject model = apiBack.getJSONObject("model");
		newCoinPlat.setBuy(model.getBigDecimal("buyprice"));
		newCoinPlat.setHigh(model.getBigDecimal("highprice"));
		newCoinPlat.setLast(model.getBigDecimal("newclinchtype"));
		newCoinPlat.setSell(model.getBigDecimal("sellprice"));
		newCoinPlat.setVol(model.getBigDecimal("count24"));
		newCoinPlat.setLow(model.getBigDecimal("lowprice"));
		newCoinPlat.setIncrease(model.getBigDecimal("range24"));
		return newCoinPlat;
	}

	@Override
	public List<CoinPlatModel> getAllTicker(List<CoinPlatModel> list) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AppBack trade(String apiKey, String secret, String symbol, int type,
			BigDecimal price, BigDecimal amount) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UserInfo getUserInfo(String apiKey, String secret, int platId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<OrderInfo> getOrderInfo(String apiKey, String secret,
			Integer coinPlatId, String symbol) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AppBack cancelOrder(String apiKey, String secret, String orderId,
			String symbol) {
		// TODO Auto-generated method stub
		return null;
	}

}
