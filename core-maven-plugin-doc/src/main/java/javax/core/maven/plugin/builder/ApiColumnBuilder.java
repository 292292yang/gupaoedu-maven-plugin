package javax.core.maven.plugin.builder;

import java.util.List;

/**
 * api的列构建
 *
 * @author Liukx
 * @create 2018-01-23 11:23
 * @email liukx@elab-plus.com
 **/
public class ApiColumnBuilder {

    private String name;

    private String type;

    private String desc;

    private String defaultValue;

    private boolean isRequired;

    private List<ApiColumnBuilder> apiColumnBuilderNodes;


    public ApiColumnBuilder(String name, String type, String desc, String defaultValue, boolean isRequired) {
        this.name = name;
        this.type = type;
        this.desc = desc;
        this.defaultValue = defaultValue;
        this.isRequired = isRequired;
    }

    public List<ApiColumnBuilder> getApiColumnBuilderNodes() {
        return apiColumnBuilderNodes;
    }

    public void setApiColumnBuilderNodes(List<ApiColumnBuilder> apiColumnBuilderNodes) {
        this.apiColumnBuilderNodes = apiColumnBuilderNodes;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public void setRequired(boolean required) {
        isRequired = required;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
