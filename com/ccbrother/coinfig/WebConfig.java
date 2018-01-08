package com.hykj.ccbrother.config;

import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import javax.annotation.PostConstruct;
import java.text.ParseException;
import java.util.Date;

@Configuration
public class WebConfig {//TODO 时间转换类

    // IMPORTANT: keep the order
    private final static String[] dateFormats = {
            "EEE, d MMM yyyy HH:mm:ss z",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd HH:mm:ss.SSSZ",
            "yyyy-MM-dd HH:mm:ssZ",
            "yyyy-MM-dd HH:mm:ss.SSS",
            "yyyy-MM-dd HH:mm:ss"};

    @Autowired
    private RequestMappingHandlerAdapter handlerAdapter;

    @PostConstruct
    public void addConversionConfig() {
        ConfigurableWebBindingInitializer initializer = (ConfigurableWebBindingInitializer)handlerAdapter.getWebBindingInitializer();
        if (initializer.getConversionService() != null) {
            GenericConversionService genericConversionService = (GenericConversionService)initializer.getConversionService();
            genericConversionService.addConverter(new StringToDateConverter());
        }
    }

    public class StringToDateConverter implements Converter<String, Date> {

        @Override
        public Date convert(String dateString) {
            Assert.hasText(dateString, "Null or emtpy date string");
            // Supported date formats:
            // EEE, d MMM yyyy HH:mm:ss z          e.g. Fri, 20 Aug 2016 08:45:15 GMT
            // yyyy-MM-dd HH:mm:ss.SSSZ            e.g. 2016-08-19 17:36:20.235+0800
            // yyyy-MM-dd'T'HH:mm:ss.SSSZ          e.g. 2016-08-19T17:36:20.235+0800
            // yyyy-MM-dd HH:mm:ssZ                e.g. 2016-08-19 17:36:20+0800
            // yyyy-MM-dd HH:mm:ss.SSS (UTC time), e.g. 2016-08-19 09:36:20.235
            // yyyy-MM-dd HH:mm:ss (UTC time),     e.g. 2016-08-19 09:36:20

            Date date = null;
            try {
                date = DateUtils.parseDate(dateString, dateFormats);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if (date == null) {
                String errMsg = String.format("Failed to convert [%s] to [%s] for value '%s'", String.class.toString(), Date.class.toString(), dateString);
                throw new IllegalArgumentException(errMsg);
            }

            return date;
        }
    }
}