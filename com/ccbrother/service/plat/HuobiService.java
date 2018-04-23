package com.hykj.ccbrother.service.plat;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

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
import com.hykj.ccbrother.service.CoinPlatService;
import com.hykj.ccbrother.utils.HttpUtil;

@Service
public class HuobiService implements PlatService {

	private static final Logger logger = LoggerFactory
			.getLogger(HuobiService.class);

	@Override
	public CoinPlatModel getTicker(CoinPlatModel coinPlatModel) {

		String symbol = coinPlatModel.getSymbol();
		String url = "https://api.huobipro.com/market/detail/merged?symbol=";
		String r = HttpUtil.get(url + symbol, null, getGetHeader());
		logger.debug(r);
		JSONObject apiBack = JSON.parseObject(r);
		
		CoinPlatModel newCoinPlat = new CoinPlatModel();
		newCoinPlat.setId(coinPlatModel.getId());
		newCoinPlat.setTradingTime(new Date(apiBack.getLongValue("ts")));
		
		JSONObject tick = apiBack.getJSONObject("tick");
		newCoinPlat.setBuy(tick.getJSONArray("bid").getBigDecimal(0));
		newCoinPlat.setSell(tick.getJSONArray("ask").getBigDecimal(0));
		newCoinPlat.setHigh(new BigDecimal(tick.getString("high")));
		newCoinPlat.setLast(new BigDecimal(tick.getString("close")));
		newCoinPlat.setLow(new BigDecimal(tick.getString("low")));
		newCoinPlat.setVol(new BigDecimal(tick.getString("amount")));
		return newCoinPlat;
	}

	@Override
	public List<CoinPlatModel> getAllTicker(List<CoinPlatModel> list) {
		return null;
	}

	@Override
	public AppBack trade(String apiKey, String secret, String symbol, int type,
			BigDecimal price, BigDecimal amount) {

		Map params = new HashMap();
		String accountId = this.getAccessID(apiKey, secret);
		if (accountId == null) {
			return new AppBack(-1, "获取账号失败或您还没有开通此类型的账户");
		}

		this.createSignature(apiKey, secret, "post", "api.huobipro.com",
				"/v1/order/orders/place", params);
		params.put("symbol", symbol);
		params.put("amount", amount);
		params.put("price", price);
		params.put("account-id", accountId);
		switch (type) {
		case 1:
			params.put("type", "buy-limit");
			break;
		case 2:
			params.put("type", "sell-limit");
			break;
		}
		String resultx1 = HttpUtil.post(
				"https://api.huobipro.com/v1/order/orders/place", params,
				getPostHeader());
		logger.debug("trade" + resultx1);
		JSONObject resultx = JSON.parseObject(resultx1);
		if ("ok".equals(resultx.getShort("ok"))) {
			String orderId = resultx.getString("data");
			return new AppBack().add("orderId", orderId);
		}

		return new AppBack(-1, "下单失败");
	}

	@Override
	public UserInfo getUserInfo(String apiKey, String secret, int platId) {

		Map params = new HashMap();
		String accountId = this.getAccessID(apiKey, secret);
		if (accountId == null) {
			return null;
		}
		UserInfo userInfo = new UserInfo();

		this.createSignature(apiKey, secret, "GET", "api.huobipro.com",
				"/v1/account/accounts/" + accountId + "/balance", params);
		String resltx = HttpUtil.get(
				"https://api.huobipro.com/v1/account/accounts/" + accountId
						+ "/balance", params, getGetHeader());
		logger.info("getUserInfo " + resltx);
		JSONObject appBack = JSON.parseObject(resltx);
		if (!"ok".equals(appBack.getString("status"))) {
			return userInfo;
		}
		JSONArray jsonArray = appBack.getJSONObject("data")
				.getJSONArray("list");
		for (int i = 0; i < jsonArray.size(); i++) {
			JSONObject coin = jsonArray.getJSONObject(i);
			if ("trade".equals(coin.getString("type"))) {
				if(coin.getBigDecimal("balance").compareTo(BigDecimal.ZERO)>0){
					CoinInfo coinInfo = new CoinInfo();
					coinInfo.setName(coin.getString("currency"));
					coinInfo.setAmount(new BigDecimal(coin.getString("balance")));
					coinInfo.setPlatId(platId);
					userInfo.getFreeCoinList().add(coinInfo);
				}
			}
			if ("frozen".equals(coin.getString("type"))) {
				if(coin.getBigDecimal("balance").compareTo(BigDecimal.ZERO)>0){
					CoinInfo coinInfo = new CoinInfo();
					coinInfo.setName(coin.getString("currency"));
					coinInfo.setAmount(new BigDecimal(coin.getString("balance")));
					coinInfo.setPlatId(platId);
					userInfo.getFreezedCoinList().add(coinInfo);
				}
			}
		}
		userInfo.setPlatId(platId);
		return userInfo;
	}

	@Override
	public List<OrderInfo> getOrderInfo(String apiKey, String secret,
			Integer coinPlatId, String symbol) {
		
		Map params = new HashMap();
		params.put("symbol", symbol);
		params.put("states", "filled,canceled,partial-filled");
		this.createSignature(apiKey,secret,"GET","api.huobipro.com","/v1/order/orders",params);
		String resltx = HttpUtil.get("https://api.huobipro.com/v1/order/orders",
				params, getGetHeader());
		logger.info("getUserInfo " + resltx);
		JSONObject appBack = JSON.parseObject(resltx);
		List<OrderInfo> orders = new ArrayList();
		if (!"ok".equals(appBack.getString("status"))) {
			return orders;
		}

		JSONArray jsonArray = appBack.getJSONArray("data");
		for (int i = 0; i < jsonArray.size(); i++) {
			JSONObject order = jsonArray.getJSONObject(i);
			OrderInfo orderInfo = new OrderInfo();
			orderInfo.setCoinPlatId(coinPlatId);
			orderInfo.setOrderId(order.getLong("id").toString());
			orderInfo.setAmount(new BigDecimal(order.getString("amount")));// 订单要求数量？
			orderInfo.setDealAmount(new BigDecimal(order
					.getString("field-amount")));// 成交数量
			orderInfo.setPrice(new BigDecimal(order.getString("price")));
			if ("bul-limit".equalsIgnoreCase(order.getString("type"))) {
				orderInfo.setType(1);
			} else if ("sell-limit".equalsIgnoreCase(order.getString("type"))) {
				orderInfo.setType(2);
			}
			orderInfo.setSymbol(order.getString("symbol"));
			if ("canceled".equalsIgnoreCase(order.getString("state"))) {
				// -1:已撤销 0:未成交 1:部分成交 2:完全成交 4:撤单处理中 okex
				orderInfo.setStatus(-1);
			} else if ("partial-filled".equalsIgnoreCase(order
					.getString("state"))) {
				orderInfo.setStatus(1);
			} else if ("filled".equalsIgnoreCase(order.getString("state"))) {
				orderInfo.setStatus(2);
			}
			if(coinPlatId!=null){
				orderInfo.setCoinPlatId(coinPlatId);
			}
			orderInfo.setCreateDate(new Date(order.getLongValue("created-at")));
			orderInfo.setSymbol(symbol);
			orders.add(orderInfo);
		}
		return orders;
	}

	@Override
	public AppBack cancelOrder(String apiKey, String secret, String orderId,
			String symbol) {

		Map params = new HashMap();
		String accountId = this.getAccessID(apiKey,secret);
		if(accountId == null){
			return null;
		}
		this.createSignature(apiKey,secret,"POST","api.huobipro.com","/v1/order/orders/"+orderId+"/submitcancel",params);
		String resltx = HttpUtil.get("https://api.huobipro.com/v1/order/orders/"
				+ orderId + "/submitcancel", params, getGetHeader());
		logger.info(resltx);
		JSONObject appBack = JSON.parseObject(resltx);
		if (!"ok".equalsIgnoreCase(appBack.getString("status"))) {
			return new AppBack(-1, "撤销失败");
		}
		return new AppBack();
	}

	/**
	 * 获取accessId,这个数据获取用户信息需要的
	 * 
	 * @return
	 */
	private String getAccessID(String apiKey, String secret) {
		Map<String, String> params = new HashMap<String, String>();
		this.createSignature(apiKey, secret, "get", "api.huobipro.com",
				"/v1/account/accounts", params);

		String result = HttpUtil.get(
				"https://api.huobipro.com/v1/account/accounts", params,
				getGetHeader());
		JSONObject result1 = JSON.parseObject(result);
		JSONArray jsonArray = result1.getJSONArray("data");
		JSONObject result2 = jsonArray.getJSONObject(0);
		String accountId = result2.getString("id");

		return accountId;
	}

	private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter
			.ofPattern("uuuu-MM-dd'T'HH:mm:ss");
	private static final ZoneId ZONE_GMT = ZoneId.of("Z");

	/**
	 * 创建一个有效的签名。该方法为客户端调用，将在传入的params中添加AccessKeyId、Timestamp、SignatureVersion、
	 * SignatureMethod、Signature参数。
	 * 
	 * @param appKey
	 *            AppKeyId.
	 * @param appSecretKey
	 *            AppKeySecret.
	 * @param method
	 *            请求方法，"GET"或"POST"
	 * @param host
	 *            请求域名，例如"be.huobi.com"
	 * @param uri
	 *            请求路径，注意不含?以及后的参数，例如"/v1/api/info"
	 * @param params
	 *            原始请求参数，以Key-Value存储，注意Value不要编码
	 */
	public Map createSignature(String appKey, String appSecretKey,
			String method, String host, String uri, Map<String, String> params) {
		StringBuilder sb = new StringBuilder(1024);
		sb.append(method.toUpperCase()).append('\n') // GET
				.append(host.toLowerCase()).append('\n') // Host
				.append(uri).append('\n'); // /path
		params.remove("Signature");
		params.put("AccessKeyId", appKey);
		params.put("SignatureVersion", "2");
		params.put("SignatureMethod", "HmacSHA256");
		params.put(
				"Timestamp",
				Instant.ofEpochSecond(Instant.now().getEpochSecond())
						.atZone(ZONE_GMT).format(DT_FORMAT));// 获取当前国际标准时间（不是东八区时间）并转换成日期格式
		// build signature:
		SortedMap<String, String> map = new TreeMap<>(params);
		for (Map.Entry<String, String> entry : map.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			sb.append(key).append('=').append(urlEncode(value)).append('&');
		}
		// remove last '&':
		sb.deleteCharAt(sb.length() - 1);
		// sign:
		Mac hmacSha256 = null;
		try {
			hmacSha256 = Mac.getInstance("HmacSHA256");
			SecretKeySpec secKey = new SecretKeySpec(
					appSecretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
			hmacSha256.init(secKey);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("No such algorithm: " + e.getMessage());
		} catch (InvalidKeyException e) {
			throw new RuntimeException("Invalid key: " + e.getMessage());
		}
		String payload = sb.toString();
		byte[] hash = hmacSha256.doFinal(payload
				.getBytes(StandardCharsets.UTF_8));
		String actualSign = Base64.getEncoder().encodeToString(hash);
		params.put("Signature", actualSign);
		return params;
		/*
		 * if (log.isDebugEnabled()) { log.debug("Dump parameters:"); for
		 * (Map.Entry<String, String> entry : params.entrySet()) {
		 * log.debug("  key: " + entry.getKey() + ", value: " +
		 * entry.getValue()); } }
		 */
	}

	/**
	 * 使用标准URL Encode编码。注意和JDK默认的不同，空格被编码为%20而不是+。
	 * 
	 * @param s
	 *            String字符串
	 * @return URL编码后的字符串
	 */
	private String urlEncode(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8").replaceAll("\\+", "%20");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException("UTF-8 encoding not supported!");
		}
	}

	/**
	 * get方式要有的请求头
	 * 
	 * @return
	 */
	private Map getGetHeader() {
		Map header = new HashMap<>();
		header.put("Content-Type", "application/x-www-form-urlencoded");
		header.put("Accept-Language", "zh-cn");
		header.put(
				"User-Agent",
				"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Safari/537.36");
		return header;
	}

	/**
	 * post方式要有的请求头
	 * 
	 * @return
	 */
	private Map getPostHeader() {
		Map header = new HashMap<>();
		header.put("Content-Type", "application/json");
		header.put("Accept-Language", "zh-cn");
		header.put(
				"User-Agent",
				"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Safari/537.36");
		return header;
	}

}
