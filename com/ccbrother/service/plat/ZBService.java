package com.hykj.ccbrother.service.plat;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import com.hykj.ccbrother.apimodel.CoinInfo;
import com.hykj.ccbrother.apimodel.OrderInfo;
import com.hykj.ccbrother.apimodel.UserInfo;
import com.hykj.ccbrother.base.AppBack;
import com.hykj.ccbrother.base.MsgException;
import com.hykj.ccbrother.model.CoinPlatModel;
import com.hykj.ccbrother.service.CoinPlatService;
import com.hykj.ccbrother.utils.HttpUtil;
import org.springframework.stereotype.Service;

@Service
public class ZBService implements PlatService {

	private static final Logger logger = LoggerFactory
			.getLogger(ZBService.class);

	@Override
	public CoinPlatModel getTicker(CoinPlatModel coinPlatModel) {
		String symbol = coinPlatModel.getSymbol();
		String url = "http://api.zb.com/data/v1/ticker?market=";
		String r = HttpUtil.get(url + symbol, null);
		logger.debug(r);
		System.out.println(r);
		JSONObject appBack = JSON.parseObject(r);
		if(appBack.getString("result") != null ){//这时候会提示服务端忙碌
			return  null;
		}
		CoinPlatModel newCoinPlat = new CoinPlatModel();
		newCoinPlat.setId(coinPlatModel.getId());
		newCoinPlat.setTradingTime(appBack.getDate("date"));
		JSONObject ticker = appBack.getJSONObject("ticker");
		newCoinPlat.setVol(ticker.getBigDecimal("vol"));
		newCoinPlat.setLast(ticker.getBigDecimal("last"));
		newCoinPlat.setSell(ticker.getBigDecimal("sell"));
		newCoinPlat.setBuy(ticker.getBigDecimal("buy"));
		newCoinPlat.setHigh(ticker.getBigDecimal("high"));
		newCoinPlat.setLow(ticker.getBigDecimal("low"));

		return newCoinPlat;
	}

	@Override
	public List<CoinPlatModel> getAllTicker(List<CoinPlatModel> list) {
		return null;
	}

	@Override
	public AppBack trade(String apiKey, String secret, String symbol, int type,
			BigDecimal price, BigDecimal amount) {

		String url = "https://trade.zb.com/api/order";
		Map params = new HashMap();
		params.put("accesskey", apiKey);
		params.put("method", "order");
		params.put("price", price);
		params.put("amount", amount);
		params.put("currency", "symbol");
		switch (type) {
		case 1:
			params.put("tradeType", 1);
			break;
		case 2:
			params.put("tradeType", 2);
			break;
		}
		String sortMap = sortMap(params);
		String digestSecret = digest(secret);
		String sign = hmacSign(sortMap(params), digestSecret);
		params.put("sign", sign);
		params.put("reqTime", System.currentTimeMillis() + "");
		String r = HttpUtil.get(url, params);
		logger.info("trade" + r);

		JSONObject appBack = JSON.parseObject(r);
		if (r.contains("code")) {
			return new AppBack(-1, appBack.getString("message"));
		}
		String orderId = appBack.getString("id");
		return new AppBack().add("orderId", orderId);
	}

	@Override
	public UserInfo getUserInfo(String apiKey, String secret, int platId) {
		String url = "https://trade.zb.com/api/getAccountInfo";
		Map params = new HashMap();
		params.put("accesskey", apiKey);
		params.put("method", "getAccountInfo");
		String sortMap = sortMap(params);
		String digestSecret = digest(secret);// 加密密匙
		String sign = hmacSign(sortMap(params), digestSecret);// 签名
		params.put("sign", sign);
		params.put("reqTime", System.currentTimeMillis() + "");
		String r = HttpUtil.get(url, params);
		logger.info("getUserInfo" + r);
		JSONObject appBack = JSON.parseObject(r);
		UserInfo userInfo = new UserInfo();
		if(r.contains("code")){
			return userInfo;
		}
		JSONArray jsonArray = appBack.getJSONObject("result").getJSONArray(
				"coins");
		for (int i = 0; i < jsonArray.size(); i++) {
			JSONObject coin = jsonArray.getJSONObject(i);
			BigDecimal available = coin.getBigDecimal("available");
			BigDecimal freez = coin.getBigDecimal("freez");

			if (available.compareTo(BigDecimal.ZERO) > 0) {
				CoinInfo coinInfo = new CoinInfo();
				coinInfo.setName(coin.getString("key"));
				coinInfo.setAmount(available);
				userInfo.getFreeCoinList().add(coinInfo);
			}
			if (freez.compareTo(BigDecimal.ZERO) > 0) {
				CoinInfo coinInfo = new CoinInfo();
				coinInfo.setName(coin.getString("key"));
				coinInfo.setAmount(freez);
				userInfo.getFreezedCoinList().add(coinInfo);
			}
		}

		return userInfo;
	}

	@Override
	public List<OrderInfo> getOrderInfo(String apiKey, String secret,
			Integer coinPlatId, String symbol) {

		String url = "https://trade.zb.com/api/getOrdersIgnoreTradeType";
		Map params = new HashMap();
		params.put("accesskey", apiKey);
		params.put("method", "getOrdersIgnoreTradeType");
		params.put("currency", symbol);
		params.put("pageIndex", 1);
		params.put("pageSize", 99);

		String sortMap = sortMap(params);
		String digestSecret = digest(secret);// 加密密匙
		String sign = hmacSign(sortMap(params), digestSecret);// 签名
		params.put("sign", sign);
		params.put("reqTime", System.currentTimeMillis() + "");
		String r = HttpUtil.get(url, params);
		logger.info("getUserInfo" + r);
		
		List orderList = new ArrayList();
		if(!"1000".equals(JSON.parseObject(r).getString("code"))){
			return orderList;
		}
		JSONArray appBack = JSON.parseArray(r);
		for (int i = 0; i < appBack.size(); i++) {
			JSONObject order = appBack.getJSONObject(i);
			OrderInfo orderInfo = new OrderInfo();

			if (coinPlatId != null) {//TODO 不需要 by innel
				orderInfo.setCoinPlatId(coinPlatId);
			}
			orderInfo.setAmount(order.getBigDecimal("total_amount"));
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");// 小写的mm表示的是分钟2017-05-12T17:17:57.437Z
			orderInfo.setCreateDate(order.getDate("trade_date"));
			orderInfo.setOrderId(order.getString("id"));
			orderInfo.setPrice(order.getBigDecimal("price"));
			orderInfo.setDealAmount(order.getBigDecimal("trade_amount"));
			orderInfo.setSymbol(symbol);
			if ("1".equals(order.getString("type"))) {
				orderInfo.setType(1);
			} else if ("0".equals(order.getString("type"))) {
				orderInfo.setType(2);
			}

			if ("1".equals(order.getString("status"))) {
				orderInfo.setStatus(0);
			} else if ("2".equals(order.getString("status"))) {
				orderInfo.setStatus(2);
			} else if ("3".equals(order.getString("status"))) {
				orderInfo.setStatus(1);
			}
			orderList.add(orderInfo);
		}
		return orderList;
	}

	@Override
	public AppBack cancelOrder(String apiKey, String secret, String orderId,
			String symbol) {
		
		String url = "https://trade.zb.com/api/cancelOrder";
		Map params = new HashMap();
		params.put("accesskey", apiKey);
		params.put("method", "cancelOrder");
		params.put("currency", symbol);
		params.put("id", orderId);
		
		String sortMap = sortMap(params);
		String digestSecret = digest(secret);// 加密密匙
		String sign = hmacSign(sortMap(params), digestSecret);// 签名
		params.put("sign", sign);
		params.put("reqTime", System.currentTimeMillis() + "");
		String r = HttpUtil.get(url, params);
		logger.info(r);
		
		JSONObject appBack = JSON.parseObject(r);
		if(!"1000".equals(appBack.getString("code"))){
			return new AppBack(-1,appBack.getString("message"));
		}
		
		return new AppBack();
	}

	/**
	 * 按照ASCII码的顺序对参数名进行排序
	 * 
	 * @param bytes
	 * @return
	 */
	private String sortMap(Map params) {
		List<String> list = new ArrayList<String>(params.keySet());
		// 所有参数按key进行字典升序排列
		Collections.sort(list);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			sb.append(list.get(i) + "=" + params.get(list.get(i)) + "&");
		}
		// 删除末尾的&
		sb.delete(sb.length() - 1, sb.length());
		return sb.toString();
	}

	/**
	 * SHA加密(用于对密匙的加密)
	 * 
	 * @param aValue
	 * @return
	 */
	private String digest(String aValue) {
		aValue = aValue.trim();
		byte value[];
		try {
			value = aValue.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			value = aValue.getBytes();
		}
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
		return toHex(md.digest(value));

	}

	private String toHex(byte input[]) {
		if (input == null)
			return null;
		StringBuffer output = new StringBuffer(input.length * 2);
		for (int i = 0; i < input.length; i++) {
			int current = input[i] & 0xff;
			if (current < 16)
				output.append("0");
			output.append(Integer.toString(current, 16));
		}
		return output.toString();
	}

	/**
	 * 生成签名消息
	 * 
	 * @param aValue
	 *            要签名的字符串
	 * @param aKey
	 *            签名密钥
	 * @return
	 */
	private  String hmacSign(String aValue, String aKey) {
		byte k_ipad[] = new byte[64];
		byte k_opad[] = new byte[64];
		byte keyb[];
		byte value[];
		try {
			keyb = aKey.getBytes("UTF-8");
			value = aValue.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			keyb = aKey.getBytes();
			value = aValue.getBytes();
		}

		Arrays.fill(k_ipad, keyb.length, 64, (byte) 54);
		Arrays.fill(k_opad, keyb.length, 64, (byte) 92);
		for (int i = 0; i < keyb.length; i++) {
			k_ipad[i] = (byte) (keyb[i] ^ 0x36);
			k_opad[i] = (byte) (keyb[i] ^ 0x5c);
		}

		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");// "MD5"
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
		md.update(k_ipad);
		md.update(value);
		byte dg[] = md.digest();
		md.reset();
		md.update(k_opad);
		md.update(dg, 0, 16);
		dg = md.digest();
		return toHex(dg);
	}

}
