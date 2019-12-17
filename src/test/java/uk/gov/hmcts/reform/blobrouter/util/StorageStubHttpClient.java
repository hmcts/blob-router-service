package uk.gov.hmcts.reform.blobrouter.util;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.test.http.MockHttpResponse;
import org.junit.jupiter.api.Assertions;
import reactor.core.publisher.Mono;

import static uk.gov.hmcts.reform.blobrouter.util.ResourceFilesHelper.getFileContents;

class StorageStubHttpClient implements HttpClient {

    private static final String LIST_CONTAINERS = "?comp=list";
    private static final String LIST_ONE_BLOB = "/bulkscan?include=&restype=container&comp=list";
    private static final String LIST_ZERO_BLOBS = "/empty?include=&restype=container&comp=list";

    @Override
    public Mono<HttpResponse> send(HttpRequest request) {
        String path = request.getUrl().getPath();
        String query = request.getUrl().getQuery();

        switch (path + "?" + query) {
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
            default:
                Assertions.fail("Request '" + request.getUrl() + "' is not setup");

                return Mono.empty();
        }
    }
}
