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
import com.hykj.ccbrother.utils.Encrypt;
import com.hykj.ccbrother.utils.EncryptUtils;
import com.hykj.ccbrother.utils.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
public class KrakenService implements PlatService {

    private static final Logger logger = LoggerFactory.getLogger(KrakenService.class);

    @Override
    public CoinPlatModel getTicker(CoinPlatModel coinPlatModel) {
        String symbol = coinPlatModel.getSymbol();
        String url = "https://api.kraken.com/0/public/Ticker?pair=";
        String r = HttpUtil.get(url + symbol, null);
        logger.debug(r);
        CoinPlatModel newCoinPlat = new CoinPlatModel();
        newCoinPlat.setId(coinPlatModel.getId());
        JSONObject apiBack = JSON.parseObject(r);
        JSONObject result = apiBack.getJSONObject("result");
        JSONObject coinJson = result.getJSONObject(symbol);
        newCoinPlat.setTradingTime(new Date());
        newCoinPlat.setBuy(coinJson.getJSONArray("b").getBigDecimal(0));
        newCoinPlat.setHigh(coinJson.getJSONArray("h").getBigDecimal(1));
        newCoinPlat.setLast(coinJson.getJSONArray("c").getBigDecimal(0));
        newCoinPlat.setLow(coinJson.getJSONArray("l").getBigDecimal(1));
        newCoinPlat.setSell(coinJson.getJSONArray("a").getBigDecimal(0));
        newCoinPlat.setVol(coinJson.getJSONArray("v").getBigDecimal(1));
        return newCoinPlat;
    }

    @Override
    public List<CoinPlatModel> getAllTicker(List<CoinPlatModel> list) {
        StringBuilder symbol = new StringBuilder();
        for(int i=0;i<list.size();i++){
            if(i!=0){
                symbol.append(",");
            }
            symbol.append(list.get(i).getSymbol());
        }

        String url = "https://api.kraken.com/0/public/Ticker?pair=";
        logger.debug("getAllTicker "+url + symbol);
        String r = HttpUtil.get(url + symbol, null);
        logger.info("getAllTicker " + r);
        
        JSONObject apiBack = JSON.parseObject(r);
        JSONObject result = apiBack.getJSONObject("result");
        List newList=new ArrayList<CoinPlatModel>();
        for(int i=0;i<list.size();i++) {
            try {
                CoinPlatModel newCoinPlat = new CoinPlatModel();
                newCoinPlat.setId(list.get(i).getId());
                JSONObject coinJson = result.getJSONObject(list.get(i).getSymbol());
                if(coinJson != null){ //判断交易对是不是存在
                	 newCoinPlat.setTradingTime(new Date());
                     newCoinPlat.setBuy(coinJson.getJSONArray("b").getBigDecimal(0));
                     newCoinPlat.setHigh(coinJson.getJSONArray("h").getBigDecimal(1));
                     newCoinPlat.setLast(coinJson.getJSONArray("c").getBigDecimal(0));
                     newCoinPlat.setLow(coinJson.getJSONArray("l").getBigDecimal(1));
                     newCoinPlat.setSell(coinJson.getJSONArray("a").getBigDecimal(0));
                     newCoinPlat.setVol(coinJson.getJSONArray("v").getBigDecimal(1));
                     newList.add(newCoinPlat);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return newList;
    }

    @Override
    public AppBack trade(String apiKey, String secret, String symbol, int type, BigDecimal price, BigDecimal amount) {

        String url = "https://api.kraken.com/0/private/AddOrder";
        String path = "0/private/AddOrder";
        Map params = new HashMap();
        Map header = new HashMap();

        //String nonce = System.currentTimeMillis()+"";
        String nonce = "1234";
        params.put("nonce", nonce);
        params.put("pair", symbol);
        if(type==1){
            params.put("type", "buy");
        }else if(type==2){
            params.put("type", "sell");
        }

        params.put("ordertype", "market");
        params.put("price", price+"");
        params.put("volume", amount+"");
        String parmString= HttpUtil.getUrlParamsByMap(params);



        byte[] toHash=arraycat(path.getBytes(),Encrypt.SHA256_(nonce + parmString));
        byte[] key= org.apache.commons.codec.binary.Base64.decodeBase64(secret);

        byte[] res2 = getSignature( toHash,key);
        String signature = new String(Base64.getEncoder().encode(res2));

        header.put("API-Key", apiKey);
        header.put("API-Sign", signature);
        logger.debug("post   "+parmString);
        logger.debug("toHash "+new String(toHash));
        logger.debug("key "+new String(key));
        logger.debug("sign "+byte2hex(res2));
        logger.debug("sign "+new String(res2));
        logger.debug("API-Sign "+signature);
        String r = HttpUtil.post(url, parmString, header);
        logger.debug("totrade "+r);

        return null;
    }

    @Override
    public UserInfo getUserInfo(String apiKey, String secret, int platId) {
        String url = "https://api.kraken.com/0/private/Balance";
        String path = "/0/private/Balance";
        Map params = new HashMap();
        Map header = new HashMap();

        String nonce = System.currentTimeMillis()+"";
        params.put("nonce", nonce);

        byte[] toHash=arraycat(path.getBytes(),Encrypt.SHA256_(nonce + HttpUtil.getUrlParamsByMap(params)));
        byte[] key= org.apache.commons.codec.binary.Base64.decodeBase64(secret);

        byte[] res2 = getSignature( toHash,key);
        String signature = new String(Base64.getEncoder().encode(res2));

        header.put("API-Key", apiKey);
        header.put("API-Sign", signature);



        String r = HttpUtil.post(url, HttpUtil.getUrlParamsByMap(params), header);
        logger.debug("getUserInfo " + r);

        JSONObject apiBack=JSON.parseObject(r);

        JSONArray error = apiBack.getJSONArray("error");
        if(error.size()>0){
            throw new MsgException(-1,error.getString(0));
        }
        UserInfo userInfo = new UserInfo();
        Map result = apiBack.getJSONObject("result");
        Iterator<Map.Entry<String, String>> it = result.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            System.out.println("key= " + entry.getKey() + " and value= " + entry.getValue());
            CoinInfo coinInfo = new CoinInfo();
            coinInfo.setName(entry.getKey());
            coinInfo.setAmount(new BigDecimal(entry.getValue()));
            coinInfo.setPlatId(platId);
            userInfo.getFreeCoinList().add(coinInfo);
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
     * 生成签名数据
     *
     * @param data 待加密的数据
     * @param key  加密使用的key
     * @return 生成MD5编码的字符串
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     */
    public static byte[] getSignature(byte[] data, byte[] key)  {
        try {
            byte[] keyBytes = key;
            SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA512");

            // Get an hmac_sha1 Mac instance and initialize with the signing key
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(signingKey);

            byte[] rawHmac = mac.doFinal(data);
            logger.debug("rawHmac "+new String(rawHmac));
            return rawHmac;
        }catch (Exception e){
            throw new MsgException("编码错误");
        }
    }

    byte[] arraycat(byte[] buf1,byte[] buf2)
    {
        byte[] bufret=null;
        int len1=0;
        int len2=0;
        if(buf1!=null)
            len1=buf1.length;
        if(buf2!=null)
            len2=buf2.length;
        if(len1+len2>0)
            bufret=new byte[len1+len2];
        if(len1>0)
            System.arraycopy(buf1,0,bufret,0,len1);
        if(len2>0)
            System.arraycopy(buf2,0,bufret,len1,len2);
        return bufret;
    }

    static String byte2hex(final byte[] b)  {
        String hs = "";
        String stmp = "";
        for (int n = 0; n < b.length; n++) {
            stmp = (java.lang.Integer.toHexString(b[n] & 0xFF));

            if (stmp.length() == 1) {
                hs = hs + "0" + stmp;
            } else hs = hs + stmp;
        }


            return hs;


        //     return hs;
    }
}
