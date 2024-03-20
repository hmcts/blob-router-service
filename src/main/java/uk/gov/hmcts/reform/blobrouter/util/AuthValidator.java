package uk.gov.hmcts.reform.blobrouter.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidApiKeyException;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * The `AuthValidator` class provides a method `validateAuthorization` to check
 * if the provided authorization key matches the expected format and value.
 */
public final class AuthValidator {
    private static final Logger logger = getLogger(AuthValidator.class);

    private AuthValidator() {
        // utility class constructor
    }

    /**
     * The function `validateAuthorization` checks if the provided authorization key
     * matches the expected format and value, throwing an exception if it is missing or invalid.
     *
     * @param authorizationKey The `authorizationKey` parameter is typically a string that represents the
     *                         authorization token used for authentication. In this specific method, it
     *                         is expected to be in the format "Bearer {apiKey}", where `{apiKey}` is the
     *                         actual API key used for authorization.
     * @param apiKey Sure, I see that the `apiKey` parameter is used as part of the authorization process in
     *               the `validateAuthorization` method. If you provide me with the value of the `apiKey`
     *               parameter, I can help you understand how it is being used in the method.
     */
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
