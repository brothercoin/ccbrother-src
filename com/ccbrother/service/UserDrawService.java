package com.hykj.ccbrother.service;

import org.springframework.stereotype.Service;

import com.hykj.ccbrother.base.BaseService;
import com.hykj.ccbrother.mapper.UserDrawMapper;
import com.hykj.ccbrother.model.UserDrawModel;
/**
 * 用户提币表
 * @author 封君
 *
 */
@Service
public class UserDrawService extends BaseService<UserDrawModel, UserDrawMapper>{

	
	//接入智能合约后，要能够定时刷新和更改用户提币状态，实时
	public void updateStatus(int id, int status){
		
	}
	
	
}
