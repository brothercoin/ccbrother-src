package com.hykj.ccbrother.service;

import org.springframework.stereotype.Service;

import com.hykj.ccbrother.base.BaseService;
import com.hykj.ccbrother.mapper.ArticleSourceMapper;
import com.hykj.ccbrother.model.ArticleSourceModel;

@Service
public class ArticleSourceService extends BaseService<ArticleSourceModel, ArticleSourceMapper> {

	 public ArticleSourceModel getTitleLast(int articleSourceId){
	    return mapper.getTitleLast(articleSourceId);
	 }
}
