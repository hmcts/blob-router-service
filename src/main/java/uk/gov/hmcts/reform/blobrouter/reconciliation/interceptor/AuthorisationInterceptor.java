package uk.gov.hmcts.reform.blobrouter.reconciliation.interceptor;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidApiKeyException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.microsoft.applicationinsights.boot.dependencies.google.common.net.HttpHeaders.AUTHORIZATION;

@Component
public class AuthorisationInterceptor implements HandlerInterceptor {

    private final String apiKey;

    public AuthorisationInterceptor(@Value("${reconciliation.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorizationKey = request.getHeader(AUTHORIZATION);

        if (StringUtils.isEmpty(authorizationKey)) {
            throw new InvalidApiKeyException("API Key is missing");
        } else if (!authorizationKey.equals(apiKey)) {
            throw new InvalidApiKeyException("Invalid API Key");
        }

        return true;
    }
}
