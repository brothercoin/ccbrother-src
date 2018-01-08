package com.hykj.ccbrother.base;


import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2017/5/19 0019.
 */
public class AppBack {


    private int status;
    private Object result;
    private String msg;
    private String e_msg;
    private String action;



    public AppBack(int status, String msg){
        this.status=status;
        this.msg=msg;
        this.e_msg="error";
        result=null;
    }

    public AppBack(int status, String msg,String e_msg){
        this.status=status;
        this.msg=msg;
        this.e_msg=e_msg;
        result=null;
    }

    public AppBack(Object data){
        result=data;
        status=0;
        msg="";
    }

    public AppBack(){
        result=null;
        status=0;
        msg="成功";
        e_msg="success";
    }

    public AppBack add(String key,Object value){//返回格式result内容变成键值对
        if(result==null){
            result=new HashMap<String,Object>();
        }
        if(result instanceof Map){
            ( (Map )result).put(key,value);
        }
        return this;
    }

    public Object get(String key){//获取值
        if(result==null){
           return null;
        }
        if(result instanceof Map){
           return  ( (Map )result).get(key);
        }
        return null;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getE_msg() {
        return e_msg;
    }

    public void setE_msg(String e_msg) {
        this.e_msg = e_msg;
    }
}
