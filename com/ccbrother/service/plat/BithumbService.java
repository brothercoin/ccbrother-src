package com.hykj.ccbrother.service.plat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hykj.ccbrother.apimodel.CoinInfo;
import com.hykj.ccbrother.apimodel.OrderInfo;
import com.hykj.ccbrother.apimodel.UserInfo;
import com.hykj.ccbrother.base.AppBack;
import com.hykj.ccbrother.model.CoinPlatModel;
import com.hykj.ccbrother.utils.Encrypt;
import com.hykj.ccbrother.utils.HttpUtil;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
public class BithumbService implements  PlatService {

    private static final Logger logger = LoggerFactory.getLogger(BithumbService.class);

    private final String baseUrl = "https://api.bithumb.com";
    
    @Override
    public CoinPlatModel getTicker(CoinPlatModel coinPlatModel) {
        return null;
    }

    @Override
    public List<CoinPlatModel> getAllTicker(List<CoinPlatModel> list) {

    	String endpoint = "/public/ticker/all";
    	
        String url = baseUrl + endpoint;
        String r = HttpUtil.get(url , null);
        logger.info(r);
        JSONObject apiBack = JSON.parseObject(r);
        JSONObject data = apiBack.getJSONObject("data");
        List newList=new ArrayList<CoinPlatModel>();
        logger.info("dateTime "+data.getLong("date"));
        Date date= new Date(data.getLong("date"));
        logger.info("date "+date.getTime());
        for(int i=0;i<list.size();i++) {
            try {
                CoinPlatModel newCoinPlat = new CoinPlatModel();
                newCoinPlat.setId(list.get(i).getId());
                JSONObject coinJson = data.getJSONObject(list.get(i).getSymbol());
                newCoinPlat.setTradingTime(date);
                newCoinPlat.setBuy(coinJson.getBigDecimal("buy_price"));
                newCoinPlat.setHigh(coinJson.getBigDecimal("max_price"));
                newCoinPlat.setLow(coinJson.getBigDecimal("min_price"));
                newCoinPlat.setSell(coinJson.getBigDecimal("sell_price"));
                newCoinPlat.setVol(coinJson.getBigDecimal("volume_1day"));
                newCoinPlat.setLast(coinJson.getBigDecimal("buy_price"));
                newList.add(newCoinPlat);
            }catch (Exception e){
                logger.error(e.getMessage());
            }

        }
        return newList;
    }

    @Override
    public AppBack trade(String apiKey, String secret, String symbol, int type, BigDecimal price, BigDecimal amount) {
        return null;
    }

    @Override
    public UserInfo getUserInfo(String apiKey, String secret, int platId) {
    	
    	String endpoint = "/info/balance";
		Map<String, String> params = new HashMap<String, String>();
		params.put("endpoint", endpoint);//参数
		String str = "endpoint=" + endpoint + "&currency=all";//要转码的参数字符串(目前bitHumb的字符串不按照字典排序，只能先手动拼装)

		HashMap<String, String> header = headerAndSign(apiKey, secret,
				endpoint, str);
		String url = baseUrl + endpoint;
		String r = HttpUtil.post(url, params, header);
		logger.info("getUserInfo:" + r);
		
		JSONObject apiBack = JSON.parseObject(r);
		UserInfo userInfo = new UserInfo();
		if(!"0000".equals(apiBack.getString("status"))){//状态码除了0000都是异常
			return userInfo;
		}
		
		Map result = apiBack.getJSONObject("data");
		Iterator<Map.Entry<String, String>> it = result.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, String> entry = it.next();
			if(!entry.getKey().contains("total_") && !entry.getKey().contains("in_use_") 
					&& !entry.getKey().contains("available_")){//格式比较特殊，所有货币资产全在一个json对象中，需要一个个匹配
				continue;
			}
			CoinInfo coinInfo = new CoinInfo();
			coinInfo.setName(entry.getKey());
		}
		
        return userInfo;
    }

    @Override
    public List<OrderInfo> getOrderInfo(String apiKey, String secret, Integer coinPlatId, String symbol) {
        return null;
    }

    @Override
    public AppBack cancelOrder(String apiKey, String secret, String orderId, String symbol) {
        return null;
    }
    
    
    /**
     * 生成请求头以及对需要的数据进行签名
     * @param apiKey
     * @param secret
     * @param endpoint
     * @param str
     * @return
     */
    private HashMap<String, String> headerAndSign(String apiKey, String secret,
			String endpoint, String str) {
		String nNonce = System.currentTimeMillis() + "";
		str = encodeURIComponent(str);//url编码
		str = endpoint + ";"	+ str + ";" + nNonce;//需要签名的字符串
		byte[] encryptData = Encrypt.hmacEncrypt1(str, secret, "HmacSHA512", "UTF-8");
		String encrypt = asHex(encryptData);
		HashMap<String, String> header = new HashMap<String, String>();//设置请求头
		header.put("Api-Key", apiKey);
		header.put("Api-Sign", encrypt);
		header.put("Api-Nonce", nNonce);
		header.put("api-client-type", "2");
		return header;
	}
    
    //符合bitHumb标准的转码
    public String encodeURIComponent(String s){
        String result = null;
        try{
          result = URLEncoder.encode(s, "UTF-8")
                             .replaceAll("\\+", "%20")
                             .replaceAll("\\%21", "!")
                             .replaceAll("\\%27", "'")
                             .replaceAll("\\%28", "(")
                             .replaceAll("\\%29", ")")
                             .replaceAll("\\%26", "&")
                             .replaceAll("\\%3D", "=")
                             .replaceAll("\\%7E", "~");
        } catch (UnsupportedEncodingException e){
          result = s;
        }
     
        return result;
      }
    
    //进行base64转码后再转成字符串
    private String asHex(byte[] encryptData) {
		
		byte[] hex = new Hex().encode( encryptData );
		String strData = new String(Base64.encodeBase64(hex));
		return strData;
	}
}
