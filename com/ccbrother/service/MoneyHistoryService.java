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

import com.hykj.ccbrother.base.BaseService;
import com.hykj.ccbrother.mapper.MoneyHistoryMapper;
import com.hykj.ccbrother.model.MoneyHistoryModel;
import com.hykj.ccbrother.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 用户额度记录
 *
 * @author innel
 * @email 2638086622@qq.com
 * @date 2018-02-05 10:59:02
 */
@Service
public class MoneyHistoryService extends BaseService<MoneyHistoryModel, MoneyHistoryMapper> {

    @Autowired
    UserService userService;
    @Override
    protected MoneyHistoryModel change(MoneyHistoryModel model, Map condition) {
        if(condition.get("backList")!=null){
            UserModel userModel=userService.getById(model.getUserId());
            if(userModel!=null){
                model.setUserName(userModel.getNickname());
            }
        }


        return super.change(model, condition);
    }
}