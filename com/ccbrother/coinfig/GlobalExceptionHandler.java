package com.hykj.ccbrother.config;


import com.hykj.ccbrother.base.AppBack;
import com.hykj.ccbrother.base.HttpException;
import com.hykj.ccbrother.base.MsgException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@RestController
@ControllerAdvice
class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BindException.class)
    @ResponseBody
    public AppBack constraintHandler() {
        AppBack appBack = new AppBack(-11, "请检查输入是否正确","input error");
        return appBack;
    }

    @ExceptionHandler(MsgException.class)
    @ResponseBody
    public AppBack MsgException(MsgException e) {
        AppBack appBack = new AppBack(e.getStatus(), e.getMsg());
        logger.error(e.getMessage(), e);
        return appBack;
    }


    @ExceptionHandler(HttpException.class)
    @ResponseBody
    public AppBack HttpException(MsgException e) {
        AppBack appBack = new AppBack(e.getStatus(), e.getMsg());
        logger.error(e.getMessage());
        return appBack;
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public AppBack exceptionHandler(HttpServletRequest request, Exception e) {
        logger.info(e.getClass().getName());
        AppBack appBack = new AppBack(-97, "出现异常","error");
        String info = "";
        Map<String, String[]> map = request.getParameterMap();
        Set<Map.Entry<String, String[]>> set = map.entrySet();
        Iterator<Map.Entry<String, String[]>> it = set.iterator();
        while (it.hasNext()) {
            Map.Entry<String, String[]> entry = it.next();
            info += "  | " + entry.getKey() + "  - ";
            for (String i : entry.getValue()) {
                info += i + "  ";
            }
        }
        appBack.setAction(request.getRequestURI() + info);
        appBack.setResult(e.getMessage());
        logger.error(request.getRequestURI());
        logger.error(e.getMessage(), e);
        return appBack;
    }

}