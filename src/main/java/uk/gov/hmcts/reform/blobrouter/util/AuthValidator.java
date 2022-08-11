package uk.gov.hmcts.reform.blobrouter.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidApiKeyException;

import static org.slf4j.LoggerFactory.getLogger;

public final class AuthValidator {
    private static final Logger logger = getLogger(AuthValidator.class);

    private AuthValidator() {
        // utility class constructor
    }

    public static void validateAuthorization(String authorizationKey, String apiKey) {
        if (StringUtils.isEmpty(authorizationKey)) {
            logger.error("API Key is missing");
            throw new InvalidApiKeyException("API Key is missing");
        } else if (!authorizationKey.equals("Bearer " + apiKey)) {
            logger.error("Invalid API Key");
            throw new InvalidApiKeyException("Invalid API Key");
        }
    }
}
