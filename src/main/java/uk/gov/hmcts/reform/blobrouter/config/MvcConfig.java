package uk.gov.hmcts.reform.blobrouter.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.hmcts.reform.blobrouter.reconciliation.interceptor.AuthorisationInterceptor;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Autowired AuthorisationInterceptor authorisationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authorisationInterceptor)
            .addPathPatterns("/reform-scan/reconciliation-report/*");
    }

}
