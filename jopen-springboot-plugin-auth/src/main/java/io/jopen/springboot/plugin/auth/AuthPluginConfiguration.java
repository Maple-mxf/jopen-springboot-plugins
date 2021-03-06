package io.jopen.springboot.plugin.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author maxuefeng
 * @see org.springframework.web.util.pattern.PathPattern
 * @since 2020/1/26
 */
@Configuration
@Component
public class AuthPluginConfiguration implements ImportAware, WebMvcConfigurer {

    private AuthenticationInterceptor authenticationInterceptor;

    @Autowired
    public AuthPluginConfiguration(AuthenticationInterceptor authenticationInterceptor) {
        this.authenticationInterceptor = authenticationInterceptor;
    }

    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authenticationInterceptor)
                .addPathPatterns(authenticationInterceptor.getPathPatterns())
                .excludePathPatterns(authenticationInterceptor.getExcludePathPatterns())
                .order(authenticationInterceptor.getOrder());
    }

    /**
     * @param importMetadata 导入的元数据信息
     */
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        AnnotationAttributes enableAuth = AnnotationAttributes
                .fromMap(importMetadata.getAnnotationAttributes(EnableJopenAuth.class.getName(), false));

        if (enableAuth == null) {
            throw new IllegalArgumentException(
                    "@EnableJopenAuth is not present on importing class " + importMetadata.getClassName());
        }

        String[] pathPatterns = enableAuth.getStringArray("pathPatterns");
        String[] excludePathPatterns = enableAuth.getStringArray("excludePathPattern");
        int order = enableAuth.getNumber("order");

        // 设置当前对象的拦截器的顺序
        this.authenticationInterceptor.setPathPatterns(pathPatterns);
        this.authenticationInterceptor.setExcludePathPatterns(excludePathPatterns);
        this.authenticationInterceptor.setOrder(order);
    }
}
