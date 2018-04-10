package com.hykj.ccbrother.config;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.Cache;  
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
/**
 * Redis缓存配置类
 * @author 封君
 *
 */
@Configuration
@EnableCaching//启用缓存
@Component
public class RedisConfig extends CachingConfigurerSupport{

	private RedisTemplate<Serializable, Serializable> redisTemplate;
	
	@Autowired
    public void setRedisTemplate(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
	public RedisTemplate getRedisTemplate() {
        return this.redisTemplate = redisTemplate;
    }
	@SuppressWarnings("rawtypes")
    @Bean
    public CacheManager cacheManager(RedisTemplate redisTemplate) {
        RedisCacheManager rcm = new RedisCacheManager(redisTemplate);
        //设置缓存过期时间
        Map<String, Long> expires = new HashMap<>();
        expires.put("12h",3600 * 12L);
        expires.put("1h",3600 * 1L);
        expires.put("10s",10L);
        expires.put("article",60 * 5L);
        expires.put("articleclass",3600 * 1L);
        expires.put("coin",3600 * 1L);
        expires.put("config", 3600 * 24L);
        expires.put("platMessage", 3600 * 24L * 3);
        expires.put("versionUpgrade", 3600 * 24L * 1);
        expires.put("app", 3600 * 24L * 1);
        
        rcm.setExpires(expires);
        //设置缓存过期时间(秒)默认
        rcm.setDefaultExpiration(600);
        return rcm;
    }
	
	/**
	 * redis模板，存储关键字是字符串，值是Jdk序列化
	 */
	@Bean
    public RedisTemplate<Serializable, Serializable> redisTemplate(
            JedisConnectionFactory redisConnectionFactory) {
		//本类已经在顶部设置
        //RedisTemplate<Serializable, Serializable> redisTemplate = new RedisTemplate<Serializable, Serializable>();
        //key序列化方式;（不然会出现乱码;）,但是如果方法上有Long等非String类型的话，会报类型转换错误；
        //所以在没有自己定义key生成策略的时候，以下这个代码建议不要这么写，可以不配置或者自己实现 ObjectRedisSerializer
        //或者JdkSerializationRedisSerializer序列化方式;（已实现）
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);
        
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        
//        redisTemplate.setKeySerializer(new StringRedisSerializer());
//        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
//        redisTemplate
//                .setValueSerializer(new JdkSerializationRedisSerializer());
//        redisTemplate
//                .setHashValueSerializer(new JdkSerializationRedisSerializer());
        //以上4条配置可以不用
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }
	
	 /**
     * 自定义key.
     * 此方法将会根据类名+方法名+所有参数的值生成唯一的一个key（在方法中指定key则此方法自动忽略）
     */
    @Override
    public KeyGenerator keyGenerator() {
    	System.out.println("aaaaaaaaa");
       return new KeyGenerator() {
           @Override
           public Object generate(Object o, Method method, Object... objects){ 
              StringBuilder sb = new StringBuilder();
              sb.append(o.getClass().getName());
              sb.append(method.getName());
        	  for (Object obj : objects) {
        		  sb.append(":" + obj);
        		  if(obj != null){
                      sb.append(obj.toString());
        		  }
              }
              return sb.toString();
           }
       };
    }
	
    /**
     * 如果没有自定义此方法，那么当redis服务器报错，那么前端请求也会停止而不继续往数据库查询（未测）
     */
    @Bean  
    @Override  
    public CacheErrorHandler errorHandler() {  
        CacheErrorHandler cacheErrorHandler = new CacheErrorHandler() {  
            @Override  
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {  
                //System.out.println(key);  
            }  
  
            @Override  
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {  
                //System.out.println(value);  
            }  
  
            @Override  
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {  
  
            }  
  
            @Override  
            public void handleCacheClearError(RuntimeException e, Cache cache) {  
  
            }  
        };  
        return cacheErrorHandler;  
    }  
}
