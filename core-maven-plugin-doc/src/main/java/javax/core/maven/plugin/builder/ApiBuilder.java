package javax.core.maven.plugin.builder;

import javax.core.common.doc.annotation.*;
import javax.core.maven.plugin.constants.DocConstants;
import javax.core.maven.plugin.utils.ObjectUtils;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class ApiBuilder {
    private Method m;
    private String mdesc;
    private String name;
    private String author;
    private String createtime;
    private boolean checkEnc;
    private boolean checkIp;
    private boolean checkVersion;
    private Demo demo;
    private Map<String, ApiColumnBuilder> paramObject;
    private Map<String, ApiColumnBuilder> returnObject;
    private Map<String, String> paramDesc;
    private List<String[]> returnList;
    //    private List<String> paramsType;
    private Map<String, List<ApiColumnBuilder>> modelCacheMap;

    private List<ApiColumnBuilder> resultObjectList;

    private List<ApiColumnBuilder> paramsObjectList;

    public ApiBuilder(Method m) {
        this.m = m;
        this.returnList = new ArrayList<String[]>();
//        this.paramsType = new ArrayList<String>();
        this.paramDesc = new LinkedHashMap<String, String>();
        this.paramObject = new LinkedHashMap<String, ApiColumnBuilder>();
        this.returnObject = new LinkedHashMap<String, ApiColumnBuilder>();
        this.modelCacheMap = new HashMap<String, List<ApiColumnBuilder>>();
        this.resultObjectList = new ArrayList<ApiColumnBuilder>();
        this.paramsObjectList = new ArrayList<ApiColumnBuilder>();
    }

    public ApiBuilder builder() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        //获取我们自定义的API配置信息
        if (m.isAnnotationPresent(Api.class)) {
            Annotation a = m.getAnnotation(Api.class);

            Method nm = a.getClass().getMethod("name", null);
            name = nm.invoke(a).toString();

            Method dm = a.getClass().getMethod("desc", null);
            mdesc = dm.invoke(a).toString();

            Method am = a.getClass().getMethod("author", null);
            author = am.invoke(a).toString();

            Method cm = a.getClass().getMethod("createtime", null);
            createtime = cm.invoke(a).toString();

            // 获取注解上定义的参数
            Method pm = a.getClass().getMethod("params", null);
            Rule[] params = (Rule[]) pm.invoke(a);

            for (Rule param : params) {
                Class<?> clazz = param.clazz();
                paramDesc.put(param.name(), param.desc());
                parseApiColumnRequest(clazz, param.name());
            }

            // 如果里面定义的是类
            Method paramsObject = a.getClass().getMethod("paramsObject", null);
            Class<?> clazz = (Class<?>) paramsObject.invoke(a);
            parseApiColumnRequest(clazz);


            Method rm = a.getClass().getMethod("returns", null);
            Rule[] returns = (Rule[]) rm.invoke(a);

            for (Rule r : returns) {
                returnList.add(new String[]{r.name(), "无", r.type(), r.desc()});
                Class<?> paramsClazz = r.clazz();
                // 第一级
                ApiColumnBuilder apiColumnBuilder = new ApiColumnBuilder(r.name(), r.type(), r.desc(), "", false);
                // 递归可能多级
                parseApiColumnResponse(paramsClazz, apiColumnBuilder);
                resultObjectList.add(apiColumnBuilder);
            }

            Method auth = a.getClass().getMethod("auth", null);
            Auth[] auths = (Auth[]) auth.invoke(a);
            for (Auth au : auths) {
                checkEnc = au.checkEnc();

                checkIp = au.checkIp();

                checkVersion = au.checkVersion();

            }

            Method dem = a.getClass().getMethod("demo", null);
            Object o = dem.invoke(a);

            if (o != null) {
                demo = (Demo) o;
            }
        }

        return this;
    }

//    private void parseApiColumnResponse(Class<?> returnsClazz, String name, ApiColumnBuilder apiColumnBuilder) {
//        if (!ObjectUtils.isNull(returnsClazz)) {
//            Field[] fields = returnsClazz.getDeclaredFields();
//            for (Field field : fields) {
//                String type = field.getType().getSimpleName();
//                String name = field.getName();
//                ApiColumn apiColumn = field.getAnnotation(ApiColumn.class);
//                if (ObjectUtils.isNull(apiColumn)) {
//                    continue;
//                }
//                ApiColumnBuilder apiBuilder = new ApiColumnBuilder(name, type, apiColumn.desc(), apiColumn.defaultValue(), apiColumn.isRequired());
//                returnList.add(new String[]{name, type, apiColumn.desc()});
//            }
//        }
//    }

    /**
     * 解析apiColumn返回值注解
     *
     * @param returnsClazz
     * @param apiColumnBuilder
     */
    private void parseApiColumnResponse(Class<?> returnsClazz, ApiColumnBuilder apiColumnBuilder) {
        if (!ObjectUtils.isNull(returnsClazz)) {
            List<ApiColumnBuilder> apiColumnBuilders = modelCacheMap.get(returnsClazz.getSimpleName());
            if (ObjectUtils.isNull(apiColumnBuilders)) {
                Field[] fields = returnsClazz.getDeclaredFields();
                apiColumnBuilders = new ArrayList<ApiColumnBuilder>();
                // 避免死循环
                modelCacheMap.put(returnsClazz.getSimpleName(), apiColumnBuilders);
                for (Field field : fields) {
                    String type = field.getType().getSimpleName();
                    String name = field.getName();
                    ApiColumn apiColumn = field.getAnnotation(ApiColumn.class);
                    if (ObjectUtils.isNull(apiColumn)) {
                        continue;
                    }
                    ApiColumnBuilder apiBuilder = new ApiColumnBuilder(name, type, apiColumn.desc(), apiColumn.defaultValue(), apiColumn.isRequired());
                    //returnList.add(new String[]{name, parentName, type, apiColumn.desc()});
                    apiColumnBuilders.add(apiBuilder);
                    // 如果对象类型不是枚举中定义的,可能需要递归对象查找
                    if (ObjectUtils.isNull(DocConstants.types.get(type.toLowerCase()))) {
                        parseApiColumnResponse(field.getType(), apiBuilder);
                    }
                }
            }
            apiColumnBuilder.setApiColumnBuilderNodes(apiColumnBuilders);
        }
    }

    private void parseApiColumnRequest(Class<?> clazz) {
        if (!ObjectUtils.isNull(clazz)) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                String type = field.getType().getSimpleName();
                String name = field.getName();
                ApiColumn apiColumn = field.getAnnotation(ApiColumn.class);
                if (ObjectUtils.isNull(apiColumn)) {
                    continue;
                }
                ApiColumnBuilder apiBuilder = new ApiColumnBuilder(name, type, apiColumn.desc(), apiColumn.defaultValue(), apiColumn.isRequired());
                if (ObjectUtils.isNull(DocConstants.types.get(type.toLowerCase()))) {
                    parseApiColumnResponse(field.getType(), apiBuilder);
                }
                paramsObjectList.add(apiBuilder);
            }
        }
    }

    private void parseApiColumnRequest(Class<?> clazz, String parentName) {
        if (!ObjectUtils.isNull(clazz)) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                String type = field.getType().getSimpleName();
                String name = field.getName();
                ApiColumn apiColumn = field.getAnnotation(ApiColumn.class);
                if (ObjectUtils.isNull(apiColumn)) {
                    continue;
                }
                ApiColumnBuilder apiBuilder = new ApiColumnBuilder(name, type, apiColumn.desc(), apiColumn.defaultValue(), apiColumn.isRequired());
                parseApiColumnResponse(clazz, apiBuilder);
                paramObject.put(parentName, apiBuilder);
//                paramsType.add(type);
            }
        }
    }

    public List<ApiColumnBuilder> getResultObjectList() {
        return resultObjectList;
    }

    public void setResultObjectList(List<ApiColumnBuilder> resultObjectList) {
        this.resultObjectList = resultObjectList;
    }

//    public List<String> getParamsType() {
//        return paramsType;
//    }
//
//    public void setParamsType(List<String> paramsType) {
//        this.paramsType = paramsType;
//    }

    public Method getM() {
        return m;
    }

    public void setM(Method m) {
        this.m = m;
    }

    public String getMdesc() {
        return mdesc;
    }

    public void setMdesc(String mdesc) {
        this.mdesc = mdesc;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCreatetime() {
        return createtime;
    }

    public void setCreatetime(String createtime) {
        this.createtime = createtime;
    }

    public boolean isCheckEnc() {
        return checkEnc;
    }

    public void setCheckEnc(boolean checkEnc) {
        this.checkEnc = checkEnc;
    }

    public boolean isCheckIp() {
        return checkIp;
    }

    public void setCheckIp(boolean checkIp) {
        this.checkIp = checkIp;
    }

    public boolean isCheckVersion() {
        return checkVersion;
    }

    public void setCheckVersion(boolean checkVersion) {
        this.checkVersion = checkVersion;
    }

    public Demo getDemo() {
        return demo;
    }

    public void setDemo(Demo demo) {
        this.demo = demo;
    }

    public Map<String, ApiColumnBuilder> getParamObject() {
        return paramObject;
    }

    public Map<String, List<ApiColumnBuilder>> getModelCacheMap() {
        return modelCacheMap;
    }

    public void setModelCacheMap(Map<String, List<ApiColumnBuilder>> modelCacheMap) {
        this.modelCacheMap = modelCacheMap;
    }

    public void setParamObject(Map<String, ApiColumnBuilder> paramObject) {
        this.paramObject = paramObject;
    }

    public Map<String, ApiColumnBuilder> getReturnObject() {
        return returnObject;
    }

    public void setReturnObject(Map<String, ApiColumnBuilder> returnObject) {
        this.returnObject = returnObject;
    }

    public Map<String, String> getParamDesc() {
        return paramDesc;
    }

    public void setParamDesc(Map<String, String> paramDesc) {
        this.paramDesc = paramDesc;
    }

    public List<String[]> getReturnList() {
        return returnList;
    }

    public void setReturnList(List<String[]> returnList) {
        this.returnList = returnList;
    }


}