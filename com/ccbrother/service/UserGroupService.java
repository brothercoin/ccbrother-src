package com.hykj.ccbrother.service;

import com.hykj.ccbrother.base.BaseService;
import com.hykj.ccbrother.mapper.UserGroupMapper;
import com.hykj.ccbrother.model.UserGroupModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 用户加入的组群
 *
 * @author innel
 * @email 2638086622@qq.com
 * @date 2017-11-02 16:21:49
 */
@Service
public class UserGroupService extends BaseService<UserGroupModel, UserGroupMapper> {

    @Autowired
    GroupService groupService;

    @Autowired
    UserService userService;

    @Override
    public UserGroupModel change(UserGroupModel model, Map condition) {

        if(condition.get("groupId")==null){
            model.setGroupModel(groupService.getById(model.getGroupId()));
        }else {
            model.setUserModel(userService.getById(model.getUserId()));
        }


        return super.change(model, condition);
    }
}