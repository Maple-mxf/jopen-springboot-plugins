package automation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Verify;
import com.jcraft.jsch.JSchException;
import com.onepushing.cloud.website.entity.DomainRecord;
import com.onepushing.cloud.website.util.DomainRecordUtil;
import com.onepushing.springboot.support.exception.ServiceException;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 重新配置nginx网关
 *
 * @author maxuefeng
 */
//@Component
//@Slf4j
@Deprecated
public class ReloadGatewayConfigPerformer {

    private final FileTransfer fileTransfer;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private DomainRecordUtil domainRecordUtil;

    /**
     * 宿主机的nginx的配置目录（此目录下可包含多个配置文件）
     */
    private final String nginxConfigurationDir = "/usr/local/webserver/nginx/conf/dynamic";

    // 正式域名前缀
    private final String formalDomainPrefix = "formal.";

    // 预览域名前缀
    private final String previewDomainPrefix = "preview.";

    private ObjectMapper mapper = new ObjectMapper();

    private final String domainName;


    public ReloadGatewayConfigPerformer(Adapter adapter) throws JSchException {
        Verify.verify(adapter != null, "adapter required");
        this.fileTransfer = new FileTransfer(adapter);

        try {
            String content = StreamUtils.copyToString(ReloadGatewayConfigPerformer.class.getResourceAsStream("domain-config.json"), StandardCharsets.UTF_8);
            Verify.verify(StringUtils.isNotBlank(content));
            JsonNode rootNode = mapper.readTree(content);

            this.domainName = rootNode.path("domain").textValue();
            Verify.verify(StringUtils.isNotBlank(domainName));

        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }


    public void reload(Docker docker) {
        ReloadGatewayConfigPerformer.NginxConfigurationFileDescriber configurationFileDescriber
                = new ReloadGatewayConfigPerformer.NginxConfigurationFileDescriber();
        configurationFileDescriber.setFileName(String.format("%s-nginx.conf", docker.getId()));
        configurationFileDescriber.setNoDistDirCreate(true);
        configurationFileDescriber.setPutAbsolutePath(nginxConfigurationDir);

        ReloadGatewayConfigPerformer.NginxConfigurationProperties configurationProperties
                = new ReloadGatewayConfigPerformer.NginxConfigurationProperties();

        configurationProperties.setProxyAddr(String.format("http://127.0.0.1:%s", docker.getOutHostMappingPort()));
        configurationProperties.setErrorProxy("500.html");

        DomainRecord record = randomDomain();
        record.setValue(docker.getPublicIp());
        record = domainRecordUtil.addDomainRecord(record);
        configurationProperties.setServerName(String.format("%s.%s", record.getRR(), record.getDomainName()));

        // 保存域名的数据库记录
        mongoTemplate.save(record);

        this.doReload(configurationFileDescriber, configurationProperties);
    }

    /**
     * @see com.onepushing.cloud.website.entity.DomainRecord
     */
    public DomainRecord randomDomain() {
        Query query = new Query();
        query.fields().include("RR");
        Set<String> rrs = mongoTemplate.find(query, DomainRecord.class)
                .stream().map(DomainRecord::getRR)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        String random = getRandomString(rrs);
        DomainRecord record = new DomainRecord();
        record.setType("A");
        record.setRR(this.formalDomainPrefix + random);
        record.setDomainName(this.domainName);
        return record;
    }

    public String getRandomString(Set<String> rrs) {
        String random;
        do {
            random = String.valueOf(RandomUtils.nextInt(999, 10000));
        } while (rrs.contains(random));
        return random;
    }


    private void doReload(NginxConfigurationFileDescriber configurationFileDescriber,
                          NginxConfigurationProperties configurationProperties) {

        // fileTransfer
        String configurationText = GenConfiguration.gen(configurationProperties);
        InputStream inputStream = GenConfiguration.mappedStream(configurationText);

        try {
            fileTransfer.transfer(
                    configurationFileDescriber.getPutAbsolutePath(),
                    inputStream,
                    configurationFileDescriber.getFileName(),
                    configurationFileDescriber.getNoDistDirCreate());
            fileTransfer.releaseSource();
        } catch (Throwable throwable) {
            throwable.getMessage();
            throw new ServiceException(throwable.getMessage());
        }
    }

    /**
     * nginx配置文件的描述
     */
    @Getter
    @Setter
    public static class NginxConfigurationFileDescriber {
        // 文件名称
        private String fileName;
        // 放置的位置
        private String putAbsolutePath;
        // 如果没有目标位置是否创建
        private Boolean noDistDirCreate;
    }

    /**
     * nginx配置内容
     */
    @Getter
    @Setter
    public static class NginxConfigurationProperties {
        /**
         * 域名peizhi
         */
        private String serverName;
        private String proxyAddr;
        private String errorProxy;
    }

    public static class GenConfiguration {
        private static final VelocityEngine VE = new VelocityEngine();

        static {
            VE.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
            VE.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
            VE.init();
        }

        /**
         * @param configurationProperties shell/nginx_children_template.conf
         * @see java.io.Writer
         */
        public static String gen(NginxConfigurationProperties configurationProperties) {
            Template configTemplate = VE.getTemplate("shell/nginx_children_template.conf");

            VelocityContext ctx = new VelocityContext();
            ctx.put("serverName", configurationProperties.getServerName());
            ctx.put("proxyAddr", configurationProperties.getProxyAddr());
            ctx.put("errorProxy", configurationProperties.getErrorProxy());

            StringWriter writer = new StringWriter();
            configTemplate.merge(ctx, writer);

            try {
                writer.flush();
                writer.close();

                String res = writer.getBuffer().toString();
                System.err.println(res);
                return res;
            } catch (IOException e) {
                e.printStackTrace();
                throw new ServiceException(e.getMessage());
            }
        }

        private static InputStream mappedStream(String src) {
            if (StringUtils.isNotBlank(src))
                return new ByteArrayInputStream(src.getBytes());
            return null;
        }
    }
}
