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

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class BinanceService implements PlatService {

	private static final Logger logger = LoggerFactory
			.getLogger(BinanceService.class);

	@Override
	public CoinPlatModel getTicker(CoinPlatModel coinPlatModel) {

		String url = "https://api.binance.com/api/v1/ticker/24hr?symbol=";
		String symbol = coinPlatModel.getSymbol();
		String r = HttpUtil.get(url + symbol, null);
		JSONObject apiBack = JSON.parseObject(r);
		logger.debug(r);

		CoinPlatModel newCoinPlat = new CoinPlatModel();
		newCoinPlat.setId(coinPlatModel.getId());
		newCoinPlat.setTradingTime(new Date());
		newCoinPlat.setBuy(apiBack.getBigDecimal("bidPrice"));
		newCoinPlat.setSell(apiBack.getBigDecimal("askPrice"));
		newCoinPlat.setLast(apiBack.getBigDecimal("lastPrice"));
		newCoinPlat.setHigh(apiBack.getBigDecimal("highPrice"));
		newCoinPlat.setLow(apiBack.getBigDecimal("lowPrice"));
		newCoinPlat.setVol(apiBack.getBigDecimal("volume"));
		newCoinPlat.setIncrease(apiBack.getBigDecimal("priceChangePercent"));
		return newCoinPlat;
	}

	@Override
	public List<CoinPlatModel> getAllTicker(List<CoinPlatModel> list) {
		String url = "https://api.binance.com/api/v1/ticker/allBookTickers";
		String r = HttpUtil.get(url, null);
		logger.info(r);
		JSONArray result = JSON.parseArray(r);
		List newList = new ArrayList<CoinPlatModel>();
		Map<String, CoinPlatModel> tempMap = new HashMap<>();

		for (int i = 0; i < result.size(); i++) {
			JSONObject coinPlatJson = result.getJSONObject(i);
			CoinPlatModel newCoinPlat = new CoinPlatModel();
			newCoinPlat.setBuy(coinPlatJson.getBigDecimal("bidPrice"));
			newCoinPlat.setSell(coinPlatJson.getBigDecimal("askPrice"));
			newCoinPlat.setLast(coinPlatJson.getBigDecimal("bidPrice"));
			newCoinPlat.setVol(coinPlatJson.getBigDecimal("bidQty").add(
					coinPlatJson.getBigDecimal("askQty")));
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

		String url = "https://api.binance.com/api/v3/order";
		Map map = new HashMap();
		map.put("symbol", symbol);
		switch (type) {
		case 1:
			map.put("side", "BUY");
			break;
		case 2:
			map.put("side", "SELL");
			break;
		}
		map.put("type", "LIMIT");
		map.put("timeInForce", "GTC");
		map.put("quantity", amount);// 数量
		map.put("price", price);
		map.put("timestamp", System.currentTimeMillis());
		map.put("recvWindow", 6000000L);
		String params = HttpUtil.sortMap(map);
		String signature = hmacSha256Signature(secret, params);
		params += "&signature=" + signature;
		Map<String, String> header = new HashMap<String, String>();
		header.put("X-MBX-APIKEY", apiKey);
		String r = HttpUtil.post(url, params, header);
		logger.info(r);
		JSONObject apiBack = JSON.parseObject(r);
		if (r.contains("code") || apiBack.size() == 0) {
			return new AppBack(-1, "交易错误 错误编码: " + apiBack.getString("msg"));
		}

		return new AppBack().add("orderId", apiBack.getString("orderId"));
	}

	@Override
	public UserInfo getUserInfo(String apiKey, String secret, int platId) {

		String url = "https://api.binance.com/api/v3/account?";
		String s = "timestamp=" + (System.currentTimeMillis() -1000);//用的日本的服务器，所以时间要减少1秒
		String signature = hmacSha256Signature(secret, s); 
		s += "&signature=" + signature;
		Map<String, String> header = new HashMap<String, String>();
		header.put("X-MBX-APIKEY", apiKey);
		String r = HttpUtil.get(url + s, null, header);
		logger.info("获取结果 " + r);
		
		UserInfo userInfo = new UserInfo();
		JSONObject apiBack = JSON.parseObject(r);
		if (r.contains("code")) { // 修改逻辑判断，返回JSON为错误时，会获取失败
			return userInfo;
		}
		JSONArray orders = apiBack.getJSONArray("balances");
		for (int i = 0; i < orders.size(); i++) {
			JSONObject order = orders.getJSONObject(i);
			if (order.getBigDecimal("free").compareTo(BigDecimal.ZERO) > 0) {
				CoinInfo coinInfo = new CoinInfo();
				coinInfo.setName(order.getString("asset"));
				coinInfo.setAmount(order.getBigDecimal("free"));
				userInfo.getFreeCoinList().add(coinInfo);
			}
			if (order.getBigDecimal("locked").compareTo(BigDecimal.ZERO) > 0) {
				CoinInfo coinInfo = new CoinInfo();
				coinInfo.setName(order.getString("asset"));
				coinInfo.setAmount(order.getBigDecimal("free"));
				userInfo.getFreezedCoinList().add(coinInfo);
			}
		}
		userInfo.setPlatId(platId);
		return userInfo;
	}

	@Override
	public List<OrderInfo> getOrderInfo(String apiKey, String secret,
			Integer coinPlatId, String symbol) {
		String url = "https://api.binance.com/api/v3/allOrders?";
		String s = "symbol=" + symbol + "&timestamp="
				+ System.currentTimeMillis();
		String signature = hmacSha256Signature(secret, s);
		s += "&signature=" + signature;
		Map<String, String> header = new HashMap<String, String>();
		header.put("X-MBX-APIKEY", apiKey);
		String r = HttpUtil.get(url + s, null, header);
		logger.info(r);
		JSONArray apiBack = JSON.parseArray(r);
		if (apiBack.size() == 0) {
			return null;
		}
		List<OrderInfo> orders = new ArrayList<>();
		for (int i = 0; i < apiBack.size(); i++) {
			JSONObject order = apiBack.getJSONObject(i);
			OrderInfo orderInfo = new OrderInfo();
			orderInfo.setAmount(order.getBigDecimal("origQty"));
			orderInfo.setCreateDate(order.getDate("time"));
			orderInfo.setCoinPlatId(coinPlatId);
			orderInfo.setSymbol(order.getString("symbol"));
			if ("BUY".equals(order.getString("side"))) {
				orderInfo.setType(1);
			} else if ("SELL".equals(order.getString("side"))) {
				orderInfo.setType(2);
			}
			orderInfo.setPrice(order.getBigDecimal("price"));
			orderInfo.setDealAmount(order.getBigDecimal("executedQty"));
			orderInfo.setOrderId(order.getString("orderId"));
			if ("CANCELED".equalsIgnoreCase(order.getString("status"))) {
				orderInfo.setStatus(-1);
			} else if ("REJECTED".equalsIgnoreCase(order.getString("status"))) {
				orderInfo.setStatus(0);
			} else if ("PARTIALLY_FILLED".equalsIgnoreCase(order
					.getString("status"))) {
				orderInfo.setStatus(1);
			} else if ("FILLED".equalsIgnoreCase(order.getString("status"))) {
				orderInfo.setStatus(2);
			} else if ("PENDING_CANCEL".equalsIgnoreCase(order
					.getString("status"))) {
				orderInfo.setStatus(4);
			} else if ("NEW".equalsIgnoreCase(order.getString("status"))) {
				orderInfo.setStatus(0);
			}

			orders.add(orderInfo);
		}

		return orders;
	}

	@Override
	public AppBack cancelOrder(String apiKey, String secret, String orderId,
			String symbol) {
		String url = "https://api.binance.com/api/v3/order?";
		String s = "orderId=" + orderId + "&symbol=" + symbol + "&timestamp="
				+ System.currentTimeMillis();
		String signature = hmacSha256Signature(secret, s);
		s += "&signature=" + signature;
		Map<String, String> header = new HashMap<String, String>();
		header.put("X-MBX-APIKEY", apiKey);
		String r = HttpUtil.get(url + s, null, header);
		logger.debug("返回结果 " + r);
		JSONObject apiBack = JSON.parseObject(r);
		if (r.contains("code")) {
			return new AppBack(-1, "错误编码" + apiBack.getString("msg"));
		}
		return new AppBack();
	}

	/**
	 * hmacSha256加密
	 * 
	 * @param secretKey密匙
	 * @param requestParams要加密的参数
	 * @return
	 */
	private static String hmacSha256Signature(final String secretKey,
			final String requestParams) {
		try {
			final Charset asciiCs = Charset.forName("US-ASCII");
			final Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
			final SecretKeySpec secret_key = new javax.crypto.spec.SecretKeySpec(
					asciiCs.encode(secretKey).array(), "HmacSHA256");
			sha256_HMAC.init(secret_key);
			final byte[] mac_data = sha256_HMAC.doFinal(asciiCs.encode(
					requestParams).array());
			String result = "";
			for (final byte element : mac_data) {
				result += Integer.toString((element & 0xff) + 0x100, 16)
						.substring(1);
			}
			return result;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		}
		return "";
	}
}
