package ams.asmtravel.api.services.admin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

/** A page is sent as {@code {content, page: {size, number, totalElements, totalPages}}}, the same shape every time. */
@Configuration(proxyBeanMethods = false)
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class PagingConfig {
}
