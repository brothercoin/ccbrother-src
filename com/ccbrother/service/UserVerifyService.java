package com.hykj.ccbrother.service;

import com.hykj.ccbrother.base.BaseService;
import com.hykj.ccbrother.mapper.UserVerifyMapper;
import com.hykj.ccbrother.model.UserVerifyModel;
import org.springframework.stereotype.Service;

/**
 * 
 *
 * @author innel
 * @email 2638086622@qq.com
 * @date 2017-10-31 15:48:59
 */
@Service
public class UserVerifyService extends BaseService<UserVerifyModel, UserVerifyMapper> {

    /**
     * 验证码已使用
     * @param id
     */
    public void clearnVerify(Integer id){

        if(id==null){
            return;
        }
        UserVerifyModel userVerifyModel = new UserVerifyModel();
        userVerifyModel.setStatus(-1);
        userVerifyModel.setId(id);
        mapper.update(userVerifyModel);
    }


}