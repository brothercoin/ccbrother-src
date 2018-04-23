package com.hykj.ccbrother.service.plat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hykj.ccbrother.apimodel.CoinInfo;
import com.hykj.ccbrother.apimodel.OrderInfo;
import com.hykj.ccbrother.apimodel.UserInfo;
import com.hykj.ccbrother.base.AppBack;
import com.hykj.ccbrother.base.MsgException;
import com.hykj.ccbrother.model.CoinPlatModel;
import com.hykj.ccbrother.utils.Encrypt;
import com.hykj.ccbrother.utils.HttpUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import springfox.documentation.spring.web.json.Json;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 1.单个IP限制每分钟1000次访问，超过1000次将被锁定1小时，一小时后自动解锁。
 * 2.每个市场限制每秒钟10次访问，一秒钟内10次以上的请求，将会视作无效 网站有时候可能会不能连上，有时候需要代理才可以
 * 
 * @author 封君
 *
 */
@Service
public class ExxService implements PlatService {

	private static final Logger logger = LoggerFactory
			.getLogger(ExxService.class);

	@Override
	public CoinPlatModel getTicker(CoinPlatModel coinPlatModel) {

		String symbol = coinPlatModel.getSymbol();
		String url = "https://api.exx.com/data/v1/ticker?currency=";
		String r = HttpUtil.get(url + symbol, null);
		logger.debug(r);
		JSONObject apiBack = JSON.parseObject(r);
		CoinPlatModel newCoinPlat = new CoinPlatModel();
		newCoinPlat.setId(coinPlatModel.getId());
		newCoinPlat.setTradingTime(apiBack.getDate("date"));
		JSONObject ticker = apiBack.getJSONObject("ticker");
		newCoinPlat.setVol(ticker.getBigDecimal("vol"));
		newCoinPlat.setLast(ticker.getBigDecimal("last"));
		newCoinPlat.setSell(ticker.getBigDecimal("sell"));
		newCoinPlat.setBuy(ticker.getBigDecimal("buy"));
		newCoinPlat.setIncrease(ticker.getBigDecimal("riseRate"));
		newCoinPlat.setHigh(ticker.getBigDecimal("high"));
		newCoinPlat.setLow(ticker.getBigDecimal("low"));
		return newCoinPlat;
	}

	@Override
	public List<CoinPlatModel> getAllTicker(List<CoinPlatModel> list) {
		String url = "https://api.exx.com/data/v1/tickers";
		String r = HttpUtil.get(url, null);
		logger.info(r);
		JSONObject apiBack = JSON.parseObject(r);
		List newList = new ArrayList<CoinPlatModel>();
		for (int i = 0; i < list.size(); i++) {

			CoinPlatModel coinPlatModel = new CoinPlatModel();

			String symbol = list.get(i).getSymbol();
			Integer id = list.get(i).getId();
			JSONObject webModel = apiBack.getJSONObject(symbol);
			if (webModel == null) {
				continue;
			}

			coinPlatModel.setId(id);
			coinPlatModel.setTradingTime(new Date());
			coinPlatModel.setLow(webModel.getBigDecimal("low"));
			coinPlatModel.setBuy(webModel.getBigDecimal("buy"));
			coinPlatModel.setHigh(webModel.getBigDecimal("high"));
			coinPlatModel.setLast(webModel.getBigDecimal("last"));
			coinPlatModel.setSell(webModel.getBigDecimal("sell"));
			coinPlatModel.setVol(webModel.getBigDecimal("vol"));
			newList.add(coinPlatModel);
		}

		return newList;
	}

	@Override
	public AppBack trade(String apiKey, String secret, String symbol, int type,
			BigDecimal price, BigDecimal amount) {

		String url = "https://trade.exx.com/api/order?";
		Map params = new HashMap();
		params.put("accesskey", apiKey);
		params.put("currency", symbol);
		params.put("nonce", System.currentTimeMillis() + "");
		params.put("amount", amount);
		params.put("price", price);
		switch (type) {
		case 1:
			params.put("type", "buy");
			break;
		case 2:
			params.put("type", "sell");
			break;
		}
		String s = HttpUtil.sortMap(params);
		String signature = Encrypt
				.hmacEncrypt(s, secret, "HmacSHA512", "UTF-8").toLowerCase();
		s += "&signature=" + signature;
		String r = HttpUtil.get(url + s, null);
		logger.info("trade" + r);
		JSONObject apiBack = JSON.parseObject(r);
		if (apiBack.getInteger("code") != 100) {
			return new AppBack(-1, apiBack.getString("message"));
		}
		return new AppBack().add("orderId", apiBack.getString("id"));
	}

	@Override
	public UserInfo getUserInfo(String apiKey, String secret, int platId) {
		System.out.println("1-----------");
		String url = "https://trade.exx.com/api/getBalance?";
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("accesskey", apiKey);
		params.put("nonce", System.currentTimeMillis() + "");
		String s = HttpUtil.sortMap(params);
		String signature = Encrypt
				.hmacEncrypt(s, secret, "HmacSHA512", "UTF-8").toLowerCase();
		s += "&signature=" + signature;
		System.out.println("s:" + s);
		System.out.println("2----");
		String r = HttpUtil.get(url + s, null);
		System.out.println("3---------");
		logger.info("getUserInfo " + r);
		JSONObject apiBack = JSON.parseObject(r);
		UserInfo userInfo = new UserInfo();
		if (apiBack.getString("code") != null) {
			return userInfo;
		}

		Map funds = apiBack.getJSONObject("funds");
		Iterator<Map.Entry<String, JSONObject>> it = funds.entrySet()
				.iterator();
		while (it.hasNext()) {
			Map.Entry<String, JSONObject> entry = it.next();
			CoinInfo coinInfo = new CoinInfo();
			coinInfo.setName(entry.getKey());
			coinInfo.setAmount(entry.getValue().getBigDecimal("balance"));
			if (0 != coinInfo.getAmount().compareTo(BigDecimal.ZERO)) {
				userInfo.getFreeCoinList().add(coinInfo);
			}
			BigDecimal freeze = entry.getValue().getBigDecimal("freeze");
			if(freeze.compareTo(BigDecimal.ZERO) > 0){
				coinInfo = new CoinInfo();
				coinInfo.setName(entry.getKey());
				coinInfo.setAmount(freeze);
				userInfo.getFreezedCoinList().add(coinInfo);
			}
		}
		userInfo.setPlatId(platId);
		System.out.println(JSON.toJSONString(userInfo));
		return userInfo;
	}

	@Override
	public List<OrderInfo> getOrderInfo(String apiKey, String secret,
			Integer coinPlatId, String symbol) {

		String url = "https://trade.exx.com/api/getOpenOrders?";
		Map params = new HashMap();
		params.put("accesskey", apiKey);
		params.put("currency", symbol);
		params.put("nonce", System.currentTimeMillis() + "");
		params.put("pageIndex", "1");// 分页，默认第一页，返回十条
		params.put("type", "buy");
		String s1 = HttpUtil.sortMap(params);
		String signature1 = Encrypt.hmacEncrypt(s1, secret, "HmacSHA512",
				"UTF-8").toLowerCase();
		s1 += "&signature=" + signature1;
		String r1 = HttpUtil.get(url + s1, null);
		logger.info("getOrderInfoBuy " + r1);
		
		params.remove("type");
		params.put("type", "sell");
		String s2 = HttpUtil.sortMap(params);
		String signature2 = Encrypt.hmacEncrypt(s2, secret, "HmacSHA512",
				"UTF-8").toLowerCase();
		s2 += "&signature=" + signature2;
		String r2 = HttpUtil.get(url + s2, null);
		logger.info("getOrderInfoSell " + r2);

		List<OrderInfo> orderList = new ArrayList();
		if (s1.contains("code") && s2.contains("code")) {//如果有记录返回的是array,没记录返回的时候JSONObj包含code等信息
			return orderList;
		}
		
		JSONArray apiBack1 = null;
		JSONArray apiBack2 = null;
		if(!r1.contains("code")){
			apiBack1 = JSON.parseArray(r1);
			ergodicArray(apiBack1, orderList, 1, symbol, coinPlatId);
		}
		if(!r2.contains("code")){
			apiBack2 = JSON.parseArray(r2);
			ergodicArray(apiBack2, orderList, 2, symbol, coinPlatId);
		}

		return orderList;
	}

	@Override
	public AppBack cancelOrder(String apiKey, String secret, String orderId,
			String symbol) {

		String url = "https://trade.exx.com/api/cancel?";
		Map params = new HashMap();
		params.put("accesskey", apiKey);
		params.put("currency", symbol);
		params.put("nonce", System.currentTimeMillis() + "");
		params.put("id", orderId);
		String s = HttpUtil.sortMap(params);
		String signature = Encrypt
				.hmacEncrypt(s, secret, "HmacSHA512", "UTF-8").toLowerCase();
		s += "&signature=" + signature;
		String r = HttpUtil.get(url + s, null);
		logger.info("cancelOrder:" + r);
		JSONObject apiBack = JSON.parseObject(r);
		if (apiBack.getInteger("code") != 100) {
			return new AppBack(-1, apiBack.getString("message"));
		}
		return new AppBack();
	}

	/**
	 * exx的Buy和sell是分开请求的，需要遍历两次
	 * 
	 * @param apiBack
	 * @param orderList
	 * @param type
	 * @return
	 */
	private List<OrderInfo> ergodicArray(JSONArray apiBack,
			List<OrderInfo> orderList, int type, String symbol,
			Integer coinPlatId) {
		for (int i = 0; i < apiBack.size(); i++) {
			JSONObject order = apiBack.getJSONObject(i);
			OrderInfo orderInfo = new OrderInfo();
			orderInfo.setAmount(order.getBigDecimal("total_amount"));
			orderInfo.setDealAmount(order.getBigDecimal("trade_amount"));
			orderInfo.setPrice(order.getBigDecimal("price"));
			orderInfo.setCreateDate(order.getDate("trade_date"));
			orderInfo.setSymbol(symbol);
			orderInfo.setCoinPlatId(coinPlatId);
			orderInfo.setOrderId(order.getString("id"));
			orderInfo.setType(type);
			if (0 == order.getInteger("status")) {
				orderInfo.setStatus(0);
			} else if (1 == order.getInteger("status")) {
				orderInfo.setStatus(-1);
			} else if (2 == order.getInteger("status")) {
				orderInfo.setStatus(2);
			} else if (3 == order.getInteger("status")) {
				orderInfo.setStatus(1);
			}
			orderList.add(orderInfo);
		}
		return orderList;
	}
}
