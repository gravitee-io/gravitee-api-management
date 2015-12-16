/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.services.monitoring;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.Api;
import io.gravitee.gateway.core.reporter.ReporterService;
import io.gravitee.reporter.api.monitor.HealthStatus;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.net.SocketTimeoutException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class EndpointMonitor implements Runnable {

    private final static Logger LOGGER = LoggerFactory.getLogger(EndpointMonitor.class);

    private final static int GLOBAL_TIMEOUT = 2000;

    private ReporterService reporterService;

    private final HttpClient httpClient;

    private final Api api;

    public EndpointMonitor(Api api) {
        this.api = api;

        httpClient = createHttpClient();
    }

    @Override
    public void run() {
        LOGGER.debug("Running monitor for {}", api);

        HealthStatus health = HealthStatus
                .forApi(api.getId())
                .on(System.currentTimeMillis())
                .build();

        // Bullshit from Apache HTTP client
        HttpRequestBase request = (HttpRequestBase)
                RequestBuilder.get().setUri(api.getMonitoring().getEndpoint()).build();

        try {
            HttpResponse resp = httpClient.execute(request);
            LOGGER.debug("{} ::: {}", api.getMonitoring().getEndpoint(), resp.getStatusLine().getStatusCode());

            health.setStatus(resp.getStatusLine().getStatusCode());
        } catch (SocketTimeoutException ste) {
            health.setStatus(HttpStatusCode.REQUEST_TIMEOUT_408);
        } catch (Exception e) {
            e.printStackTrace();
            health.setStatus(HttpStatusCode.SERVICE_UNAVAILABLE_503);
        } finally {
            request.releaseConnection();
        }

        LOGGER.debug("Report health results for {}", api);
        reporterService.report(health);
    }

    private HttpClient createHttpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(GLOBAL_TIMEOUT)
                .setConnectTimeout(GLOBAL_TIMEOUT)
                .setConnectionRequestTimeout(GLOBAL_TIMEOUT)
                .build();

        HttpClientBuilder clientBuilder = HttpClients.custom();
        PoolingHttpClientConnectionManager clientConnectionManager = new PoolingHttpClientConnectionManager();

        try {
            // Try to initialize SSL connection using trustAll and clientAuth
            // Setup a Trust Strategy that allows all certificates.
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, (arg0, arg1) -> true).build();
            clientBuilder.setSslcontext(sslContext);

            // don't check Hostnames, either.
            //      -- use SSLConnectionSocketFactory.getDefaultHostnameVerifier(), if you don't want to weaken
            // here's the special part:
            //      -- need to create an SSL Socket Factory, to use our weakened "trust strategy";
            //      -- and create a Registry, to register it.
            //
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                    sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", sslSocketFactory)
                    .build();

            clientConnectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            e.printStackTrace();
        }

        clientConnectionManager.setMaxTotal(5);
        clientConnectionManager.setDefaultMaxPerRoute(5);

        clientBuilder
                .setConnectionManager(clientConnectionManager)
                .setDefaultRequestConfig(requestConfig);

        return clientBuilder.build();
    }

    public void setReporterService(ReporterService reporterService) {
        this.reporterService = reporterService;
    }
}
