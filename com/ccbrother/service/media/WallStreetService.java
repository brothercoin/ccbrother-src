package com.hykj.ccbrother.service.media;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.hykj.ccbrother.model.ArticleModel;
import com.hykj.ccbrother.service.ArticleService;
import com.hykj.ccbrother.utils.DateUtils;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Selectable;

/**
 * 华尔街见闻
 * 
 * @author 封君
 *
 */
@Service
public class WallStreetService implements PageProcessor {

	private static final Logger logger = LoggerFactory
			.getLogger(WallStreetService.class);
	// 抓取网站的相关配置，包括编码、抓取间隔、重试次数等
	private Site site = Site.me().setRetryTimes(3).setSleepTime(1000);
	private String[] dictionaries = { "数字货币", "加密货币", "比特", "比特币", "以太坊",
			"莱特币", "区块链", "币圈", "挖矿", "矿工", "矿机", "矿池", "btc", "eth" }; // 要搜索的字典
	private String articleBase = "https://wallstreetcn.com/search?tab=article&q="; // 文章搜索
	private String liveBase = "https://wallstreetcn.com/search?tab=live&q="; // 快讯

	@Autowired
	private ArticleService articleService;

	private int size = 10;
	private int articleSourceId = 5;

	@Override
	public void process(Page page) {
		Selectable pageUrl = page.getUrl();
		String base = "https://wallstreetcn.com";
		String format = "yyyy-MM-dd HH:mm";
		// 因为哈希的只查一次，所以以此为遍历然后发送
		if (pageUrl.regex(
				"https://wallstreetcn\\.com/search\\?tab=article&q=哈希").match()) {
			int classId = 7;
			for (int i = 1; i <= size; i++) {
				String href = page
						.getHtml()
						.xpath("//*[@id=\"app\"]/div/main/div/div[1]/div/div[2]/div[2]/div[1]/div["
								+ i + "]/div[2]/a/@href").get();
				String url = base + href;
				String title = page
						.getHtml()
						.xpath("//*[@id=\"app\"]/div/main/div/div[1]/div/div[2]/div[2]/div[1]/div["
								+ i + "]/div[2]/a/text()").get();
				Integer urlCount = getUrlCount(url, classId, null);
				if (urlCount > 0) {
					continue;
				}
				getUrlCount(null, 0, title);
				String facePhoto = page
						.getHtml()
						.$("#app > div > main > div > div.wscn-search__content > div > div.wscn-search__content-main > div:nth-child(2) > div.wscn-search__articles > div:nth-child("
								+ i + ") > div.search-article-cover > a > img",
								"src").get();
				String time = page
						.getHtml()
						.xpath("//*[@id=\"app\"]/div/main/div/div[1]/div/div[2]/div[2]/div[1]/div["
								+ i + "]/div[2]/div[2]/span/span/text()").get();
				Date date = DateUtils.getFormatDate(time, format);
				save(title, date, url, null, facePhoto, articleSourceId,
						classId);
			}
			// 然后分别查询走elseif
			for (String dictionariy : dictionaries) {
				String articleUrl = articleBase + dictionariy;
				page.addTargetRequest(articleUrl);
				String liveUrl = liveBase + dictionariy;
				page.addTargetRequest(liveUrl);
			}
		} else if (pageUrl.regex(
				"https://wallstreetcn\\.com/search\\?tab=article").match()) {// 匹配新闻
			int classId = 4;
			// String baseUrl = page.getUrl().get();
			// String name = baseUrl.split("&")[1].split("=")[1];
			for (int i = 1; i <= size; i++) {
				String string = page.getUrl().get();
				String title = page
						.getHtml()
						.xpath("//*[@id=\"app\"]/div/main/div/div[1]/div/div[2]/div[2]/div[1]/div["
								+ i + "]/div[2]/a/text()").get();
				Boolean isBoolean = false;
				for (String dictionariy : dictionaries) {// 判断包含不包含那些关键词
					if (title.contains(dictionariy)) {
						isBoolean = true;
						break;
					}
				}
				if (!isBoolean) {// 如果不包含就退出
					continue;
				}
				if (title.contains("分析") || title.contains("行情")) {
					classId = 7;
				}
				String href = page
						.getHtml()
						.xpath("//*[@id=\"app\"]/div/main/div/div[1]/div/div[2]/div[2]/div[1]/div["
								+ i + "]/div[2]/a/@href").get();
				String url = base + href;
				Integer urlCount = getUrlCount(url, classId, null);
				if (getUrlCount(url, classId, null) > 0) {
					continue;
				}
				int  titleCoun = getTitleCount(classId, title);
				if(titleCoun > 0){
					continue;
				}
				String facePhoto = page
						.getHtml()
						.$("#app > div > main > div > div.wscn-search__content > div > div.wscn-search__content-main > div:nth-child(2) > div.wscn-search__articles > div:nth-child("
								+ i + ") > div.search-article-cover > a > img",
								"src").get();
				String time = page
						.getHtml()
						.xpath("//*[@id=\"app\"]/div/main/div/div[1]/div/div[2]/div[2]/div[1]/div["
								+ i + "]/div[2]/div[2]/span/span/text()").get();
				Date date = DateUtils.getFormatDate(time, format);
				save(title, date, url, null, facePhoto, articleSourceId,
						classId);
			}
		} else if (pageUrl.regex("https://api-prod.wallstreetcn.com").match()) {// 匹配快讯
			int classId = 10;
			String url = "https://wallstreetcn.com/live/blockchain";
			String result = page.getRawText();
			JSONArray items = JSON.parseObject(result).getJSONObject("data")
					.getJSONArray("items");
			for (int i = items.size() - 1; i >= 0; i--) {
				String content = items.getJSONObject(i)
						.getString("content_text").replace("\n", "");// 去掉换行
				content = "【华尔街见闻】" + content;
				Long dateL = items.getJSONObject(i).getLong("display_time") * 1000;
				Date date = new Date(dateL);
				Map map = new HashMap();
				map.put("createTime", date);
				map.put("articleSourceId", articleSourceId);
				map.put("classId", 10);
				int count = articleService.compareTime(map);
				if (count > 0) {
					continue;
				}
				save(null, date, url, content, null, articleSourceId, classId);
			}
		}
	}


	private int getTitleCount(int classId, String title) {
		Map map = new HashMap();
		map.put("classId", classId);
		map.put("title", title);
		int titleCount = articleService.getTitleCount(map);
		return titleCount;
	}

	
	/**
	 * 查询数据库中是否已经存在了
	 * 
	 * @param url
	 * @return
	 */
	private Integer getUrlCount(String url, int classId, String title) {
		Map map = new HashMap();
		map.put("url", url);
		map.put("classId", classId);
		map.put("title", title);
		Integer count = articleService.getCount(map);
		return count;
	}

	/**
	 * 将获取到的公告保存起来 穿入的时间是Date
	 * 
	 * @param title
	 * @param time
	 * @param url
	 * @param articleSourceId
	 * @param classId
	 */
	private void save(String title, Date time, String url, String content,
			String facePhoto, int articleSourceId, int classId) {
		ArticleModel articleModel = new ArticleModel();
		articleModel.setArticleSourceId(articleSourceId);// 官网Id
		articleModel.setClassId(classId);
		articleModel.setVisitTime(0);
		articleModel.setCreateTime(time);
		articleModel.setStatus(0);
		articleModel.setContent(content);
		articleModel.setFacePhoto(facePhoto);
		articleModel.setTitle(title);
		articleModel.setType(2);
		articleModel.setUrl(url);
		articleService.create(articleModel);
	}

	@Override
	public Site getSite() {
		return site;
	}

	public void starCatch() {
		// 开5个线程,先抓哈希派，之后再搜索抓取其他资讯专栏等，多线程会不安全
		Spider.create(this)
				.addUrl("https://wallstreetcn.com/search?tab=article&q="
						+ "哈希派").thread(1).run();

		// 开启一个线程抓取快讯
		// 因为这个需要当前的时间戳才能用，不然不会是最新消息
		long now = System.currentTimeMillis() / 1000;
		Spider.create(this)
				.addUrl("https://api-prod.wallstreetcn.com/apiv1/content/lives?channel=blockchain-channel&client=pc&cursor="
						+ now + "&limit=10").thread(1).run();

	}

}
