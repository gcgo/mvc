# 大作业一：手写MVC框架基础上增加如下功能

1）定义注解@Security（有value属性，接收String数组），该注解用于添加在Controller类或者Handler方法上，表明哪些用户拥有访问该Handler方法的权限（注解配置用户名）
2）访问Handler时，用户名直接以参数名username紧跟在请求的url后面即可，比如http://localhost:8080/demo/handle01?username=zhangsan
3）程序要进行验证，有访问权限则放行，没有访问权限在页面上输出
注意：自己造几个用户以及url，上交作业时，文档提供哪个用户有哪个url的访问权限

## 项目提交说明：

controller包下SecurityController类为添加了@Security注解的测试类：

- 类上添加注解：即用户kobe有所有方法的执行权限，代码片段如下：

```java
@MyController
@MyRequestMapping("/sc")
@Security(value = {"kobe"})
public class SecurityController {
   ...
}
```

- 方法query1()上没有添加@Security注解，故其只有kobe能执行：

  ```java
   @MyRequestMapping("/query1")
   public String query1(HttpServletRequest request, HttpServletResponse response, String name) {
      	System.out.println("拥有权限，接受访问！：name:" + name);
      	return demoService.get(name);
    }
  ```

- 方法query2()添加@Security注解，即用户harden可以执行：

  ```java
   @MyRequestMapping("/query2")
   @Security(value = {"harden"})
   public String query2(HttpServletRequest request, HttpServletResponse response, String name) {
          System.out.println("拥有权限，接受访问！：name:" + name);
          return demoService.get(name);
   }
  ```

## 测试：综上所述，用户及其可执行方法的对应关系为：

| 用户   | 可执行的方法       |
| ------ | ------------------ |
| kobe   | query1()、query2() |
| harden | query2()           |

### 预期结果：

- 浏览器输入：http://localhost:8080/sc/query1?name=kobe

  结果：拥有权限，接受访问！：name: kobe

- 浏览器输入：http://localhost:8080/sc/query2?name=kobe

  结果：拥有权限，接受访问！：name:  kobe

- **浏览器输入：http://localhost:8080/sc/query1?name=harden**

  **结果：浏览器页面及控制台显示：用户： harden  没有访问权限**

- 浏览器输入：http://localhost:8080/sc/query2?name=harden

  结果：拥有权限，接受访问！：name:  harden