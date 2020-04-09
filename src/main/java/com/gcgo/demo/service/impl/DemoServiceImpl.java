package com.gcgo.demo.service.impl;

import com.gcgo.demo.service.DemoService;
import com.gcgo.framework.annotation.MyService;

@MyService
public class DemoServiceImpl implements DemoService {
    @Override
    public String get(String name) {
        System.out.println("service层正在处理来自controller的请求，参数为："+name);
        return name;
    }
}
