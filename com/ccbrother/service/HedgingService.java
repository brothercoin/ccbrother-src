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
import com.hykj.ccbrother.mapper.HedgingMapper;
import com.hykj.ccbrother.model.HedgingModel;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 对冲策略表
 *
 * @author innel
 * @email 2638086622@qq.com
 * @date 2018-01-02 09:27:48
 */
@Service
public class HedgingService extends BaseService<HedgingModel, HedgingMapper> {

    @Override
    public HedgingModel change(HedgingModel model, Map condition) {
        model.setOtherHedging(getById(model.getOtherId()));
        return super.change(model, condition);
    }
}