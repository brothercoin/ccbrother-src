package com.hykj.ccbrother.service;

import com.hykj.ccbrother.base.BaseService;
import com.hykj.ccbrother.mapper.UserMapper;
import com.hykj.ccbrother.model.UserModel;
import io.rong.RongCloud;
import io.rong.models.CodeSuccessResult;
import org.springframework.stereotype.Service;

/**
 * 
 *
 * @author innel
 * @email 2638086622@qq.com
 * @date 2017-10-31 15:48:59
 */
@Service
public class UserService extends BaseService<UserModel, UserMapper> {



    /**
     * 修改资料 修改融云
     * @param user
     */
    public void changeInfo(UserModel user) throws Exception {
        update(user);

//        CodeSuccessResult userRefreshResult = RongCloud.getInstance().user
//                .refresh(user.getId()+"", user.getNickname(), user.getHeadPhoto());


    }


}