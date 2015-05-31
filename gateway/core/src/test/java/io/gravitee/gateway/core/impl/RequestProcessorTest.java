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
package io.gravitee.gateway.core.impl;

import io.gravitee.gateway.core.AsyncHandler;
import io.gravitee.gateway.http.Request;
import io.gravitee.gateway.http.Response;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.HttpCookieStore;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class RequestProcessorTest {

    @Test
    public void testClient() throws Exception {
        HttpClient client = createHttpClient();

        org.eclipse.jetty.client.api.Request req = client
                .newRequest("http://yrfrlmasbam.corp.leroymerlin.com/api-product/v1/products/69135185?storeId=142&webmetadata=true")
                .method(HttpMethod.GET)
                .version(HttpVersion.HTTP_1_1);

        req.send(new org.eclipse.jetty.client.api.Response.CompleteListener() {
            @Override
            public void onComplete(Result result) {
                System.out.println("onComplete : " + result.getResponse());
            }
        });

        req.timeout(5000, TimeUnit.MILLISECONDS);
        /*
         client.newRequest("clubic.com").method(HttpMethod.GET)
         .send(new org.eclipse.jetty.client.api.Response.CompleteListener() {
         @Override
         public void onComplete(Result result) {
         System.out.println("Send request");
         System.out.println("Status : " + result);
         }
         });

         */

        Thread.sleep(5000);
    }

    protected HttpClient createHttpClient() throws Exception {
        HttpClient client = new HttpClient();

        // Redirects must be proxied as is, not followed
        client.setFollowRedirects(false);

        // Must not store cookies, otherwise cookies of different clients will mix
        client.setCookieStore(new HttpCookieStore.Empty());

        QueuedThreadPool qtp = new QueuedThreadPool(200);

        qtp.setName("dispatcher-test");

        client.setExecutor(qtp);
        client.setIdleTimeout(30000);
        client.setRequestBufferSize(16384);
        client.setResponseBufferSize(163840);
        client.setMaxConnectionsPerDestination(256);
        client.setMaxRequestsQueuedPerDestination(1024);

        client.start();

        // Content must not be decoded, otherwise the client gets confused
        client.getContentDecoderFactories().clear();

        return client;
    }

    protected class ProxyResponseListener extends org.eclipse.jetty.client.api.Response.Listener.Adapter {

        @Override
        public void onComplete(Result result) {
            super.onComplete(result);

            System.out.println("onComplete : " + result.getResponse());
        }

        @Override
        public void onBegin(org.eclipse.jetty.client.api.Response response) {
            super.onBegin(response);

            System.out.println("onBegin : " + response.getStatus());
        }

        @Override
        public boolean onHeader(org.eclipse.jetty.client.api.Response response, HttpField field) {
            System.out.println("onHeader : " + response.getStatus());

            return super.onHeader(response, field);
        }

        @Override
        public void onHeaders(org.eclipse.jetty.client.api.Response response) {
            super.onHeaders(response);

            System.out.println("onHeader : " + response.getStatus());
        }

        @Override
        public void onContent(org.eclipse.jetty.client.api.Response response, ByteBuffer content) {
            super.onContent(response, content);

            System.out.println("onContent : " + response.getStatus());
        }

        @Override
        public void onContent(org.eclipse.jetty.client.api.Response response, ByteBuffer content, Callback callback) {
            super.onContent(response, content, callback);

            System.out.println("onContent : " + response.getStatus());
        }

        @Override
        public void onSuccess(org.eclipse.jetty.client.api.Response response) {
            super.onSuccess(response);

            System.out.println("onSuccess : " + response.getStatus());
        }

        @Override
        public void onFailure(org.eclipse.jetty.client.api.Response response, Throwable failure) {
            super.onFailure(response, failure);

            System.out.println("onFailure : " + response.getStatus());
        }
    }
}
