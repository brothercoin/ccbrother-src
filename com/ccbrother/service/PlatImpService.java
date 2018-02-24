package com.hykj.ccbrother.service;

import com.alibaba.fastjson.JSON;
import com.hykj.ccbrother.apimodel.CoinInfo;
import com.hykj.ccbrother.apimodel.OrderInfo;
import com.hykj.ccbrother.apimodel.UserInfo;
import com.hykj.ccbrother.base.AppBack;
import com.hykj.ccbrother.base.MsgException;
import com.hykj.ccbrother.model.*;
import com.hykj.ccbrother.service.plat.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Future;

/**
 * 交易服务类
 *
 * @author innel
 * @email 2638086622@qq.com
 * @date 2017-11-09 18:04:40
 */
@Service
public class PlatImpService implements Plat {

    private static final Logger logger = LoggerFactory
            .getLogger(PlatImpService.class);

    @Autowired
    MoneyHistoryService moneyHistoryService;

    @Autowired
    HedgingService hedgingService;

    @Autowired
    CoinService coinService;

    @Autowired
    CoinPlatService coinPlatService;

    @Autowired
    UserConfigPlatService userConfigPlatService;

    @Autowired
    UserConfigPlatCoinService userConfigPlatCoinService;

    @Autowired
    OrderService orderService;

    @Autowired
    UserService userService;

    @Autowired
    TradingPlatformService tradingPlatformService;

    @Autowired
    ExchangeRateService exchangeRateService;

    @Autowired
    OtcBtcService otcBtcService; //1
    
    @Autowired
    OkexService okexService; // 2

    @Autowired
    AllcoinService allcoinService;// 3

    @Autowired
    KrakenService krakenService;// 4

    @Autowired
    BithumbService bithumbService;// 5

    @Autowired
    BittrexService bittrexService; // 6

    @Autowired
    HitbtcService hitbtcService; // 7

    @Autowired
    CryptopiaService cryptopiaService; // 8

    @Autowired
    BitstampService bitstampService; // 9

    @Autowired
    LiquiService liquiService; // 10

    @Autowired
    LivecoinService livecoinService; // 11

    @Autowired
    QuadrigacxService quadrigacxService; // 12

    @Autowired
    GdaxService gdaxService; // 13

    @Autowired
    BitflyerService bitflyerService; // 14

    @Autowired
    CoincheckService coincheckService; // 15

    @Autowired
    ZaifService zaifService; // 16

    @Autowired
    CoinoneService coinoneService;// 17

    @Autowired
    KorbitService korbitService;// 18

    @Autowired
    QuoineService quoineService;// 19

    @Autowired
    GeminiService geminiService;// 20

    @Autowired
    PoloniexService poloniexService;// 21

    @Autowired
    LocalbitcoinsService localbitcoinsService;// 22

    @Autowired
    BitfinexService bitfinexService;// 23

    @Autowired
    BinanceService binanceService;// 24

    @Autowired
    GateService gateService;// 25

    @Autowired
    ExxService exxService;// 26

    @Autowired
    BitzService bitzService;// 27

    @Autowired
    CoinwService coinwService;// 28

    @Autowired
    HuobiService huobiService;// 29

    @Autowired
    HKSYService hksyService;// 30

    @Autowired
    KucoinService kucoinService;// 31

    @Autowired
    ZBService zbService;// 32

    /**
     * 根据交易所Id，选出具体的交易所，执行具体的方法
     *
     * @param platId
     * @return
     */
    public PlatService getPlatService(int platId) {
        switch (platId) {
        	case 1:
        		return otcBtcService;
            case 2:
                return okexService;
            case 3:
                return allcoinService;
            case 4:
                return krakenService;
            case 5:
                return bithumbService;
            case 6:
                return bittrexService;
            case 7:
                return hitbtcService;
            case 8:
                return cryptopiaService;
            case 9:
                return bitstampService;
            case 10:
                return liquiService;
            case 11:
                return livecoinService;
            case 12:
                return quadrigacxService;
            case 13:
                return gdaxService;
            case 14:
                return bitflyerService;
            case 15:
                return coincheckService;
            case 16:
                return zaifService;
            case 17:
                return coinoneService;
            case 18:
                return korbitService;
            case 19:
                return quoineService;
            case 20:
                return geminiService;
            case 21:
                return poloniexService;
            case 22:
                return localbitcoinsService;
            case 23:
                return bitfinexService;
            case 24:
                return binanceService;
            case 25:
                return gateService;
            case 26:
                return exxService;
            case 27:
                return bitzService;
            case 28:
                return coinwService;
            case 29:
                return huobiService;
            case 30:
                return hksyService;
            case 31:
                return kucoinService;
            case 32:
                return zbService;

        }
        throw new MsgException("目前还不支持该交易所");

    }

    private CoinPlatModel changeCoinPlatModel(CoinPlatModel coinPlatModel,
                                              CoinPlatModel newCoinPlat) {
        if (coinPlatModel == null) {
            return newCoinPlat;
        }
        if (newCoinPlat.getIncrease() == null && coinPlatModel.getYesterPrice() != null
                && coinPlatModel.getYesterPrice().compareTo(BigDecimal.ZERO) != 0) {// 如果有增长率那就不用计算 。 有些冗余的交易对需要判断是否等于0。
            // 设置增长率
            BigDecimal increase = newCoinPlat
                    .getLast()
                    .subtract(coinPlatModel.getYesterPrice())
                    .divide(coinPlatModel.getYesterPrice(), 4,
                            BigDecimal.ROUND_HALF_UP);
            newCoinPlat.setIncrease(increase);
        }

        BigDecimal rateUsd = exchangeRateService.getRateUsd(coinPlatModel
                .getBuyCoinId());
        BigDecimal rateCny = exchangeRateService.getRateCny(coinPlatModel
                .getBuyCoinId());
        if (rateUsd != null && rateCny != null && newCoinPlat.getLast() != null) { // 同时要判断人民币和最新价是否存在
            newCoinPlat.setCnyPrice(newCoinPlat.getLast().multiply(rateCny));
            newCoinPlat.setUsdPrice(newCoinPlat.getLast().multiply(rateUsd));
        }

        return newCoinPlat;
    }

    @Async
    public void getTicker(CoinPlatModel coinPlatModel) {

        try {
            PlatService platService = getPlatService(coinPlatModel.getPlatId());
            CoinPlatModel newCoinPlat = platService.getTicker(coinPlatModel);
            if (newCoinPlat == null) {
                return;
            }
            changeCoinPlatModel(coinPlatModel, newCoinPlat);
            coinPlatService.update(newCoinPlat);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private CoinPlatModel getCoinPlat(List<CoinPlatModel> list,
                                      Integer coinPlatId) {
        if (coinPlatId == null) {
            return null;
        }

        for (int i = 0; i < list.size(); i++) {
            if (coinPlatId.equals(list.get(i).getId()))
                return list.get(i);
        }
        return null;
    }

    @Override
    @Async
    public void getAllTicker(List<CoinPlatModel> coinPlatModelList) {
        try {
            if (coinPlatModelList.size() == 0) {
                return;
            }
            PlatService platService = getPlatService(coinPlatModelList.get(0)
                    .getPlatId());
            List<CoinPlatModel> newList = platService
                    .getAllTicker(coinPlatModelList);
            if (newList == null) {
                return;
            }
            for (CoinPlatModel newCoinPlat : newList) {
                CoinPlatModel coinPlatModel = getCoinPlat(coinPlatModelList,
                        newCoinPlat.getId());
                changeCoinPlatModel(coinPlatModel, newCoinPlat);
            }
            coinPlatService.batchUpdate(newList);
        } catch (Exception e) {

            e.printStackTrace();
        }

    }

    /**
     * api_key apiKey of the user symbol btc_usd:Bitcoin ltc_usd :Litecoin type
     * order type：buy/sell price Order price (within the scope of the
     * corresponding exchange restrictions) amount Order quantity (within the
     * scope of the corresponding exchange restrictions) sign signature of
     * request parameters
     */

    public AppBack trade(Integer userId, Integer coinPlatId, Integer type,
                         BigDecimal price, BigDecimal amount) {// 1buy 2sell

        CoinPlatModel coinPlatModel = coinPlatService.getById(coinPlatId);
        Map conditon = new HashMap();
        conditon.put("userId", userId);
        conditon.put("platId", coinPlatModel.getPlatId());
        List<UserConfigPlatModel> list = userConfigPlatService
                .getList(conditon);
        if (list.size() == 0) {
            return new AppBack(-1, "用户还没有授权该交易所");
        }
        UserConfigPlatModel userConfigPlatModel = list.get(0);

        PlatService platService = getPlatService(coinPlatModel.getPlatId());
        AppBack appBack = platService.trade(userConfigPlatModel.getApikey(),
                userConfigPlatModel.getSecretkey(), coinPlatModel.getSymbol(),
                type, price, amount);
        if (appBack.getStatus() == 0) {// 创建订单记录
            String orderId = appBack.get("orderId").toString();
            OrderModel orderModel = new OrderModel();
            orderModel.setOrderId(orderId);
            orderModel.setStatus(0);
            orderModel.setPlatId(coinPlatModel.getPlatId());
            orderModel.setCoinPlatId(coinPlatModel.getId());
            orderModel.setSymbol(coinPlatModel.getName());
            orderModel.setOrderStatus(0);
            orderModel.setType(1);
            orderModel.setTotalAmount(amount);
            orderModel.setPrecatoryPrice(price);
            orderModel.setDealType(type);
            orderModel.setUserId(userId);
            orderModel.setTotalPrice(price.multiply(amount));

            if (type == 1) {//计算买入手续费
                BigDecimal feePrice = userConfigPlatService.getFeePriceBuy(
                        userConfigPlatModel,
                        coinPlatModel,
                        amount);
                orderModel.setServiceCharge(feePrice);
            }

            if (type == 2) {//计算卖出手续费
                BigDecimal feePrice = userConfigPlatService.getFeePriceSell(
                        userConfigPlatModel,
                        coinPlatModel,
                        amount);
                orderModel.setServiceCharge(feePrice);
            }


            TradingPlatformModel tradingPlatformModel = tradingPlatformService
                    .getById(coinPlatModel.getPlatId());
            orderModel.setPlatName(tradingPlatformModel.getName());
            orderService.create(orderModel);
        }
        return appBack;
    }

    private UserInfo getUserInfoLite(Integer userId, Integer platId) {

        Map conditon = new HashMap();
        conditon.put("userId", userId);
        conditon.put("platId", platId);
        List<UserConfigPlatModel> list = userConfigPlatService
                .getList(conditon);
        if (list.size() == 0) {
            throw new MsgException(-1, "用户还没有授权该交易所");
        }
        UserConfigPlatModel userConfigPlatModel = list.get(0);
        PlatService platService = getPlatService(platId);
        logger.info(platService.getClass().getName());
        UserInfo userInfo = platService.getUserInfo(
                userConfigPlatModel.getApikey(),
                userConfigPlatModel.getSecretkey(), platId);
        if (userInfo == null) {
            throw new MsgException(-1, "获取失败");
        }

        // 刷新用户数据
        for (CoinInfo freeCoin : userInfo.getFreeCoinList()) {
            userConfigPlatCoinService.createAndUpdate(freeCoin, userId, platId,
                    1);
        }

        // 刷新用户数据
        for (CoinInfo freeCoin : userInfo.getFreezedCoinList()) {
            userConfigPlatCoinService.createAndUpdate(freeCoin, userId, platId,
                    0);
        }
        userInfo.setPlatId(platId);
        userInfo.setUserId(userId);
        return userInfo;
    }

    /**
     * 获取用户手下指定账户余额，过久就刷新。
     *
     * @param userId
     * @param platId
     * @param coinId
     * @param autoFlash 是否自动刷新
     * @return
     */
    public UserConfigPlatCoinModel getUserCoin(Integer userId, Integer platId,
                                               Integer coinId, boolean autoFlash) { // true 过期会刷新
        UserConfigPlatCoinModel userCoin = new UserConfigPlatCoinModel();
        Map c = new HashMap();
        c.put("userId", userId);
        c.put("platId", platId);
        c.put("coinId", coinId);
        c.put("onlyCoin", 1);
        List<UserConfigPlatCoinModel> cList = userConfigPlatCoinService
                .getListLite(c);
        if (cList.size() == 0 && !autoFlash) {// 没钱
            userCoin.setFreeAmount(BigDecimal.ZERO);
            userCoin.setMinHold(BigDecimal.ZERO);
            userCoin.setMaxHold(new BigDecimal("9999999999999999"));
            userCoin.setCoinId(coinId);
            userCoin.setPlatId(platId);
            return userCoin;
        }
        // 刷新再获取
        if (cList.size() == 0 && autoFlash) {
            getUserInfoLite(userId, platId);
            return getUserCoin(userId, platId, coinId, false);
        }
        userCoin = cList.get(0);
        if (!autoFlash) {
            return userCoin;
        }

        // 再次联网获取最新数据
        Date now = new Date();
        if (now.getTime() - userCoin.getUpdateTime().getTime() < 1000 * 60 * 10) {// 十分钟前，就刷新
            return userCoin;
        }
        // 刷新再获取
        getUserInfoLite(userId, platId);
        return getUserCoin(userId, platId, coinId, false);// 获得最新数据 false不循环

    }

    @Override
    public void getHedgingOrder(HedgingModel hedgingModel) {
        OrderModel buyOrder;
        OrderModel sellOrder;
        Integer buyOrderId = hedgingModel.getBuyOrderId();
        Integer sellOrderId = hedgingModel.getSellOrderId();
        List<OrderModel> buyList = new ArrayList<>();
        buyList.add(orderService.getById(buyOrderId));
        buyList = orderService.changeList(60, buyList);
        buyOrder = buyList.get(0);
        List<OrderModel> sellList = new ArrayList<>();
        sellList.add(orderService.getById(buyOrderId));
        sellList = orderService.changeList(60, sellList);
        sellOrder = sellList.get(0);
        HedgingModel newH = new HedgingModel();
        newH.setId(hedgingModel.getId());
        if (buyOrder.getOrderStatus() == -1
                || buyOrder.getOrderStatus() == 4
                || sellOrder.getOrderStatus() == -1
                || sellOrder.getOrderStatus() == 4) {
            newH.setStatus(1);
            newH.setStatusError(11);
            hedgingService.update(newH);
            return;
        }

        if (buyOrder.getOrderStatus() == 2 && sellOrder.getOrderStatus() == 2) {
            newH.setStatus(0);
            newH.setStatusError(0);
            hedgingService.update(newH);
            return;
        }

    }

    @Async
    public Future<UserInfo> getUserInfo(Integer userId, Integer platId) {
        UserInfo userInfo = getUserInfoLite(userId, platId);
        return new AsyncResult<>(userInfo);
    }

    @Async
    public Future<List<OrderInfo>> getOrderInfo(Integer userId,
                                                Integer coinPlatId) {

        CoinPlatModel coinPlatModel = coinPlatService.getById(coinPlatId);
        Map conditon = new HashMap();
        conditon.put("userId", userId);
        conditon.put("platId", coinPlatModel.getPlatId());
        List<UserConfigPlatModel> list = userConfigPlatService.getList(conditon);
        if (list.size() == 0) {
            orderService.refreshFailure(userId, coinPlatId);
            //       throw  new MsgException(-1, "用户还没有授权该交易所");
        }
        UserConfigPlatModel userConfigPlatModel = list.get(0);
        PlatService platService = getPlatService(coinPlatModel.getPlatId());
        List<OrderInfo> orderList = null;
        try {
            orderList = platService.getOrderInfo(
                    userConfigPlatModel.getApikey(),
                    userConfigPlatModel.getSecretkey(),
                    coinPlatId,
                    coinPlatModel.getSymbol());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        if (orderList == null) {
            orderService.refreshFailure(userId, coinPlatId);
            //  throw  new MsgException(-2, "获取失败");
        }
        if (orderList == null) {//本地为空的话，会报错，所以还要进行再次判断
            return null;
        }
        orderService.refreshOrder(userId, orderList, coinPlatModel);
        return new AsyncResult<>(orderList);
    }

    public AppBack cancelOrder(Integer userId, Integer coinPlatId,
                               String orderId) {// 1buy 2sell

        CoinPlatModel coinPlatModel = coinPlatService.getById(coinPlatId);
        Map conditon = new HashMap();
        conditon.put("userId", userId);
        conditon.put("platId", coinPlatModel.getPlatId());
        List<UserConfigPlatModel> list = userConfigPlatService
                .getList(conditon);
        if (list.size() == 0) {
            return new AppBack(-1, "用户还没有授权该交易所");
        }
        UserConfigPlatModel userConfigPlatModel = list.get(0);

        PlatService platService = getPlatService(coinPlatModel.getPlatId());
        AppBack appBack = platService.cancelOrder(
                userConfigPlatModel.getApikey(),
                userConfigPlatModel.getSecretkey(),
                orderId,
                coinPlatModel.getSymbol());
        System.out.println("1-----------");
        if (appBack.getStatus() != -1) {
        	System.out.println("2----------------");
            orderService.toCancel(coinPlatModel, orderId);
        }
        return appBack;
    }

    /**
     * @param hedgingModel 对冲策略 差价标准货币单位
     * @return
     */
    @Async
    public void hedging(HedgingModel hedgingModel) {

        herdgingImp(hedgingModel);

    }

    private boolean herdgingImp(HedgingModel hedgingModel) {


        HedgingModel newH = new HedgingModel();
        newH.setId(hedgingModel.getId());
        newH.setLastUpdateTime(new Date());

        CoinPlatModel coinPlatBuy = coinPlatService.getById(hedgingModel
                .getCoinPlatIdBuy());// 获取行情
        CoinPlatModel coinPlatSell = coinPlatService.getById(hedgingModel
                .getCoinPlatIdSell());//

        // 判断数据的实时性
//		Date now = new Date();
//		if (now.getTime() - coinPlatBuy.getTradingTime().getTime() > 10 * 60 * 1000 * 1000 * 1000) {
//			System.out.println("相差" +(now.getTime() - coinPlatBuy.getTradingTime().getTime()));
//			newH.setStatus(1);
//			newH.setStatusError(8);
//			hedgingService.update(newH);
//			return false;
//		}// 测试先注释
//
//		if (now.getTime() - coinPlatSell.getTradingTime().getTime() > 10 * 60 * 1000 * 1000 * 1000) {
//			newH.setStatus(1);
//			newH.setStatusError(8);
//			hedgingService.update(newH);
//			return false;
//		}// 测试先注释

        BigDecimal sell = coinPlatSell.getSell();// 卖出价
        if (sell == null) {
            sell = coinPlatSell.getLast();
        }
        BigDecimal buy = coinPlatBuy.getBuy();// 买入价
        if (buy == null) {
            buy = coinPlatSell.getLast();
        }
        BigDecimal dPrice = sell.subtract(buy);
        // 计算利润率
        // 收益率=100*差价-手续费）/（买入金额+卖出金额)

        BigDecimal feePrice = hedgingModel.getFeePrice();
        BigDecimal d1 = dPrice.subtract(feePrice);
        BigDecimal s1 = buy.add(sell);
        BigDecimal nowRate = d1.divide(s1, 6);
        newH.setNowRate(nowRate);

        if (dPrice.compareTo(hedgingModel.getEquPrice()) < 0) {
            newH.setStatusError(1);
            hedgingService.update(newH);
            return false;
        }
        // 有利润了，开始查看用户是否有钱，是否限制

        // 买入市场查看

        logger.info("hedging1  " + JSON.toJSONString(hedgingModel));
        // 满足对冲条件，开始对冲
        Map conditon = new HashMap();
        conditon.put("userId", hedgingModel.getUserId());
        conditon.put("platId", hedgingModel.getPlatIdBuy());
        List<UserConfigPlatModel> list = userConfigPlatService
                .getList(conditon);

        if (list.size() == 0) {
            logger.info("没有key配置");
            newH.setStatus(1);
            newH.setStatusError(9);
            hedgingService.update(newH);
            return false;
        }
        UserConfigPlatModel configPlatBuy = list.get(0); // 买入市场配置
        conditon.clear();
        conditon.put("userId", hedgingModel.getUserId());
        conditon.put("platId", hedgingModel.getPlatIdSell());
        list = userConfigPlatService.getList(conditon);
        if (list.size() == 0) {
            logger.info("没有key配置");
            newH.setStatus(1);
            newH.setStatusError(9);
            hedgingService.update(newH);
            return false;
        }
        logger.info("hedging2  " + JSON.toJSONString(hedgingModel));

        UserConfigPlatModel configPlatSell = list.get(0); // 卖出市场配置

        BigDecimal amount = hedgingModel.getMaxAmount();// 购买数量默认是最大的
        BigDecimal buyMoney = amount.multiply(buy);// 买入需要花的钱 TODO 忽略了手续费
        BigDecimal sellMoney = amount.multiply(sell);// 卖掉需要花的钱    卖掉需要花钱？是需要卖掉的币或者得到的钱
        BigDecimal lastMoney;

        UserConfigPlatCoinModel userCoinBuyBase = // 买入市场基准货币是不是太少了
                getUserCoin(hedgingModel.getUserId(), hedgingModel.getPlatIdBuy(),
                        hedgingModel.getBaseCoinId(), true);
        if (userCoinBuyBase == null) { // 需要哦按段如果为空的情况
            newH.setStatus(1);
            newH.setStatusError(2);
            hedgingService.update(newH);
            return false;
        }
        lastMoney = userCoinBuyBase.getFreeAmount().subtract(buyMoney);
        if (lastMoney.compareTo(userCoinBuyBase.getMinHold()) < 0) {
            // 买方市场基准的钱不够了
            // TODO 再次计算 amount
            newH.setStatus(1);
            newH.setStatusError(2);
            hedgingService.update(newH);
            return false;
        }
        logger.info("hedging3  " + JSON.toJSONString(hedgingModel));
        UserConfigPlatCoinModel userCoinSellTrade = // 卖出市场交易货币是不是太少了
                getUserCoin(hedgingModel.getUserId(), hedgingModel.getPlatIdSell(),
                        hedgingModel.getTradeCoinId(), false);
        lastMoney = userCoinSellTrade.getFreeAmount().subtract(amount);
        if (lastMoney.compareTo(userCoinSellTrade.getMinHold()) < 0) {
            // 卖方市场交易的货币太少了
            newH.setStatus(1);
            newH.setStatusError(4);
            hedgingService.update(newH);
            return false;
        }

        UserConfigPlatCoinModel userCoinSellBase = // 卖出市场基准货币是不是太多了
                getUserCoin(hedgingModel.getUserId(), hedgingModel.getPlatIdSell(),
                        hedgingModel.getBaseCoinId(), true);

        lastMoney = userCoinSellBase.getFreeAmount().add(sellMoney);
        if (lastMoney.compareTo(userCoinSellBase.getMaxHold()) > 0) {
            // 卖方方市场基准的太多了
            newH.setStatus(1);
            newH.setStatusError(5);
            hedgingService.update(newH);
            return false;
        }
        logger.info("hedging4  " + JSON.toJSONString(hedgingModel));
        UserConfigPlatCoinModel userCoinBuyTrade = // 买入市场交易货币是不是太多了
                getUserCoin(hedgingModel.getUserId(), hedgingModel.getPlatIdBuy(),
                        hedgingModel.getTradeCoinId(), false);
        lastMoney = userCoinBuyTrade.getFreeAmount().add(amount);
        if (lastMoney.compareTo(userCoinBuyTrade.getMaxHold()) > 0) {
            // 买方市场买的太多了。
            newH.setStatus(1);
            newH.setStatusError(3);
            hedgingService.update(newH);
            return false;
        }

        UserModel userModel = userService.getById(hedgingModel.getUserId());

        // 计算用户有无额度可以对冲
        BigDecimal addMoney = amount.multiply(coinPlatBuy.getCnyPrice());
        addMoney = amount.multiply(coinPlatSell.getCnyPrice()).add(addMoney);
        if (userModel.getMoney().compareTo(addMoney) < 0) {
            // 用户剩余额度不足
            newH.setStatus(1);
            newH.setStatusError(10);
            hedgingService.update(newH);
            return false;
        }

        // 买方市场买入货币
        PlatService platServiceBuy = getPlatService(hedgingModel.getPlatIdBuy());
        AppBack appBack = platServiceBuy.trade(configPlatBuy.getApikey(),
                configPlatBuy.getSecretkey(), coinPlatBuy.getSymbol(), 1, buy,
                amount);

        if (appBack.getStatus() != 0) {// 买方市场买入失败

            newH.setStatus(1);
            newH.setStatusError(6);
            hedgingService.update(newH);
            return false;
        }
        logger.info("hedging4  " + JSON.toJSONString(hedgingModel));
        // 记录订单
        String orderId = appBack.get("orderId").toString();
        OrderModel orderModel = new OrderModel();
        orderModel.setOrderId(orderId);
        orderModel.setStatus(0);
        orderModel.setPlatId(configPlatBuy.getPlatId());
        orderModel.setCoinPlatId(coinPlatBuy.getId());
        orderModel.setSymbol(coinPlatBuy.getName());
        orderModel.setOrderStatus(0);
        orderModel.setType(2);
        orderModel.setTotalAmount(amount);
        orderModel.setPrecatoryPrice(buy);
        orderModel.setDealType(1);
        orderModel.setHedgingId(hedgingModel.getId());
        orderModel.setUserId(hedgingModel.getUserId());
        orderModel.setTotalPrice(buyMoney);
        TradingPlatformModel tradingPlatformModel = tradingPlatformService
                .getById(coinPlatBuy.getPlatId());
        orderModel.setPlatName(tradingPlatformModel.getName());

        BigDecimal feePriceBuy = userConfigPlatService.getFeePriceBuy(
                configPlatBuy,
                coinPlatBuy,
                amount);

        orderModel.setServiceCharge(feePriceBuy);

        orderService.create(orderModel);
        newH.setBuyOrderId(orderModel.getId());

        // 卖房市场卖出货币  //需要拿到卖出市场的platId
        PlatService platServiceSell = getPlatService(hedgingModel.getPlatIdSell());
        appBack = platServiceSell.trade(configPlatSell.getApikey(),
                configPlatSell.getSecretkey(), coinPlatSell.getSymbol(), 2,
                sell, amount);

        if (appBack.getStatus() != 0) {// 买卖成功 TODO 买卖失败时记录处理，与取消订单处理 需要判断是否撤单成功
            newH.setStatus(1);
            newH.setStatusError(7);
            hedgingService.update(newH);
            return false;
        }

        // 记录订单
        orderId = appBack.get("orderId").toString();
        orderModel = new OrderModel();
        orderModel.setOrderId(orderId);
        orderModel.setStatus(0);
        orderModel.setPlatId(configPlatSell.getPlatId());
        orderModel.setCoinPlatId(coinPlatSell.getId());
        orderModel.setSymbol(coinPlatSell.getName());
        orderModel.setOrderStatus(0);
        orderModel.setType(2);
        orderModel.setTotalAmount(amount);
        orderModel.setPrecatoryPrice(sell);
        orderModel.setDealType(2);
        orderModel.setHedgingId(hedgingModel.getId());
        orderModel.setUserId(hedgingModel.getUserId());
        orderModel.setTotalPrice(sellMoney);
        tradingPlatformModel = tradingPlatformService.getById(configPlatSell
                .getPlatId());
        orderModel.setPlatName(tradingPlatformModel.getName());
        BigDecimal feePriceSell = userConfigPlatService.getFeePriceBuy(
                configPlatSell,
                coinPlatSell,
                amount);
        orderModel.setServiceCharge(feePriceSell);
        orderService.create(orderModel);
        newH.setSellOrderId(orderModel.getId());
        logger.info("hedging5  " + JSON.toJSONString(hedgingModel));
        // 计算高价/低价的值.
        // 更新用户剩余额度
        newH.setStatusError(0);
        newH.setStatus(2);
        hedgingService.update(newH);

        UserModel newUser = new UserModel();
        newUser.setAddMoney(new BigDecimal("0").subtract(addMoney));
        newUser.setId(hedgingModel.getUserId());
        userService.update(newUser);

        MoneyHistoryModel moneyHistoryModel=new MoneyHistoryModel();
        moneyHistoryModel.setChangeMoney(newUser.getAddMoney());
        moneyHistoryModel.setMemo("对冲消耗额度 id"+hedgingModel.getId());
        moneyHistoryModel.setType(2);
        moneyHistoryModel.setRemainMoney(userModel.getMoney().subtract(addMoney));
        moneyHistoryModel.setUserId(hedgingModel.getUserId());
        moneyHistoryService.create(moneyHistoryModel);


        return true;
    }

    // 识别交易货币名
    void createNewCoin(CoinPlatModel coinPlat) {
        CoinPlatModel newCoinPlat = new CoinPlatModel();
        newCoinPlat.setId(coinPlat.getId());
        coinPlat = coinPlatService.getById(coinPlat.getId());
        String symbol = coinPlat.getSymbol().toLowerCase();
        String[] s = null;
        if (coinPlat.getPlatId() == 2) {
            s = symbol.split("_");
            // }else if (coinPlat.getPlatId() == 5) {
            // s =new String[2];
            // s[0]=symbol;
            // s[1]="krw";
            // }else if (coinPlat.getPlatId() == 6) {
            // s = symbol.split("-");
            // String t=s[0];
            // s[0]=s[1];
            // s[1]=s[0];

        } else if (coinPlat.getPlatId() == 19) {// 7 23
            Map c = new HashMap();
            c.put("base", "1");
            List<CoinModel> cList = coinService.getListLite(c);
            boolean find = false;

            for (CoinModel coinModel : cList) {
                String coinName = coinModel.getShortName().toLowerCase();
                if (symbol.endsWith(coinName)) {
                    find = true;
                    s = new String[2];
                    s[1] = coinName;
                    s[0] = symbol.substring(0,
                            symbol.length() - coinName.length());
                    logger.info("symbol.endsWith" + symbol + "  " + s[0] + "/"
                            + s[1]);
                    break;
                }
            }
            if (!find) {
                return;
            }
        } else if (coinPlat.getPlatId() == 4) {
            Map c = new HashMap();
            c.put("base", "1");
            List<CoinModel> cList = coinService.getListLite(c);
            boolean find = false;

            for (CoinModel coinModel : cList) {
                String coinName = coinModel.getShortName().toLowerCase();
                if (symbol.endsWith(coinName)) {
                    find = true;
                    s = new String[2];
                    s[1] = coinName;
                    s[0] = clearCoinString(symbol.substring(0, symbol.length()
                            - coinName.length()));
                    logger.info("symbol.endsWith" + symbol + "  " + s[0] + "/"
                            + s[1]);
                    break;
                }
            }
            if (!find) {
                return;
            }
            // }else if (coinPlat.getPlatId() == 8) {
            // String name = coinPlat.getName();
            // s = name.split("/");
            // }else if (coinPlat.getPlatId() == 10) {
            // s = symbol.split("_");
            // }else if (coinPlat.getPlatId() == 11) {
            // s = symbol.split("/");
            // }else if (coinPlat.getPlatId() == 12) {
            // s = symbol.split("_");
            // }else if (coinPlat.getPlatId() == 13) {
            // s = symbol.split("-");
            // }else if (coinPlat.getPlatId() == 16) {
            // s = symbol.split("_");
            // }else if (coinPlat.getPlatId() == 17) {
            // s =new String[2];
            // s[0]=symbol;
            // s[1]="krw";
            // }else if (coinPlat.getPlatId() == 18) {
            // s = symbol.split("_");
            // }else if (coinPlat.getPlatId() == 16) {
            // s = symbol.split("_");
        } else {
            return;
        }

        Map c = new HashMap();
        c.put("shortName", s[0]);
        List<CoinModel> cList = coinService.getListLite(c);
        CoinModel coin;
        if (cList.size() == 0) {
            coin = new CoinModel();
            coin.setName(s[0]);
            coin.setShortName(s[0]);
            coinService.create(coin);
        } else {
            coin = cList.get(0);
        }
        c.clear();
        // 基准货币
        c.put("shortName", s[1]);
        cList = coinService.getListLite(c);
        CoinModel coinBase;
        if (cList.size() == 0) {
            coinBase = new CoinModel();
            coinBase.setName(s[1]);
            coinBase.setShortName(s[1]);
            coinService.create(coinBase);
        } else {
            coinBase = cList.get(0);
        }
        newCoinPlat.setBuyCoinId(coinBase.getId());
        newCoinPlat.setCoinId(coin.getId());
        newCoinPlat.setName(s[0] + "/" + s[1]);
        // logger.info("symbol  "+ symbol);
        // logger.info("s  "+ s[0]+" "+s[1]);
        // logger.info("coinBase  "+ JSON.toJSONString(coinBase));
        // logger.info("coin  "+ JSON.toJSONString(coin));
        // logger.info("createNewCoin  "+ JSON.toJSONString(newCoinPlat));
        coinPlatService.update(newCoinPlat);
        // return true;

    }

    private String clearCoinString(String s) {
        String r = null;
        if (s.startsWith("x")) {
            r = clearCoinString(s.substring(1));
        }
        if (s.endsWith("x") || s.endsWith("z")) {
            r = clearCoinString(s.substring(0, s.length() - 1));
        }
        return r;
    }

    // public static void main(String[] args) {
    // String symbol = "DASHBTC".toLowerCase();
    // Map c = new HashMap();
    // c.put("base", "1");
    // List<String> cList = new ArrayList<>();
    // cList.add("btc");
    // boolean find = false;
    // String[] s = new String[2];
    // for (String coinName : cList) {
    // if (symbol.endsWith(coinName)) {
    // find = true;
    // s[1] = coinName;
    // s[0] = symbol.substring(0, symbol.length() - coinName.length());
    // logger.info("symbol.endsWith" + symbol + "  " + s[0] + "/" + s[1]);
    // break;
    // }
    // }
    // }

    public static void main(String[] args) {

        // create 3 BigDecimal objects
        BigDecimal bg1, bg2, bg3;

        bg1 = new BigDecimal("100.123");
        bg2 = new BigDecimal("50.56");

        // subtract bg1 with bg2 and assign result to bg3
        bg3 = bg1.subtract(bg2);

        String str = "The Result of Subtraction is " + bg3;

        // print bg3 value
        System.out.println(bg1);
        System.out.println(bg2);
        System.out.println(str);
    }
}