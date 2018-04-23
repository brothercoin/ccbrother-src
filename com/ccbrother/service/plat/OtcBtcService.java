package com.hykj.ccbrother.service.plat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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
 * OtcBtc交易所，以后可能会Api变化频繁
 * @author 封君
 *
 */
@Service
public class OtcBtcService implements PlatService{
    private static final Logger logger = LoggerFactory.getLogger(OtcBtcService.class);

    private static final String baseUrl = "https://bb.otcbtc.com";
    private static final String method = "HmacSHA256";
    private static final String charset = "UTF-8";
    
	@Override
	public CoinPlatModel getTicker(CoinPlatModel coinPlatModel) {
		return null;
	}

	@Override
	public List<CoinPlatModel> getAllTicker(List<CoinPlatModel> list) {
		
		
		String url = baseUrl + "/api/v2/tickers";
		String r = HttpUtil.get(url, null);
		logger.info(r);
		Map map = JSON.parseObject(r);
		List newList = new ArrayList<CoinPlatModel>();
		for (CoinPlatModel coinPlatModel : list) {
			String symbol = coinPlatModel.getSymbol();
			String name = symbol.substring(0,symbol.length()-3) + "_" + symbol.substring(symbol.length()-3);
			JSONObject first = (JSONObject) map.get(name);
			JSONObject ticker = first.getJSONObject("ticker");
			CoinPlatModel newCoinPlat = new CoinPlatModel();
			newCoinPlat.setSymbol(coinPlatModel.getSymbol());
			newCoinPlat.setBuy(ticker.getBigDecimal("buy"));
			newCoinPlat.setSell(ticker.getBigDecimal("sell"));
			newCoinPlat.setLow(ticker.getBigDecimal("low"));
			newCoinPlat.setHigh(ticker.getBigDecimal("high"));
			newCoinPlat.setLast(ticker.getBigDecimal("last"));
			newCoinPlat.setVol(ticker.getBigDecimal("vol"));
			newCoinPlat.setId(coinPlatModel.getId());
			newCoinPlat.setTradingTime(new Date(first.getLong("at")*1000));
			newList.add(newCoinPlat);
		}
		return newList;
	}

	@Override
	public AppBack trade(String apiKey, String secret, String symbol, int type,
			BigDecimal price, BigDecimal amount) {
		
		String quro = "/api/v2/orders";
		String url = baseUrl + quro;
		Map params = new HashMap();
		params.put("access_key", apiKey);
		params.put("market", symbol);
		params.put("volume", amount.toString());
		params.put("price", price.toString());
		switch (type) {
		case 1:
			params.put("side", "buy");
			break;
		case 2:
			params.put("side", "sell");
			break;
		}
		String s = HttpUtil.sortMap(params);
		s = "POST|" + quro + "|" + s;
		String sign = Encrypt.hmacEncrypt(s, secret, method, charset).toLowerCase();
		params.put("signature", sign);
		String r = HttpUtil.post(url, params);
		logger.info(r);
		JSONObject appBack = JSON.parseObject(r);
		if(r.contains("error")){
			return new AppBack(-1,appBack.getJSONObject("error").getString("message"));
		}
		String orderId = appBack.getString("id");
		return new AppBack().add("orderId",orderId);
	}

	@Override
	public UserInfo getUserInfo(String apiKey, String secret, int platId) {
		String quro = "/api/v2/users/me";
		String url = baseUrl + quro;
		
		String s = "GET|" + quro + "|access_key=" + apiKey;//需要签名的内容格式
		String sign = Encrypt.hmacEncrypt(s, secret, "HmacSHA256", "UTF-8").toLowerCase();
		Map params = new HashMap();
		params.put("signature", sign);
		params.put("access_key", apiKey);
		
		String r = HttpUtil.get(url, params);
		logger.info(r);
		UserInfo userInfo = new UserInfo();
		JSONObject appBack = JSON.parseObject(r);
		if(appBack.getString("error") != null){
			return userInfo;
		}
		JSONArray result = appBack.getJSONArray("accounts");
		for (int i = 0; i < result.size(); i++) {
			JSONObject accounts = result.getJSONObject(i);
			if(accounts.getBigDecimal("balance").compareTo(BigDecimal.ZERO) > 0){
				CoinInfo coinInfo = new CoinInfo();
				coinInfo.setAmount(accounts.getBigDecimal("balance"));
				coinInfo.setName(accounts.getString("currency"));
				coinInfo.setPlatId(platId);
				userInfo.getFreeCoinList().add(coinInfo);
			}
			if(accounts.getBigDecimal("locked").compareTo(BigDecimal.ZERO) > 0){
				CoinInfo coinInfo = new CoinInfo();
				coinInfo.setAmount(accounts.getBigDecimal("locked"));
				coinInfo.setName(accounts.getString("currency"));
				coinInfo.setPlatId(platId);
				userInfo.getFreezedCoinList().add(coinInfo);
			}
		}
		userInfo.setPlatId(platId);
		return userInfo;
	}

	@Override
	public List<OrderInfo> getOrderInfo(String apiKey, String secret,
			Integer coinPlatId, String symbol) {
		String quro = "/api/v2/orders";
		String url = baseUrl + quro;
		Map params = new HashMap();
		params.put("access_key", apiKey);
		params.put("market", symbol);
		String s = HttpUtil.sortMap(params);
		s = "GET|" + quro + "|" + s;
		String sign = Encrypt.hmacEncrypt(s, secret, method, charset).toLowerCase();
		params.put("signature", sign);
		String r = HttpUtil.get(url, params);
		logger.info(r);
		List<OrderInfo> list = new ArrayList<OrderInfo>();
		if(r.contains("error") || r.length() == 2){//签名什么的错误或者返回的列表为空
			return list;
		}
		JSONArray appBack = JSON.parseArray(r);
		for (int i = 0; i < appBack.size(); i++) {
			JSONObject order = appBack.getJSONObject(i);
			OrderInfo orderInfo = new OrderInfo();
			orderInfo.setAmount(order.getBigDecimal("volume"));
			orderInfo.setCoinPlatId(coinPlatId);
			orderInfo.setCreateDate(order.getDate("created_at"));
			orderInfo.setOrderId(order.getString("id"));
			if ("buy".equals(order.getString("side"))) {
	               orderInfo.setType(1);
	        } else if ("sell".equals(order.getString("side"))) {
	               orderInfo.setType(2);
	        }
			orderInfo.setPrice(order.getBigDecimal("price"));
			orderInfo.setSymbol(symbol);
			orderInfo.setDealAmount(order.getBigDecimal("executed_volume"));
			if("wait".equals(order.getString("state"))){
				orderInfo.setStatus(0);
			} else if("done".equals(order.getString("state"))){
				orderInfo.setStatus(2);
			} else if("cancel".equals(order.getString("state"))){
				orderInfo.setStatus(-1);
			}
			list.add(orderInfo);
		}
		return list;
	}

	@Override
	public AppBack cancelOrder(String apiKey, String secret, String orderId,
			String symbol) {
		String quro = "/api/v2/order/delete";
		String url = baseUrl + quro;
		Map params = new HashMap();
		params.put("access_key", apiKey);
		params.put("id", orderId);
		String s = HttpUtil.sortMap(params);
		s = "POST|" + quro + "|" + s;
		String sign = Encrypt.hmacEncrypt(s, secret, method, charset).toLowerCase();
		params.put("signature", sign);
		String r = HttpUtil.post(url, params);
		logger.info(r);
		JSONObject appBack = JSON.parseObject(r);
		if(r.contains("error")){
			return new AppBack(-1,appBack.getJSONObject("error").getString("message"));
		}
		return new AppBack();
	}
}
