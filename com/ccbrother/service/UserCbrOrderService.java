package com.hykj.ccbrother.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hykj.ccbrother.base.BaseService;
import com.hykj.ccbrother.mapper.UserCbrOrderMapper;
import com.hykj.ccbrother.model.UserCbrOrderModel;
import com.hykj.ccbrother.model.UserModel;
/**
 * 用户cbr订单表
 * @author 封君
 *
 */
@Service
public class UserCbrOrderService extends BaseService<UserCbrOrderModel, UserCbrOrderMapper>{

    @Autowired
    UserService userService;
    @Override
    protected UserCbrOrderModel change(UserCbrOrderModel model, Map condition) {
        if(condition.get("backList")!=null){
            UserModel userModel=userService.getById(model.getUserId());
            if(userModel!=null){
                model.setUserName(userModel.getNickname());
            }
        }
		return model;
    }
}
