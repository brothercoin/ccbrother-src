package com.hykj.ccbrother.service.plat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hykj.ccbrother.apimodel.CoinInfo;
import com.hykj.ccbrother.apimodel.OrderInfo;
import com.hykj.ccbrother.apimodel.UserInfo;
import com.hykj.ccbrother.base.AppBack;
import com.hykj.ccbrother.model.CoinPlatModel;
import com.hykj.ccbrother.utils.Base64Util;
import com.hykj.ccbrother.utils.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class HitbtcService implements PlatService {

	private static final Logger logger = LoggerFactory
			.getLogger(HitbtcService.class);

	@Override
	public CoinPlatModel getTicker(CoinPlatModel coinPlatModel) {
		return null;
	}

	@Override
	public List<CoinPlatModel> getAllTicker(List<CoinPlatModel> list) {
		String url = "https://api.hitbtc.com/api/2/public/ticker";
		String r = HttpUtil.get(url, null);
		logger.debug("getAllTicker" + r);
		JSONArray result = JSON.parseArray(r);
		List newList = new ArrayList<CoinPlatModel>();
		Map<String, CoinPlatModel> tempMap = new HashMap<>();
		for (int i = 0; i < result.size(); i++) {
			JSONObject coinPlatJson = result.getJSONObject(i);
			CoinPlatModel newCoinPlat = new CoinPlatModel();
			newCoinPlat.setBuy(coinPlatJson.getBigDecimal("bid"));
			newCoinPlat.setHigh(coinPlatJson.getBigDecimal("high"));
			newCoinPlat.setLow(coinPlatJson.getBigDecimal("low"));
			newCoinPlat.setSell(coinPlatJson.getBigDecimal("ask"));
			newCoinPlat.setVol(coinPlatJson.getBigDecimal("volume"));
			newCoinPlat.setLast(coinPlatJson.getBigDecimal("last"));
			newCoinPlat.setTradingTime(new Date());
			tempMap.put(coinPlatJson.getString("symbol"), newCoinPlat);
		}
		for (int i = 0; i < list.size(); i++) {
			CoinPlatModel coinPlatModel = tempMap.get(list.get(i).getSymbol());
			if (coinPlatModel != null) {
				coinPlatModel.setId(list.get(i).getId());
				newList.add(coinPlatModel);
			}
		}
		logger.debug("newList" + JSON.toJSONString(newList));
		return newList;
	}

	@Override
	public AppBack trade(String apiKey, String secret, String symbol, int type,
			BigDecimal price, BigDecimal amount) {
		String url = "https://api.hitbtc.com/api/2/order";
		Map<String, String> postData = new TreeMap<>();
		Map header = new HashMap();
		String value = apiKey + ":" + secret;
		header.put("Authorization",
				"Basic " + Base64Util.encode(value.getBytes()));
		postData.put("symbol", symbol);
		if (type == 1) {
			postData.put("side", "buy");
		}
		if (type == 2) {
			postData.put("side", "sell");
		}
		postData.put("type", "limit");
		postData.put("quantity", amount.toString());
		postData.put("price", price.toString());

		String r = HttpUtil.post(url, postData, header);

		logger.info("trade " + r);
		JSONObject apiBack = JSON.parseObject(r);
		JSONObject error = apiBack.getJSONObject("error");
		if (error != null) {
			return new AppBack(-1, error.getString("message"));
		}
		String orderId = apiBack.getString("clientOrderId");//clientOrderId可以是自己生成，也可以是服务器自动生成
		return new AppBack().add("orderId", orderId);
	}

	@Override
	public UserInfo getUserInfo(String apiKey, String secret, int platId) {

		String url = "https://api.hitbtc.com/api/2/trading/balance";
		Map<String, String> postData = new TreeMap<>();
		Map header = new HashMap();
		String value = apiKey + ":" + secret;
		header.put("Authorization",
				"Basic " + Base64Util.encode(value.getBytes()));
		String r = HttpUtil.get(url, postData, header);

		logger.info("getUserInfo " + r);
		JSONArray apiBack = JSON.parseArray(r);
		UserInfo userInfo = new UserInfo();
		if(apiBack == null && apiBack.size() == 0){
			return userInfo;
		}
		
		for (int i = 0; i < apiBack.size(); i++) {
			JSONObject coin = apiBack.getJSONObject(i);
			CoinInfo coinInfo = new CoinInfo();
			coinInfo.setName(coin.getString("currency"));
			coinInfo.setAmount(coin.getBigDecimal("available"));
			coinInfo.setPlatId(platId);
			if (0 != coinInfo.getAmount().compareTo(BigDecimal.ZERO)) {
				userInfo.getFreeCoinList().add(coinInfo);
			}

			coinInfo = new CoinInfo();
			coinInfo.setName(coin.getString("currency"));
			coinInfo.setAmount(coin.getBigDecimal("reserved"));
			coinInfo.setPlatId(platId);
			if (0 != coinInfo.getAmount().compareTo(BigDecimal.ZERO)) {
				userInfo.getFreezedCoinList().add(coinInfo);
			}

		}
		userInfo.setPlatId(platId);
		return userInfo;
	}

	@Override
	public List<OrderInfo> getOrderInfo(String apiKey, String secret,
			Integer coinPlatId, String symbol) {
		String url = "https://api.hitbtc.com/api/2/order";
		Map header = new HashMap();
		String value = apiKey + ":" + secret;
		header.put("Authorization",
				"Basic " + Base64Util.encode(value.getBytes()));

		Map<String, String> postData = new TreeMap<>();
		postData.put("symbol", symbol);

		String r = HttpUtil.get(url, postData, header);

		logger.info("getOrderInfo " + r);
		JSONArray orders = JSON.parseArray(r);
		List orderList = new ArrayList();
		if (orders == null && orders.size() == 0) {
			return orderList;
		}
		
		for (int i = 0; i < orders.size(); i++) {

			JSONObject order = orders.getJSONObject(i);

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");// 小写的mm表示的是分钟2017-05-12T17:17:57.437Z
			String dstr = order.getString("createdAt");

			OrderInfo orderInfo = new OrderInfo();
			orderInfo.setAmount(order.getBigDecimal("quantity"));
			try {
				orderInfo.setCreateDate(sdf.parse(dstr));
			} catch (ParseException e) {
				e.printStackTrace();
			}
			orderInfo.setCoinPlatId(coinPlatId);
			if ("buy".equals(order.getString("side"))) {
				orderInfo.setType(1);
			} else if ("sell".equals(order.getString("side"))) {
				orderInfo.setType(2);
			}
			orderInfo.setSymbol(order.getString("symbol"));
			orderInfo.setPrice(order.getBigDecimal("price"));
			switch (order.getString("status")) {// new, suspended,
												// partiallyFilled, filled,
												// canceled, expired
			case "new":
				orderInfo.setStatus(0);
				break;
			case "partiallyFilled":
				orderInfo.setStatus(1);
				break;

			}

			// orderInfo.setDealAmount(orderInfo.getAmount().subtract(order.getBigDecimal("remainingQuantity")));
			orderInfo.setOrderId(order.getString("clientOrderId"));
			orderList.add(orderInfo);

		}
		return orderList;
	}

	@Override
	public AppBack cancelOrder(String apiKey, String secret, String orderId,
			String symbol) {
		String url = "https://api.hitbtc.com/api/2/order/" + orderId;
		Map header = new HashMap();
		String value = apiKey + ":" + secret;
		header.put("Authorization",
				"Basic " + Base64Util.encode(value.getBytes()));

		String r = HttpUtil.delete(url, header);

		logger.info("trade " + r);
		JSONObject apiBack = JSON.parseObject(r);
		JSONObject error = apiBack.getJSONObject("error");
		if (error != null) {
			return new AppBack(-1, error.getString("message"));
		}
		return new AppBack();
	}
}
