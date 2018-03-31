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

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;

import com.hykj.ccbrother.model.ArticleModel;
import com.hykj.ccbrother.service.ArticleService;
/**
 * bitcoin资讯分析抓取
 * @author 封君
 *
 */
@Service
public class BitCoin86Service implements PageProcessor{
	private static final Logger logger = LoggerFactory
			.getLogger(BitCoin86Service.class);
	// 抓取网站的相关配置，包括编码、抓取间隔、重试次数等
	private Site site = Site.me().setRetryTimes(3).setSleepTime(1000);
	@Autowired
	private ArticleService articleService;
	private static int articleSourceId = 4;
	private String base = "http://www.bitcoin86.com";
	
	@Override
	public void process(Page page) {
		
		//匹配获取新闻资讯
		if(page.getUrl().regex("http://www\\.bitcoin86\\.com/news/").match()){
			int classId = 4;
			getInformation(page , classId); 
		//匹配获取行情分析
		}else if (page.getUrl().regex("http://www\\.bitcoin86\\.com/hq/").match()){
			int classId = 7;
			getInformation(page , classId); 
		}
	}

	@Override
	public Site getSite() {
		return site;
	}
	
	private void getInformation(Page page, int classId) {
		//获取列表长度
		//List<String> all = page.getHtml().xpath("/html/body/section/div/div/article").all();太长去掉
		for (int i = 1; i < 10; i++) {
			
			//获取Lianjie
			String href = page.getHtml().$("body > section > div > div > article:nth-child(" + i + ") > a", "href").get();
			String url = base + href;
			Map map = new HashMap();
			map.put("url", url);
			map.put("classId", classId);
			Integer count = articleService.getCount(map);
			if(count > 0){
				continue;
			}
	
			//时间只能获取日期不能获取时分，以后定时刷新，以当时刷新的时间为真实时间
//			String time = null;
//			//获取新闻时的时间获取方式
//			if(classId == 4){
//				time = page.getHtml().xpath("/html/body/section/div/div/article[" + i + "]/p[1]/time/font/text()").get();
//			}
//			//获取分析时的时间获取方式
//			if(classId == 7){
//				time = page.getHtml().xpath("/html/body/section/div/div/article[" + i + "]/p[1]/time/text()").get();
//			}
//			String format = "yyyy-MM-dd";
//			SimpleDateFormat sdf = new SimpleDateFormat(format);  
//			long realTime = 0;
//			try {
//				realTime = sdf.parse(time).getTime();
//			} catch (ParseException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}	
			Date date = new Date();
			String img = page.getHtml().$("body > section > div > div > article:nth-child(" + i + ") > a > img", "data-original").get();
			String facePhoto = base + img;
			String title = page.getHtml().$("body > section > div > div > article:nth-child(" + i + ") > a > img", "alt").get();
			
			ArticleModel articleModel = new ArticleModel();
			articleModel.setArticleSourceId(articleSourceId);
			articleModel.setClassId(classId);
			articleModel.setCreateTime(date);
			articleModel.setFacePhoto(facePhoto);
			articleModel.setStatus(0);
			articleModel.setTitle(title);
			articleModel.setType(2);
			articleModel.setVisitTime(0);
			articleModel.setUrl(url);
			articleService.create(articleModel);
		}
	}
}
