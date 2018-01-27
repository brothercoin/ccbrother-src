package com.hykj.ccbrother.service.plat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hykj.ccbrother.apimodel.CoinInfo;
import com.hykj.ccbrother.apimodel.OrderInfo;
import com.hykj.ccbrother.apimodel.UserInfo;
import com.hykj.ccbrother.base.AppBack;
import com.hykj.ccbrother.model.CoinPlatModel;
import com.hykj.ccbrother.utils.HttpUtil;
import com.hykj.ccbrother.utils.MD5;
import com.hykj.ccbrother.utils.StringUtil;
import com.okcoin.rest.MD5Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 国际站 apiKey:  4ccd9e4c-a2b7-4639-9b4f-2a18bd02afda
 * 国际站 secretKey:  D9BFA5239FA4B56BDF918235E11BECE6
 */
@Service
public class OkexService implements PlatService {

    private static final Logger logger = LoggerFactory.getLogger(OkexService.class);

    @Override
    public CoinPlatModel getTicker(CoinPlatModel coinPlatModel) {

        String symbol = coinPlatModel.getSymbol();
        String url = "https://www.okex.com/api/v1/ticker.do?symbol=";
        logger.debug(url + symbol);
        String r = HttpUtil.get(url + symbol, null);

        logger.debug(r);
        CoinPlatModel newCoinPlat = new CoinPlatModel();
        newCoinPlat.setId(coinPlatModel.getId());
        JSONObject apiBack = JSON.parseObject(r);
        //时间戳转化为Sting或Date
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Long time = new Long(apiBack.getString("date"));

        String d = format.format(time * 1000);
        Date date = null;
        try {
            date = format.parse(d);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        JSONObject ticker = apiBack.getJSONObject("ticker");
        newCoinPlat.setTradingTime(date);
        newCoinPlat.setBuy(new BigDecimal(ticker.getString("buy")));
        newCoinPlat.setHigh(new BigDecimal(ticker.getString("high")));
        newCoinPlat.setLast(new BigDecimal(ticker.getString("last")));
        newCoinPlat.setLow(new BigDecimal(ticker.getString("low")));
        newCoinPlat.setSell(new BigDecimal(ticker.getString("sell")));
        newCoinPlat.setVol(new BigDecimal(ticker.getString("vol")));
        return newCoinPlat;
    }

    @Override
    public List<CoinPlatModel> getAllTicker(List<CoinPlatModel> list) {
        return null;
    }

    @Override
    //访问频率 20次/2秒
    public AppBack trade(String apiKey, String secret, String symbol, int type, BigDecimal price, BigDecimal amount) {
        String url = "https://www.okex.com/api/v1/trade.do";
        Map params = new HashMap();
        params.put("api_key", apiKey);
        params.put("symbol", symbol);
        switch (type) {
            case 1:
                params.put("type", "buy");
                break;
            case 2:
                params.put("type", "sell");
                break;
        }

        params.put("price", price.toString());
        params.put("amount", amount.toString());

        String sign = MD5Util.buildMysignV1(params, secret);
        params.put("sign", sign);

        logger.info(JSON.toJSONString(params));
        String r = HttpUtil.post(url, params);
        logger.info(r);
        JSONObject apiBack = JSON.parseObject(r);
        Boolean result = apiBack.getBoolean("result");
        if (result == null || result == false) {
            String error_code = apiBack.getString("error_code");
            return new AppBack(-1, "交易错误 错误编码: " + error_code);
        }
        
        JSONArray orderInfo = apiBack.getJSONArray("order_info");
        String orderId = orderInfo.getJSONObject(0).getString("order_id");
        return new AppBack().add("orderId", orderId);
    }

    @Override
    public UserInfo getUserInfo(String apiKey, String secret, int platId) {
        String url = "https://www.okex.com/api/v1/userinfo.do";

        // 构造参数签名
        Map<String, String> params = new HashMap<String, String>();
        params.put("api_key", apiKey);
        String sign = MD5Util.buildMysignV1(params, secret);
        params.put("sign", sign);
        logger.info(JSON.toJSONString(params));
        String r = HttpUtil.post(url, params);
        logger.info(r);
        JSONObject apiBack = JSON.parseObject(r);
        UserInfo userInfo = new UserInfo();
        if (!apiBack.getBoolean("result")) {
            logger.debug("获取失败 " + r);
            return userInfo;
        }

        JSONObject info = apiBack.getJSONObject("info");
        JSONObject funds = info.getJSONObject("funds");
        Map free = funds.getJSONObject("free");
        Iterator<Map.Entry<String, String>> it = free.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            System.out.println("key= " + entry.getKey() + " and value= " + entry.getValue());
            CoinInfo coinInfo = new CoinInfo();
            coinInfo.setName(entry.getKey());
            coinInfo.setAmount(new BigDecimal(entry.getValue()));
            coinInfo.setPlatId(platId);
            if (0 != coinInfo.getAmount().compareTo(BigDecimal.ZERO)) {
                userInfo.getFreeCoinList().add(coinInfo);
            }
        }
        Map freezed = funds.getJSONObject("freezed");
        it = freezed.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            System.out.println("key= " + entry.getKey() + " and value= " + entry.getValue());
            CoinInfo coinInfo = new CoinInfo();
            coinInfo.setName(entry.getKey());
            coinInfo.setAmount(new BigDecimal(entry.getValue()));
            coinInfo.setPlatId(platId);
            if (0 != coinInfo.getAmount().compareTo(BigDecimal.ZERO)) {
                userInfo.getFreezedCoinList().add(coinInfo);
            }
        }
        return userInfo;
    }

    /**
     * api_key
     * String
     * 是
     * 用户申请的apiKey
     * symbol
     * String
     * 是
     * ltc_btc eth_btc etc_btc bch_btc btc_usdt eth_usdt ltc_usdt etc_usdt bch_usdt etc_eth bt1_btc bt2_btc btg_btc qtum_btc hsr_btc neo_btc gas_btc qtum_usdt hsr_usdt neo_usdt gas_usdt
     * order_id
     * Long
     * 是
     * 订单ID -1:未完成订单，否则查询相应订单号的订单
     * sign
     * String
     * 是
     * 请求参数的签名
     * <p>
     * <p>
     * amount:委托数量
     * create_date: 委托时间
     * avg_price:平均成交价
     * deal_amount:成交数量
     * order_id:订单ID
     * orders_id:订单ID(不建议使用)
     * price:委托价格
     * status:-1:已撤销  0:未成交  1:部分成交  2:完全成交 4:撤单处理中
     * type:buy_market:市价买入 / sell_market:市价卖出
     *
     * @param apiKey
     * @param secret
     * @param coinPlatId
     * @param symbol
     * @return
     */
    @Override
    public List<OrderInfo> getOrderInfo(String apiKey, String secret, Integer coinPlatId, String symbol) {
        String url = "https://www.okex.com/api/v1/order_info.do";
        Map params = new HashMap();
        params.put("api_key", apiKey);
        if (!StringUtil.isEmptyString(symbol)) {
            params.put("symbol", symbol);
        }
        params.put("order_id", -1);
        String s = HttpUtil.sortMap(params);
        String s1 = s + "&secret_key=" + secret;
        String sign = MD5.sign(s1, "UTF-8").toUpperCase();
        s += "&sign=" + sign;
        logger.debug(s);
        Map<String, String> map = HttpUtil.getUrlParams(s);
        String r = HttpUtil.post(url, map);
        logger.debug("获取结果 " + r);
        JSONObject apiBack = JSON.parseObject(r);
        List orderList = new ArrayList();
        if (!apiBack.getBoolean("result")) {
            return orderList;
        }
        JSONArray orders = apiBack.getJSONArray("orders");
        for (int i = 0; i < orders.size(); i++) {
            JSONObject order = orders.getJSONObject(i);
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setAmount(order.getBigDecimal("amount"));
            orderInfo.setCreateDate(new Date(order.getLong("create_data")));
            orderInfo.setCoinPlatId(coinPlatId);
            orderInfo.setSymbol(symbol);
            if ("buy".equals(order.getString("type"))) {
                orderInfo.setType(1);
            } else if ("sell".equals(order.getString("type"))) {
                orderInfo.setType(2);
            }
            orderInfo.setSymbol(order.getString("symbol"));
            orderInfo.setPrice(order.getBigDecimal("price"));
            orderInfo.setStatus(order.getInteger("status"));
            orderInfo.setDealAmount(order.getBigDecimal("deal_amount"));
            orderInfo.setOrderId(order.getString("order_id"));
            orderList.add(orderInfo);

        }
        return orderList;
    }

    @Override
    public AppBack cancelOrder(String apiKey, String secret, String orderId, String symbol) {

        String url = "https://www.okex.com/api/v1/cancel_order.do";
        Map params = new HashMap();
        params.put("api_key", apiKey);
        params.put("symbol", symbol);
        params.put("order_id", orderId);

        String s = HttpUtil.sortMap(params);
        String s1 = s + "&secret_key=" + secret;
        String sign = MD5.sign(s1, "UTF-8").toUpperCase();
        s += "&sign=" + sign;
        Map<String, String> map = HttpUtil.getUrlParams(s);
        String r = HttpUtil.post(url, map);
        logger.info("cancelOrder "+r);
        JSONObject apiBack = JSON.parseObject(r);
        Boolean result = apiBack.getBoolean("result");
        logger.debug("返回结果 " + r);
        if (result==null||!result) {
            String error_code = apiBack.getString("error_code");
            return new AppBack(-1, "错误 错误编码: " + error_code);
        }

        return new AppBack();
    }
}
/**
 * 错误代码（现货）
 * 错误代码	详细描述
 * 10000	必选参数不能为空
 * 10001	用户请求频率过快，超过该接口允许的限额
 * 10002	系统错误
 * 10004	请求失败
 * 10005	SecretKey不存在
 * 10006	Api_key不存在
 * 10007	签名不匹配
 * 10008	非法参数
 * 10009	订单不存在
 * 10010	余额不足
 * 10011	买卖的数量小于BTC/LTC最小买卖额度
 * 10012	当前网站暂时只支持btc_usd ltc_usd
 * 10013	此接口只支持https请求
 * 10014	下单价格不得≤0或≥1000000
 * 10015	下单价格与最新成交价偏差过大
 * 10016	币数量不足
 * 10017	API鉴权失败
 * 10018	借入不能小于最低限额[usd:100,btc:0.1,ltc:1]
 * 10019	页面没有同意借贷协议
 * 10020	费率不能大于1%
 * 10021	费率不能小于0.01%
 * 10023	获取最新成交价错误
 * 10024	可借金额不足
 * 10025	额度已满，暂时无法借款
 * 10026	借款(含预约借款)及保证金部分不能提出
 * 10027	修改敏感提币验证信息，24小时内不允许提现
 * 10028	提币金额已超过今日提币限额
 * 10029	账户有借款，请撤消借款或者还清借款后提币
 * 10031	存在BTC/LTC充值，该部分等值金额需6个网络确认后方能提出
 * 10032	未绑定手机或谷歌验证
 * 10033	服务费大于最大网络手续费
 * 10034	服务费小于最低网络手续费
 * 10035	可用BTC/LTC不足
 * 10036	提币数量小于最小提币数量
 * 10037	交易密码未设置
 * 10040	取消提币失败
 * 10041	提币地址不存在或未认证
 * 10042	交易密码错误
 * 10043	合约权益错误，提币失败
 * 10044	取消借款失败
 * 10047	当前为子账户，此功能未开放
 * 10048	提币信息不存在
 * 10049	小额委托（<0.15BTC)的未成交委托数量不得大于50个
 * 10050	重复撤单
 * 10052	提币受限
 * 10064	美元充值后的48小时内，该部分资产不能提出
 * 10100	账户被冻结
 * 10101	订单类型错误
 * 10102	不是本用户的订单
 * 10103	私密订单密钥错误
 * 10216	非开放API
 * 1002	交易金额大于余额
 * 1003	交易金额小于最小交易值
 * 1004	交易金额小于0
 * 1007	没有交易市场信息
 * 1008	没有最新行情信息
 * 1009	没有订单
 * 1010	撤销订单与原订单用户不一致
 * 1011	没有查询到该用户
 * 1013	没有订单类型
 * 1014	没有登录
 * 1015	没有获取到行情深度信息
 * 1017	日期参数错误
 * 1018	下单失败
 * 1019	撤销订单失败
 * 1024	币种不存在
 * 1025	没有K线类型
 * 1026	没有基准币数量
 * 1027	参数不合法可能超出限制
 * 1028	保留小数位失败
 * 1029	正在准备中
 * 1030	有融资融币无法进行交易
 * 1031	转账余额不足
 * 1032	该币种不能转账
 * 1035	密码不合法
 * 1036	谷歌验证码不合法
 * 1037	谷歌验证码不正确
 * 1038	谷歌验证码重复使用
 * 1039	短信验证码输错限制
 * 1040	短信验证码不合法
 * 1041	短信验证码不正确
 * 1042	谷歌验证码输错限制
 * 1043	登陆密码不允许与交易密码一致
 * 1044	原密码错误
 * 1045	未设置二次验证
 * 1046	原密码未输入
 * 1048	用户被冻结
 * 1201	账号零时删除
 * 1202	账号不存在
 * 1203	转账金额大于余额
 * 1204	不同种币种不能转账
 * 1205	账号不存在主从关系
 * 1206	提现用户被冻结
 * 1207	不支持转账
 * 1208	没有该转账用户
 * 1209	当前api不可用
 * 1216	市价交易暂停，请选择限价交易
 * 1217	您的委托价格超过最新成交价的±5%，存在风险，请重新下单
 * 1218	下单失败，请稍后再试
 * HTTP错误码403	用户请求过快，IP被屏蔽
 * Ping不通	用户请求过快，IP被屏蔽
 * 错误代码（合约）
 * 错误代码	详细描述
 * 20001	用户不存在
 * 20002	用户被冻结
 * 20003	用户被爆仓冻结
 * 20004	合约账户被冻结
 * 20005	用户合约账户不存在
 * 20006	必填参数为空
 * 20007	参数错误
 * 20008	合约账户余额为空
 * 20009	虚拟合约状态错误
 * 20010	合约风险率信息不存在
 * 20011	10倍/20倍杠杆开BTC前保证金率低于90%/80%，10倍/20倍杠杆开LTC前保证金率低于80%/60%
 * 20012	10倍/20倍杠杆开BTC后保证金率低于90%/80%，10倍/20倍杠杆开LTC后保证金率低于80%/60%
 * 20013	暂无对手价
 * 20014	系统错误
 * 20015	订单信息不存在
 * 20016	平仓数量是否大于同方向可用持仓数量
 * 20017	非本人操作
 * 20018	下单价格高于前一分钟的103%或低于97%
 * 20019	该IP限制不能请求该资源
 * 20020	密钥不存在
 * 20021	指数信息不存在
 * 20022	接口调用错误（全仓模式调用全仓接口，逐仓模式调用逐仓接口）
 * 20023	逐仓用户
 * 20024	sign签名不匹配
 * 20025	杠杆比率错误
 * 20026	API鉴权错误
 * 20027	无交易记录
 * 20028	合约不存在
 * 20029	转出金额大于可转金额
 * 20030	账户存在借款
 * 20038	根据相关法律，您所在的国家或地区不能使用该功能。
 * 20049	用户请求接口过于频繁
 * HTTP错误码403	用户请求过快，IP被屏蔽
 * Ping不通	用户请求过快，IP被屏蔽
 */