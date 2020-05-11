package io.jopen.springboot.plugin.auth;

import java.lang.annotation.*;

/**
 * 权限认证
 *
 * @author maxuefeng
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Verify {

    /**
     * 默认任何角色都可以访问接口的角色
     *
     * @return 角色数组
     */
    String[] role() default {"*"};

    /**
     * 是否使用全局配置
     */
    @Deprecated
    boolean usingGlobalConfig() default true;

    /**
     * @see AuthMetadata
     */
    @Deprecated
    Class<? extends CredentialFunction> credentialFunctionType() default CredentialFunction.EmptyCredentialFunction.class;

    /**
     * @return 拦截之后的错误信息
     */
    @Deprecated
    String errMsg() default "access deny! because you has not access this api interface grant!";

    /**
     * 默认的认证组
     */
    String group() default "Default";
}
