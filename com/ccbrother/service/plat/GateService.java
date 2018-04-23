package com.hykj.ccbrother.service.plat;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
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
import com.hykj.ccbrother.base.MsgException;
import com.hykj.ccbrother.model.CoinPlatModel;
import com.hykj.ccbrother.utils.HttpUtil;

@Service
public class GateService implements PlatService {

	private static final Logger logger = LoggerFactory
			.getLogger(GateService.class);

	@Override
	public CoinPlatModel getTicker(CoinPlatModel coinPlatModel) {

		String url = "http://data.gate.io/api2/1/ticker/"
				+ coinPlatModel.getSymbol();// 直接写路径里，不用参数？占位符
		String r = HttpUtil.get(url, null);
		logger.debug(r);
		CoinPlatModel newCoinPlat = new CoinPlatModel();
		JSONObject appBack = JSON.parseObject(r);
		if (!appBack.getBoolean("result")) {
			return null;
		}
		newCoinPlat.setId(coinPlatModel.getId());
		newCoinPlat.setTradingTime(new Date());
		newCoinPlat.setLast(appBack.getBigDecimal("last"));
		newCoinPlat.setVol(appBack.getBigDecimal("baseVolume"));
		newCoinPlat.setHigh(appBack.getBigDecimal("high24hr"));
		newCoinPlat.setBuy(appBack.getBigDecimal("highestBid"));
		newCoinPlat.setSell(appBack.getBigDecimal("lowestAsk"));
		newCoinPlat.setLow(appBack.getBigDecimal("low24hr"));
		newCoinPlat.setIncrease(appBack.getBigDecimal("percentChange"));

		return newCoinPlat;
	}

	@Override
	public List<CoinPlatModel> getAllTicker(List<CoinPlatModel> list) {
		String url = "http://data.gate.io/api2/1/tickers";
		String r = HttpUtil.get(url, null);
		logger.debug("getAllTicker" + r);
		JSONObject apiBack = JSON.parseObject(r);
		List<CoinPlatModel> newList = new ArrayList<CoinPlatModel>();
		Map<String, CoinPlatModel> tempMap = new HashMap<>();
		for (Map.Entry<String, Object> entry : apiBack.entrySet()) {
			String symbol = entry.getKey();
			JSONObject result = apiBack.getJSONObject(symbol);
			CoinPlatModel newCoinPlat = new CoinPlatModel();
			newCoinPlat.setTradingTime(new Date());
			newCoinPlat.setLast(result.getBigDecimal("last"));
			newCoinPlat.setVol(result.getBigDecimal("baseVolume"));
			newCoinPlat.setHigh(result.getBigDecimal("high24hr"));
			newCoinPlat.setBuy(result.getBigDecimal("highestBid"));
			newCoinPlat.setSell(result.getBigDecimal("lowestAsk"));
			newCoinPlat.setLow(result.getBigDecimal("low24hr"));
			newCoinPlat.setIncrease(result.getBigDecimal("percentChange"));
			tempMap.put(symbol, newCoinPlat);
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
			url = "https://api.gate.io/api2/1/private/buy";
		}
		if (type == 2) {
			url = "https://api.gate.io/api2/1/private/sell";
		}
		Map params = new HashMap();
		params.put("currencyPair", symbol);
		params.put("rate", price.toString());
		params.put("amount", amount.toString());
		String sign = sha512(params, secret);
		Map<String ,String> header = new HashMap<String ,String>();
		header.put("Content-Type", "application/x-www-form-urlencoded");
		header.put("Key", apiKey);
		header.put("Sign", sign);
		String r = HttpUtil.post(url, params, header);
		logger.info("trade " + r);
		JSONObject apiBack = JSON.parseObject(r);
		if (!"true".equalsIgnoreCase(apiBack.getString("result"))) {
			return new AppBack(-1, "下单失败:" + apiBack.getString("message"));
		}
		String orderId = apiBack.getString("orderNumber");
		return new AppBack().add("orderId", orderId);
	}

	@Override
	public UserInfo getUserInfo(String apiKey, String secret, int platId) {

		String url = "https://api.gate.io/api2/1/private/balances";
		Map<String ,String> params = new HashMap<String ,String>();
		String sign = sha512(params, secret);
		Map<String ,String> header = new HashMap<String ,String>();
		header.put("Content-Type", "application/x-www-form-urlencoded");
		header.put("Key", apiKey);
		header.put("Sign", sign);
		String r = HttpUtil.post(url, params, header);
		logger.info("getUserInfo " + r);
		JSONObject apiBack = JSON.parseObject(r);
		UserInfo userInfo = new UserInfo();
		if (!apiBack.getBoolean("result")) {
			return userInfo;
		}
		
		Map free = apiBack.getJSONObject("available");
		Iterator<Map.Entry<String, Object>> it = free.entrySet().iterator();//返回类型只能用obj接收
		while (it.hasNext()) {
			Map.Entry<String, Object> entry = it.next();
			CoinInfo coinInfo = new CoinInfo();
			coinInfo.setName(entry.getKey());
			coinInfo.setAmount(new BigDecimal(entry.getValue().toString()));
			coinInfo.setPlatId(platId);
			if (0 != coinInfo.getAmount().compareTo(BigDecimal.ZERO)) {
				userInfo.getFreeCoinList().add(coinInfo);
			}
		}
		
		Map freezed = apiBack.getJSONObject("locked");
		if(freezed == null){
			return userInfo;
		}
		it = freezed.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Object> entry = it.next();
			CoinInfo coinInfo = new CoinInfo();
			coinInfo.setName(entry.getKey());
			coinInfo.setAmount(new BigDecimal(entry.getValue().toString()));
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
		
		String url = "https://api.gate.io/api2/1/private/openOrders";
		Map<String ,String> params = new HashMap<String ,String>();
		String sign = sha512(params, secret);
		Map<String ,String> header = new HashMap<String ,String>();
		header.put("Content-Type", "application/x-www-form-urlencoded");
		header.put("Key", apiKey);
		header.put("Sign", sign);
		String r = HttpUtil.post(url, params, header);
		logger.info("getOrderInfo " + r);
		JSONObject apiBack = JSON.parseObject(r);
		List<OrderInfo> orderList = new ArrayList();
		if (!apiBack.getBoolean("result")) {
			return orderList;
		}
		JSONArray orders = apiBack.getJSONArray("orders");
		
		for (int i = 0; i < orders.size(); i++) {
			JSONObject order = orders.getJSONObject(i);
			OrderInfo orderInfo = new OrderInfo();
			orderInfo.setAmount(order.getBigDecimal("initialAmount"));
			orderInfo.setCreateDate(order.getDate("timestamp"));
			orderInfo.setCoinPlatId(coinPlatId);
			orderInfo.setSymbol(symbol);
			if ("buy".equals(order.getString("type"))) {
                orderInfo.setType(1);
            } else if ("sell".equals(order.getString("type"))) {
                orderInfo.setType(2);
            }
			orderInfo.setPrice(order.getBigDecimal("initialRate"));
			orderInfo.setOrderId(order.getString("orderNumber"));
			orderInfo.setDealAmount(order.getBigDecimal("filledAmount"));
			if("cancelled".equalsIgnoreCase(order.getString("status"))){
				orderInfo.setStatus(-1);
			}else if("done".equalsIgnoreCase(order.getString("status"))){
				orderInfo.setStatus(2);
			}
			orderList.add(orderInfo);
		}
		return orderList;
	}

	@Override
	public AppBack cancelOrder(String apiKey, String secret, String orderId,
			String symbol) {
		
		String url = "https://api.gate.io/api2/1/private/cancelOrder";
		Map<String ,String> params = new HashMap<String ,String>();
		params.put("orderNumber", orderId);
		params.put("currencyPair", symbol);
		String sign = sha512(params, secret);
		Map<String ,String> header = new HashMap<String ,String>();
		header.put("Content-Type", "application/x-www-form-urlencoded");
		header.put("Key", apiKey);
		header.put("Sign", sign);
		String r = HttpUtil.post(url, params, header);
		logger.info("cancelOrder " + r);
		JSONObject apiBack = JSON.parseObject(r);
		if (!apiBack.getBoolean("result")) {
			throw new MsgException(apiBack.getString("message"));
		}
		return new AppBack();
	}

	
	/**
	 * sha512签名
	 * 
	 * @param params
	 * @param Secret
	 * @return
	 */
	public String sha512(Map params, String secretKey) {
		Mac mac = null;
		SecretKeySpec key = null;
		String postData = "";
		List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();

		for (Iterator<Entry<String, String>> argumentIterator = params
				.entrySet().iterator(); argumentIterator.hasNext();) {
			Map.Entry<String, String> argument = argumentIterator.next();
			urlParameters.add(new BasicNameValuePair(argument.getKey()
					.toString(), argument.getValue().toString()));
			if (postData.length() > 0) {
				postData += "&";
			}
			postData += argument.getKey() + "=" + argument.getValue();

		}
		try {
			key = new SecretKeySpec(secretKey.getBytes("UTF-8"), "HmacSHA512");
		} catch (UnsupportedEncodingException uee) {
			System.err.println("Unsupported encoding exception: "
					+ uee.toString());
		}
		try {
			mac = Mac.getInstance("HmacSHA512");
		} catch (NoSuchAlgorithmException nsae) {
			System.err.println("No such algorithm exception: "
					+ nsae.toString());
		}
		try {
			mac.init(key);
		} catch (InvalidKeyException ike) {
			System.err.println("Invalid key exception: " + ike.toString());
		}
		try {
			String sign = Hex.encodeHexString(mac.doFinal(postData
					.getBytes("UTF-8")));
			return sign;
		} catch (IllegalStateException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

}
