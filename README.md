# 基于SpringBoot封装的快速项目构建插件

**AOP插件jopen-springboot-aop-plugin**

我们在编写业务的时候尽量将业务分的更细致，才能达到让代码更加可读，可维护，所以笔者基于此理念，写了一个简单的AOP插件，能够达到开发者只需要定义切面位置和原子化的编写你的业务。笔者定义了一个抽象的AOP类，开发者只需要继承此抽象类并且实现抽象方法即可。定义的这个抽象类为AbstractAopAction，里面抽象出来了部分公共代码。涉及到四个关键的类。

ThrowingBeforeFunction是一个函数式范式接口，我们需要定义的AOP前置业
务是体现在它的方法体中，ThrowingBiAfterFunction也是一个函数式的接口，
需要定义的AOP后置业务是体现在它的方法体中，ReturnValue是继承了HashMap
的一个键值对，表示的是AOP处理完成之后的结果封装在键值对里面。ReturnHandlerr是一个返回值处理器，也需要自己来定义，属于函数式接口，充当一个消费者，消费AOP处理
处理ReturnValue中的结果。

具体使用如下


    package io.jopen.apply.plugin.demo.aop;
    
    import io.jopen.springboot.plugin.aop.*;
    import org.aspectj.lang.annotation.Aspect;
    import org.aspectj.lang.annotation.Pointcut;
    import org.springframework.stereotype.Component;
    
    import java.util.Map;
    
    /**
     * {@link ResultHandler}
     * {@link ReturnValue}
     *
     * @author maxuefeng
     * @since 2020/1/9
     */
    @Component
    @Aspect
    public class LoginAopDemo extends AbstractAopAction {
    
    
        /*切面位置定义*/
        @Override
        @Pointcut("execution(public * io.jopen.apply.plugin.demo.controller.LoginApi.login(..))")
        public void pointCut() {
        }
    
        // 无参数构造方法
        public LoginAopDemo() {
            super();
    
            // 登录前操作  此Action可以定义多个，只需要按照顺序排列存放即可
            this.beforeActions.put(this.checkAccount, ResultHandler.ignore());
    
            // 登录后操作
            this.afterActions.put(this.initRewardInfo, ResultHandler.ignore());
        }
    
        // 登录之后先判断用户的账户是否正常
        private ThrowingBeforeFunction checkAccount = (args) -> {
            // 获取控制器入参
            Object[] params = args;
            // TODO 检测逻辑
            return ReturnValue.empty();
        };
    
        /*登录成功后初始化登录奖励信息*/
        private ThrowingBiAfterFunction initRewardInfo = (args, result) -> {
            Map<String, Object> loginRet = (Map<String, Object>) result;
            if (loginRet.get("success").equals(Boolean.TRUE)) {
                // TODO  记录操作
            }
            return ReturnValue.empty();
        };
    }

代码解读（需要以下四个步骤）



1 实现抽象类AbstractAopAction类；
2 实现抽象方法pointCut，声明切面位置；
3 定义类型ThrowingBeforeFunction和ThrowingBiAfterFunction的成员变量，而在这两个函数实现体中定义自己的业务；
4 创建无参构造方法，并且将定义的ThrowingBeforeFunction成员变量和ThrowingBiAfterFunction添加到父类的HashMap容器中，（在无参构造函数中实现）。

经过以上四个步骤我们就完成了复杂业务的原子化编写，很明显的结果就是解耦和，子类只需要原子化定义业务，父类负责执行，分工明确。

**参数检验插件jopen-springboot-param-test**

在业务代码中为了防止空参数传入，很多if else都需要写，如果引入这个插件，可以降低我们的if else代码量，只需要声明一个不为空的注解。有两个关键的注解@CheckParamNotNull和@NotNull。@CheckParamNotNull这个注解只用用在方法上，如果加到某个方法上，则在调用这个方法的时候，或检验所有参数都不可为空，包括字符串和集合和Map。字符串长度不可为0，集合和Map的size不可为0。如果在一个方法上添加了@CheckParamNotNull这个注解，在方法中的某一个参数前面加了@NotNull注解，那么只会检测添加@Notnull注解的参数不为空，一旦参数为空，则会抛出异常，由开发者进行全局异常处理。

具体使用如下
 

    /**
         * 参数检验注解
         *
         * @param username
         * @param password
         */
        @CheckParamNotNull
        private void login(@NotNull String username, String password) {
            if (!(username.equals("admin") && password.equals("123"))) {
                throw new RuntimeException("登录失败");
            }
        }


代码解读（分为两个步骤）
1 选择某一个方法添加@CheckParamNotNull注解；（必选）
2 如果选择检验部分参数不为空，只需要将@Notnull注解加入到指定参数前面。（可选）


**Fluent风格的MongoDB查询插件-jopen-springboot-mongo-builder**

此插件基于Spring自带的MongoTemplate工具，使用Fluent风格编写流畅，内置Lambda字段引用式条件构造器，使得编写条件查询构造不易出错，增加了Lambda字段引用缓存机制加速整个插件运行，可以消除在性能的上影响，使用舒服，性能高，并且传统的Spring MongoTemplate无法满足部分特殊查询（内部封装了IQuery），此插件也进一步弥补了Spring的不足之处。

普通查询构建代码示例

     public Collection<User> getUserListByExcludeSomeFields() {
        Query query = QueryBuilder.builderFor(User.class)
                .eq(User::getId,"1")
                /*只查询部分字段 */
                .excludeFields(User::getBirth, User::getName).build();
        return mongoTemplate.find(query, User.class);
    }

高级聚合查询构建代码示例
 

    public List<Map> getAggUserList() {
            // 根据用户年龄和地址进行分组  并且对于年龄进行求和
            Aggregation aggregation = AggregationBuilder.builderFor(User.class).groupByAndSumMultiFieldSum(
                    ImmutableList.of(User::getAddress, User::getAge),
                    /*Key表示求和的字段  value表示给当前这个字段起个别名*/
                    ImmutableMap.of(User::getAge, "all_age", User::getBirth, "all_birth")
            ).build();
    
            return mongoTemplate.aggregate(aggregation, BaseModel.getCollectionName(User.class), Map.class).getMappedResults();
        }

条件式修改构建代码示例

    public void updateUserAge(int age){
        Update update = UpdateBuilder.builderFor(User.class).set(User::getAge, age).build();
        Query query = QueryBuilder.builderFor(User.class).eq(User::getId, "1").build();
        mongoTemplate.updateFirst(query,update,User.class);
    }



**接口注解缓存插件jopen-springboot-annotation-cache-plugin**

如果我们使用了很多的自定义注解，比如限流注解，参数检验注解，等等，每次请求进入接口的时候需要根据注解调用指定的代码的诟病总在于效率过滤，对于一些热门接口，我们频繁的使用反射获取注解无疑在性能上挖了一个坑，注解过多导致接口访问缓慢，而这个问题的罪魁祸首就是反射式获取注解。所以笔者写了一个注解缓存插件，用于加速获取指定接口注解。具体实现需要继承笔者实现的基础拦截类BaseInterceptor。整个缓存加载过程是在程序启动过程中，所以在访问时不会造成变量安全等问题。这个实现类中有一个核心方法叫做getMark，我们只需要传入指定的接口对象和注解类型，此方法即可返回对应的注解。最好的使用办法是继承这个基础类（笔者建议）。而不是把它当做一个工具类去使用。

具体实现代码如下

    

    package io.jopen.springboot.plugin.annotation.cache;
    
    import com.google.common.collect.ClassToInstanceMap;
    import com.google.common.collect.ImmutableSet;
    import com.google.common.collect.MutableClassToInstanceMap;
    import io.jopen.springboot.plugin.common.ReflectUtil;
    import org.checkerframework.checker.nullness.qual.NonNull;
    import org.checkerframework.checker.nullness.qual.Nullable;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import org.springframework.boot.CommandLineRunner;
    import org.springframework.stereotype.Component;
    import org.springframework.stereotype.Controller;
    import org.springframework.web.bind.annotation.*;
    import org.springframework.web.method.HandlerMethod;
    import org.springframework.web.servlet.HandlerInterceptor;
    
    import javax.annotation.PostConstruct;
    import java.lang.annotation.Annotation;
    import java.lang.reflect.Method;
    import java.util.*;
    
    /**
     * 通用方法抽象
     * <p>
     * {@link HandlerInterceptor}
     * {@link Method}
     *
     * @author maxuefeng
     * {@link Annotation}
     * 注解实例调用<code>getClass()<code/>方法的结果是一个Proxy对象 具体打印结果是com.sun.proxy.$Proxy72
     * 注解实例调用<code>annotationType()<code/>方法的结果是一个正确的Class对象  而非一个Proxy对象
     * @see Annotation#annotationType()
     */
    @Component
    public class BaseInterceptor implements HandlerInterceptor, CommandLineRunner {
    
        private static final Logger LOGGER = LoggerFactory.getLogger(BaseInterceptor.class);
    
        /**
         * 缓存接口的注解，此处对数据有强一致性的要求，读远大于写
         * read >> write
         *
         * @see ClassToInstanceMap
         * @see java.util.concurrent.ConcurrentHashMap
         */
        private final static Map<Integer, ClassToInstanceMap<Annotation>> ANNOTATION_CACHE = new HashMap<>(300);
    
    
        /**
         * 获取指定标记
         *
         * @param type    注解类型
         * @param handler 目标接口方法
         * @return 返回指定的注解实例
         */
        @Nullable
        public <TYPE extends Annotation> TYPE getMark(@NonNull Class<TYPE> type,
                                                      @NonNull Object handler) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            int key = handlerMethod.getMethod().toGenericString().hashCode();
            ClassToInstanceMap<Annotation> classToInstanceMap = ANNOTATION_CACHE.get(key);
            return classToInstanceMap == null ? null : classToInstanceMap.getInstance(type);
        }
    
        @Override
        public void run(String... args) throws Exception {
            LOGGER.info("load api interface annotation");
    
            // TODO  需要设置controller包  API访问策略
            List<Class<?>> classList = ReflectUtil.getClasses("com.planet.biz.modules.controller");
    
            // 需要过滤的注解
            final Set<Class<?>> filterTypes = ImmutableSet.of(
                    RestController.class,
                    Controller.class,
                    ResponseBody.class,
                    Component.class,
                    RequestMapping.class,
                    PostMapping.class,
                    GetMapping.class,
                    PostConstruct.class);
    
            classList.parallelStream()
                    // 进行过滤
                    .filter(controllerType -> controllerType.getDeclaredAnnotation(Controller.class) != null || controllerType.getDeclaredAnnotation(RestController.class) != null)
                    // 进行消费
                    .forEach(controllerType -> {
                        // 类级别的注解
                        Annotation[] typeAnnotations = controllerType.getDeclaredAnnotations();
    
                        Method[] methods = controllerType.getDeclaredMethods();
                        for (Method method : methods) {
                            Annotation[] methodAnnotations = method.getDeclaredAnnotations();
                            Set<Annotation> mergeSet = new HashSet<>();
                            int key = method.toGenericString().hashCode();
    
                            // 添加类级别的注解
                            if (typeAnnotations != null) {
                                Collections.addAll(mergeSet, typeAnnotations);
                            }
    
                            // 添加方法级别的注解
                            if (methodAnnotations != null) {
                                Collections.addAll(mergeSet, methodAnnotations);
                            }
    
                            MutableClassToInstanceMap<Annotation> classToInstanceMap = MutableClassToInstanceMap.create();
                            for (Annotation annotation : mergeSet) {
                                if (filterTypes.contains(annotation.annotationType())) {
                                    continue;
                                }
                                classToInstanceMap.put(annotation.annotationType(), annotation);
                            }
                            this.ANNOTATION_CACHE.put(key, classToInstanceMap);
                        }
                    });
    
            LOGGER.info("cache api interface annotation complete");
        }}


**接口限流插件jopen-springboot-limit-plugin**

接口限流插件基于接口注解缓存插件，我们需要继承上述实现的BaseInterceptor插件。笔者自定义了@Limiting注解，里面有两个参数可需要调整，第一个参数time表示时间度量单位，第二那个参数为count，表示在设定的时间内可以访问此接口多少次。此注解可放在在接口类级别，也可放在方法级别，放在类级别表示对这个接口下的所有接口进行限流，放在某一个方法上表示只对当前方法对应的接口进行限流。

#代码示例

      @RequestMapping(value = "")
    @Limiting(time = 1,count = 100)  // 1秒钟可以访问100次 
    @CheckParamNotNull // 检验参数不可为空
    public Map<String, Object> login(@RequestBody Map<String, Object> body) {
        return ImmutableMap.of();
    }

**接口加密插件jopen-springboot-encrypty-plugin**

为了保证接口安全，可以采用接口加密的方式，使用方式也很简单，我们只需要在application.yml文件中定义秘钥和声明加解密的注解即可完成接口加密解密。

#代码使用示例

     @RequestMapping(value = "")
    @Limiting(time = 1,count = 100)  // 1秒钟可以访问100次 进行限流   只对当前方法限流，如果加到控制器则对于当前控制器下的所有接口进行限流
    @DESDecryptBody  // 对Http请求体进行解密
    @DESEncryptBody  // 对于返回参数进行加密
    @CheckParamNotNull // 检验参数不可为空
    public Map<String, Object> login(@RequestBody Map<String, Object> body) {
        return ImmutableMap.of();
    }
    
#使用前提
在application.yml文件中配置秘钥，配置示例如下：

    
#加密配置
    encrypt:
      body:
        aes-key: KPF2PLLgMCIJsvyh #AES加密秘钥
        des-key: nLMB9Kz3Puv7TTxi #DES加密秘钥

1 需要选择是DES还是AES加密，假定我们选择DES加密，则步骤如下
    1 对接口使用@DESDecryptBody表示对接口接受的数据进行解密。
    2 对接口使用@DESEecryptBody表示对接口返回的数据进行加密。    


**web应用初始化插件jopen-springboot-init-plugin**

为了实现模块化的项目初始化，笔者定义了@init插件，为了更方便的声明式项目初始化，我们在需要初始化的类上添加@init注解即可完成模块初始化。有两个参数需要配置，initialization参数表示初始化方式，可选的有执行静态代码块，或者成员方法，或者静态代码块，value参数表示如果是执行某一个初始化方法，则需要指定方法名称。

使用如下

    
    /**
     * @author maxuefeng
     * @since 2020/1/9
     */
    @Init(initialization = Init.InitMode.STATIC_METHOD,value = "init")
    public class InitDemo {
    
        public static void init() {
            System.err.println("初始化工作");
        }
    }
    
整理的以上的springboot插件使用步骤：

1 进入开源拾椹的开放源代码地址：https://github.com/ubuntu-m/opensource/tree/master/jopen-springboot-plugins
2 使用git克隆到本地；
3 打开命令行，并且切换到该项目目录下，执行mvn clean install；
4 引入maven依赖

    
    <!--依赖AOP模块插件-->
            <dependency>
                <groupId>io.jopen</groupId>
                <artifactId>jopen-springboot-plugin-aop</artifactId>
                <version>1.0</version>
            </dependency>
    
            <!--限流模块插件-->
            <dependency>
                <groupId>io.jopen</groupId>
                <artifactId>jopen-springboot-plugin-limit</artifactId>
                <version>1.0</version>
            </dependency>
    
            <!--初始化模块插件-->
            <dependency>
                <groupId>io.jopen</groupId>
                <artifactId>jopen-springboot-plugin-init</artifactId>
                <version>1.0</version>
            </dependency>
    
            <!--注解缓存插件-->
            <dependency>
                <groupId>io.jopen</groupId>
                <artifactId>jopen-springboot-plugin-annotation-cache</artifactId>
                <version>1.0</version>
            </dependency>
    
            <!--加密解密插件-->
            <dependency>
                <groupId>io.jopen</groupId>
                <artifactId>jopen-springboot-plugin-encryption</artifactId>
                <version>1.0</version>
            </dependency>
    
            <!--mongo Fluent风格构建起-->
            <dependency>
                <groupId>io.jopen</groupId>
                <artifactId>jopen-springboot-plugin-mongo-template-builder</artifactId>
                <version>1.0</version>
            </dependency>
            
    
    项目启动类配置，示例代码如下
    
    
    @SpringBootApplication
    
    /*注解式插件*/
    @EnableJopenLimit
    @EnableJopenEncryptBody
    @EnableJopenInit
    @EnableJopenParamTest
    @EnableJopenAnnotationCache
    public class Application {
    
        public static void main(String[] args) {
            SpringApplication.run(Application.class);
        }
    }
    
    整合插件示例项目地址：
    https://github.com/ubuntu-m/opensource/tree/master/jopen-springboot-plugins/jopen-springboot-apply-plugin-demo/src/main/java/io/jopen/apply/plugin/demo