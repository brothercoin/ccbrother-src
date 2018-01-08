package com.hykj.ccbrother.base;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface BaseMapper<T extends BaseModel> {

   T getById(@Param("id") int id);

   List<T> getList(Map map);

   int getCount(Map map);

   int update(T model);

   int create(T model);

    int delete(Map map);

   int deleteById(@Param("id") int id);

}
