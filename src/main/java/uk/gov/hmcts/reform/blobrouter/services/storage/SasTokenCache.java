package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.common.Utility;
import com.azure.storage.common.implementation.Constants;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.blobrouter.clients.bulkscanprocessor.BulkScanProcessorClient;
import uk.gov.hmcts.reform.blobrouter.clients.pcq.PcqClient;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidSasTokenException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAccessor;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.azure.storage.common.implementation.Constants.UrlConstants.SAS_EXPIRY_TIME;
import static com.azure.storage.common.implementation.StorageImplUtils.parseQueryStringSplitValues;
import static java.time.temporal.ChronoField.INSTANT_SECONDS;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * The `SasTokenCache` class in Java manages the retrieval and caching of
 * SAS tokens for different containers efficiently.
 */
@Service
public class SasTokenCache {

    private static final Logger logger = getLogger(SasTokenCache.class);
    private final BulkScanProcessorClient bulkScanSasTokenClient;
    private final long refreshSasBeforeExpiry;
    private final PcqClient pcqClient;
    private final AuthTokenGenerator authTokenGenerator;

    //key= container name, value = sastoken
    private static Cache<String, String> tokenCache;

    public SasTokenCache(
        BulkScanProcessorClient bulkScanSasTokenClient,
        PcqClient pcqClient,
        AuthTokenGenerator authTokenGenerator,
        @Value("${sas-token-cache.refresh-before-expire-in-sec}") long refreshSasBeforeExpiry
    ) {
        this.bulkScanSasTokenClient = bulkScanSasTokenClient;
        this.pcqClient = pcqClient;
        this.authTokenGenerator = authTokenGenerator;
        this.refreshSasBeforeExpiry = refreshSasBeforeExpiry;
        tokenCache = Caffeine.newBuilder()
            .expireAfter(new SasTokenCacheExpiry())
            .build();
    }

    /**
     * The `getSasToken` function retrieves a SAS token for a specified container, utilizing a token cache to avoid
     * unnecessary token generation.
     *
     * @param containerName It looks like you have provided a method `getSasToken` that retrieves a
     *                      SAS token for a given container name. The method uses a `tokenCache` to store
     *                      and retrieve SAS tokens efficiently. When `getSasToken` is called with a
     *                      specific `containerName`.
     * @return The method `getSasToken` is returning a SAS token for the specified container name after
     *      retrieving it from a token cache or creating a new one if it doesn't exist in the cache.
     */
    public String getSasToken(String containerName) {
        logger.info("Getting sas token for Container: {}", containerName);

        final String sasToken = tokenCache.get(containerName, this::createSasToken);

        logger.info("Finished getting sas token for Container: {}", containerName);

        return sasToken;
    }

    /**
     * The `getPcqSasToken` function retrieves a SAS token for a specified container name, utilizing a token cache for
     * efficiency.
     *
     * @param containerName The `getPcqSasToken` method is used to retrieve a Shared Access Signature (SAS)
     *                      token for a given container name. The method first logs an info message
     *                      indicating that it is getting the SAS token for the specified container.
     *                      It then attempts to retrieve the SAS token from a tokenCache object.
     * @return The method `getPcqSasToken` is returning a SAS token for the specified container name after retrieving it
     *      from the token cache or creating a new one if it doesn't exist in the cache.
     */
    public String getPcqSasToken(String containerName) {
        logger.info("Getting sas token for Container: {}", containerName);

        final String sasToken = tokenCache.get(containerName, this::createPcqSasToken);

        logger.info("Finished getting sas token for Container: {}", containerName);

        return sasToken;
    }

    /**
     * The `removeFromCache` function invalidates the cache for a specified container and logs the action.
     *
     * @param containerName The `removeFromCache` method takes a `containerName` as a parameter.
     *                      This parameter is used to identify the specific container for which
     *                      the cache needs to be invalidated.
     */
    public void removeFromCache(String containerName) {
        logger.info("Invalidating cache for Container: {}", containerName);

        tokenCache.invalidate(containerName);

        logger.info("Finished invalidating cache for Container: {}", containerName);
    }

    /**
     * The function `createSasToken` generates a SAS token for a specified container and logs the process.
     *
     * @param containerName The `createSasToken` method takes a `containerName` as a parameter.
     *                      This method logs a message indicating that it is making a SAS token call for
     *                      the specified container, retrieves the SAS token using a `bulkScanSasTokenClient`,
     *                      logs a message indicating that the call has finished.
     * @return The method `createSasToken` returns a String value, which is the SAS token
     *      obtained for the specified container name.
     */
    private String createSasToken(String containerName) {
        logger.info("Making sas token call for Container: {}", containerName);

        final String sasToken = bulkScanSasTokenClient.getSasToken(containerName).sasToken;

        logger.info("Finished making sas token call for Container: {}", containerName);

        return sasToken;
    }

    /**
     * This Java function creates a Shared Access Signature (SAS) token for a
     * specified container using a PCQ client and an authentication token generator.
     *
     * @param containerName The `createPcqSasToken` method takes a `containerName` as a parameter,
     *                      which is used to specify the name of the container for which the SAS token is
     *                      being generated. The method logs information about the container for which
     *                      the SAS token is being created and then calls the `getSasToken` method.
     * @return The method `createPcqSasToken` is returning the SAS token generated
     *      for the specified container name.
     */
    private String createPcqSasToken(String containerName) {
        logger.info("Making sas token call for Container: {}", containerName);

        final String sasToken = pcqClient.getSasToken(authTokenGenerator.generate()).sasToken;

        logger.info("Finished making sas token call for Container: {}", containerName);

        return sasToken;
    }

    /**
     * SasTokenCacheExpiry - implements the Expiry class from the Caffeine caching Java library.
     * See also {@link com.github.benmanes.caffeine.cache.Expiry}
     */
    private class SasTokenCacheExpiry implements Expiry<String, String> {

        public static final String MESSAGE = "Invalid SAS, the SAS expiration time parameter not found.";

        /**
         * Defines the cache eviction policy for a SAS token after its creation.
         * Decodes and parses the SAS token string into String/String[] key value pairs e.g.
         * 'se=2020-03-05T14%3A54%3A20Z' becomes like <'se', [2020-03-05T14:35:32.820Z]>
         * The SAS token expiration value from these key/value pairs is then used to
         * calculate when the SAS token should be evicted from the cache
         * @param containerName - the key the SAS token is stored under in the cache i.e. the container the token
         *                      was generated for.
         *
         * @param sasToken - the SAS token
         * @param currentTime
         * @return long - the length of time before the SAS token will be removed from cache
         */
        @Override
        public long expireAfterCreate(
            @NonNull String containerName,
            @NonNull String sasToken,
            long currentTime
        ) {
            Map<String, String[]> map = parseQueryStringSplitValues(Utility.urlDecode(sasToken));
            return calculateTimeToExpire(
                Constants.ISO_8601_UTC_DATE_FORMATTER.parse(
                    map.computeIfAbsent(
                        SAS_EXPIRY_TIME, key -> {
                            throw new InvalidSasTokenException(MESSAGE);
                        }
                    )[0]
                )
            );
        }

        @Override
        public long expireAfterUpdate(
            @NonNull String containerName,
            @NonNull String sasToken,
            long currentTime,
            @NonNegative long currentDuration
        ) {
            return expireAfterCreate(containerName, sasToken, currentTime);
        }

        @Override
        public long expireAfterRead(
            @NonNull String containerName,
            @NonNull String sasToken,
            long currentTime,
            @NonNegative long currentDuration
        ) {
            return currentDuration;
        }

        /**
         * calculates the remaining time to expire
         * Do not wait for the full time for expiry if remaining time is less
         * or equal to refreshSasBeforeExpiry it means cached value is expired.
         * calculation:
         * expirytime - (currenttime + refreshSasBeforeExpiry) = remaining time to expire in nano seconds
         *
         * @param expiry expiry time for sas token
         * @return remaining time to expire in nano secs,
         */
        private long calculateTimeToExpire(TemporalAccessor expiry) {
            return
                TimeUnit.NANOSECONDS.convert(
                    expiry.getLong(INSTANT_SECONDS)
                        - (OffsetDateTime.now(ZoneOffset.UTC).getLong(INSTANT_SECONDS) + refreshSasBeforeExpiry),
                    TimeUnit.SECONDS
                );
        }
    }
}
