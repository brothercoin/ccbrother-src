package com.hykj.ccbrother.service;

import com.hykj.ccbrother.base.BaseService;
import com.hykj.ccbrother.mapper.ArticleMapper;
import com.hykj.ccbrother.model.ArticleModel;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 文章资讯
 *
 * @author innel
 * @email 2638086622@qq.com
 * @date 2017-11-09 10:08:31
 */
@Service
public class ArticleService extends BaseService<ArticleModel, ArticleMapper> {

    public Map getMap(){
        return mapper.getMap();
    }

    public Map getMap2(){
        return mapper.getMap();
    }
}