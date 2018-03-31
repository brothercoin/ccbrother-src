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

import com.hykj.ccbrother.model.ArticleModel;
import com.hykj.ccbrother.service.ArticleService;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;
/**
 * 金色财经爬抓
 * 因为正则校验太麻烦，所以在一个方法内实现资讯和快讯的爬虫，会报错但是无碍（已解决）
 * 但是可以通过完善正则或者拆分到另一个类中可以停下异常
 * @author 封君
 *
 */
@Service
public class JSCJService implements PageProcessor{

	private static final Logger logger = LoggerFactory
			.getLogger(JSCJService.class);
	// 抓取网站的相关配置，包括编码、抓取间隔、重试次数等
	private Site site = Site.me().setRetryTimes(3).setSleepTime(1000);
	
	@Autowired
	private ArticleService articleService;
	
	private static int size = 10;
	private static int articleSourceId = 2;
	
	@Override
	public void process(Page page) {

		//匹配是否和快讯类似
		if(page.getUrl().regex("http://www.jinse.com/lives").match()){
			//获取快讯
			//获取快讯的日期
			String span = page.getHtml().$("body > div.wrap.margin-b.clearfix > div.wrapleft.marginb.left.slive > div > div.con-item.clearfix.lost-area > div.time > div > span:nth-child(2)").get();
			String day = span.substring(7, 17).replace(".", "-");  //将<span>2018.01.31..格式换成2018-01-31
			//获取要抓的快讯列表 （只提供长度）
			List<String> list = page.getHtml().xpath("//*[@id=\"lost-" + day + "\"]/li").all();
			//logger.info(list.toString());
			arrayFlashNews(page, day, list.size());
		} else {
			
			//获取资讯专家等分类内容  1是热点，2是资产大鳄 3是分析 4深度 5百科 6 专访
			for (int i = 1; i < 7; i++) {
				if(i < 3){
					arrayInformation(page, size, i, 4);//设置成资讯分类 10是设置抓取后遍历获得的次数
				} else if(i == 3 || i == 4){
					arrayInformation(page, size, i, 7);//设置成分析分类
				} else if(i == 6){
					arrayInformation(page, size, i, 13);//设置成专家分类
				}
			}
		}
	}

	@Override
	public Site getSite() {
		return site;
	}
	
	/**
	 * 
	 * @param page
	 * @param day 快讯的当天时间
	 * @param size 快讯的列长度
	 */
	private void arrayFlashNews(Page page, String day, int size) {
		int classId = 10;
		for(int i = size ; i > 0 ; i--){
			Map condition = new HashMap();
			//获取时分
			String time = page.getHtml().xpath("//*[@id=\"lost-" + day + "\"]/li[" + i + "]/section/div[1]/p[1]/text()").get();
			String rt = day +" " + time;
			String format = "yyyy-MM-dd HH:mm";
			SimpleDateFormat sdf = new SimpleDateFormat(format);
			Date date = null;
			try {
				long realTime = sdf.parse(rt).getTime();
				date = new Date(realTime);
				condition.put("createTime", date);
				condition.put("articleSourceId", articleSourceId);//2为金色财经
				condition.put("classId", classId);		//4为新闻 7为行情分析 10为快讯  13为专家 16为中文媒体
				int compare = articleService.compareTime(condition);
				if(compare > 0){
					continue;
				}
			} catch (ParseException e) {
				e.printStackTrace();
			}
			
			//获取文章内容
			String content = page.getHtml().xpath("//*[@id=\"lost-" + day + "\"]/li[" + i + "]/section/div[2]/a/text()").get();
			//获取查看原文链接（如果有的话）
			String url = page.getHtml().$("#lost-" + day + " > li:nth-child(" + i + ") > section > div.live-info > span > a","href").get();
			content =  "【金色财经】" + content.trim();
			
			ArticleModel articleModel = new ArticleModel();
			articleModel.setCreateTime(date);
			articleModel.setArticleSourceId(articleSourceId);
			articleModel.setClassId(classId);
			articleModel.setStatus(0);
			articleModel.setVisitTime(0);
			articleModel.setType(2);
			articleModel.setContent(content);
			if(url != null){
				articleModel.setUrl(url);
			} else {
				url = "http://www.chainfor.com/live/list.html";
				articleModel.setUrl(url);
			}
			articleService.create(articleModel);
//			url = "/article/html?id=9" + articleModel.getId();
//			articleModel.setUrl(url);
//			articleService.update(articleModel);
		}
	}

	/**
	 * 抓取资讯信息内容数据，存到数据库
	 * @param page获取的页面	
	 * @param urlCounts遍历的次数
	 * @param column在页面内属于第几列 一共6列 每一列都不同的分类
	 * @param classId文章分类 自己项目的分类
	 */
	private void arrayInformation(Page page,int urlCounts, int column , int classId) {
		for(int i = 1; i <= urlCounts; i++){//根据长度来获取内容，实际上List只提供了一个size，下面与其已经无关
//			if(i == 1 && column == 1){//去掉置顶广告
//				continue;
//			}
			String time = page.getHtml().xpath("//*[@id=\"main1\"]/div["+column+"]/ol["+i+"]/ul/ul/li[2]/text()").get();//截取时间串
			if(time == null){//去掉普通广告
				continue;
			}
			String url = page.getHtml().xpath("//*[@id=\"main1\"]/div["+column+"]/ol["+i+"]/a/@href").get();//获取文章的链接
			Map condition = new HashMap();
			condition.put("url", url);
			condition.put("classId", classId);
			//判断数据库中是否存在，存在的话，就退出循环，因为获取的日期是倒叙的后面的也会存在
			Integer count = articleService.getCount(condition);
			if(count > 0){
				continue;
			}
			
			ArticleModel articleModel = new ArticleModel();
			articleModel.setArticleSourceId(articleSourceId);
			articleModel.setClassId(classId);
			articleModel.setUrl(url);
			articleModel.setVisitTime(0);
			
			String title = page.getHtml().$("#main1 > div:nth-child("+column+") > ol:nth-child("+i+") > a","title").get();//获取a标签里的title属性值
			//获取略缩图
			String facePhoto = page.getHtml().xpath("//*[@id=\"main1\"]/div["+column+"]/ol["+i+"]/a/img/@data-original").get();
			articleModel.setTitle(title);
			articleModel.setFacePhoto(facePhoto);
			
			//将获取的时间字符串转换成Date类型存到数据库
			if(time.contains("刚刚")){
				articleModel.setCreateTime(new Date());
			}else if (time.contains("分钟")){
				String sub = time.substring(time.length()-5, time.length() -1);
				Long dateAgo = Long.parseLong(sub.substring(0,2).trim());//截取前两位转换
				Long realTime = System.currentTimeMillis() - dateAgo * 60 * 1000;
				Date date = new Date(realTime);
				articleModel.setCreateTime(date);
			}else if(time.contains("小时")){
				String sub = time.substring(time.length()-5, time.length() -1);
				Long dateAgo = Long.parseLong(sub.substring(0,2).trim());
				Long realTime = System.currentTimeMillis() - dateAgo * 60 * 60 * 1000;
				Date date = new Date(realTime);
				articleModel.setCreateTime(date);
			}else {
				String[] split = time.split(" · ");
				try {
					String format = "yyyy/MM/dd HH:mm";
					SimpleDateFormat sdf = new SimpleDateFormat(format);  
					long realTime = sdf.parse(split[1]).getTime();
					Date date = new Date(realTime);
					articleModel.setCreateTime(date);
				} catch (ParseException e) {
					e.printStackTrace();
				} 
			}
			articleModel.setStatus(0);
			articleModel.setType(2);
			articleService.create(articleModel);
		}
	}
	
}
