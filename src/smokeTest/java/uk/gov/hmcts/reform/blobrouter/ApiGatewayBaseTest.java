package uk.gov.hmcts.reform.blobrouter;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.http.conn.ssl.SSLSocketFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Base64;

import static io.restassured.config.SSLConfig.sslConfig;
import static org.assertj.core.api.Assertions.assertThat;

class ApiGatewayBaseTest {

    protected static final String SUBSCRIPTION_KEY_HEADER_NAME = "Ocp-Apim-Subscription-Key";
    protected static final String PASSWORD_FOR_UNRECOGNISED_CLIENT_CERT = "testcert";
    protected static final String KEY_STORE_TYPE_PKCS_12 = "PKCS12";

    private static Config config;
    protected static String apiGatewayUrl;
    protected static String apiGatewayUrlNew;
    protected static String jwtAccessTokenUrl;
    protected static KeyStoreWithPassword validClientKeyStore;
    protected static String validSubscriptionKey;

    protected static OauthCredential oauthCredential;

    protected static void loadConfig() throws Exception {
        config = ConfigFactory.load();
        apiGatewayUrl = getApiGatewayUrl();
        apiGatewayUrlNew = getApiGatewayUrlNew();
        jwtAccessTokenUrl = getJwtAccessTokenUrl();
        validClientKeyStore = getValidClientKeyStore();
        validSubscriptionKey = getValidSubscriptionKey();
        oauthCredential = getValidOauthCredential();
    }

    private static OauthCredential getValidOauthCredential() {
        return new OauthCredential(
            config.resolve().getString("bulk-scan-client-id"),
            config.resolve().getString("bulk-scan-client-secret"),
            config.resolve().getString("bulk-scan-app-id"),
            config.resolve().getString("bulk-scan-app-secret"),
            config.resolve().getString("tenant-id")
        );
    }

    protected String getJwtAccessToken() {
        Response response = RestAssured
            .given()
            .contentType(ContentType.URLENC)
            .formParam("grant_type", "client_credentials")
            .formParam("client_id", oauthCredential.clientId)
            .formParam("scope", "api://" + oauthCredential.appId + "/.default")
            .formParam("client_secret", oauthCredential.clientSecret)
            .post(jwtAccessTokenUrl)
            .thenReturn();

        return response.jsonPath().getString("access_token");
    }

    protected RestAssuredConfig getSslConfigForClientCertificate(
        KeyStore clientKeyStore,
        String clientKeyStorePassword
    ) throws Exception {
        SSLConfig sslConfig = sslConfig()
            .allowAllHostnames()
            .sslSocketFactory(new SSLSocketFactory(clientKeyStore, clientKeyStorePassword));

        return RestAssured.config().sslConfig(sslConfig);
    }

    private static KeyStoreWithPassword getValidClientKeyStore() throws Exception {
        return getClientKeyStore(
            config.resolve().getString("test-valid-key-store"),
            config.resolve().getString("test-valid-key-store-password")
        );
    }

    protected static KeyStoreWithPassword getExpiredClientKeyStore() throws Exception {
        return getClientKeyStore(
            config.resolve().getString("test-expired-key-store"),
            config.resolve().getString("test-expired-key-store-password")
        );
    }

    protected static KeyStoreWithPassword getNotYetValidClientKeyStore() throws Exception {
        return getClientKeyStore(
            config.resolve().getString("test-not-yet-valid-key-store"),
            config.resolve().getString("test-not-yet-valid-key-store-password")
        );
    }

    private static String getValidSubscriptionKey() {
        String subscriptionKey = config.resolve().getString("test-subscription-key");
        assertThat(subscriptionKey).as("Subscription key").isNotEmpty();
        return subscriptionKey;
    }

    private static KeyStoreWithPassword getClientKeyStore(String base64Content, String password) throws Exception {
        byte[] rawContent = Base64.getMimeDecoder().decode(base64Content);

        KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE_PKCS_12);
        keyStore.load(new ByteArrayInputStream(rawContent), password.toCharArray());

        return new KeyStoreWithPassword(keyStore, password);
    }

    protected static KeyStoreWithPassword getUnrecognisedClientKeyStore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE_PKCS_12);

        try (
            InputStream keyStoreStream =
                GetSasTokenTest.class.getClassLoader().getResourceAsStream("unrecognised-client-certificate.pfx")
        ) {
            // loading from null stream would cause a quiet failure
            assertThat(keyStoreStream).isNotNull();

            keyStore.load(keyStoreStream, PASSWORD_FOR_UNRECOGNISED_CLIENT_CERT.toCharArray());
        }

        return new KeyStoreWithPassword(keyStore, PASSWORD_FOR_UNRECOGNISED_CLIENT_CERT);
    }

    private static String getJwtAccessTokenUrl() {
        String jwtAccessTokenUrl = config.resolve().getString("jwt-access-token-url");
        assertThat(jwtAccessTokenUrl).as("JWT Access Token URL").isNotEmpty();
        return jwtAccessTokenUrl;
    }

    private static String getApiGatewayUrl() {
        String apiUrl = config.resolve().getString("api-gateway-url");
        assertThat(apiUrl).as("API gateway URL").isNotEmpty();
        return apiUrl;
    }

    private static String getApiGatewayUrlNew() {
        String apiUrl = config.resolve().getString("api-gateway-url-new");
        assertThat(apiUrl).as("New API gateway URL").isNotEmpty();
        return apiUrl;
    }

    protected static class KeyStoreWithPassword {
        final KeyStore keyStore;
        final String password;

        KeyStoreWithPassword(KeyStore keyStore, String password) {
            this.keyStore = keyStore;
            this.password = password;
        }
    }

    protected static class OauthCredential {
        final String clientId;
        final String clientSecret;
        final String appId;
        final String appSecret;

        final String tenantId;


        protected OauthCredential(String clientId, String clientSecret, String appId,
                                  String appSecret, String tenantId) {
            this.clientId = clientId;
            this.clientSecret = clientSecret;
            this.appId = appId;
            this.appSecret = appSecret;
            this.tenantId = tenantId;
        }
    }
}
