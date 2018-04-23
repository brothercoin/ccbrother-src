package com.hykj.ccbrother.service.plat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hykj.ccbrother.apimodel.CoinInfo;
import com.hykj.ccbrother.apimodel.OrderInfo;
import com.hykj.ccbrother.apimodel.UserInfo;
import com.hykj.ccbrother.base.AppBack;
import com.hykj.ccbrother.model.CoinPlatModel;
import com.hykj.ccbrother.utils.HttpUtil;
import com.hykj.ccbrother.utils.MD5;
import com.hykj.ccbrother.utils.StringUtil;
/**
 * 这个交易所挂单后无法返回id，属于api不全，获取订单历史记录也会没有，也无法根据Id查询撤销
 * @author 封君
 *
 */
@Service
public class CoinwService implements PlatService {

	private static final Logger logger = LoggerFactory
			.getLogger(CoinwService.class);

	@Override
	public CoinPlatModel getTicker(CoinPlatModel coinPlatModel) {
		String symbol = coinPlatModel.getSymbol();
		String url = "https://www.coinw.com/appApi.html?action=market&symbol=";
		String r = HttpUtil.get(url + symbol, null);
		logger.debug(r);
		CoinPlatModel newCoinPlat = new CoinPlatModel();
		newCoinPlat.setId(coinPlatModel.getId());
		JSONObject appBack = JSON.parseObject(r);
		newCoinPlat.setTradingTime(appBack.getDate("time"));
		newCoinPlat.setHigh(appBack.getBigDecimal("high"));
		newCoinPlat.setVol(appBack.getBigDecimal("vol"));
		newCoinPlat.setLast(appBack.getJSONObject("data").getBigDecimal("last"));
		newCoinPlat.setLow(appBack.getBigDecimal("low"));
		newCoinPlat.setBuy(appBack.getBigDecimal("buy"));
		newCoinPlat.setSell(appBack.getBigDecimal("sell"));
		
		return newCoinPlat;
	}

	@Override
	public List<CoinPlatModel> getAllTicker(List<CoinPlatModel> list) {
		return null;
	}

	@Override
	public AppBack trade(String apiKey, String secret, String symbol, int type,
			BigDecimal price, BigDecimal amount) {

		String url = "https://www.coinw.com/appApi.html";
		Map params = new HashMap();
		params.put("api_key", apiKey);
		params.put("symbol", symbol);
		switch (type) {
		case 1:
			params.put("type", "0");
			break;
		case 2:
			params.put("type", "1");
			break;
		}
		params.put("price", price.toString());
		params.put("amount", amount.toString());
		String s = HttpUtil.sortMap(params);
		String s1 = s + "&secret_key=" + secret;
		String sign = MD5.sign(s1, "UTF-8").toUpperCase();
		s += "&sign=" + sign;
		params.put("sign", sign);
		params.put("action", "trade");
		String r = HttpUtil.post(url, params);
		JSONObject appBack = JSON.parseObject(r);
		if (!"200".equals(appBack.getString("code"))) {
			return new AppBack(-1,"交易错误 错误编码: " + appBack.getString("msg"));
		}
		logger.info(r);
		//返回格式{"time":1516269956426,"code":200,"msg":"委托成功"}，没有Id
		//JSONObject data = appBack.getJSONObject("data");
		
		return new AppBack();
	}

	@Override
	public UserInfo getUserInfo(String apiKey, String secret, int platId) {
		
		String url = "https://www.coinw.com/appApi.html";
		String s = "api_key=" + apiKey + "&secret_key=" + secret;
		String sign = MD5.sign(s, "UTF-8").toUpperCase();
		s += "&sign=" + sign;
		Map<String, String> params = HttpUtil.getUrlParams(s);
		params.put("action", "userinfo");
		params.remove("secret_key");
		String r = HttpUtil.post(url, params);
		JSONObject appBack = JSON.parseObject(r);
		UserInfo userInfo = new UserInfo();
		if (!"200".equals(appBack.getString("code"))) {
			logger.debug("获取失败 " + r);
			return userInfo;
		}
		
		JSONObject data = appBack.getJSONObject("data");
		Map free = data.getJSONObject("free");
		Iterator<Map.Entry<String, Object>> it = free.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Object> entry = it.next();
			CoinInfo coinInfo = new CoinInfo();
			coinInfo.setName(entry.getKey());
			BigDecimal value = new BigDecimal(entry.getValue().toString());//返回的类型如果为0会自动转换成Integer，如果返回的数量大于0又自动转换成Bigdecimal，所以用Object接收然后tostring再转换成需要的格式
			coinInfo.setAmount(value);
			coinInfo.setPlatId(platId);
			if (0 != coinInfo.getAmount().compareTo(BigDecimal.ZERO)) {
				userInfo.getFreeCoinList().add(coinInfo);
			}
		}
		
		Map freezed = data.getJSONObject("frozen");
		it = freezed.entrySet().iterator();
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
		userInfo.setPlatId(platId);
		return userInfo;
	}

	@Override
	public List<OrderInfo> getOrderInfo(String apiKey, String secret,
			Integer coinPlatId, String symbol) {
//		String url = "https://www.coinw.com/appApi.html";
//		if (StringUtil.isEmptyString(symbol)) {
//			return null;
//		}
//		String s = 	"api_key="+apiKey+"&symbol="+symbol+"&secret_key="+secret;
//		String sign = MD5.sign(s, "UTF-8").toUpperCase();
//		s += "&sign=" + sign;
//		logger.debug(s);
//		Map<String, String> params = HttpUtil.getUrlParams(s);
//		params.put("action", "lastentrust");//获取最新十条委托记录
//		params.remove("secret_key");
//		String r = HttpUtil.post(url, params);
//		logger.debug("获取结果 " + r);
//		JSONObject appBack = JSON.parseObject(r);
//		List orderList = new ArrayList();
//		if (!"200".equals(appBack.getString("200"))) {
//			return orderList;
//		}
//		
//		
		
		
		return null;
	}

	@Override
	public AppBack cancelOrder(String apiKey, String secret, String orderId,
			String symbol) {
		
//		String url = "https://www.coinw.com/appApi.html";
//		String s = 	"api_key="+apiKey+"id="+orderId+"&symbol="+symbol+"&secret_key="+secret;
//		String sign = MD5.sign(s, "UTF-8").toUpperCase();
//		logger.debug(s);
//		Map<String, String> params = HttpUtil.getUrlParams(s);
//		params.put("action", "cancel_entrust");
//		params.remove("secret_key");
//		String r = HttpUtil.post(url, params);
//		logger.debug("获取结果 " + r);
//		JSONObject appBack = JSON.parseObject(r);
//		if (!"200".equals(appBack.getString("code"))) {
//			return new AppBack(-1, "错误 错误编码: " + appBack.getString("msg"));
//		}
		return new AppBack(-1,"已撤销，但是根据币赢要求，撤销成功与否需要查询币赢官网");
	}

}
