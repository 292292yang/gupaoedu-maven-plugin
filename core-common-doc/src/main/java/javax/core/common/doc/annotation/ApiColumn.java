package javax.core.common.doc.annotation;

import java.lang.annotation.*;

/**
 * Created by liukx on 2018/1/22.
 */
@Target(ElementType.FIELD)
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface ApiColumn {


    // 描述
    String desc() default "";

    // 默认值
    String defaultValue() default "";

    //是否必填
    boolean isRequired() default false;

}
