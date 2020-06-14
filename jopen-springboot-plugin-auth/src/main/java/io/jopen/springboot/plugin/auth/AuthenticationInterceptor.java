package io.jopen.springboot.plugin.auth;

import com.google.common.collect.ImmutableMap;
import io.jopen.springboot.plugin.annotation.cache.BaseInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.web.servlet.HandlerMapping.LOOKUP_PATH;

/**
 * 身份验证拦截器
 * {@link PathMatcher}
 * {@link AuthRegistration}
 * {@link CredentialFunction}
 *
 * @author maxuefeng
 */
@Slf4j
@Component
public class AuthenticationInterceptor extends BaseInterceptor implements CommandLineRunner {

    /**
     * @see PathMatcher
     * {@link AntPathMatcher}
     */
    private final PathMatcher pathMatcher = new AntPathMatcher("/");

    /**
     * 当前拦截器的顺序
     */
    private int order;

    /**
     * 要拦截的路径
     */
    private String[] pathPatterns;

    /**
     * @see UrlPathHelper
     */
    private final UrlPathHelper urlPathHelper = new UrlPathHelper();

    /**
     * 要排除的路径
     */
    private String[] excludePathPatterns;

    /**
     * {@link ImmutableMap#entry(Object, Object)}
     * key表示group
     */
    private ImmutableMap<String, List<AuthRegistration>> authGroup;

    /**
     *
     */
    @Autowired
    private AuthContext authContext;

    @Autowired
    private AuthMetadata authMetadata;

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String[] getPathPatterns() {
        return pathPatterns;
    }

    public void setPathPatterns(String[] pathPatterns) {
        this.pathPatterns = pathPatterns;
    }

    public String[] getExcludePathPatterns() {
        return excludePathPatterns;
    }

    public void setExcludePathPatterns(String[] excludePathPatterns) {
        this.excludePathPatterns = excludePathPatterns;
    }

    /**
     * @param request  {@link HttpServletRequest}
     * @param response {@link HttpServletResponse}
     * @param handler  {@link org.springframework.web.method.HandlerMethod}
     * @return {@link Boolean} if true pass else throw a new RuntimeException
     * @see org.springframework.web.util.pattern.PathPattern
     * @see org.springframework.web.util.pattern.PathPattern
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Verify verify = super.getApiServiceAnnotation(Verify.class, handler);
        if (verify != null) {

            boolean require = verify.require();

            List<AuthRegistration> authRules = this.authGroup.get(verify.group());

            if (authRules == null || authRules.isEmpty()) {
                throw new RuntimeException(String.format("Server config Error Please setup auth rules of group %s", verify.group()));
            }

            // 获取请求路径
            String lookupPath = this.urlPathHelper.getLookupPathForRequest(request, LOOKUP_PATH);

            AuthRegistration rule = authRules.stream()
                    .filter(authRule -> authRule.getPathPatterns().stream().anyMatch(pathPattern -> this.matches(pathPattern, lookupPath)))
                    .findFirst()
                    .orElse(null);


            if (rule == null) {
                log.error("Server config error Please setup AuthRegistration of lookup path {} group {}  ", lookupPath, verify.group());
                return true;
            }

            if (rule.getCredentialFunction() == null) {
                log.error("Server config error Please setup CredentialFunction of lookup path {} group {}  ", lookupPath, verify.group());
                return true;
            }

            CredentialFunction credentialFunction = rule.getCredentialFunction();

            Credential credential = credentialFunction.apply(request);

            checkupCredential(credential, verify, credentialFunction, require);

            authContext.setCredential(request, credential);
        }
        return true;
    }

    private void checkupCredential(Credential credential,
                                   Verify verify,
                                   CredentialFunction credentialFunction,
                                   boolean require
    ) {
        if (!credential.getValid()) throw credentialFunction.ifErrorThrowing();
        // 没有设定角色 || 或者设定了*号  任何角色都可以访问
        String[] requireAllowRoles = verify.role();
        if (requireAllowRoles.length == 0 || "*".equals(requireAllowRoles[0])) return;

        // 用户角色
        String[] roles = credential.getRoles();
        // 求两个数组的交集
        List<String> requireAllowRoleList = Arrays.asList(requireAllowRoles);
        if (Arrays.stream(roles).anyMatch(requireAllowRoleList::contains)) return;

        if (require)
            throw credentialFunction.ifErrorThrowing();
    }

    /**
     * Determine a match for the given lookup path.
     *
     * @param pathPattern setup pathPattern
     * @param lookupPath  the current request path
     * @return {@code true} if the interceptor applies to the given request path
     */
    private boolean matches(String pathPattern, @NonNull String lookupPath) {
        return this.pathMatcher.match(pathPattern, lookupPath);
    }

    @Override
    public void run(String... args) {
        Collection<AuthRegistration> authRules = this.authMetadata.setupAuthRules();

        this.authGroup = ImmutableMap.copyOf(
                authRules.stream()
                        .collect(Collectors.groupingBy(AuthRegistration::getGroup)));

        if (!authGroup.containsKey("Default")) {
            throw new RuntimeException("Server auth config error please setup Default auth rule");
        }
    }
}
