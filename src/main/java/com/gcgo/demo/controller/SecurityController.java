package com.gcgo.demo.controller;

import com.gcgo.demo.service.DemoService;
import com.gcgo.framework.annotation.MyAutowired;
import com.gcgo.framework.annotation.MyController;
import com.gcgo.framework.annotation.MyRequestMapping;
import com.gcgo.framework.annotation.Security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 只在类上加@Security注解
 */
@MyController
@MyRequestMapping("/sc")
@Security(value = {"kobe"})
public class SecurityController {
    @MyAutowired
    private DemoService demoService;

    @MyRequestMapping("/query1")
    public String query1(HttpServletRequest request, HttpServletResponse response, String name) {
        System.out.println("拥有权限，接受访问！：name:" + name);
        return demoService.get(name);
    }

    @MyRequestMapping("/query2")
    @Security(value = {"harden"})
    public String query2(HttpServletRequest request, HttpServletResponse response, String name) {
        System.out.println("拥有权限，接受访问！：name:" + name);
        return demoService.get(name);
    }
}
