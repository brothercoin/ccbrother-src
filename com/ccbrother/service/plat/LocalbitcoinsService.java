package com.hykj.ccbrother.service.plat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hykj.ccbrother.apimodel.CoinInfo;
import com.hykj.ccbrother.apimodel.OrderInfo;
import com.hykj.ccbrother.apimodel.UserInfo;
import com.hykj.ccbrother.base.AppBack;
import com.hykj.ccbrother.base.MsgException;
import com.hykj.ccbrother.model.CoinPlatModel;
import com.hykj.ccbrother.utils.Encrypt;
import com.hykj.ccbrother.utils.HttpUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * 因为api与我们冲突太严重，可以看资产但是不能下单，不能看订单记录
 * @author 封君
 *
 */
@Service
public class LocalbitcoinsService implements PlatService {

	private static final Logger logger = LoggerFactory
			.getLogger(LocalbitcoinsService.class);

	@Override
	public CoinPlatModel getTicker(CoinPlatModel coinPlatModel) {

		return null;
	}

	@Override
	public List<CoinPlatModel> getAllTicker(List<CoinPlatModel> list) {
		String url = "https://localbitcoins.com//bitcoinaverage/ticker-all-currencies";
		logger.debug(url);
		String r = HttpUtil.get(url, null);//访问可能会返回500状态码
		logger.debug("getAllTicker" + r);
		JSONObject apiBack = JSON.parseObject(r);//会报一个错，但是下次访问还会正常显示
		List newList = new ArrayList<CoinPlatModel>();
		for (int i = 0; i < list.size(); i++) {
			JSONObject coinPlatJson = apiBack.getJSONObject(list.get(i)
					.getSymbol());
			if (coinPlatJson != null) {
				CoinPlatModel newCoinPlat = new CoinPlatModel();
				newCoinPlat.setId(list.get(i).getId());
				newCoinPlat.setVol(coinPlatJson.getBigDecimal("volume_btc"));
				newCoinPlat.setLast(coinPlatJson.getJSONObject("rates")
						.getBigDecimal("last"));
				newCoinPlat.setTradingTime(new Date());
				newList.add(newCoinPlat);
			}

		}
		logger.debug("newList" + JSON.toJSONString(newList));
		return newList;
	}

	@Override
	public AppBack trade(String apiKey, String secret, String symbol, int type,
			BigDecimal price, BigDecimal amount) {
		return null;
	}

	@Override
	public UserInfo getUserInfo(String apiKey, String secret, int platId) {

		String path = "/api/wallet-balance/";
		String url = "https://localbitcoins.com" + path;
		String nonce = System.currentTimeMillis() + "";
		String s = nonce + apiKey + path;// 要加密的数据，还有一个params字符串，但是为空就不填了
		String signature = Encrypt
				.hmacEncrypt(s, secret, "HmacSHA256", "UTF-8");

		Map header = new HashMap();
		header.put("Content-Type", "application/x-www-form-urlencoded");
		header.put("Apiauth-Key", apiKey);
		header.put("Apiauth-Nonce", nonce);
		header.put("Apiauth-Signature", signature);
		String r = HttpUtil.get(url, null,header);
		logger.info("getUserInfo " + r);
		
		JSONObject apiBack = JSON.parseObject(r);
		if(!apiBack.getString("error").isEmpty()){
			return null;
		}
		JSONObject data = apiBack.getJSONObject("data");
		UserInfo userInfo = new UserInfo();
		CoinInfo coinInfo = new CoinInfo();
		coinInfo.setPlatId(platId);
		coinInfo.setName("btc");
		coinInfo.setAmount(data.getJSONObject("total").getBigDecimal("balance"));
		userInfo.getFreeCoinList().add(coinInfo);
		
		userInfo.setPlatId(platId);
		return userInfo;
	}

	@Override
	public List<OrderInfo> getOrderInfo(String apiKey, String secret,
			Integer coinPlatId, String symbol) {
		
		return null;
	}

	@Override
	public AppBack cancelOrder(String apiKey, String secret, String orderId,
			String symbol) {
		return null;
	}
}
