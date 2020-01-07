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

    @Override
    public Mono<HttpResponse> send(HttpRequest request) {
        String path = request.getUrl().getPath();
        String query = request.getUrl().getQuery();

        if (LIST_CONTAINERS.equals(path + "?" + query)) {
            return Mono.just(
                new MockHttpResponse(request, 200, getFileContents("storage/list-containers.json"))
            );
        }
        Assertions.fail("Request '" + request.getUrl() + "' is not setup");

        return Mono.empty();
    }
}
