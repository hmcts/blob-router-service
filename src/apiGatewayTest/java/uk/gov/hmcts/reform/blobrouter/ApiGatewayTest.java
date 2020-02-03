package uk.gov.hmcts.reform.blobrouter;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class ApiGatewayTest {

    private static final String SUBSCRIPTION_KEY_HEADER_NAME = "Ocp-Apim-Subscription-Key";
    private static final String PASSWORD_FOR_UNRECOGNISED_CLIENT_CERT = "testcert";

    private static final Config CONFIG = ConfigFactory.load();
    private static final String API_URL = CONFIG.getString("api.gateway-url");
    private static final String SAS_TOKEN_ENDPOINT = "/token/bulkscan";
    private static final String SUBSCRIPTION_KEY = CONFIG.getString("client.subscription-key");

    private static File validJavaKeyStore;
    private static final String VALID_KEY_STORE_PASSWORD = CONFIG.getString("client.valid-key-store.password");

    @BeforeAll
    static void checkConfig() throws IOException {
        assertThat(API_URL).as("API gateway URL").isNotEmpty();
        assertThat(SUBSCRIPTION_KEY).as("Subscription key").isNotEmpty();

        // create tmp file for valid certificate retrieved from key vault
        validJavaKeyStore = File.createTempFile("appGW", "test");

        try (var fos = new FileOutputStream(validJavaKeyStore)) {
            fos.write(Base64.getDecoder().decode(CONFIG.getString("client.valid-key-store.content")));
        }
    }

    @Test
    void should_accept_request_with_valid_certificate_and_subscription_key() {
        Response response = callSasTokenEndpoint(getValidClientKeyStore(), SUBSCRIPTION_KEY);

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.body().jsonPath().getString("sas_token")).isNotEmpty();
    }

    @Test
    void should_reject_request_with_invalid_subscription_key() {
        Response response = callSasTokenEndpoint(getValidClientKeyStore(), "invalid-subscription-key123");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body().asString()).contains("Access denied due to invalid subscription key");
    }

    @Test
    void should_reject_request_lacking_subscription_key() {
        Response response = callSasTokenEndpoint(getValidClientKeyStore(), null);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body().asString()).contains("Access denied due to missing subscription key");
    }

    @Test
    void should_reject_request_with_unrecognised_client_certificate() {
        Response response = callSasTokenEndpoint(getUnrecognisedClientKeyStore(), SUBSCRIPTION_KEY);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body().asString()).isEqualTo("Invalid client certificate");
    }

    @Test
    void should_reject_request_lacking_client_certificate() {
        Response response = callSasTokenEndpoint(null, SUBSCRIPTION_KEY);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body().asString()).isEqualTo("Missing client certificate");
    }

    @Test
    void should_not_expose_http_version() {
        Response response = RestAssured
            .given()
            .baseUri(API_URL.replace("https://", "http://"))
            .header(SUBSCRIPTION_KEY_HEADER_NAME, SAS_TOKEN_ENDPOINT)
            .when()
            .get(SAS_TOKEN_ENDPOINT);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body().asString()).contains("Resource not found");
    }

    private Response callSasTokenEndpoint(KeyStoreWithPassword clientKeyStore, String subscriptionKey) {
        RequestSpecification request = RestAssured.given().baseUri(API_URL);

        if (clientKeyStore != null) {
            request = request.config(
                getSslConfigForClientCertificate(
                    clientKeyStore.javaKeyStore,
                    clientKeyStore.password
                )
            );
        }

        if (subscriptionKey != null) {
            request = request.header(SUBSCRIPTION_KEY_HEADER_NAME, subscriptionKey);
        }

        return request.get(SAS_TOKEN_ENDPOINT);
    }

    private RestAssuredConfig getSslConfigForClientCertificate(
        File clientKeyStore,
        String clientKeyStorePassword
    ) {
        return RestAssured.config().sslConfig(
            new SSLConfig()
                .allowAllHostnames()
                .keyStore(clientKeyStore, clientKeyStorePassword)
        );
    }

    private KeyStoreWithPassword getValidClientKeyStore() {
        return getClientKeyStore(validJavaKeyStore, VALID_KEY_STORE_PASSWORD);
    }

    private KeyStoreWithPassword getUnrecognisedClientKeyStore() {
        return getClientKeyStore(
            new File(getClass().getClassLoader().getResource("unrecognised-client-certificate.jks").getFile()),
            PASSWORD_FOR_UNRECOGNISED_CLIENT_CERT
        );
    }

    private KeyStoreWithPassword getClientKeyStore(File javaKeyStore, String password) {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");

            try (InputStream keyStoreStream = new FileInputStream(javaKeyStore)) {
                // loading from null stream would cause a quiet failure
                assertThat(keyStoreStream).isNotNull();

                keyStore.load(keyStoreStream, password.toCharArray());
            }

            return new KeyStoreWithPassword(javaKeyStore, password);
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException exception) {
            throw new RuntimeException("Failed to load key store", exception);
        }
    }

    private static class KeyStoreWithPassword {
        final File javaKeyStore;
        final String password;

        KeyStoreWithPassword(File javaKeyStore, String password) {
            this.javaKeyStore = javaKeyStore;
            this.password = password;
        }
    }
}
