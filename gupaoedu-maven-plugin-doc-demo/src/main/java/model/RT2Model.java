package model;

import javax.core.common.doc.annotation.ApiColumn;

/**
 * @author Liukx
 * @create 2018-01-23 12:07
 * @email liukx@elab-plus.com
 **/
public class RT2Model {
    @ApiColumn(desc = "真实姓名", isRequired = false)
    private String realName;

    @ApiColumn(desc = "性别")
    private String sex;

}
