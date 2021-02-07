package uk.gov.hmcts.reform.blobrouter.config;

import com.azure.core.http.HttpClient;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import feign.Client;
import feign.httpclient.ApacheHttpClient;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HttpConfiguration {

    @Bean
    public Client getFeignHttpClient() {
        return new ApacheHttpClient(getHttpClient());
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate(clientHttpRequestFactory());
    }

    @Bean
    public HttpComponentsClientHttpRequestFactory clientHttpRequestFactory() {
        return new HttpComponentsClientHttpRequestFactory(getHttpClient());
    }

    @Bean
    public HttpClient azureHttpClient() {
        // Creates a reactor-netty client with netty logging enabled.
        reactor.netty.http.client.HttpClient baseHttpClient = reactor.netty.http.client.HttpClient
            .create()
            .tcpConfiguration(
                tcp -> tcp.bootstrap(b -> b.handler(new LoggingHandler(LogLevel.DEBUG))));
        // Create an HttpClient based on above reactor-netty client and configure EventLoop count.
        HttpClient client = new NettyAsyncHttpClientBuilder(baseHttpClient)
            .eventLoopGroup(new NioEventLoopGroup(15))
            .disableBufferCopy(true)
            .build();
        return client;
    }

    private CloseableHttpClient getHttpClient() {
        RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(30000)
            .setConnectionRequestTimeout(30000)
            .setSocketTimeout(60000)
            .build();

        return HttpClientBuilder
            .create()
            .useSystemProperties()
            .setDefaultRequestConfig(config)
            .build();
    }
}
