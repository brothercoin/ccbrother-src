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
import com.hykj.ccbrother.mapper.UserOptionalPlatMapper;
import com.hykj.ccbrother.model.UserOptionalPlatModel;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 用户自选交易所表
 *
 * @author innel
 * @email 2638086622@qq.com
 * @date 2018-01-24 16:24:12
 */
@Service
public class UserOptionalPlatService extends BaseService<UserOptionalPlatModel, UserOptionalPlatMapper> {

    @Override
    public UserOptionalPlatModel change(UserOptionalPlatModel model, Map condition) {

        if(condition.get("change")!=null){
            model.setId(model.getPlatId());
        }
        return super.change(model, condition);
    }
}