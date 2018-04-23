package com.hykj.ccbrother.service.plat;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hykj.ccbrother.apimodel.CoinInfo;
import com.hykj.ccbrother.apimodel.OrderInfo;
import com.hykj.ccbrother.apimodel.UserInfo;
import com.hykj.ccbrother.base.AppBack;
import com.hykj.ccbrother.model.CoinPlatModel;
import com.hykj.ccbrother.utils.Encrypt;
import com.hykj.ccbrother.utils.HttpUtil;

/**
 * 取消订单有Bug,交易所给出的api无论订单号给是什么，无论是买还是卖，都会返回成功
 * 
 * @author 封君
 *
 */
@Service
public class KucoinService implements PlatService {
	private static final Logger logger = LoggerFactory
			.getLogger(KucoinService.class);

	@Override
	public CoinPlatModel getTicker(CoinPlatModel coinPlatModel) {
		String url = "https://api.kucoin.com/v1/open/tick?symbol=";
		String symbol = coinPlatModel.getSymbol();
		String r = HttpUtil.get(url + symbol, null);
		logger.info("getTicker" + r);
		JSONObject apiBack = JSONArray.parseObject(r).getJSONObject("data");
		CoinPlatModel newCoinPlat = new CoinPlatModel();
		newCoinPlat.setBuy(apiBack.getBigDecimal("buy"));
		newCoinPlat.setHigh(apiBack.getBigDecimal("high"));
		newCoinPlat.setLow(apiBack.getBigDecimal("low"));
		newCoinPlat.setSell(apiBack.getBigDecimal("sell"));
		newCoinPlat.setVol(apiBack.getBigDecimal("vol"));
		newCoinPlat.setLast(apiBack.getBigDecimal("lastDealPrice"));
		newCoinPlat.setTradingTime(apiBack.getDate("datetime"));
		newCoinPlat.setIncrease(apiBack.getBigDecimal("changeRate"));
		newCoinPlat.setSymbol(apiBack.getString("symbol"));
		return newCoinPlat;
	}

	@Override
	public List<CoinPlatModel> getAllTicker(List<CoinPlatModel> list) {

		String url = "https://api.kucoin.com/v1/open/tick";
		String r = HttpUtil.get(url, null);
		logger.info("getAllTicker" + r);
		JSONObject apiBack = JSONArray.parseObject(r);
		JSONArray result = apiBack.getJSONArray("data");
		List newList = new ArrayList<CoinPlatModel>();
		Map<String, CoinPlatModel> tempMap = new HashMap<>();
		Date timestamp = apiBack.getDate("timestamp");
		logger.info("timestamp " + timestamp);
		for (int i = 0; i < result.size(); i++) {
			JSONObject coinPlatJson = result.getJSONObject(i);
			CoinPlatModel newCoinPlat = new CoinPlatModel();
			newCoinPlat.setBuy(coinPlatJson.getBigDecimal("buy"));
			newCoinPlat.setHigh(coinPlatJson.getBigDecimal("high"));
			newCoinPlat.setLow(coinPlatJson.getBigDecimal("low"));
			newCoinPlat.setSell(coinPlatJson.getBigDecimal("sell"));
			newCoinPlat.setVol(coinPlatJson.getBigDecimal("vol"));
			newCoinPlat.setLast(coinPlatJson.getBigDecimal("lastDealPrice"));
			newCoinPlat.setTradingTime(timestamp);
			newCoinPlat.setIncrease(coinPlatJson.getBigDecimal("changeRate"));
			newCoinPlat.setSymbol(coinPlatJson.getString("symbol"));
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

		String url = "https://api.kucoin.com";
		String endpoint = "/v1/order";
		Long nonce = System.currentTimeMillis();
		Map params = new HashMap();
		params.put("symbol", symbol);
		params.put("price", price + "");
		params.put("amount", amount + "");
		switch (type) {
		case 1:
			params.put("type", "BUY");
			break;
		case 2:
			params.put("type", "SELL");
			break;
		}
		String s = HttpUtil.sortMap(params);
		String strForSign = endpoint + "/" + nonce + "/" + s;
		String signatureStr = null;
		try {
			signatureStr = Base64.getEncoder().encodeToString(
					strForSign.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String signature = Encrypt.hmacEncrypt(signatureStr, secret,
				"HmacSHA256", "UTF-8").toLowerCase();
		Map header = getHeader(apiKey, signature, nonce);
		String r = HttpUtil.post(url + endpoint, params, header);
		logger.info("trade" + r);

		JSONObject apiBack = JSON.parseObject(r);
		if (!"OK".equals(apiBack.getString("code"))) {
			return new AppBack(-1, "交易错误: " + apiBack.getString("msg"));
		}
		return new AppBack().add("orderId", apiBack.getString("orderOid"));
	}

	@Override
	public UserInfo getUserInfo(String apiKey, String secret, int platId) {

		String url = "https://api.kucoin.com";
		String endpoint = "/v1/account/balances";
		Long nonce = System.currentTimeMillis();

		Map params = new HashMap();
		params.put("limit", 20);// 返回最近
		params.put("page", 1);
		String s = HttpUtil.sortMap(params);
		String strForSign = endpoint + "/" + nonce + "/" + s;
		String signatureStr = null;
		try {
			signatureStr = Base64.getEncoder().encodeToString(
					strForSign.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String signature = Encrypt.hmacEncrypt(signatureStr, secret,
				"HmacSHA256", "UTF-8").toLowerCase();
		Map header = getHeader(apiKey, signature, nonce);
		String r = HttpUtil.get(url + endpoint, params, header);
		logger.info("getUserInfo " + r);
		UserInfo userInfo = new UserInfo();
		JSONObject apiBack = JSON.parseObject(r);
		if (!"OK".equals(apiBack.getString("code"))) {
			return userInfo;
		}
		JSONArray result = apiBack.getJSONObject("data").getJSONArray("datas");
		for (int i = 0; i < result.size(); i++) {
			BigDecimal balance = result.getJSONObject(i).getBigDecimal(
					"balance");
			BigDecimal freezeBalance = result.getJSONObject(i).getBigDecimal(
					"freezeBalance");
			if (balance.compareTo(BigDecimal.ZERO) > 0) {
				CoinInfo coinInfo = new CoinInfo();
				coinInfo.setName(result.getJSONObject(i).getString("coinType"));
				coinInfo.setAmount(balance);
				userInfo.getFreeCoinList().add(coinInfo);
			}
			if (freezeBalance.compareTo(BigDecimal.ZERO) > 0) {
				CoinInfo coinInfo = new CoinInfo();
				coinInfo.setName(result.getJSONObject(i).getString("coinType"));
				coinInfo.setAmount(freezeBalance);
				userInfo.getFreezedCoinList().add(coinInfo);
			}
		}
		userInfo.setPlatId(platId);
		return userInfo;
	}

	@Override
	public List<OrderInfo> getOrderInfo(String apiKey, String secret,
			Integer coinPlatId, String symbol) {

		String url = "https://api.kucoin.com";
		String endpoint = "/v1/order/active";
		Long nonce = System.currentTimeMillis();
		String s = "symbol=" + symbol;
		String strForSign  = endpoint + "/" + nonce + "/" + s;
		String signatureStr = null;
		try {
			signatureStr = Base64.getEncoder().encodeToString(strForSign.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String signature = Encrypt.hmacEncrypt(signatureStr, secret, "HmacSHA256", "UTF-8").toLowerCase();
		Map header = getHeader(apiKey, signature ,nonce);
		String r = HttpUtil.get(url + endpoint + "?" + s , null, header);
		logger.info(r);
		
		JSONObject apiBack = JSON.parseObject(r);
		List orderList = new ArrayList();
		if(!"OK".equals(apiBack.getString("code"))){
			return orderList;
		}
		JSONArray buyOrders = apiBack.getJSONObject("data").getJSONArray("BUY");
		JSONArray sellOrders = apiBack.getJSONObject("data").getJSONArray("SELL");
		if(buyOrders.size() > 0){
			for (int i = 0; i < buyOrders.size(); i++) {
				JSONArray order = buyOrders.getJSONArray(i);
				OrderInfo orderInfo = new OrderInfo();
				orderInfo.setCoinPlatId(coinPlatId);
				orderInfo.setSymbol(symbol);
				orderInfo.setCreateDate(order.getDate(0));
				orderInfo.setType(1);
				orderInfo.setPrice(order.getBigDecimal(2));
				orderInfo.setAmount(order.getBigDecimal(3));
				orderInfo.setDealAmount(order.getBigDecimal(4));
				orderInfo.setOrderId(order.getString(5));
				orderInfo.setStatus(1);
				orderList.add(orderInfo);
			}
		}
		if(sellOrders.size() > 0){
			for (int i = 0; i < sellOrders.size(); i++) {
				JSONArray order = sellOrders.getJSONArray(i);
				OrderInfo orderInfo = new OrderInfo();
				orderInfo.setCoinPlatId(coinPlatId);
				orderInfo.setSymbol(symbol);
				orderInfo.setCreateDate(order.getDate(0));
				orderInfo.setType(1);
				orderInfo.setPrice(order.getBigDecimal(2));
				orderInfo.setAmount(order.getBigDecimal(3));
				orderInfo.setDealAmount(order.getBigDecimal(4));
				orderInfo.setOrderId(order.getString(5));
				orderInfo.setStatus(1);
				orderList.add(orderInfo);
			}
		}
		return orderList;
	}

	@Override
	public AppBack cancelOrder(String apiKey, String secret, String orderId,
			String symbol) {
		String url = "https://api.kucoin.com";
		String endpoint = "/v1/cancel-order";

		Long nonce1 = System.currentTimeMillis();
		Map params = new HashMap();
		params.put("symbol", symbol);
		params.put("orderOid", orderId);
		params.put("type", "BUY");
		String s1 = HttpUtil.sortMap(params);// api只能查询买或者买，所以不知道穿过的类型的时候，要买卖都发送一次
		String strForSign1 = endpoint + "/" + nonce1 + "/" + s1;
		String signatureStr1 = null;
		try {
			signatureStr1 = Base64.getEncoder().encodeToString(
					strForSign1.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String signature1 = Encrypt.hmacEncrypt(signatureStr1, secret,
				"HmacSHA256", "UTF-8").toLowerCase();
		Map header1 = getHeader(apiKey, signature1, nonce1);
		String r1 = HttpUtil.post(url + endpoint, params, header1);

		Long nonce2 = System.currentTimeMillis();
		params.remove("type");
		params.put("type", "sell");
		String s2 = HttpUtil.sortMap(params);
		String strForSign2 = endpoint + "/" + nonce2 + "/" + s2;
		String signatureStr2 = null;
		try {
			signatureStr2 = Base64.getEncoder().encodeToString(
					strForSign2.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String signature2 = Encrypt.hmacEncrypt(signatureStr2, secret,
				"HmacSHA256", "UTF-8").toLowerCase();
		Map header2 = getHeader(apiKey, signature2, nonce2);
		String r2 = HttpUtil.post(url + endpoint, params, header2);

		JSONObject buyBack = JSON.parseObject(r1);
		JSONObject sellBack = JSON.parseObject(r2);

		// 无论给出是卖还是买，无论订单号多少，只要key和secret正确、签名正确，返回的就是true
		if (!"OK".equals(buyBack.getString("code")) && !"OK".equals(sellBack.getString("code"))) {
			return new AppBack(-1, "撤销买单时：" + buyBack.getString("msg")
					+ "，撤销卖单时：" + sellBack.getString("msg"));
		}
		return new AppBack();
	}

	private Map getHeader(String apiKey, String signature, Long nonce) {
		Map header = new HashMap();
		header.put("KC-API-KEY", apiKey);
		header.put("KC-API-NONCE", nonce + "");
		header.put("KC-API-SIGNATURE", signature);
		header.put("Accept-Language", "zh_CN");
		return header;
	}

}
