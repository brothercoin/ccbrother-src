package com.hykj.ccbrother.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hykj.ccbrother.base.BaseService;
import com.hykj.ccbrother.mapper.UserDrawMapper;
import com.hykj.ccbrother.model.UserDrawModel;
import com.hykj.ccbrother.model.UserModel;
/**
 * 用户提币表
 * @author 封君
 *
 */
@Service
public class UserDrawService extends BaseService<UserDrawModel, UserDrawMapper>{

	@Autowired
    UserService userService;
    @Override
    protected UserDrawModel change(UserDrawModel model, Map condition) {
        if(condition.get("backList")!=null){
            UserModel userModel=userService.getById(model.getUserId());
            if(userModel!=null){
                model.setUserName(userModel.getNickname());
            }
        }
		return model;
    }
	
	
	//接入智能合约后，要能够定时刷新和更改用户提币状态，实时
	public void updateStatus(int id, int status){
		
	}
	
	
}
