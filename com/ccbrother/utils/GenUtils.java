package com.hykj.ccbrother.utils;


import com.hykj.ccbrother.auto.ColumnEntity;
import com.hykj.ccbrother.auto.TableEntity;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 代码生成器   工具类
 *
 * @author chenshun
 * @email sunlightcs@gmail.com
 * @date 2016年12月19日 下午11:40:24
 */
public class GenUtils {

    public static List<String> getTemplates(){
        List<String> templates = new ArrayList<String>();
        templates.add("template/Entity.java.vm");
        templates.add("template/Dao.java.vm");
        templates.add("template/Dao.xml.vm");
        templates.add("template/Service.java.vm");
        templates.add("template/Controller.java.vm");
        templates.add("template/List.html.vm");
        templates.add("template/Add.html.vm");
        templates.add("template/Detail.html.vm");
        templates.add("template/Edit.html.vm");
        return templates;
    }

    /**
     * 生成代码
     */
    public static void generatorCode(Map<String, String> table,
                                     List<Map<String, String>> columns, ZipOutputStream zip){
        //配置信息
        Configuration config = getConfig();

        //表信息
        TableEntity tableEntity = new TableEntity();
        tableEntity.setTableName(table.get("tableName"));
        tableEntity.setComments(table.get("tableComment"));
        //表名转换成Java类名
        String className = tableToJava(tableEntity.getTableName(), config.getString("tablePrefix"));
        tableEntity.setClassName(className);
        tableEntity.setClassname(StringUtils.uncapitalize(className));

        //列信息
        List<ColumnEntity> columsList = new ArrayList<>();
        int hasStatus=0;
        int hasUserId=0;
        for(Map<String, String> column : columns){
            ColumnEntity columnEntity = new ColumnEntity();
            columnEntity.setColumnName(column.get("columnName"));
            columnEntity.setDataType(column.get("dataType"));
            columnEntity.setComments(column.get("columnComment"));
            columnEntity.setExtra(column.get("extra"));

            //列名转换成Java属性名
            String attrName = columnToJava(columnEntity.getColumnName());
  //         String attrName = columnEntity.getColumnName();
            columnEntity.setAttrName(attrName);
            columnEntity.setAttrname(StringUtils.uncapitalize(attrName));

            //列的数据类型，转换成Java类型
            String attrType = config.getString(columnEntity.getDataType(), Object.class.getSimpleName());
            columnEntity.setAttrType(attrType);

            //是否主键
            if("PRI".equalsIgnoreCase(column.get("columnKey")) && tableEntity.getPk() == null){
                tableEntity.setPk(columnEntity);
            }

            columsList.add(columnEntity);
            if("userId".equals(columnEntity.getAttrname())){
                hasUserId=1;
            }
            if("status".equals(columnEntity.getAttrname())){
                hasStatus=1;
            }
        }
        tableEntity.setHasStatus(hasStatus);
        tableEntity.setHasUserId(hasUserId);
        tableEntity.setColumns(columsList);

        //没主键，则第一个字段为主键
        if(tableEntity.getPk() == null){
            tableEntity.setPk(tableEntity.getColumns().get(0));
        }

        //设置velocity资源加载器
        Properties prop = new Properties();
        prop.put("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init(prop);

        //封装模板数据
        Map<String, Object> map = new HashMap<>();
        map.put("tableName", tableEntity.getTableName());
        map.put("comments", tableEntity.getComments());
        map.put("pk", tableEntity.getPk());
        map.put("hasUserId", tableEntity.getHasUserId());
        map.put("hasStatus", tableEntity.getHasStatus());
        map.put("className", tableEntity.getClassName());
        map.put("classname", tableEntity.getClassname());
        map.put("pathName", tableEntity.getClassname().toLowerCase());
        map.put("columns", tableEntity.getColumns());
        map.put("package", config.getString("package"));
        map.put("author", config.getString("author"));
        map.put("email", config.getString("email"));
        map.put("datetime", DateUtils.format(new Date(), DateUtils.DATE_TIME_PATTERN));
        VelocityContext context = new VelocityContext(map);

        //获取模板列表
        List<String> templates = getTemplates();
        for(String template : templates){
            //渲染模板
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, "UTF-8");
            tpl.merge(context, sw);

            try {
                //添加到zip
                zip.putNextEntry(new ZipEntry(getFileName(template, tableEntity.getClassName(), config.getString("package"),tableEntity.getClassname(),tableEntity.getClassname().toLowerCase())));
                IOUtils.write(sw.toString(), zip, "UTF-8");
                IOUtils.closeQuietly(sw);
                zip.closeEntry();
            } catch (IOException e) {
                throw new RuntimeException("渲染模板失败，表名：" + tableEntity.getTableName(), e);
            }
        }
    }


    /**
     * 列名转换成Java属性名
     */
    public static String columnToJava(String columnName) {
        return WordUtils.capitalizeFully(columnName, new char[]{'_'}).replace("_", "");
    }

    /**
     * 表名转换成Java类名
     */
    public static String tableToJava(String tableName, String tablePrefix) {
        if(StringUtils.isNotBlank(tablePrefix)){
            tableName = tableName.replace(tablePrefix, "");
        }
        return columnToJava(tableName);
    }

    /**
     * 获取配置信息
     */
    public static Configuration getConfig(){
        try {
            return new PropertiesConfiguration("generator.properties");
        } catch (ConfigurationException e) {
            throw new RuntimeException("获取配置文件失败，", e);
        }
    }

    /**
     * 获取文件名
     */
    public static String getFileName(String template, String className, String packageName, String classname, String pathname){
        String packagePath = "main" + File.separator + "java" + File.separator;
        if(StringUtils.isNotBlank(packageName)){
            packagePath += packageName.replace(".", File.separator) + File.separator;
        }

        if(template.contains("Entity.java.vm")){
            return packagePath + "model" + File.separator + className + "Model.java";
        }


        if(template.contains("Dao.java.vm")){
            return packagePath + "mapper" + File.separator + className + "Mapper.java";
        }

        if(template.contains("Service.java.vm")){
            return packagePath + "service" + File.separator + className + "Service.java";
        }

        if(template.contains("Controller.java.vm")){
            return packagePath + "controller" + File.separator + className + "Controller.java";
        }

        if(template.contains("Dao.xml.vm")){
            return "main" + File.separator + "resources" + File.separator + "mybatis" + File.separator + className + "Mapper.xml";
        }

        if(template.contains("List.html.vm")){
            return "main" + File.separator + "resources" + File.separator + "static" + File.separator +"admin" + File.separator +pathname + File.separator+ classname + "List.html";
        }
        if(template.contains("Add.html.vm")){
            return "main" + File.separator + "resources" + File.separator + "static" + File.separator +"admin" + File.separator +pathname + File.separator+ classname + "Add.html";
        }
        if(template.contains("Detail.html.vm")){
            return "main" + File.separator + "resources" + File.separator + "static" + File.separator +"admin" + File.separator +pathname + File.separator+ classname + "Detail.html";
        }
        if(template.contains("Edit.html.vm")){
            return "main" + File.separator + "resources" + File.separator + "static" + File.separator +"admin" + File.separator +pathname + File.separator+ classname + "Edit.html";
        }

        return null;
    }


}
