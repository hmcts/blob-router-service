package uk.gov.hmcts.reform.blobrouter.util;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpMethod;
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
    private static final String FILE_1 = "/bulkscan/file1.zip";
    private static final String FILE_3 = "/bulkscan/file3.zip";
    private static final String FILE_5 = "/bulkscan/causes_404.zip";
    private static final String FILE_6 = "/bulkscan/causes_500.zip";

    @Override
    public Mono<HttpResponse> send(HttpRequest request) {
        HttpMethod method = request.getHttpMethod();
        String path = request.getUrl().getPath();
        String query = request.getUrl().getQuery();

        if (query == null) {
            switch (path) {
                case FILE_1:
                    return Mono.just(
                        method == HttpMethod.DELETE
                            ?
                            new MockHttpResponse(request, 202) :
                            new MockHttpResponse(request, 200, getFileContents("storage/file1.json"))
                    );
                case FILE_3:
                    return Mono.just(
                        method == HttpMethod.DELETE
                            ?
                            new MockHttpResponse(request, 202) :
                            new MockHttpResponse(request, 200, getFileContents("storage/file3.json"))
                    );
                case FILE_5:
                    return Mono.just(
                            new MockHttpResponse(request, 404)
                    );
                case FILE_6:
                    return Mono.just(
                            new MockHttpResponse(request, 500)
                    );
                default:
                    Assertions.fail("Request '" + request.getUrl() + "' is not set up");
                    return Mono.empty();
            }
        } else {
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
                    Assertions.fail("Request '" + request.getUrl() + "' is not set up");
                    return Mono.empty();
            }
        }
    }
}
