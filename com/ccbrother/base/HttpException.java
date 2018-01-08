package com.hykj.ccbrother.base;

/**
 * 请求网络错误，打印少量
 */
public class HttpException extends RuntimeException {


    private String msg;

    private Integer status=-20;

    public HttpException(String msg){
        super(msg);

        this.msg=msg;
    }

    public HttpException(int status, String msg){
        super(msg);
        this.status=status;
        this.msg=msg;
    }
    public HttpException(Exception e){
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
