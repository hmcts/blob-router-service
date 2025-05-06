package uk.gov.hmcts.reform.blobrouter;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static org.assertj.core.api.Assertions.assertThat;

class ApiGatewayBaseTest {

    private static Config config;
    protected static String oauthGatewayUrl;

    protected static OauthCredential oauthCredential;

    protected static void loadConfig() {
        config = ConfigFactory.load();
        oauthGatewayUrl = getOauthApiGatewayUrl();
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
            .post("https://login.microsoftonline.com/" + oauthCredential.tenantId + "/oauth2/v2.0/token")
            .thenReturn();

        return response.jsonPath().getString("access_token");
    }

    private static String getOauthApiGatewayUrl() {
        String apiUrl = config.resolve().getString("oauth-api-gateway-url");
        assertThat(apiUrl).as("New API gateway URL").isNotEmpty();
        return apiUrl;
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
