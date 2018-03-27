package com.gupaoedu.action;

import model.RTModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.core.common.doc.annotation.Api;
import javax.core.common.doc.annotation.Domain;
import javax.core.common.doc.annotation.Rule;

@Controller
@RequestMapping("/x")
@Domain(desc = "测试管理")
public class XAction {

    @Api(author = "liukx",
            createtime = "2017-01-04",
            name = "测试的注册接口",
            desc = "这个是我测试的一个url",
//            params = {
//                    @Rule(name = "username", desc = "用户名"),
//                    @Rule(name = "password", desc = "密码"),
//                    @Rule(name = "address", desc = "住址")
//            },
            remark = "我想知道remark干啥了"
            , returns = {
            @Rule(name = "single", desc = "用户对象", type = "object", clazz = RTModel.class),
            @Rule(name = "list", desc = "朋友对象", type = "list", clazz = RTModel.class),

    }
            , paramsObject = RTModel.class)
    @RequestMapping(value = "/regist.json", method = RequestMethod.POST)
    public ModelAndView regist(@RequestParam(value = "username") String userName,
                               @RequestParam(value = "password") String password,
                               @RequestParam(value = "address", required = false, defaultValue = "不详") String address) {
        return null;
    }
}
