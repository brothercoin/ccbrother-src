package com.hykj.ccbrother.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;


public class IntegerUtil {

    public static void main(String[] args){
        int a=1;
        Map map=new HashMap();
        map.put("a",1);
        logger.info(objToInt(map.get("a"))+"");
        map.put("a","1");
        logger.info(objToInt(map.get("a"))+"");
        map.put("a",new ThreadPoolExecutor.AbortPolicy());
        logger.info(objToInt(map.get("a"))+"");
    }

	private static Logger logger = LoggerFactory.getLogger(IntegerUtil.class);

    static public int objToInt(Object object) throws NumberFormatException {
        if(object==null){
            throw new NumberFormatException();
        }
        if (object instanceof Integer) {
            logger.info("Integer");
            return (Integer)object;
        }
        logger.info("Obj");
        return  Integer.parseInt(object.toString());


    }




}
