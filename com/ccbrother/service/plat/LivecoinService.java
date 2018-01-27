package com.hykj.ccbrother.service.plat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hykj.ccbrother.apimodel.CoinInfo;
import com.hykj.ccbrother.apimodel.OrderInfo;
import com.hykj.ccbrother.apimodel.UserInfo;
import com.hykj.ccbrother.base.AppBack;
import com.hykj.ccbrother.model.CoinPlatModel;
import com.hykj.ccbrother.utils.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.*;

@Service
public class LivecoinService implements PlatService {

	private static final Logger logger = LoggerFactory
			.getLogger(LivecoinService.class);

	@Override
	public CoinPlatModel getTicker(CoinPlatModel coinPlatModel) {

		String url = "https://api.livecoin.net/exchange/ticker?CurrencyPair=";
		String symbol = coinPlatModel.getSymbol();
		String r = HttpUtil.get(url + symbol, null);
		logger.debug("getTicker" + r);
		JSONObject apiBack = JSON.parseObject(r);
		CoinPlatModel newCoinPlat = new CoinPlatModel();
		newCoinPlat.setId(coinPlatModel.getId());
		newCoinPlat.setTradingTime(new Date());
		newCoinPlat.setBuy(apiBack.getBigDecimal("best_bid"));
		newCoinPlat.setHigh(apiBack.getBigDecimal("high"));
		newCoinPlat.setLow(apiBack.getBigDecimal("low"));
		newCoinPlat.setSell(apiBack.getBigDecimal("best_ask"));
		newCoinPlat.setVol(apiBack.getBigDecimal("volume"));
		newCoinPlat.setLast(apiBack.getBigDecimal("last"));
		return newCoinPlat;
	}

	@Override
	public List<CoinPlatModel> getAllTicker(List<CoinPlatModel> list) {

		String url = "https://api.livecoin.net/exchange/ticker";
		String r = HttpUtil.get(url, null);
		logger.debug("getAllTicker" + r);
		JSONArray result = JSON.parseArray(r);
		List newList = new ArrayList<CoinPlatModel>();
		Map<String, CoinPlatModel> tempMap = new HashMap<>();
		for (int i = 0; i < result.size(); i++) {
			JSONObject coinPlatJson = result.getJSONObject(i);
			CoinPlatModel newCoinPlat = new CoinPlatModel();
			newCoinPlat.setBuy(coinPlatJson.getBigDecimal("best_bid"));
			newCoinPlat.setHigh(coinPlatJson.getBigDecimal("high"));
			newCoinPlat.setLow(coinPlatJson.getBigDecimal("low"));
			newCoinPlat.setSell(coinPlatJson.getBigDecimal("best_ask"));
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
		return newList;
	}

	@Override
	public AppBack trade(String apiKey, String secret, String symbol, int type,
			BigDecimal price, BigDecimal amount) {

		String url = null;
		if (type == 1) {
			url = "https://api.livecoin.net/exchange/buylimit";
		}
		if (type == 2) {
			url = "https://api.livecoin.net/exchange/selllimit";
		}

		Map<String, String> postData = new TreeMap<>();
		postData.put("currencyPair", symbol);
		postData.put("price", price.setScale(8).toString());//该交易所要求保留8位小说
		postData.put("quantity", amount.toString());
		String queryString = buildQueryString(postData);
		String signature = createSignature(queryString, secret);
		Map header = new HashMap();
		header.put("Api-Key", apiKey);
		header.put("Sign", signature);
		String r = HttpUtil.post(url, postData, header);
		logger.info(r);
		JSONObject apiBack = JSON.parseObject(r);
		Boolean success = apiBack.getBoolean("success");
		if (!success) {
			String msg = apiBack.getString("exception");
			return new AppBack(-1, msg, msg);
		}

		return new AppBack().add("orderId", apiBack.getString("orderId"));
	}

	@Override
	public UserInfo getUserInfo(String apiKey, String secret, int platId) {
		String url = "https://api.livecoin.net/payment/balances";

		Map<String, String> postData = new TreeMap<>();
		// postData.put("currency", "");
		String queryString = buildQueryString(postData);
		String signature = createSignature(queryString, secret);
		Map header = new HashMap();
		header.put("Api-Key", apiKey);
		header.put("Sign", signature);

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
			coinInfo.setAmount(coin.getBigDecimal("value"));
			coinInfo.setPlatId(platId);
			if (0 != coinInfo.getAmount().compareTo(BigDecimal.ZERO)) {
				if ("available".equals(coin.getString("type"))) {
					userInfo.getFreeCoinList().add(coinInfo);
				}

				if ("trade".equals(coin.getString("type"))) {
					userInfo.getFreezedCoinList().add(coinInfo);
				}
			}
		}
		return userInfo;
	}

	@Override
	public List<OrderInfo> getOrderInfo(String apiKey, String secret,
			Integer coinPlatId, String symbol) {
		String url = "https://api.livecoin.net/exchange/client_orders";

		Map<String, String> postData = new TreeMap<>();
		postData.put("currencyPair", symbol);
		String queryString = buildQueryString(postData);
		String signature = createSignature(queryString, secret);
		Map header = new HashMap();
		header.put("Api-Key", apiKey);
		header.put("Sign", signature);

		String r = HttpUtil.get(url, postData, header);
		logger.info("getOrderInfo " + r);
		JSONObject apiBack = JSON.parseObject(r);
		JSONArray orders = apiBack.getJSONArray("data");
		List orderList = new ArrayList();
		if (orders == null && orders.size() == 0) {
			return orderList;
		}
		for (int i = 0; i < orders.size(); i++) {
			JSONObject order = orders.getJSONObject(i);
			OrderInfo orderInfo = new OrderInfo();
			orderInfo.setAmount(order.getBigDecimal("quantity"));
			orderInfo.setCreateDate(new Date(order.getLong("issueTime")));
			orderInfo.setCoinPlatId(coinPlatId);
			orderInfo.setSymbol(symbol);
			if ("LIMIT_BUY".equals(order.getString("type"))) {
				orderInfo.setType(1);
			} else if ("LIMIT_SELL".equals(order.getString("type"))) {
				orderInfo.setType(2);
			}
			orderInfo.setSymbol(order.getString("currencyPair"));
			orderInfo.setPrice(order.getBigDecimal("price"));
			if("EXECUTED".equals(order.getString("orderStatus"))){
				orderInfo.setStatus(2);
			}else {
				orderInfo.setStatus(0);
			}

			orderInfo.setDealAmount(orderInfo.getAmount().subtract(
					order.getBigDecimal("remainingQuantity")));
			orderInfo.setOrderId(order.getString("id"));
			orderList.add(orderInfo);

		}
		return orderList;

	}

	// /exchange/cancellimit
	@Override
	public AppBack cancelOrder(String apiKey, String secret, String orderId,
			String symbol) {

		String url = "https://api.livecoin.net/exchange/cancellimit";

		Map<String, String> postData = new TreeMap<>();
		postData.put("currencyPair", symbol);
		postData.put("orderId", orderId);
		String queryString = buildQueryString(postData);
		String signature = createSignature(queryString, secret);
		Map header = new HashMap();
		header.put("Api-Key", apiKey);
		header.put("Sign", signature);
		String r = HttpUtil.post(url, postData, header);
		logger.info("cancelOrder" + r);
		JSONObject apiBack = JSON.parseObject(r);
		Boolean success = apiBack.getBoolean("cancelled");
		if (!success) {
			String msg = apiBack.getString("message");
			return new AppBack(-1, msg, msg);
		}

		Boolean cancelled = apiBack.getBoolean("cancelled");
		if (!cancelled) {
			String msg = apiBack.getString("message");
			return new AppBack(-1, msg, msg);
		}

		return new AppBack();
	}

	public static final java.lang.String HMAC_SHA256_ALGORITHM = "HmacSHA256";
	public static final java.lang.String UNICODE_CODE = "UTF-8";

	private static String buildQueryString(Map<String, String> args) {
		StringBuilder result = new StringBuilder();
		for (String hashKey : args.keySet()) {
			if (result.length() > 0)
				result.append('&');
			try {
				result.append(URLEncoder.encode(hashKey, "UTF-8")).append("=")
						.append(URLEncoder.encode(args.get(hashKey), "UTF-8"));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return result.toString();
	}

	private static String createSignature(String paramData,
			String plainSecretKey) {
		try {
			SecretKeySpec secretKey = new SecretKeySpec(
					plainSecretKey.getBytes(UNICODE_CODE),
					HMAC_SHA256_ALGORITHM);
			Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
			mac.init(secretKey);
			byte[] hmacData = mac.doFinal(paramData.getBytes(UNICODE_CODE));
			return byteArrayToHexString(hmacData).toUpperCase();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static String byteArrayToHexString(byte[] bytes) {
		final char[] hexArray = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
				'9', 'A', 'B', 'C', 'D', 'E', 'F' };
		char[] hexChars = new char[bytes.length * 2];
		int v;
		for (int j = 0; j < bytes.length; j++) {
			v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

}
