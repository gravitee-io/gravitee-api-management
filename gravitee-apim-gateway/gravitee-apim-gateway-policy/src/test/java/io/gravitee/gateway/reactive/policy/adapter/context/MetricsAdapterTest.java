/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.reactive.policy.adapter.context;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.reporter.api.log.Log;
import io.gravitee.reporter.api.v4.metric.Metrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MetricsAdapterTest {

    @Mock
    private Metrics metricsV4;

    private MetricsAdapter metricsAdapter;

    @BeforeEach
    void beforeEach() {
        when(metricsV4.getTimestamp()).thenReturn(System.currentTimeMillis());
        metricsAdapter = new MetricsAdapter(metricsV4);
    }

    @Test
    void should_delegate_getTimestamp_to_metrics_v4() {
        // When
        metricsAdapter.getTimestamp();

        // Then
        verify(metricsV4, times(2)).getTimestamp();
    }

    @Test
    void should_delegate_timestamp_to_metrics_v4() {
        // When
        metricsAdapter.timestamp();

        // Then
        verify(metricsV4).timestamp();
    }

    @Test
    void should_delegate_getProxyResponseTimeMs_to_metrics_v4() {
        // When
        metricsAdapter.getProxyResponseTimeMs();

        // Then
        verify(metricsV4).getGatewayResponseTimeMs();
    }

    @Test
    void should_delegate_getProxyLatencyMs_to_metrics_v4() {
        // When
        metricsAdapter.getProxyLatencyMs();

        // Then
        verify(metricsV4).getGatewayLatencyMs();
    }

    @Test
    void should_delegate_getApiResponseTimeMs_to_metrics_v4() {
        // When
        metricsAdapter.getApiResponseTimeMs();

        // Then
        verify(metricsV4).getEndpointResponseTimeMs();
    }

    @Test
    void should_delegate_getRequestId_to_metrics_v4() {
        // When
        metricsAdapter.getRequestId();

        // Then
        verify(metricsV4).getRequestId();
    }

    @Test
    void should_delegate_getApi_to_metrics_v4() {
        // When
        metricsAdapter.getApi();

        // Then
        verify(metricsV4).getApiId();
    }

    @Test
    void should_delegate_getApiName_to_metrics_v4() {
        // When
        metricsAdapter.getApiName();

        // Then
        verify(metricsV4).getApiName();
    }

    @Test
    void should_delegate_getApplication_to_metrics_v4() {
        // When
        metricsAdapter.getApplication();

        // Then
        verify(metricsV4).getApplicationId();
    }

    @Test
    void should_delegate_getTransactionId_to_metrics_v4() {
        // When
        metricsAdapter.getTransactionId();

        // Then
        verify(metricsV4).getTransactionId();
    }

    @Test
    void should_delegate_getClientIdentifier_to_metrics_v4() {
        // When
        metricsAdapter.getClientIdentifier();

        // Then
        verify(metricsV4).getClientIdentifier();
    }

    @Test
    void should_delegate_getTenant_to_metrics_v4() {
        // When
        metricsAdapter.getTenant();

        // Then
        verify(metricsV4).getTenant();
    }

    @Test
    void should_delegate_getMessage_to_metrics_v4() {
        // When
        metricsAdapter.getMessage();

        // Then
        verify(metricsV4).getErrorMessage();
    }

    @Test
    void should_delegate_getPlan_to_metrics_v4() {
        // When
        metricsAdapter.getPlan();

        // Then
        verify(metricsV4).getPlanId();
    }

    @Test
    void should_delegate_getLocalAddress_to_metrics_v4() {
        // When
        metricsAdapter.getLocalAddress();

        // Then
        verify(metricsV4).getLocalAddress();
    }

    @Test
    void should_delegate_getRemoteAddress_to_metrics_v4() {
        // When
        metricsAdapter.getRemoteAddress();

        // Then
        verify(metricsV4).getRemoteAddress();
    }

    @Test
    void should_delegate_getHttpMethod_to_metrics_v4() {
        // When
        metricsAdapter.getHttpMethod();

        // Then
        verify(metricsV4).getHttpMethod();
    }

    @Test
    void should_delegate_getHost_to_metrics_v4() {
        // When
        metricsAdapter.getHost();

        // Then
        verify(metricsV4).getHost();
    }

    @Test
    void should_delegate_getUri_to_metrics_v4() {
        // When
        metricsAdapter.getUri();

        // Then
        verify(metricsV4).getUri();
    }

    @Test
    void should_delegate_getRequestContentLength_to_metrics_v4() {
        // When
        metricsAdapter.getRequestContentLength();

        // Then
        verify(metricsV4).getRequestContentLength();
    }

    @Test
    void should_delegate_getResponseContentLength_to_metrics_v4() {
        // When
        metricsAdapter.getResponseContentLength();

        // Then
        verify(metricsV4).getResponseContentLength();
    }

    @Test
    void should_delegate_getStatus_to_metrics_v4() {
        // When
        metricsAdapter.getStatus();

        // Then
        verify(metricsV4).getStatus();
    }

    @Test
    void should_delegate_getEndpoint_to_metrics_v4() {
        // When
        metricsAdapter.getEndpoint();

        // Then
        verify(metricsV4).getEndpoint();
    }

    @Test
    void should_delegate_getLog_to_metrics_v4() {
        // When
        metricsAdapter.getLog();

        // Then
        verify(metricsV4).getLog();
    }

    @Test
    void should_delegate_getPath_to_metrics_v4() {
        // When
        metricsAdapter.getPath();

        // Then
        verify(metricsV4).getPathInfo();
    }

    @Test
    void should_delegate_getMappedPath_to_metrics_v4() {
        // When
        metricsAdapter.getMappedPath();

        // Then
        verify(metricsV4).getMappedPath();
    }

    @Test
    void should_delegate_getUserAgent_to_metrics_v4() {
        // When
        metricsAdapter.getUserAgent();

        // Then
        verify(metricsV4).getUserAgent();
    }

    @Test
    void should_delegate_getUser_to_metrics_v4() {
        // When
        metricsAdapter.getUser();

        // Then
        verify(metricsV4).getUser();
    }

    @Test
    void should_delegate_getSecurityType_to_metrics_v4() {
        // When
        metricsAdapter.getSecurityType();

        // Then
        verify(metricsV4).getSecurityType();
    }

    @Test
    void should_delegate_getSecurityToken_to_metrics_v4() {
        // When
        metricsAdapter.getSecurityToken();

        // Then
        verify(metricsV4).getSecurityToken();
    }

    @Test
    void should_delegate_getErrorKey_to_metrics_v4() {
        // When
        metricsAdapter.getErrorKey();

        // Then
        verify(metricsV4).getErrorKey();
    }

    @Test
    void should_delegate_getSubscription_to_metrics_v4() {
        // When
        metricsAdapter.getSubscription();

        // Then
        verify(metricsV4).getSubscriptionId();
    }

    @Test
    void should_delegate_getZone_to_metrics_v4() {
        // When
        metricsAdapter.getZone();

        // Then
        verify(metricsV4).getZone();
    }

    @Test
    void should_delegate_getCustomMetrics_to_metrics_v4() {
        // When
        metricsAdapter.getCustomMetrics();

        // Then
        verify(metricsV4).getCustomMetrics();
    }

    @Test
    void should_delegate_setProxyResponseTimeMs_to_metrics_v4() {
        // When
        metricsAdapter.setProxyResponseTimeMs(-1);

        // Then
        verify(metricsV4).setGatewayResponseTimeMs(-1);
    }

    @Test
    void should_delegate_setProxyLatencyMs_to_metrics_v4() {
        // When
        metricsAdapter.setProxyLatencyMs(-1);

        // Then
        verify(metricsV4).setGatewayLatencyMs(-1);
    }

    @Test
    void should_delegate_setApiResponseTimeMs_to_metrics_v4() {
        // When
        metricsAdapter.setApiResponseTimeMs(-1);

        // Then
        verify(metricsV4).setEndpointResponseTimeMs(-1);
    }

    @Test
    void should_delegate_setRequestId_to_metrics_v4() {
        // When
        metricsAdapter.setRequestId(null);

        // Then
        verify(metricsV4).setRequestId(null);
    }

    @Test
    void should_delegate_setApi_to_metrics_v4() {
        // When
        metricsAdapter.setApi(null);

        // Then
        verify(metricsV4).setApiId(null);
    }

    @Test
    void should_delegate_setApiName_to_metrics_v4() {
        // When
        metricsAdapter.setApiName(null);

        // Then
        verify(metricsV4).setApiName(null);
    }

    @Test
    void should_delegate_setApplication_to_metrics_v4() {
        // When
        metricsAdapter.setApplication(null);

        // Then
        verify(metricsV4).setApplicationId(null);
    }

    @Test
    void should_delegate_setTransactionId_to_metrics_v4() {
        // When
        metricsAdapter.setTransactionId(null);

        // Then
        verify(metricsV4).setTransactionId(null);
    }

    @Test
    void should_delegate_setClientIdentifier_to_metrics_v4() {
        // When
        metricsAdapter.setClientIdentifier(null);

        // Then
        verify(metricsV4).setClientIdentifier(null);
    }

    @Test
    void should_delegate_setTenant_to_metrics_v4() {
        // When
        metricsAdapter.setTenant(null);

        // Then
        verify(metricsV4).setTenant(null);
    }

    @Test
    void should_delegate_setMessage_to_metrics_v4() {
        // When
        metricsAdapter.setMessage(null);

        // Then
        verify(metricsV4).setErrorMessage(null);
    }

    @Test
    void should_delegate_setPlan_to_metrics_v4() {
        // When
        metricsAdapter.setPlan(null);

        // Then
        verify(metricsV4).setPlanId(null);
    }

    @Test
    void should_delegate_setLocalAddress_to_metrics_v4() {
        // When
        metricsAdapter.setLocalAddress(null);

        // Then
        verify(metricsV4).setLocalAddress(null);
    }

    @Test
    void should_delegate_setRemoteAddress_to_metrics_v4() {
        // When
        metricsAdapter.setRemoteAddress(null);

        // Then
        verify(metricsV4).setRemoteAddress(null);
    }

    @Test
    void should_delegate_setHttpMethod_to_metrics_v4() {
        // When
        metricsAdapter.setHttpMethod(null);

        // Then
        verify(metricsV4).setHttpMethod(null);
    }

    @Test
    void should_delegate_setHost_to_metrics_v4() {
        // When
        metricsAdapter.setHost(null);

        // Then
        verify(metricsV4).setHost(null);
    }

    @Test
    void should_delegate_setUri_to_metrics_v4() {
        // When
        metricsAdapter.setUri(null);

        // Then
        verify(metricsV4).setUri(null);
    }

    @Test
    void should_delegate_setRequestContentLength_to_metrics_v4() {
        // When
        metricsAdapter.setRequestContentLength(-1);

        // Then
        verify(metricsV4).setRequestContentLength(-1);
    }

    @Test
    void should_delegate_setResponseContentLength_to_metrics_v4() {
        // When
        metricsAdapter.setResponseContentLength(-1);

        // Then
        verify(metricsV4).setResponseContentLength(-1);
    }

    @Test
    void should_delegate_setStatus_to_metrics_v4() {
        // When
        metricsAdapter.setStatus(-1);

        // Then
        verify(metricsV4).setStatus(-1);
    }

    @Test
    void should_delegate_setEndpoint_to_metrics_v4() {
        // When
        metricsAdapter.setEndpoint(null);

        // Then
        verify(metricsV4).setEndpoint(null);
    }

    @Test
    void should_delegate_setLog_to_metrics_v4() {
        // When
        metricsAdapter.setLog(null);

        // Then
        verify(metricsV4, never()).setLog(any());

        // When
        metricsAdapter.setLog(mock(Log.class));

        // Then
        verify(metricsV4).setLog(any());
    }

    @Test
    void should_delegate_setPath_to_metrics_v4() {
        // When
        metricsAdapter.setPath(null);

        // Then
        verify(metricsV4).setPathInfo(null);
    }

    @Test
    void should_delegate_setMappedPath_to_metrics_v4() {
        // When
        metricsAdapter.setMappedPath(null);

        // Then
        verify(metricsV4).setMappedPath(null);
    }

    @Test
    void should_delegate_setUserAgent_to_metrics_v4() {
        // When
        metricsAdapter.setUserAgent(null);

        // Then
        verify(metricsV4).setUserAgent(null);
    }

    @Test
    void should_delegate_setUser_to_metrics_v4() {
        // When
        metricsAdapter.setUser(null);

        // Then
        verify(metricsV4).setUser(null);
    }

    @Test
    void should_delegate_setSecurityType_to_metrics_v4() {
        // When
        metricsAdapter.setSecurityType(null);

        // Then
        verify(metricsV4).setSecurityType(null);
    }

    @Test
    void should_delegate_setSecurityToken_to_metrics_v4() {
        // When
        metricsAdapter.setSecurityToken(null);

        // Then
        verify(metricsV4).setSecurityToken(null);
    }

    @Test
    void should_delegate_setErrorKey_to_metrics_v4() {
        // When
        metricsAdapter.setErrorKey(null);

        // Then
        verify(metricsV4).setErrorKey(null);
    }

    @Test
    void should_delegate_setSubscription_to_metrics_v4() {
        // When
        metricsAdapter.setSubscription(null);

        // Then
        verify(metricsV4).setSubscriptionId(null);
    }

    @Test
    void should_delegate_setZone_to_metrics_v4() {
        // When
        metricsAdapter.setZone(null);

        // Then
        verify(metricsV4).setZone(null);
    }

    @Test
    void should_delegate_setCustomMetrics_to_metrics_v4() {
        // When
        metricsAdapter.setCustomMetrics(null);

        // Then
        verify(metricsV4).setCustomMetrics(null);
    }

    @Test
    void should_delegate_addCustomMetric_to_metrics_v4() {
        // When
        metricsAdapter.addCustomMetric(null, null);

        // Then
        verify(metricsV4).addCustomMetric(null, null);
    }
}
