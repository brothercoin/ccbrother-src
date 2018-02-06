/*
 * Copyright 2017-2101 Innel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hykj.ccbrother.service;

import com.alibaba.fastjson.JSON;
import com.hykj.ccbrother.apimodel.OrderInfo;
import com.hykj.ccbrother.base.BaseService;
import com.hykj.ccbrother.mapper.OrderMapper;
import com.hykj.ccbrother.model.CoinPlatModel;
import com.hykj.ccbrother.model.OrderModel;
import com.hykj.ccbrother.model.TradingPlatformModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Future;

/**
 * 交易记录（重要）
 *
 * @author innel
 * @email 2638086622@qq.com
 * @date 2017-12-21 09:38:17
 */
@Service
public class OrderService extends BaseService<OrderModel, OrderMapper> {

    @Autowired
    CoinPlatService coinPlatService;

    @Autowired
    TradingPlatformService tradingPlatformService;

    @Autowired
    Plat plat;


    // 根据获取的订单刷新本地订单记录

    /**
     * @param userId
     * @param orderList
     * @param coinPlatModel 这个交易对下订单
     */
    public void refreshOrder(Integer userId, List<OrderInfo> orderList, CoinPlatModel coinPlatModel) {
        logger.info("refreshOrder " + JSON.toJSONString(orderList));

        List<String> orderInfoOrderId = new ArrayList<>();//获取到的OrderInfo的orderId

        for (OrderInfo orderInfo : orderList) {
            orderInfoOrderId.add(orderInfo.getOrderId());
            writeOrderInfo(orderInfo, coinPlatModel, userId);
        }

        //把原来正在交易的订单如何返回找不到，默认认为交易成功。
        Map conditon = new HashMap();
        conditon.put("userId", userId);
        conditon.put("coinPlatId", coinPlatModel.getId());
        conditon.put("noDeal", 1);//原来正在交易的订单
        List<OrderModel> noDealList = getListLite(conditon);

        List<String> hasDealOrderId = new ArrayList<>();
        Map<String, OrderModel> noDealMap = new HashMap<>();
        for (OrderModel o : noDealList) {
            hasDealOrderId.add(o.getOrderId()); //未交易的订单号
            noDealMap.put(o.getOrderId(), o);
        }
        hasDealOrderId.removeAll(orderInfoOrderId);//变成了已交易的订单号了

        for (String orderId : hasDealOrderId) {
            OrderModel hasDealOrder = noDealMap.get(orderId);
            writeOrderHasDeal(hasDealOrder);
        }


        //把原来正在交易的订单如何返回找不到，默认认为交易成功。
        conditon = new HashMap();
        conditon.put("userId", userId);
        conditon.put("coinPlatId", coinPlatModel.getId());
        conditon.put("orderStatus", 4);//原来等待取消的订单
        List<OrderModel> waitCancelList = getListLite(conditon);

        List<String> hasCancelOrderId = new ArrayList<>();
        Map<String, OrderModel> waitCancelMap = new HashMap<>();
        for (OrderModel o : waitCancelList) {
            hasCancelOrderId.add(o.getOrderId()); //未交易的订单号
            waitCancelMap.put(o.getOrderId(), o);
        }
        hasCancelOrderId.removeAll(orderInfoOrderId);//变成了已取消订单

        for (String orderId : hasCancelOrderId) {
            OrderModel hasDealOrder = waitCancelMap.get(orderId);
            writeOrderHasCancel(hasDealOrder);
        }


    }

    private void writeOrderInfo(OrderInfo orderInfo, CoinPlatModel coinPlatModel, Integer userId) { //获得的订单处理

        Map<String, java.io.Serializable> conditon = new HashMap<>();
        conditon.put("userId", userId);
        conditon.put("coinPlatId", orderInfo.getCoinPlatId());
        conditon.put("orderId", orderInfo.getOrderId());
        List<OrderModel> oList = getListLite(conditon);
        if (oList.size() == 0) {// 订单记录里没有，是用户自己创建的。对业务逻辑无影响 忽略掉
            OrderModel orderModel = new OrderModel();
            orderModel.setOrderId(orderInfo.getOrderId());
            orderModel.setStatus(0);
            orderModel.setPlatId(coinPlatModel.getPlatId());
            orderModel.setCoinPlatId(orderInfo.getCoinPlatId());
            orderModel.setSymbol(coinPlatModel.getName());
            orderModel.setOrderStatus(orderInfo.getStatus());
            orderModel.setType(3);
            orderModel.setTotalAmount(orderInfo.getAmount());
            orderModel.setPrecatoryPrice(orderInfo.getPrice());
            orderModel.setDealType(orderInfo.getType());
            orderModel.setUserId(userId);
            orderModel.setDealAmount(orderInfo.getDealAmount());
            orderModel.setTotalPrice(orderInfo.getDealAmount().multiply(
                    orderInfo.getPrice()));
            TradingPlatformModel tradingPlatformModel = tradingPlatformService
                    .getById(coinPlatModel.getPlatId());
            orderModel.setPlatName(tradingPlatformModel.getName());
            create(orderModel);
            return;
        }
        if (orderInfo.getStatus() == 2) {
            writeOrderHasDeal(oList.get(0));//交易成功
            return;
        }
        if (orderInfo.getStatus() == -1) {
            writeOrderHasCancel(oList.get(0)); //订单取消
            return;
        }

        OrderModel orderModel = new OrderModel();
        orderModel.setId(oList.get(0).getId());
        orderModel.setOrderStatus(orderInfo.getStatus());
        orderModel.setTotalAmount(orderInfo.getAmount());
        orderModel.setRefreshTime(new Date());
        orderModel.setStatus(0);
        orderModel.setPrecatoryPrice(orderInfo.getPrice());
        orderModel.setDealAmount(orderInfo.getDealAmount());
        orderModel.setTotalPrice(orderInfo.getDealAmount().multiply(
                orderInfo.getPrice()));
        update(orderModel);

    }

    private void writeOrderHasDeal(OrderModel hasDealOrder) {//TODO 默认没收到消息的订单当做已完成订单 需要优化
        OrderModel orderModel = new OrderModel();
        orderModel.setEndTime(new Date());
        orderModel.setId(hasDealOrder.getId());
        orderModel.setOrderStatus(2);
        BigDecimal cnyPrice = null;
        BigDecimal usdPrice = null;
        BigDecimal totalPrice = hasDealOrder.getTotalPrice();
        Map conditon = new HashMap();
        conditon.put("id", hasDealOrder.getCoinPlatId());
        conditon.put("userId",hasDealOrder.getUserId());
        List<CoinPlatModel> coinPlatList = coinPlatService.getCommendList(conditon);
        CoinPlatModel coinPlat = coinPlatList.get(0);
        if (hasDealOrder.getDealType() == 1) {
            if (coinPlat.getBuyPriceCny() != null) {
                cnyPrice = coinPlat.getBuyPriceCny().multiply(totalPrice);
            }
            if (coinPlat.getBuyPriceUsd() != null) {
                usdPrice = coinPlat.getBuyPriceUsd().multiply(totalPrice);
            }
        } else if (hasDealOrder.getDealType() == 2) {
            if (coinPlat.getSellPriceCny() != null) {
                cnyPrice = coinPlat.getSellPriceCny().multiply(totalPrice);
            }
            if (coinPlat.getSellPriceUsd() != null) {
                usdPrice = coinPlat.getSellPriceUsd().multiply(totalPrice);
            }
        }
        orderModel.setTotalPriceCny(cnyPrice);
        orderModel.setTotalPriceUsd(usdPrice);
        update(orderModel);
    }

    private void writeOrderHasCancel(OrderModel hasDealOrder) {//TODO 默认没收到消息的订单当做已取消订单 需要优化
        OrderModel orderModel = new OrderModel();
        orderModel.setEndTime(new Date());
        orderModel.setId(hasDealOrder.getId());
        orderModel.setOrderStatus(-1);
        update(orderModel);
    }

    public void toCancel(CoinPlatModel coinPlatModel,
                         String orderId) {//去取消订单 状态变成待取消订单

        Map<String, java.io.Serializable> conditon = new HashMap<>();
        conditon.put("coinPlatId", coinPlatModel.getId());
        conditon.put("orderId", orderId);
        List<OrderModel> oList = getListLite(conditon);
        if (oList.size() == 0) {
            //取消的订单之前没保存。
            return;
        }
        OrderModel orderModel = oList.get(0);
        OrderModel newOrder = new OrderModel();
        newOrder.setOrderStatus(-1);//直接设置为-1
        newOrder.setId(orderModel.getId());
        update(newOrder);

    }

    // 根据获取的订单失败
    void refreshFailure(Integer userId, Integer coinPlatId) {

        Map<String, Integer> condition = new HashMap<>();
        condition.put("userId", userId);
        condition.put("coinPlatId", coinPlatId);
        List<OrderModel> oList = getListLite(condition);// 从本地获取的订单列表，还有可能为空。
        if (oList.size() > 0) {
            for (OrderModel anOList : oList) {
                OrderModel orderModel = new OrderModel();
                orderModel.setId(anOList.getId());
                orderModel.setRefreshTime(new Date());
                orderModel.setStatus(-1);
                update(orderModel);
            }
        }
    }



    public List<OrderModel> changeList(Integer time,//  刷新时间单位秒 0不刷新
                                       List<OrderModel> orderList)  {
        if (time==null  || time == 0) {
            return orderList;
        }

        Date now = new Date();
        List<String> refreshKeyList = new ArrayList<>();// 正在更新的订单号
        List<Future<List<OrderInfo>>> fList = new ArrayList<>();// 正在更新的进程

        for (OrderModel orderModel : orderList) {
            String key = orderModel.getUserId() + "_"
                    + orderModel.getCoinPlatId();
            if (refreshKeyList.contains(key)) {// 正在刷新中的数据
                continue;
            }
            if ( now.getTime() - orderModel.getRefreshTime().getTime() < 1000 *time) {//刚刚刷新过
                continue;
            }
            if (orderModel.getOrderStatus() == 2 || orderModel.getOrderStatus() == -1) {//订单已完成，已取消，不需要再刷新
                continue;
            }
            refreshKeyList.add(key);
            Future<List<OrderInfo>> f = plat.getOrderInfo(
                    orderModel.getUserId(), orderModel.getCoinPlatId());
            fList.add(f);
        }

        for (Future<List<OrderInfo>> f : fList) {
            try {
                f.get(); // 等待更新完成
            }catch (Exception e){
                logger.error(e.getMessage(),e);
                //更新失败
            }

        }

        List<OrderModel> newOrderList = new ArrayList<>();// 从数据库里取最新数据
        for (OrderModel anOrderList : orderList) {
            OrderModel newOrder = getById(anOrderList.getId());
            newOrderList.add(newOrder);
        }
        return newOrderList;

    }


}