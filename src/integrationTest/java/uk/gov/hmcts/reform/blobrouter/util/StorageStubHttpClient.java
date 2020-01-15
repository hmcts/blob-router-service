package uk.gov.hmcts.reform.blobrouter.util;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.test.http.MockHttpResponse;
import org.junit.jupiter.api.Assertions;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static uk.gov.hmcts.reform.blobrouter.util.ResourceFilesHelper.getFileContents;

class StorageStubHttpClient implements HttpClient {

    private static final String BLOB_PROPERTIES = "HEAD /bulkscan/new.blob?";
    private static final String DOWNLOAD_BLOB = "GET /bulkscan/new.blob?";
    private static final String LEASE_BLOB = "PUT /bulkscan/new.blob?comp=lease";
    private static final String LIST_CONTAINERS = "GET ?comp=list";
    private static final String LIST_ONE_BLOB = "GET /bulkscan?include=&restype=container&comp=list";
    private static final String LIST_ZERO_BLOBS = "GET /empty?include=&restype=container&comp=list";
    private static final String UPLOAD_NEW_BLOB = "PUT /bulkscan/new.blob?";

    @Override
    public Mono<HttpResponse> send(HttpRequest request) {
        String method = request.getHttpMethod().name();
        String path = request.getUrl().getPath();
        String query = Objects.toString(request.getUrl().getQuery(), "");
        Map<String, String> headers = new HashMap<>();

        if (path.contains("bogus")) {
            return Mono.just(
                new MockHttpResponse(request, 400)
            );
        }

        switch (method + " " + path + "?" + query) {
            case BLOB_PROPERTIES:
            case DOWNLOAD_BLOB:
                headers.put("ETag", "0x8D761B5AF5CA061");
                headers.put("Last-Modified", "Tue, 05 Nov 2019 06:02:05 GMT");
                headers.put("x-ms-creation-time", "Tue, 05 Nov 2019 06:02:05 GMT");

                return Mono.just(
                    new MockHttpResponse(request, 200, new HttpHeaders(headers))
                );
            case LEASE_BLOB:
                headers.put("ETag", "0x8D761B5AF5CA061");
                headers.put("Last-Modified", "Tue, 05 Nov 2019 06:02:05 GMT");
                headers.put("x-ms-client-request-id", request.getHeaders().getValue("x-ms-client-request-id"));

                boolean isAcquireLease = request
                    .getHeaders()
                    .getValue("x-ms-lease-action")
                    .equalsIgnoreCase("acquire");

                if (isAcquireLease) {
                    headers.put("x-ms-lease-id", request.getHeaders().getValue("x-ms-proposed-lease-id"));
                }

                return Mono.just(
                    new MockHttpResponse(request, isAcquireLease ? 201 : 200, new HttpHeaders(headers))
                );
            case LIST_CONTAINERS:
                return Mono.just(
                    new MockHttpResponse(request, 200, getFileContents("storage/list-containers.json"))
                );
            case LIST_ONE_BLOB:
                return Mono.just(
                    new MockHttpResponse(request, 200, getFileContents("storage/list-blobs.json"))
                );
            case LIST_ZERO_BLOBS:
                return Mono.just(
                    new MockHttpResponse(request, 200, getFileContents("storage/list-empty-blobs.json"))
                );
            case UPLOAD_NEW_BLOB:
                headers.put("ETag", "0x8D761B5AF5CA061");
                headers.put("Last-Modified", "Tue, 05 Nov 2019 06:02:05 GMT");
                headers.put("x-ms-request-server-encrypted", "false");
                headers.put("x-ms-encryption-key-sha256", "sha256");
                request.getHeaders().put("x-ms-encryption-key-sha256", "sha256");

                return Mono.just(
                    new MockHttpResponse(request, 201, new HttpHeaders(headers))
                );
            default:
                Assertions.fail("Request '" + method + " " + request.getUrl() + "' is not set up");
                return Mono.empty();
        }
    }
}
