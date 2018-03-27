package javax.core.maven.plugin.parser;

import com.alibaba.fastjson.JSON;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.core.common.doc.annotation.Demo;
import javax.core.common.doc.annotation.Domain;
import javax.core.maven.plugin.builder.ApiBuilder;
import javax.core.maven.plugin.builder.ApiColumnBuilder;
import javax.core.maven.plugin.builder.RequestMappingBuilder;
import javax.core.maven.plugin.constants.DocConstants;
import javax.core.maven.plugin.utils.StringUtils;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HTMLParser {

    //当我们不设置自定义模板的时候，使用默认模板
    private final static String template = "/templates/api.html";

    private void generate(InputStream in, String targetFile, Class<?>[] classes, String host) {
        if (in == null) {
            return;
        }

        Document doc = null;
        try {
            doc = Jsoup.parse(in, "utf-8", "");
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        if (doc == null) {
            return;
        }

        int catelogCount = 1;

        //使用的时候，传一个class的一个集合过来，从Spring的IOC容器中获取
        for (Class<?> c : classes) {
            try {
                //看你是不是一个Controller，如果是一个cotroller，那么认为是可以用http请求
                if (!c.isAnnotationPresent(Controller.class)) {
                    continue;
                }

                // 查找类的工作空间
                String namespace = findNameSpace(c);

                //可以给每个模块定制一个独立的子域名去访问，自己的注解
                // TODO 待优化
                String domain = null;
                String desc = null;

                // 生成站点相关内容
                genDomainInfo(doc, catelogCount, c, domain, desc);

                //开始扫描Controller里面的所有的方法
                int interfaceCount = 1;
                Method[] ms = c.getDeclaredMethods();
                for (Method m : ms) {
                    //判断，看这个方法有没有配置RequestMapping，如果没有配置
                    //那这个方法肯定是请求不到的
                    if (!m.isAnnotationPresent(RequestMapping.class)) {
                        continue;
                    }

                    doc.select(".interface").append(StringUtils.format(doc.select("#interfaceItem").html(), "s" + catelogCount + "-" + interfaceCount));
                    Element eleInterface = doc.select(".interface #" + "s" + catelogCount + "-" + interfaceCount).parents().first();

                    // 构建一个api相关的对象
                    ApiBuilder apiBuilder = new ApiBuilder(m).builder();
                    Demo demo = apiBuilder.getDemo();
//                    List<String[]> returnList = apiBuilder.getReturnList();

                    // 构建一个requestMapping对象
                    RequestMappingBuilder requestMappingBuilder = new RequestMappingBuilder(m).builder();

                    //服务器地址
                    String baseUrl = "http://" + (null == domain || "".equals(domain) ? "" : (domain + ".")) + host + namespace;
                    // 方法url
                    String url = requestMappingBuilder.getUrl();
                    // 接口全路径
                    String fullUrl = baseUrl + url;
                    String indexNo = catelogCount + "." + interfaceCount;
                    //往html文档里面写配置信息了
                    genConfigHtml(apiBuilder, requestMappingBuilder, eleInterface, baseUrl, indexNo);

                    // 解析入参
//                    List<String[]> params = parseRequestParams(m, apiBuilder);

                    //生成参数相关的html
                    genParamsHtml(doc, eleInterface, apiBuilder.getResultObjectList(), null);

                    // 生成返回值相关的html
                    genReturnHtml(doc, apiBuilder, eleInterface);

                    //生成和demo相关的html
                    genAnnotationDemoHtml(doc, demo, eleInterface, fullUrl);

                    interfaceCount++;
                }
                doc.select(".interface").append("<div style='clear:both;margin-bottom:20px;'></div>");
                catelogCount++;
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }

            writeFile(targetFile, doc);

        }
    }

    /**
     * 解析入参
     *
     * @param m
     * @param apiBuilder
     * @return
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    private List<String[]> parseRequestParams(Method m, ApiBuilder apiBuilder) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        List<String[]> params = parseApiColumn(apiBuilder);
//        if (apiBuilder.getParamObject().size() > 0) {
//        params = parseApiColumn(apiBuilder);
//        }
//        else {
//            // 解析SpringMVC中的RequestParam和PathVariable对象
//            params = parseMvcAnnotations(m, apiBuilder.getParamDesc(), apiBuilder.getParamsType());
//        }
        return params;
    }

    private List<String[]> parseApiColumn(ApiBuilder apiBuilder) {
        List<String[]> params = new ArrayList<String[]>();
        // 遍历map集合
        int i = 0;
        for (Map.Entry<String, ApiColumnBuilder> entry : apiBuilder.getParamObject().entrySet()) {
            String key = entry.getKey();
            ApiColumnBuilder apiColumn = entry.getValue();
            //获取参数是否必填
            boolean required = apiColumn.isRequired();
            //获取参数的名字
            String pname = key;
            String desc = apiColumn.getDesc();
            //获取是否有默认值
            String dft = apiColumn.getDefaultValue();
            if (dft.startsWith("\n")) {
                dft = null;
            }
            params.add(new String[]{
                    pname,
                    DocConstants.get(apiBuilder.getParamObject().get(pname).getType().toLowerCase()),
                    (required ? "是" : "否"),
                    (StringUtils.isEmpty(dft) ? "无" : dft),
                    (desc == null ? "无" : desc)});
            i++;
        }
        return params;
    }

    /**
     * 写入到指定的文件中
     *
     * @param targetFile
     * @param doc
     */
    private void writeFile(String targetFile, Document doc) {
        //输出到我们的磁盘
        try {
            OutputStreamWriter ow = new OutputStreamWriter(new FileOutputStream(new File(targetFile)), "utf-8");
            ow.write(doc.html());
            ow.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 生成和@demo相关注解的html
     *
     * @param doc          文档
     * @param demo         注解
     * @param eleInterface 节点
     * @param url          url
     */
    private void genAnnotationDemoHtml(Document doc, Demo demo, Element eleInterface, String url) {
        //demo
        if (demo != null) {
            //添加返回说明
            eleInterface.select(".returns").append(doc.select("#demoDetailItem").html());

            if (!StringUtils.isEmpty(demo.param())) {
                eleInterface.select(".item-bd").append(StringUtils.format(doc.select("#demoItem").html(), url + "?" + demo.param()));
            }
            if (!StringUtils.isEmpty(demo.success())) {
                String json = demo.success();
                try {
                    json = JSON.toJSONString(JSON.parse(json), true);
                    json = json.replaceAll("\n", "<br/>").replaceAll("\t", "&nbsp;&nbsp;&nbsp;");
                } catch (Exception e) {

                }
                eleInterface.select(".item-bd").append(StringUtils.format(doc.select("#successItem").html(), json));
            }
            if (!StringUtils.isEmpty(demo.error())) {
                String json = demo.error();
                try {
                    json = JSON.toJSONString(JSON.parse(json), true);
                    json = json.replaceAll("\n", "<br/>").replaceAll("\t", "&nbsp;&nbsp;&nbsp;");
                } catch (Exception e) {

                }
                eleInterface.select(".item-bd").append(StringUtils.format(doc.select("#errorItem").html(), json));
            }
        }
    }

    /**
     * 生成返回值相关的html
     *
     * @param doc          文档
     * @param apiBuilder   返回参数
     * @param eleInterface 节点
     */
    private void genReturnHtml(Document doc, ApiBuilder apiBuilder, Element eleInterface) {
        List<ApiColumnBuilder> apiColumnBuilderList = apiBuilder.getResultObjectList();
        if (apiColumnBuilderList.size() == 0) {
            eleInterface.select(".returns table").html("");
        } else {
            genReturnParamsHtml(doc, eleInterface, apiColumnBuilderList, null);
        }
    }

    private void genReturnParamsHtml(Document doc, Element eleInterface, List<ApiColumnBuilder> apiColumnBuilderList, String parentName) {
        for (int index = 0; index < apiColumnBuilderList.size(); index++) {
            ApiColumnBuilder apiColumnBuilder = apiColumnBuilderList.get(index);
            String parentItems = ".returns table";//".returns ";
            String items = "#returnItem";
            boolean isParent = apiColumnBuilder.getApiColumnBuilderNodes() == null;
            eleInterface.select(parentItems).append(
                    StringUtils.format(doc.select(items).html(),
                            (!isParent ? "odd notice-title" : "eve item-left-padding"),
                            apiColumnBuilder.getName(),
                            parentName == null ? "无" : parentName,
                            apiColumnBuilder.getType(),
                            apiColumnBuilder.getDesc()));
            if (apiColumnBuilder.getApiColumnBuilderNodes() != null) {
                genReturnParamsHtml(doc, eleInterface, apiColumnBuilder.getApiColumnBuilderNodes(), apiColumnBuilder.getName());
            }
        }
    }

    /**
     * 生成入参相关的html
     *
     * @param doc                  文档
     * @param eleInterface         当前节点
     * @param apiColumnBuilderList 参数集合对象
     */
    private void genParamsHtml(Document doc, Element eleInterface, List<ApiColumnBuilder> apiColumnBuilderList, String parentName) {
        //如果最终扫描出来结果，参数个数为0，表示访问这个接口是不需要带参数的
        if (apiColumnBuilderList.size() == 0) {
            eleInterface.select(".params table").html("无");
        } else {
            //如果有参数，填充到我们模板中，最终生成html
            for (int index = 0; index < apiColumnBuilderList.size(); index++) {
                ApiColumnBuilder apiColumnBuilder = apiColumnBuilderList.get(index);
                boolean isParent = apiColumnBuilder.getApiColumnBuilderNodes() == null;
                eleInterface.select(".params table").append(
                        StringUtils.format(doc.select("#paramItem").html(),
                                (!isParent ? "odd notice-title" : "eve item-left-padding"),
                                apiColumnBuilder.getName(),
                                apiColumnBuilder.getType(),
                                apiColumnBuilder.isRequired() == true ? "是" : "否",
                                apiColumnBuilder.getDefaultValue(),
                                apiColumnBuilder.getDesc()));
                if (apiColumnBuilder.getApiColumnBuilderNodes() != null) {
                    genParamsHtml(doc, eleInterface, apiColumnBuilder.getApiColumnBuilderNodes(), apiColumnBuilder.getName());
                }
            }
        }
    }

    /**
     * 解析SpringMVC中的注解
     *
     * @param m          方法对象
     * @param paramDesc  参数描述
     * @param paramsType 参数类型
     * @return
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    private List<String[]> parseMvcAnnotations(Method m, Map<String, String> paramDesc, List<String> paramsType) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        //开始扫描方法上面参数
        List<String[]> params = new ArrayList<String[]>();
        int i = 0;
        for (Annotation[] anns : m.getParameterAnnotations()) {
            for (Annotation ann : anns) {
                //spring的参数名字定义，有两种形式，第一种就是
                if (ann instanceof RequestParam) {
                    //获取参数是否必填
                    boolean required = (Boolean) ann.getClass().getMethod("required").invoke(ann);
                    //获取参数的名字
                    String pname = ann.getClass().getMethod("value", null).invoke(ann).toString();
                    //获取是否有默认值
                    String dft = ann.getClass().getMethod("defaultValue").invoke(ann).toString();
                    if (dft.startsWith("\n")) {
                        dft = null;
                    }
                    params.add(new String[]{
                            pname,
                            DocConstants.get(paramsType.get(i).toLowerCase()),
                            (required ? "是" : "否"),
                            (StringUtils.isEmpty(dft) ? "无" : dft),
                            (paramDesc.get(pname) == null ? "无" : paramDesc.get(pname))});
                    //直接配置在路径上的
                } else if (ann instanceof PathVariable) {
                    String pname = ann.getClass().getMethod("value", null).invoke(ann).toString();
                    params.add(new String[]{
                            pname,
                            DocConstants.get(paramsType.get(i).toLowerCase()),
                            "是",
                            "无",
                            (paramDesc.get(pname) == null ? "无" : paramDesc.get(pname))});
                }
            }
            i++;
        }
        return params;
    }

    /**
     * 查询工作空间
     *
     * @param c
     * @return
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private String findNameSpace(Class<?> c) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        //开始判断有没有配置RequestMapping
        String namespace = null;
        if (c.isAnnotationPresent(RequestMapping.class)) {
            Annotation a = c.getAnnotation(RequestMapping.class);
            Method m = a.getClass().getMethod("value", null);
            namespace = ((String[]) m.invoke(a))[0];
        }
        return namespace;
    }

    private void genDomainInfo(Document doc, int catelogCount, Class<?> c, String domain, String desc) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (c.isAnnotationPresent(Domain.class)) {
            Annotation a = c.getAnnotation(Domain.class);

            Method m = a.getClass().getMethod("value", null);
            domain = m.invoke(a).toString();

            Method dm = a.getClass().getMethod("desc", null);
            desc = dm.invoke(a).toString();
        }

        doc.select(".interface").append(StringUtils.format(doc.select("#interfaceTitleItem").html(), "p" + catelogCount, catelogCount + "、" + (StringUtils.isEmpty(desc) ? "未描述模块" : desc)));
        doc.select(".content-body").append(StringUtils.format(doc.select("#catalogItem").html(), "#p" + catelogCount, catelogCount + "、" + (StringUtils.isEmpty(desc) ? "未描述模块" : desc)));
    }

    /**
     * 构建配置文件信息
     *
     * @param apiBuilder            API注解信息
     * @param requestMappingBuilder requestMapping信息
     * @param eleInterface
     */
    private void genConfigHtml(ApiBuilder apiBuilder, RequestMappingBuilder requestMappingBuilder, Element eleInterface, String baseUrl, String indexNo) {
        eleInterface.select(".title").html(indexNo + "、" + (StringUtils.isEmpty(apiBuilder.getName()) ? "未描述接口名称" : apiBuilder.getName()));
        eleInterface.select(".desc").html(StringUtils.isEmpty(apiBuilder.getMdesc()) ? "未描述功能" : apiBuilder.getMdesc());
        eleInterface.select(".url").html(baseUrl + requestMappingBuilder.getUrl());
        eleInterface.select(".method").html((null == requestMappingBuilder.getMethod() || "".equals(requestMappingBuilder.getMethod()) ? "GET/POST" : requestMappingBuilder.getMethod().toUpperCase()));
        eleInterface.select(".author").html(StringUtils.isEmpty(apiBuilder.getAuthor()) ? "未描述作者" : apiBuilder.getAuthor());
        eleInterface.select(".createtime").html(StringUtils.isEmpty(apiBuilder.getCreatetime()) ? "未描述最后修改时间" : apiBuilder.getCreatetime());
        eleInterface.select(".checkEnc").html(apiBuilder.isCheckEnc() ? "是" : "否");
        eleInterface.select(".checkIp").html(apiBuilder.isCheckIp() ? "是" : "否");
        eleInterface.select(".checkVersion").html(apiBuilder.isCheckVersion() ? "是" : "否");
    }

    /**
     * 生成HTML文档
     *
     * @param templateFile HTML模板文件
     * @param targetFile   生成目标文件
     * @param classes      需要分析的class
     * @param host         url域名
     */
    public void generate(String templateFile, String targetFile, List<Class> classes, String host) {
        Class[] c = new Class[classes.size()];
        generate(templateFile, targetFile, classes.toArray(c), host);
    }

    /**
     * 生成HTML文档
     *
     * @param templateFile HTML模板文件
     * @param targetFile   生成目标文件
     * @param classes      需要分析的class
     * @param host         url域名
     */
    public void generate(String templateFile, String targetFile, Class[] classes, String host) {
        InputStream in = null;
        try {
            in = new FileInputStream(new File(templateFile));
        } catch (Exception e) {
            e.printStackTrace();
        }
        generate(in, targetFile, classes, host);
    }

    /**
     * 生成HTML文档
     *
     * @param targetFile 生成目标文件
     * @param clazz      需要分析的class
     * @param host       url域名
     */
    public void generate(String targetFile, List<Class<?>> clazz, String host) {
        Class<?>[] c = new Class[clazz.size()];
        generate(targetFile, clazz.toArray(c), host);
    }


    /**
     * 生成HTML文档
     *
     * @param targetFile 生成目标文件
     * @param classes    需要分析的class
     * @param host       url域名
     */
    public void generate(String targetFile, Class[] classes, String host) {
        InputStream in = null;
        try {
            in = HTMLParser.class.getResourceAsStream(template);
        } catch (Exception e) {
            e.printStackTrace();
        }
        generate(in, targetFile, classes, host);
    }

    public static void main(String[] args) {

    }

}
