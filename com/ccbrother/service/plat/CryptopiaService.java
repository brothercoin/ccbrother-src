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
import com.hykj.ccbrother.utils.HttpUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class CryptopiaService implements PlatService {

	private static final Logger logger = LoggerFactory
			.getLogger(CryptopiaService.class);

	@Override
	public CoinPlatModel getTicker(CoinPlatModel coinPlatModel) {
		String url = "https://www.cryptopia.co.nz/api/GetMarket/";
		String symbol = coinPlatModel.getSymbol();
		String r = HttpUtil.get(url + symbol, null);
		logger.info(r);
		JSONObject apiBack = JSON.parseObject(r).getJSONObject("Data");
		CoinPlatModel newCoinPlat = new CoinPlatModel();
		newCoinPlat.setId(coinPlatModel.getId());
		newCoinPlat.setTradingTime(new Date());
		newCoinPlat.setBuy(apiBack.getBigDecimal("BidPrice"));
		newCoinPlat.setSell(apiBack.getBigDecimal("AskPrice"));
		newCoinPlat.setHigh(apiBack.getBigDecimal("High"));
		newCoinPlat.setLow(apiBack.getBigDecimal("Low"));
		newCoinPlat.setVol(apiBack.getBigDecimal("Volume"));
		newCoinPlat.setLast(apiBack.getBigDecimal("LastPrice"));
		return newCoinPlat;
	}

	@Override
	public List<CoinPlatModel> getAllTicker(List<CoinPlatModel> list) {
		String url = "https://www.cryptopia.co.nz/api/GetMarkets";
		String r = HttpUtil.get(url, null);
		logger.debug("getAllTicker" + r);
		JSONObject apiBack = JSON.parseObject(r);
		JSONArray result = apiBack.getJSONArray("Data");
		List newList = new ArrayList<CoinPlatModel>();
		Map<String, CoinPlatModel> tempMap = new HashMap<>();
		for (int i = 0; i < result.size(); i++) {
			JSONObject coinPlatJson = result.getJSONObject(i);
			CoinPlatModel newCoinPlat = new CoinPlatModel();
			newCoinPlat.setBuy(coinPlatJson.getBigDecimal("BidPrice"));
			newCoinPlat.setHigh(coinPlatJson.getBigDecimal("High"));
			newCoinPlat.setLow(coinPlatJson.getBigDecimal("Low"));
			newCoinPlat.setSell(coinPlatJson.getBigDecimal("AskPrice"));
			newCoinPlat.setVol(coinPlatJson.getBigDecimal("Volume"));
			newCoinPlat.setLast(coinPlatJson.getBigDecimal("LastPrice"));
			newCoinPlat.setTradingTime(new Date());
			tempMap.put(coinPlatJson.getString("TradePairId"), newCoinPlat);
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
		String url = "https://www.cryptopia.co.nz/api/SubmitTrade";
		String nonce = String.valueOf(System.currentTimeMillis());
		JSONObject jo = new JSONObject();
		jo.put("TradePairId", symbol);
		if (type == 1) {
			jo.put("Type", "Buy");
		}
		if (type == 2) {
			jo.put("Type", "Sell");
		}
		jo.put("Rate", price.toString());
		jo.put("Amount", amount.toString());

		logger.info("parms " + jo.toJSONString());
		String reqSignature = getSign(apiKey, url, jo, nonce);
		String AUTH = "amx " + apiKey + ":"
				+ this.sha256_B64(reqSignature, secret) + ":" + nonce;
		Map header = new HashMap();
		header.put("Authorization", AUTH);
		header.put("Content-Type", "application/json");
		String r = HttpUtil.post(url, jo.toString(), header);
		logger.info("getUserInfo " + r);
		JSONObject apiBack = JSON.parseObject(r);
		Boolean success = apiBack.getBoolean("Success");
		if (success == null || !success) {
			return new AppBack(-1,"下单失败" + apiBack.getString("Error"));//返回错误而不是抛出异常
		}
		return new AppBack().add("orderId", apiBack.getJSONObject("Data").getString("OrderId"));//修改data为Data
	}

	@Override
	public UserInfo getUserInfo(String apiKey, String secret, int platId) {
		String url = "https://www.cryptopia.co.nz/api/GetBalance";
		String nonce = String.valueOf(System.currentTimeMillis());
		JSONObject jo = new JSONObject();
		String reqSignature = getSign(apiKey, url, jo, nonce);

		String AUTH = "amx " + apiKey + ":"
				+ this.sha256_B64(reqSignature, secret) + ":" + nonce;

		Map header = new HashMap();
		header.put("Authorization", AUTH);
		header.put("Content-Type", "application/json");
		String r = HttpUtil.post(url, jo.toString(), header);
		logger.info("getUserInfo " + r);

		JSONObject apiBack = JSON.parseObject(r);
		Boolean success = apiBack.getBoolean("Success");
		UserInfo userInfo = new UserInfo();
		if (success == null || !success) {
			return userInfo;
		}
		JSONArray data = apiBack.getJSONArray("Data");
		

		for (int i = 0; i < data.size(); i++) {
			JSONObject coin = data.getJSONObject(i);

			CoinInfo coinInfo = new CoinInfo();
			coinInfo.setName(coin.getString("Symbol"));
			coinInfo.setAmount(coin.getBigDecimal("Available"));
			coinInfo.setPlatId(platId);
			if (0 != coinInfo.getAmount().compareTo(BigDecimal.ZERO)) {
				userInfo.getFreeCoinList().add(coinInfo);
			}

			coinInfo = new CoinInfo();
			coinInfo.setName(coin.getString("Symbol"));
			coinInfo.setAmount(coin.getBigDecimal("Total").subtract(
					coin.getBigDecimal("Available")));
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
		String url = "https://www.cryptopia.co.nz/api/GetOpenOrders";
		String nonce = String.valueOf(System.currentTimeMillis());
		JSONObject jo = new JSONObject();
		jo.put("TradePairId", symbol);
		String reqSignature = getSign(apiKey, url, jo, nonce);

		String AUTH = "amx " + apiKey + ":"
				+ this.sha256_B64(reqSignature, secret) + ":" + nonce;

		Map header = new HashMap();
		header.put("Authorization", AUTH);
		header.put("Content-Type", "application/json");
		String r = HttpUtil.post(url, jo.toString(), header);
		logger.info("getUserInfo " + r);

		JSONObject apiBack = JSON.parseObject(r);
		Boolean success = apiBack.getBoolean("Success");
		List orderList = new ArrayList();
		if (success == null || !success) {
			return orderList;
		}
		JSONArray orders = apiBack.getJSONArray("Data");

		for (int i = 0; i < orders.size(); i++) {

			JSONObject order = orders.getJSONObject(i);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");// 小写的mm表示的是分钟2017-05-12T17:17:57.437Z
			String dstr = order.getString("TimeStamp");

			OrderInfo orderInfo = new OrderInfo();
			orderInfo.setAmount(order.getBigDecimal("Amount"));
			try {
				orderInfo.setCreateDate(sdf.parse(dstr));
			} catch (ParseException e) {
				e.printStackTrace();
			}
			orderInfo.setCoinPlatId(coinPlatId);
			if ("Buy".equals(order.getString("Type"))) {
				orderInfo.setType(1);
			} else if ("Sell".equals(order.getString("Type"))) {
				orderInfo.setType(2);
			}
			orderInfo.setSymbol(order.getString("TradePairId"));
			orderInfo.setPrice(order.getBigDecimal("Rate"));
			orderInfo.setStatus(0);
			orderInfo.setDealAmount(orderInfo.getAmount().subtract(
					order.getBigDecimal("Remaining")));
			if (0 != orderInfo.getDealAmount().compareTo(BigDecimal.ZERO)) {
				orderInfo.setStatus(1);
			}
			orderInfo.setOrderId(order.getString("OrderId"));
			orderList.add(orderInfo);

		}
		return orderList;
	}

	@Override
	public AppBack cancelOrder(String apiKey, String secret, String orderId,
			String symbol) {
		String url = "https://www.cryptopia.co.nz/api/CancelTrade";
		String nonce = String.valueOf(System.currentTimeMillis());
		JSONObject jo = new JSONObject();
		jo.put("TradePairId", symbol);
		jo.put("Type", "Trade");
		jo.put("OrderId", orderId);

		String reqSignature = getSign(apiKey, url, jo, nonce);

		String AUTH = "amx " + apiKey + ":"
				+ this.sha256_B64(reqSignature, secret) + ":" + nonce;

		Map header = new HashMap();
		header.put("Authorization", AUTH);
		header.put("Content-Type", "application/json");
		String r = HttpUtil.post(url, jo.toString(), header);
		logger.info("cancelOrder " + r);

		JSONObject apiBack = JSON.parseObject(r);
		Boolean success = apiBack.getBoolean("Success");
		if (success == null || !success) {
			String msg = apiBack.getString("Error");
			throw new MsgException(msg);
		}

		return new AppBack();
	}

	private String getMD5_B64(String postParameter) {
		try {
			return Base64.getEncoder().encodeToString(
					MessageDigest.getInstance("MD5").digest(
							postParameter.getBytes("UTF-8")));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	private String getSign(String apiKey, String url, JSONObject jo,
			String nonce) {

		String reqSignature = null;
		try {
			reqSignature = apiKey
					+ "POST"
					+ URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
							.toLowerCase() + nonce + getMD5_B64(jo.toString());
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage(), e);

		}
		return reqSignature;
	}

	private String sha256_B64(String msg, String privateKey) {
		Mac sha256_HMAC = null;
		try {
			sha256_HMAC = Mac.getInstance("HmacSHA256");
		} catch (NoSuchAlgorithmException e) {
			logger.error(e.getMessage(), e);
		}
		SecretKeySpec secret_key = new SecretKeySpec(Base64.getDecoder()
				.decode(privateKey), "HmacSHA256");
		try {
			sha256_HMAC.init(secret_key);
		} catch (InvalidKeyException e) {
			logger.error(e.getMessage(), e);
		}
		try {
			return Base64.getEncoder().encodeToString(
					sha256_HMAC.doFinal(msg.getBytes("UTF-8")));
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

}
