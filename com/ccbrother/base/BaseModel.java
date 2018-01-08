package com.hykj.ccbrother.base;

import com.alibaba.fastjson.annotation.JSONField;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by Administrator on 2017/7/7 0007.
 */
public abstract class BaseModel implements Serializable {

    protected Integer id;

    protected Date create_time;

    @JSONField(serialize = false)
    protected boolean backstageFlag = false;//true 表示在后台

    public void setBackstageFlag(boolean backstageFlag) {
        this.backstageFlag = backstageFlag;
    }

    public boolean isBackstageFlag() {
        return backstageFlag;
    }

    public Date getCreate_time() {
        return create_time;
    }

    public void setCreate_time(Date create_time) {
        this.create_time = create_time;
    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
