package javax.core.maven.plugin.builder;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class RequestMappingBuilder {
    private Method m;
    private String url;
    private String method;

    public RequestMappingBuilder(Method m) {
        this.m = m;
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public RequestMappingBuilder builder() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        // ================= 获取SpringMvc方法注解的值 ===============
        //获取RequestMapping里面的值
        Annotation mapping = m.getAnnotation(RequestMapping.class);
        Method vm = mapping.getClass().getMethod("value", null);
        url = ((String[]) vm.invoke(mapping))[0];
        //到这里为止，就可以拼装出来一个完整的url了
        Method mm = mapping.getClass().getMethod("method", null);

        Object mr = mm.invoke(mapping);
        method = null;
        if (mr != null) {
            if (((RequestMethod[]) mr).length > 0) {
                method = (((RequestMethod[]) mr)[0]).toString();
            }
        }
        return this;
    }
}