package com.gcgo.framework.pojo;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 封装Handler方法相关信息
 */
public class Handler {
    private Object controller;
    private Method method;
    private Pattern pattern;
    private Map<String, Integer> paramIndexMapping;//参数顺序,<参数名，第几个参数>
    private Set<String> adminSet;//用于存储@security添加的可访问列表

    public Handler(Object controller, Method method, Pattern pattern) {
        this.controller = controller;
        this.method = method;
        this.pattern = pattern;
        this.paramIndexMapping = new HashMap<>();
        this.adminSet = new HashSet<>();
    }

    public Set<String> getAdminSet() {
        return adminSet;
    }

    public void setAdminSet(Set<String> adminSet) {
        this.adminSet = adminSet;
    }

    public Object getController() {
        return controller;
    }

    public void setController(Object controller) {
        this.controller = controller;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public Map<String, Integer> getParamIndexMapping() {
        return paramIndexMapping;
    }

    public void setParamIndexMapping(Map<String, Integer> paramIndexMapping) {
        this.paramIndexMapping = paramIndexMapping;
    }
}
