package com.hykj.ccbrother.service.media;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hykj.ccbrother.model.ArticleModel;
import com.hykj.ccbrother.service.ArticleService;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Selectable;

/**
 * 链向财经
 * @author 封君
 *
 */
@Service
public class ChainforService implements PageProcessor{

	private static final Logger logger = LoggerFactory
			.getLogger(ChainforService.class);
	// 抓取网站的相关配置，包括编码、抓取间隔、重试次数等
	private Site site = Site.me().setRetryTimes(3).setSleepTime(1000);
	
	private static String news = "http://www.chainfor.com/home/list/news/data.do";
	private static String flashnewmore = "http://www.chainfor.com/news/list/flashnewmore/data.do";
	private static String param = "?categoryId=";
	
	private static int articleSourceId = 3;
	
	@Autowired
	private ArticleService articleService;
	
	@Override
	public void process(Page page) {
		//匹配是属于新闻还是快讯
		if(page.getUrl().regex(news).match()){
			//链向财经中用得到的列表Id分别是8 11 13
			for (int i = 8; i < 14; i++) {
				if(i == 9 || i == 10 || i == 12){ //排除9 ， 10 ， 12
					continue;
				}
				//进行深度爬抓  自动请求新闻然后获取页面
				page.addTargetRequest(new Request(news + param +  i).setPriority(0));
			}
			//获取得到的页面url
			String href = page.getRequest().getUrl();
			//获取新闻并存到数据库
			getNews(page, href);
		} else { //就属于快讯
			//获取快讯，下方快讯链接返回的是一个Json字符串
			String result = page.getRawText();
			JSONArray jsonArray = JSON.parseObject(result).getJSONObject("obj").getJSONArray("list");
			//logger.info(all.toString());
			getFlashnew(page, jsonArray);
		}
		
	}
	
	@Override
	public Site getSite() {
		return site;
	}

	/**
	 * 获取快讯
	 * @param page
	 * @param jsonArray
	 */
	private void getFlashnew(Page page, JSONArray jsonArray) {
		for (int i = jsonArray.size() -1; i > 0; i--) {
			
			JSONObject item = jsonArray.getJSONObject(i);
			//获取文章内容
			String content = item.getString("introduction");
			if(content.startsWith("{")){//返回的第一个是{ 意味着它获取的是非正常快讯
				continue;
			}
			Date time = item.getJSONObject("lastUpdateDate").getDate("time");
			
			Map map = new HashMap();
			map.put("createTime", time);
			map.put("classId", 10);//10是分类的快讯id
			map.put("articleSourceId", articleSourceId);//链向财经的Id
			int compare = articleService.compareTime(map);
			if(compare > 0){
				continue;
			}
			
			
			//获取查看原文链接（如果有的话）
			String url = url = "http://www.chainfor.com/live/list.html";
			content = "【链向财经】" + content.trim() ;
			
			ArticleModel articleModel = new ArticleModel();
			articleModel.setCreateTime(time);
			articleModel.setArticleSourceId(articleSourceId);
			articleModel.setClassId(10);
			articleModel.setStatus(0);
			articleModel.setType(2);
			articleModel.setVisitTime(0);
			articleModel.setContent(content);
			articleModel.setUrl(url);
			articleService.create(articleModel);
//			url = "/article/html?id=9" + articleModel.getId();
//			articleModel.setUrl(url);
//			articleService.update(articleModel);
		}
	}
	
	/**
	 * 抓取新闻资讯
	 * @param page
	 * @param href
	 */
	private void getNews(Page page, String href) {
		//判断链接是不是包含有参数 有就是要抓取的页面
		if(href.contains(param)){
			//获取url中的最后一个字符，如果是要爬抓的页面会有一个id
			String substring = href.substring(href.length()-1);
			String str = href.split("=")[1];
			Integer id = Integer.parseInt(str);
			int classId = 0;
			if(id != null && id == 8){
				classId = 4;
			} else if(id != null && id == 11){
				classId = 7;
			} else if(id != null && id == 13){
				classId = 4;
			}
			List<String> all = page.getHtml().xpath("/html/body/li").all();
			//logger.info(all.toString());
			for (int i = 1; i <= all.size(); i++) {
				if(i < 3 && id == 8){ //去掉广告
					continue;
				}
				//获取文章链接
				String url = page.getHtml().xpath("/html/body/li[" + i +"]/div/div[3]/h2/a/@href").get();
				Map map = new HashMap();
				map.put("url", url);
				map.put("classId", classId);
				Integer count = articleService.getCount(map);
				//查询是否已经有此链接 有的话，因为列表是时间倒叙，那么后面的内容也已经插入过了
				if(count > 0){
					continue;
				}
				String title = page.getHtml().xpath("/html/body/li[" + i +"]/div/div[3]/h2/a/text()").get();
				String facePhoto = page.getHtml().$("body > li:nth-child(" + i +") > div > div.m-news-pic.lf > img","src").get();
				String time = page.getHtml().xpath("/html/body/li[" + i +"]/div/div[3]/div/span[2]/text()").get();

				ArticleModel articleModel = new ArticleModel();
				articleModel.setArticleSourceId(articleSourceId);
				articleModel.setFacePhoto(facePhoto);
				articleModel.setStatus(0);
				articleModel.setTitle(title);
				articleModel.setVisitTime(0);
				articleModel.setType(2);
				articleModel.setUrl(url);
				articleModel.setClassId(classId);
				
				if(time.contains("分钟")){
					long minute = Long.parseLong(time.substring(0,1));
					articleModel.setCreateTime(new Date(System.currentTimeMillis() - minute * 60 * 1000));//现在的时间减去已经过了的分钟数
				} else if(time.contains("小时")){
					long hour = Long.parseLong(time.substring(0,1));
					articleModel.setCreateTime(new Date(System.currentTimeMillis() - hour * 60 * 60 * 1000));//现在的时间减去已经过了的小时数
				}else if(time.contains("-")){
					//原来时间为02-01 09:07
					time = "2018-" + time;
					Date date = null;
					try {
						String format = "yy-MM-dd HH:mm";//2018-02-01 09:07:23
						SimpleDateFormat sdf = new SimpleDateFormat(format);  
						long realTime = sdf.parse(time).getTime();
						date = new Date(realTime);
						System.out.println("getFlashnew date:" +date);
					} catch (ParseException e) {
						e.printStackTrace();
					}
					articleModel.setCreateTime(date);
				} else {
					//时间格式为 09:05
					long hour = Long.parseLong(time.split(":")[0]);
					long minute = Long.parseLong(time.split(":")[1]);
					articleModel.setCreateTime(new Date(System.currentTimeMillis() - hour * 60 * 60 * 1000 - minute * 60 * 1000));//现在的时间减去已经过了的小时数
				}
				articleService.create(articleModel);
			}
		}
	}

	public void startCatch(){
		//开启一个线程抓取资讯专栏等
    	Spider.create(this).addUrl("http://www.chainfor.com/home/list/news/data.do")
		.thread(1).run();
    	
    	//开启一个线程抓取快讯
		Spider.create(this).addUrl("http://www.chainfor.com/news/list/flashnewmore/data.do")
		.thread(1).run();
	}
	
	
	//列表
	/*<li class="show" categoryid><a href="javascript:frontPost('/home/list/news/data.do','newList-ul-id','{}',1,true)" onclick="selectedme(this)">最新</a></li> 
    <li categoryid="8"><a href="javascript:frontPost('/home/list/news/data.do','newList-ul-id','{categoryId:8}',1,true)" onclick="selectedme(this)">热点新闻</a></li> 
    <li categoryid="9"><a href="javascript:frontPost('/home/list/news/data.do','newList-ul-id','{categoryId:9}',1,true)" onclick="selectedme(this)">项目专访</a></li> 
    <li categoryid="10"><a href="javascript:frontPost('/home/list/news/data.do','newList-ul-id','{categoryId:10}',1,true)" onclick="selectedme(this)">项目报告</a></li> 
    <li categoryid="11"><a href="javascript:frontPost('/home/list/news/data.do','newList-ul-id','{categoryId:11}',1,true)" onclick="selectedme(this)">精英视点</a></li> 
    <li categoryid="6"><a href="javascript:frontPost('/home/list/news/data.do','newList-ul-id','{categoryId:6}',1,true)" onclick="selectedme(this)">热门视频</a></li> 
    <li categoryid="13"><a href="javascript:frontPost('/home/list/news/data.do','newList-ul-id','{categoryId:13}',1,true)" onclick="selectedme(this)">行业政策</a></li> 
	*///http://www.chainfor.com/news/list/flashnewmore/data.do快讯
	//http://www.chainfor.com/home/list/news/data.do?categoryId=8或者其他
}
