package uk.gov.hmcts.reform.blobrouter;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Value;

public class OauthBase {

    @Value("${oauth-api-gateway-url}")
    private String oauthApiGatewayUrl;

    @Value("${bulk-scan-app-id}")
    private String bulkScanAppId;

    @Value("${bulk-scan-app-secret}")
    private String bulkScanSecret;

    @Value("${bulk-scan-client-id}")
    private String bulkScanClientId;

    @Value("${bulk-scan-client-secret}")
    private String bulkScanClientSecret;

    @Value("${tenant-id}")
    private String tenantId;

    protected String getJwtAccessToken() {
        Response response = RestAssured
            .given()
            .contentType(ContentType.URLENC)
            .formParam("grant_type", "client_credentials")
            .formParam("client_id", bulkScanClientId)
            .formParam("scope", "api://" + bulkScanAppId + "/.default")
            .formParam("client_secret", bulkScanClientSecret)
            .post("https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token")
            .thenReturn();

        return response.jsonPath().getString("access_token");
    }

    protected String getOauthApiGatewayUrl() {
        return oauthApiGatewayUrl;
    }
}
