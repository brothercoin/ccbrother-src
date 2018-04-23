package com.hykj.ccbrother.service.plat;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
/**
 * 查不到历史订单，api问题
 * @author 封君
 *
 */
@Service
public class PoloniexService implements  PlatService {

    private static final Logger logger = LoggerFactory.getLogger(PoloniexService.class);
    private static final String postUrl = "https://poloniex.com/tradingApi";
    
    
    @Override
    public CoinPlatModel getTicker(CoinPlatModel coinPlatModel) {
        return null;
    }

    @Override
    public List<CoinPlatModel> getAllTicker(List<CoinPlatModel> list) {
        String url = "https://poloniex.com/public?command=returnTicker";


        String r = HttpUtil.get(url , null);
        logger.debug("getAllTicker"+r);
        JSONObject apiBack = JSON.parseObject(r);
        List newList=new ArrayList<CoinPlatModel>();
        for (int i=0;i<list.size();i++){
            JSONObject coinPlatJson=apiBack.getJSONObject(list.get(i).getSymbol());
            CoinPlatModel newCoinPlat = new CoinPlatModel();
            newCoinPlat.setId(list.get(i).getId());
            newCoinPlat.setBuy(coinPlatJson.getBigDecimal("highestBid"));
            newCoinPlat.setHigh(coinPlatJson.getBigDecimal("high24hr"));
            newCoinPlat.setLow(coinPlatJson.getBigDecimal("low24hr"));
            newCoinPlat.setSell(coinPlatJson.getBigDecimal("lowestAsk"));
            newCoinPlat.setVol(coinPlatJson.getBigDecimal("baseVolume"));
            newCoinPlat.setLast(coinPlatJson.getBigDecimal("last"));
            newCoinPlat.setIncrease(coinPlatJson.getBigDecimal("percentChange").multiply(new BigDecimal("100")));
            newCoinPlat.setTradingTime(new Date());
            newList.add(newCoinPlat);
        }
        logger.debug("newList"+JSON.toJSONString(newList));
        return newList;
    }

    @Override
    public AppBack trade(String apiKey, String secret, String symbol, int type, BigDecimal price, BigDecimal amount) {
    	
    	String nonce = System.currentTimeMillis() + "";
    	String command = null;
    	switch (type) {
			case 1:
				command = "buy";
				break;
			case 2:
				command = "sell";
				break;
			}
		String params = "nonce=" + nonce + "&command=" + command + "&currencyPair=" + symbol + "&rate=" + price + "&amount=" + amount ;
		Map<String, String> header = getHeader(apiKey, secret, params);
		String r = HttpUtil.post(postUrl,params,header);
		logger.info(r);
		JSONObject appBack = JSON.parseObject(r);
		if(r.contains("error")){
			return new AppBack(-1, appBack.getString("eerror"));
		}
        return new AppBack().add("orderId", appBack.getString("orderNumber"));
    }

    @Override
    public UserInfo getUserInfo(String apiKey, String secret, int platId) {
    	
    	String nonce = System.currentTimeMillis() + "";
    	String command = "returnCompleteBalances";
		String params = "nonce=" + nonce + "&command=" + command + "&account=all";
		Map<String, String> header = getHeader(apiKey, secret, params);
		String r = HttpUtil.post(postUrl,params,header);
		logger.info("getUserInfo:" + r);
		UserInfo userInfo = new UserInfo();
		if(r.contains("error")){
			return userInfo;
		}
		Map appBack = JSON.parseObject(r);
        Iterator<Map.Entry<String, JSONObject>> it = appBack.entrySet().iterator();
        while (it.hasNext()){
        	Map.Entry<String, JSONObject> entry = it.next();
        	JSONObject value = entry.getValue();
        	if(value.getBigDecimal("available").compareTo(BigDecimal.ZERO) > 0){
        		CoinInfo coinInfo = new CoinInfo();
            	coinInfo.setName(entry.getKey());
            	coinInfo.setAmount(value.getBigDecimal("available"));
                userInfo.getFreeCoinList().add(coinInfo);
        	}
        	if(value.getBigDecimal("onOrders").compareTo(BigDecimal.ZERO) > 0){
        		CoinInfo coinInfo = new CoinInfo();
            	coinInfo.setName(entry.getKey());
            	coinInfo.setAmount(value.getBigDecimal("onOrders"));
                userInfo.getFreezedCoinList().add(coinInfo);
        	}
        }
        userInfo.setPlatId(platId);
		return userInfo;
    }

    @Override
    public List<OrderInfo> getOrderInfo(String apiKey, String secret, Integer coinPlatId, String symbol) {
      
    	String nonce = System.currentTimeMillis() + "";
    	String command = "returnTradeHistory";
    	String start = System.currentTimeMillis() - 1000 * 60 * 60 + 24 * 360 + "";//用户一年的账单  但是好像不起作用，只能拿一天的
    	String end = nonce;
    	String params = "nonce=" + nonce + "&command=" + command + "&currencyPair=" + symbol +"&start=" +start +"&end=" +end ;
		Map<String, String> header = getHeader(apiKey, secret, params);
		String r = HttpUtil.post(postUrl,params,header);
		List<OrderInfo> orderList = new ArrayList<OrderInfo>();
		if(r.contains("error") || r.length() == 2){ //返回的要么是错误，要么是空[]
			return orderList;
		}
		JSONArray appBack = JSON.parseArray(r);
		for (int i = 0; i < appBack.size(); i++) {
			JSONObject order = appBack.getJSONObject(i);
			OrderInfo orderInfo = new OrderInfo();
			orderInfo.setAmount(order.getBigDecimal("amount"));
			orderInfo.setCoinPlatId(coinPlatId);
			orderInfo.setCreateDate(order.getDate("date"));
			orderInfo.setOrderId(order.getString("orderNumber"));
			orderInfo.setSymbol(symbol);
			orderInfo.setPrice(order.getBigDecimal("rate"));
			if("sell".equals(order.getString("type"))){
				orderInfo.setType(2);
			} else if("buy".equals(order.getString("type"))){
				orderInfo.setType(1);
			}
			orderInfo.setStatus(2);//只有这一种状态
			orderList.add(orderInfo);
		}
    	return orderList;
    }

    @Override
    public AppBack cancelOrder(String apiKey, String secret, String orderId, String symbol) {
    
    	String nonce = System.currentTimeMillis() + "";
    	String command = "cancelOrder";
		String params = "nonce=" + nonce + "&command=" + command + "&orderNumber=" + orderId;
		Map<String, String> header = getHeader(apiKey, secret, params);
		String r = HttpUtil.post(postUrl,params,header);
		logger.info(r);
		JSONObject appBack = JSON.parseObject(r);
		if(r.contains("error")){
			return new AppBack(-1,appBack.getString("error"));
		}
        return new AppBack();
    }

    /**
     * 添加请求头里，并且进行签名认证
     * @param apiKey
     * @param secret
     * @param params
     * @return
     */
	private Map<String, String> getHeader(String apiKey, String secret,
			String params) {
		Map<String, String> header = new HashMap<>();
		header.put("Key", apiKey);
		header.put("Content-Type", "application/x-www-form-urlencoded");
		header.put("Sign",	Encrypt.hmacEncrypt(params, secret, "HmacSHA512", "UTF-8").toLowerCase());//签名的顺序必须与发送时一样	
		return header;
	}
}
