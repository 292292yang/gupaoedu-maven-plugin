package model;

import javax.core.common.doc.annotation.ApiColumn;

/**
 * @author Liukx
 * @create 2018-01-22 10:26
 * @email liukx@elab-plus.com
 **/
public class RTModel {

    @ApiColumn(desc = "主键", defaultValue = "1", isRequired = true)
    private int id;

    //    @ApiColumn(desc = "用户名称")
    private String username;

    @ApiColumn(desc = "用户详情")
    private RT2Model rt2Model;

//    @ApiColumn(desc = "测试实体")
//    private RTModel rtModel;


    @ApiColumn(desc = "朋友信息")
    private RT3Model rt3Model;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
