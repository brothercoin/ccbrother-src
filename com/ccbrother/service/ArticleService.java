package com.hykj.ccbrother.service;

import com.hykj.ccbrother.base.BaseService;
import com.hykj.ccbrother.config.Config;
import com.hykj.ccbrother.mapper.ArticleMapper;
import com.hykj.ccbrother.model.ArticleModel;
import com.hykj.ccbrother.service.media.BitCoin86Service;
import com.hykj.ccbrother.service.media.ChainforService;
import com.hykj.ccbrother.service.media.JSCJService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import us.codecraft.webmagic.Spider;

import java.util.List;
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

	@Autowired
	private JSCJService jscjService;
	
	@Autowired
	private ChainforService chainforService;
	
	@Autowired
	private BitCoin86Service bitCoin86Service;
	
    @Override
    public ArticleModel change(ArticleModel model, Map condition) {
        model.setFacePhoto(Config.photoUrl(model.getFacePhoto()));
        model.setUrl(Config.photoUrl(model.getUrl()));
        return super.change(model, condition);
    }

    public Map getMap(){
        return mapper.getMap();
    }

    public Map getMap2(){
        return mapper.getMap();
    }
    
    public int compareTime(Map map){
    	return mapper.compareTime(map);
    }
    
    public void statCatch(){
    	jinSeStat();
    	chainStat();
    	bitCoin86Stat();
    }
    
    //bitCoin86
	private void bitCoin86Stat() {
		//开启一个线程抓取资讯
    	Spider.create(bitCoin86Service).addUrl("http://www.bitcoin86.com/news/")
		.thread(1).run();
    	
    	//开启一个线程抓取行情
		Spider.create(bitCoin86Service).addUrl("http://www.bitcoin86.com/hq/")
		.thread(1).run();
	}

    //链向财经
	private void chainStat() {
		//开启一个线程抓取资讯专栏等
    	Spider.create(chainforService).addUrl("http://www.chainfor.com/home/list/news/data.do")
		.thread(1).run();
    	
    	//开启一个线程抓取快讯
		Spider.create(chainforService).addUrl("http://www.chainfor.com/news/list/flashnewmore/data.do")
		.thread(1).run();
	}

	//金色财经
	private void jinSeStat() {
		//开启一个线程抓取资讯专栏等
    	Spider.create(jscjService).addUrl("http://www.jinse.com/")
		.thread(1).run();
    	
    	//开启一个线程抓取快讯
		Spider.create(jscjService).addUrl("http://www.jinse.com/lives")
		.thread(1).run();
	}
    
}