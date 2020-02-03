package uk.gov.hmcts.reform.blobrouter;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Base64;

import static io.restassured.config.SSLConfig.sslConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

public class GetSasTokenTest {

    private static final String SUBSCRIPTION_KEY_HEADER_NAME = "Ocp-Apim-Subscription-Key";
    private static final String PASSWORD_FOR_UNRECOGNISED_CLIENT_CERT = "testcert";
    private static final String SAS_TOKEN_ENDPOINT_PATH = "/token/bulkscan";
    private static final String PKCS_12 = "PKCS12";

    private static Config CONFIG;
    private static String API_GATEWAY_URL;
    private static KeyStoreWithPassword VALID_CLIENT_KEY_STORE;
    private static KeyStoreWithPassword UNRECOGNIZED_CLIENT_KEY_STORE;
    private static KeyStoreWithPassword EXPIRED_CLIENT_KEY_STORE;
    private static KeyStoreWithPassword NOT_YET_VALID_CLIENT_KEY_STORE;
    private static String VALID_SUBSCRIPTION_KEY;

    @BeforeAll
    static void loadConfig() throws Exception {
        CONFIG = ConfigFactory.load();
        API_GATEWAY_URL = getApiGatewayUrl();
        VALID_CLIENT_KEY_STORE = getValidClientKeyStore();
        UNRECOGNIZED_CLIENT_KEY_STORE = getUnrecognisedClientKeyStore();
        EXPIRED_CLIENT_KEY_STORE = getExpiredClientKeyStore();
        NOT_YET_VALID_CLIENT_KEY_STORE = getNotYetValidClientKeyStore();
        VALID_SUBSCRIPTION_KEY = getValidSubscriptionKey();
    }

    @Test
    void should_accept_request_with_valid_certificate_and_subscription_key() throws Exception {
        Response response =
            callSasTokenEndpoint(VALID_CLIENT_KEY_STORE, VALID_SUBSCRIPTION_KEY)
                .thenReturn();

        assertThat(response.getStatusCode()).isEqualTo(OK.value());
        assertThat(response.body().jsonPath().getString("sas_token")).isNotEmpty();
    }

    @Test
    void should_reject_request_with_invalid_subscription_key() throws Exception {
        Response response = callSasTokenEndpoint(
            VALID_CLIENT_KEY_STORE,
            "invalid-subscription-key123"
        )
            .thenReturn();

        assertThat(response.statusCode()).isEqualTo(UNAUTHORIZED.value());
        assertThat(response.body().asString()).contains("Access denied due to invalid subscription key");
    }

    @Test
    void should_reject_request_lacking_subscription_key() throws Exception {
        Response response = callSasTokenEndpoint(
            VALID_CLIENT_KEY_STORE,
            null
        )
            .thenReturn();

        assertThat(response.statusCode()).isEqualTo(UNAUTHORIZED.value());
        assertThat(response.body().asString()).contains("Access denied due to missing subscription key");
    }

    @Test
    void should_reject_request_with_unrecognised_client_certificate() throws Exception {
        Response response = callSasTokenEndpoint(
            UNRECOGNIZED_CLIENT_KEY_STORE,
            VALID_SUBSCRIPTION_KEY
        )
            .thenReturn();

        assertThat(response.statusCode()).isEqualTo(UNAUTHORIZED.value());
        assertThat(response.body().asString()).isEqualTo("Invalid client certificate");
    }

    @Test
    void should_reject_request_lacking_client_certificate() throws Exception {
        Response response =
            callSasTokenEndpoint(
                null,
                VALID_SUBSCRIPTION_KEY
            )
                .thenReturn();

        assertThat(response.statusCode()).isEqualTo(UNAUTHORIZED.value());
        assertThat(response.body().asString()).isEqualTo("Missing client certificate");
    }

    @Test
    void should_not_expose_http_version() {
        Response response = RestAssured
            .given()
            .baseUri(API_GATEWAY_URL.replace("https://", "http://"))
            .header(SUBSCRIPTION_KEY_HEADER_NAME, VALID_SUBSCRIPTION_KEY)
            .when()
            .get(SAS_TOKEN_ENDPOINT_PATH)
            .thenReturn();

        assertThat(response.statusCode()).isEqualTo(NOT_FOUND.value());
        assertThat(response.body().asString()).contains("Resource not found");
    }

    @Test
    void should_reject_request_with_expired_client_certificate() throws Exception {
        Response response = callSasTokenEndpoint(
            EXPIRED_CLIENT_KEY_STORE,
            VALID_SUBSCRIPTION_KEY
        )
            .thenReturn();

        assertThat(response.statusCode()).isEqualTo(UNAUTHORIZED.value());
        assertThat(response.body().asString()).isEqualTo("Invalid client certificate");
    }

    @Test
    void should_reject_request_with_not_yet_valid_client_certificate() throws Exception {
        Response response = callSasTokenEndpoint(
            NOT_YET_VALID_CLIENT_KEY_STORE,
            VALID_SUBSCRIPTION_KEY
        )
            .thenReturn();

        assertThat(response.statusCode()).isEqualTo(UNAUTHORIZED.value());
        assertThat(response.body().asString()).isEqualTo("Invalid client certificate");
    }

    private Response callSasTokenEndpoint(
        KeyStoreWithPassword clientKeyStore,
        String subscriptionKey
    ) throws Exception {
        RequestSpecification request = RestAssured.given().baseUri(API_GATEWAY_URL);

        if (clientKeyStore != null) {
            request = request.config(
                getSslConfigForClientCertificate(
                    clientKeyStore.keyStore,
                    clientKeyStore.password
                )
            );
        }

        if (subscriptionKey != null) {
            request = request.header(SUBSCRIPTION_KEY_HEADER_NAME, subscriptionKey);
        }

        return request.get(SAS_TOKEN_ENDPOINT_PATH);
    }

    private RestAssuredConfig getSslConfigForClientCertificate(
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
            CONFIG.resolve().getString("test-valid-key-store"),
            CONFIG.resolve().getString("test-valid-key-store-password")
        );
    }

    private static KeyStoreWithPassword getExpiredClientKeyStore() throws Exception {
        return getClientKeyStore(
            CONFIG.resolve().getString("test-expired-key-store"),
            CONFIG.resolve().getString("test-expired-key-store-password")
        );
    }

    private static KeyStoreWithPassword getNotYetValidClientKeyStore() throws Exception {
        return getClientKeyStore(
            CONFIG.resolve().getString("test-not-yet-valid-key-store"),
            CONFIG.resolve().getString("test-not-yet-valid-key-store-password")
        );
    }

    private static KeyStoreWithPassword getClientKeyStore(String base64Content, String password) throws Exception {
        byte[] rawContent = Base64.getMimeDecoder().decode(base64Content);

        KeyStore keyStore = KeyStore.getInstance(PKCS_12);
        keyStore.load(new ByteArrayInputStream(rawContent), password.toCharArray());

        return new KeyStoreWithPassword(keyStore, password);
    }

    private static KeyStoreWithPassword getUnrecognisedClientKeyStore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(PKCS_12);

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

    private static String getValidSubscriptionKey() {
        String subscriptionKey = CONFIG.resolve().getString("test-subscription-key");
        assertThat(subscriptionKey).as("Subscription key").isNotEmpty();
        return subscriptionKey;
    }

    private static String getApiGatewayUrl() {
        String apiUrl = CONFIG.resolve().getString("api-gateway-url");
        assertThat(apiUrl).as("API gateway URL").isNotEmpty();
        return apiUrl;
    }

    private static class KeyStoreWithPassword {
        final KeyStore keyStore;
        final String password;

        KeyStoreWithPassword(KeyStore keyStore, String password) {
            this.keyStore = keyStore;
            this.password = password;
        }
    }
}
