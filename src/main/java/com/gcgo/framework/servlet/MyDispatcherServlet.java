package com.gcgo.framework.servlet;

import com.gcgo.framework.annotation.MyAutowired;
import com.gcgo.framework.annotation.MyController;
import com.gcgo.framework.annotation.MyRequestMapping;
import com.gcgo.framework.annotation.MyService;
import com.gcgo.framework.pojo.Handler;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyDispatcherServlet extends HttpServlet {

    private Properties properties = new Properties();
    private List<String> classNames = new ArrayList<>();//缓存类的全限定类名
    private Map<String, Object> ioc = new HashMap<>();
    //    private Map<String, Method> handlerMapping = new HashMap<>();
    private List<Handler> handlerMapping = new ArrayList<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        //        //处理请求
        //        //根据url找到对应方法，调用
        //        String requestURI = req.getRequestURI();
        //        //获取到反射的方法
        //        Method method = handlerMapping.get(requestURI);
        //        //反射调用,需要传入对象和参数
        //        method.invoke()
        Handler handler = getHandler(req);
        if (handler == null) {
            resp.getWriter().write("404 not found");
            return;
        }
        //开始参数绑定，获取参数列表，主要拿它的长度
        Parameter[] parameters = handler.getMethod().getParameters();
        //创建一个数组存参数,按照顺序存入的
        //都是为了最后反射调用invoke方法传入的参数列表
        Object[] paramValues = new Object[parameters.length];
        //将前端请求数据，按顺序填入数组(普通类型参数)
        //获得的是前端传进来的参数，不包括MVC默认支持的HttpServletRequest和HttpServletResponse
        Map<String, String[]> parameterMap = req.getParameterMap();
        for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {
            String value = StringUtils.join(param.getValue(), ",");//把String数组元素用“,”拼接
            //匹配上了就填充
            if (handler.getParamIndexMapping().containsKey(param.getKey())) {//如果缓存的对应该url的方法参数列表中包含当前前端传进来的参数，名称一样！！
                Integer index = handler.getParamIndexMapping().get(param.getKey());//参数在方法中的顺序
                paramValues[index] = value;//填充到对应位置
            }
        }
        //HttpServletRequest，看看缓存的方法中参数列表中有没有HttpServletRequest，有就也赋值
        if (handler.getParamIndexMapping().containsKey(HttpServletRequest.class.getSimpleName())) {
            int reqIndex = handler.getParamIndexMapping().get(HttpServletRequest.class.getSimpleName());
            paramValues[reqIndex] = req;
        }
        //HttpServletResponse
        if (handler.getParamIndexMapping().containsKey(HttpServletResponse.class.getSimpleName())) {
            int respIndex = handler.getParamIndexMapping().get(HttpServletResponse.class.getSimpleName());
            paramValues[respIndex] = resp;
        }

        try {
            //最终一步，反射调用method的invoke方法，需要传入执行方法的对象以及参数数组
            handler.getMethod().invoke(handler.getController(), paramValues);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private Handler getHandler(HttpServletRequest req) {
        if (handlerMapping.isEmpty()) return null;
        String uri = req.getRequestURI();
        for (Handler handler : handlerMapping) {
            Matcher matcher = handler.getPattern().matcher(uri);
            if (matcher.matches()) {
                return handler;
            }
        }
        return null;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        /*加载配置文件*/
        //1扫描springmvc.properties
        String contextConfigLocation = config.getInitParameter("contextConfigLocation");
        doLoadConfig(contextConfigLocation);
        System.out.println("读取.properties配置文件......√");
        //2扫描类、注解
        doScan(properties.getProperty("scanPackage"));
        System.out.println("包扫描......√");
        //3初始化bean对象（实现IOC容器）
        doInstance();
        System.out.println("实例化对象......√");
        //4依赖注入
        doAutoWired();
        System.out.println("属性注入......√");
        //5初始化MVC组件：HandlerMapping，将URL和方法建立关系
        initHandlerMapping();
        System.out.println("请求方法映射......√");
        System.out.println("MVC框架初始化完成");
        //6等待请求进入，处理请求

    }

    /*最重要的！！！！！！！！！！！！！！！
     **初始化MVC组件：HandlerMapping*/
    private void initHandlerMapping() {
        if (ioc.isEmpty()) return;
        //扫描所有对象的方法，判断有没有@MyRequestMapping注解
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> aClass = entry.getValue().getClass();
            StringBuilder sbUrl = new StringBuilder();//用于拼接url
            //不是controller层就pass
            if (!aClass.isAnnotationPresent(MyController.class)) continue;
            String baseURL = "";
            //如果类上有@MyRequestMapping注解则先记录baseURL
            if (aClass.isAnnotationPresent(MyRequestMapping.class)) {
                MyRequestMapping annotation = aClass.getAnnotation(MyRequestMapping.class);
                baseURL = annotation.value();
                sbUrl.append(baseURL);
            }
            //判断方法上哪些有@MyRequestMapping注解
            Method[] methods = aClass.getMethods();

            for (Method method : methods) {
                //没有@MyRequestMapping注解跳过
                if (!method.isAnnotationPresent(MyRequestMapping.class)) continue;
                //有@MyRequestMapping注解就处理
                MyRequestMapping annotation = method.getAnnotation(MyRequestMapping.class);
                String methodURL = annotation.value();
                sbUrl.append(methodURL);
                //保存url,封装到Handler类中
                Handler handler = new Handler(entry.getValue(), method, Pattern.compile(sbUrl.toString()));
                //处理计算参数信息
                Parameter[] parameters = method.getParameters();
                for (int i = 0; i < parameters.length; i++) {
                    Parameter parameter = parameters[i];
                    //这里简化处理，只处理String、HttpServletRequest和HttpServletResponse三种类型
                    if (parameter.getType() == HttpServletRequest.class ||
                            parameter.getType() == HttpServletResponse.class) {
                        handler.getParamIndexMapping().put(parameter.getType().getSimpleName(), i);
                    } else {
                        handler.getParamIndexMapping().put(parameter.getName(), i);
                    }
                }
                //加入缓存
                handlerMapping.add(handler);
                //清空sb
                if (baseURL.equals("")) {
                    sbUrl.delete(0, sbUrl.length());
                } else {
                    sbUrl.delete(baseURL.length(), sbUrl.length());
                }
            }
        }
    }

    /*依赖注入*/
    private void doAutoWired() {
        if (ioc.isEmpty()) return;
        //遍历IOC中所有对象，扫描添加了@Autowire的注解的属性，然后属性注入
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            //先拿属性列表
            Field[] declaredFields = entry.getValue().getClass().getDeclaredFields();
            for (Field declaredField : declaredFields) {
                if (declaredField.isAnnotationPresent(MyAutowired.class)) {
                    MyAutowired annotation = declaredField.getAnnotation(MyAutowired.class);
                    //拿value属性
                    String beanName = annotation.value();
                    if (beanName.equals("")) {//需要接口注入
                        beanName = declaredField.getType().getName();
                    }
                    //开始注入
                    declaredField.setAccessible(true);
                    //set(对象，属性的对象)
                    try {
                        declaredField.set(entry.getValue(), ioc.get(beanName));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /*初始化bean对象（实现IOC容器）
     * 基于包扫描的类名，反射获取对象
     * */

    private void doInstance() {
        if (classNames.size() == 0) return;
        try {
            for (String className : classNames) {
                Class<?> aClass = Class.forName(className);
                //处理controller
                if (aClass.isAnnotationPresent(MyController.class)) {//如果添加了@MyController注解
                    MyController annotation = aClass.getAnnotation(MyController.class);
                    //获取value值
                    String beanName = annotation.value();
                    if (beanName.equals("")) {//如果没有指定id就用类名首字母小写
                        beanName = lowerFirstCharacter(aClass.getSimpleName());//类名转小写，即beanId
                    }
                    Object obj = aClass.newInstance();
                    ioc.put(beanName, obj);
                }
                //处理service,因为service经常是实现接口的，所以为了一会方便注入，这里也缓存一下接口
                else if (aClass.isAnnotationPresent(MyService.class)) {//如果添加了@MyService注解
                    MyService annotation = aClass.getAnnotation(MyService.class);
                    //获取value值
                    String beanName = annotation.value();
                    if (beanName.equals("")) {//如果没有指定id就用类名首字母小写
                        beanName = lowerFirstCharacter(aClass.getSimpleName());//类名转小写，即beanId
                    }
                    Object obj = aClass.newInstance();
                    ioc.put(beanName, obj);

                    //记录接口
                    Class<?>[] interfaces = aClass.getInterfaces();
                    for (Class<?> anInterface : interfaces) {
                        //缓存<接口名,实现类对象>
                        ioc.put(anInterface.getName(), aClass.newInstance());
                    }
                }
                //再有其他注解可以继续写else if。。。
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /*首字母转小写的方法*/
    private String lowerFirstCharacter(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] += 32;//转小写
        return String.valueOf(chars);
    }

    /*扫描注解和类,获取包下所有类的全限定类名*/
    private void doScan(String scanPackage) {

        String scanPackagePath = Thread.currentThread().getContextClassLoader().getResource("").getPath() +
                scanPackage.replaceAll("\\.", "/");
        File aPackage = new File(scanPackagePath);
        File[] files = aPackage.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {//如果是目录
                //递归搜索子目录
                doScan(scanPackage + "." + file.getName());
            } else if (file.getName().endsWith(".class")) {
                String className = scanPackage + "." +
                        file.getName().replaceAll(".class", "");
                classNames.add(className);
            }
        }
    }

    /*加载配置文件*/
    private void doLoadConfig(String contextConfigLocation) {
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            properties.load(resourceAsStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
