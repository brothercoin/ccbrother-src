package com.hykj.ccbrother.base;

/**
 * Created by Administrator on 2017/5/20 0020.
 */
public class MsgException extends RuntimeException {


    private String msg;

    private Integer status=-10;

    public MsgException( String msg){
        this.msg=msg;
    }

    public MsgException(int status, String msg){
        this.status=status;
        this.msg=msg;
    }
    public MsgException(Exception e){
        super(e);
    }


    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }


    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
