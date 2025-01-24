package uk.gov.hmcts.reform.blobrouter;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@ExtendWith(SpringExtension.class)
public class GetSasTokenTest {

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

    private static final String SAS_TOKEN_ENDPOINT_PATH = "/token/bulkscan";

    @Test
    void should_accept_request_with_valid_jwt_token() throws Exception {
        String jwtAccessToken = getJwtAccessToken();
        assertThat(jwtAccessToken).isNotEmpty();

        Response response = callSasTokenEndpointWithJwt(jwtAccessToken);
        assertThat(response.getStatusCode()).isEqualTo(UNPROCESSABLE_ENTITY.value());
        assertThat(response.body().jsonPath().getString("sas_token")).isNotEmpty();
    }

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

    private Response callSasTokenEndpointWithJwt(String jwtAccessToken) throws JsonProcessingException {
        Response response = RestAssured.given().header("Authorization", jwtAccessToken)
            .get(oauthApiGatewayUrl + SAS_TOKEN_ENDPOINT_PATH)
            .thenReturn();
        System.out.println(response.asPrettyString());
        return response;
    }

    private Response callSasTokenEndpointWithoutJwt() {
        Response response = RestAssured.given()
            .get(oauthApiGatewayUrl + SAS_TOKEN_ENDPOINT_PATH)
            .thenReturn();
        System.out.println(response.asPrettyString());
        return response;
    }
}
