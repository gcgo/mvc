package com.gcgo.demo.controller;

import com.gcgo.demo.service.DemoService;
import com.gcgo.framework.annotation.MyAutowired;
import com.gcgo.framework.annotation.MyController;
import com.gcgo.framework.annotation.MyRequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@MyController
@MyRequestMapping("/demo")
public class DemoController {
    @MyAutowired
    private DemoService demoService;

    @MyRequestMapping("/query")
    public String query(HttpServletRequest request, HttpServletResponse response, String name) {
        System.out.println("接受前端请求参数为：name:" + name);
        return demoService.get(name);
    }
}
