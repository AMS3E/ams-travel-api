package ams.asmtravel.api.services.audit.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Binds {@code app.audit.*}, enables the nightly trim and adds {@link AuditActorInterceptor} when the log is on. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(AuditProperties.class)
public class AuditConfig implements WebMvcConfigurer {

    private final ObjectProvider<AuditActorInterceptor> actorInterceptor;

    public AuditConfig(ObjectProvider<AuditActorInterceptor> actorInterceptor) {
        this.actorInterceptor = actorInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        actorInterceptor.ifAvailable(interceptor -> registry.addInterceptor(interceptor).addPathPatterns("/api/v1/**"));
    }
}
