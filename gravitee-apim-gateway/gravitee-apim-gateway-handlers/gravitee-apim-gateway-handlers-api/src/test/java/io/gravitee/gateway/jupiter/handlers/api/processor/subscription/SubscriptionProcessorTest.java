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
package io.gravitee.gateway.jupiter.handlers.api.processor.subscription;

import static io.gravitee.gateway.api.ExecutionContext.ATTR_APPLICATION;
import static io.gravitee.gateway.api.ExecutionContext.ATTR_CLIENT_IDENTIFIER;
import static io.gravitee.gateway.api.ExecutionContext.ATTR_PLAN;
import static io.gravitee.gateway.api.ExecutionContext.ATTR_SUBSCRIPTION_ID;
import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_SECURITY_SKIP;
import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION;
import static io.gravitee.gateway.jupiter.handlers.api.processor.subscription.SubscriptionProcessor.APPLICATION_ANONYMOUS;
import static io.gravitee.gateway.jupiter.handlers.api.processor.subscription.SubscriptionProcessor.DEFAULT_CLIENT_IDENTIFIER_HEADER;
import static io.gravitee.gateway.jupiter.handlers.api.processor.subscription.SubscriptionProcessor.PLAN_ANONYMOUS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.jupiter.api.context.ContextAttributes;
import io.gravitee.gateway.jupiter.api.context.InternalContextAttributes;
import io.gravitee.gateway.jupiter.handlers.api.context.SubscriptionTemplateVariableProvider;
import io.gravitee.gateway.jupiter.handlers.api.processor.AbstractProcessorTest;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionProcessorTest extends AbstractProcessorTest {

    protected static final String PLAN_ID = "planId";
    protected static final String APPLICATION_ID = "applicationId";
    protected static final String SUBSCRIPTION_ID = "subscriptionId";
    protected static final String REMOTE_ADDRESS = "remoteAddress";

    @Captor
    ArgumentCaptor<Collection<TemplateVariableProvider>> providersCaptor;

    private SubscriptionProcessor cut;

    @BeforeEach
    void initProcessor() {
        cut = SubscriptionProcessor.instance(null);
    }

    @Test
    void shouldReturnId() {
        assertThat(cut.getId()).isEqualTo("processor-subscription");
    }

    @Test
    void shouldSetMetricsWhenSecurityChainIsNotSkipped() {
        spyCtx.setInternalAttribute(ATTR_INTERNAL_SECURITY_SKIP, null);
        spyCtx.setAttribute(ATTR_PLAN, PLAN_ID);
        spyCtx.setAttribute(ATTR_APPLICATION, APPLICATION_ID);
        spyCtx.setAttribute(ATTR_SUBSCRIPTION_ID, SUBSCRIPTION_ID);

        final TestObserver<Void> obs = cut.execute(spyCtx).test();
        obs.assertResult();

        verify(mockMetrics).setPlan(PLAN_ID);
        verify(mockMetrics).setApplication(APPLICATION_ID);
        verify(mockMetrics).setSubscription(SUBSCRIPTION_ID);
    }

    @Test
    void shouldNotOverrideAttributeWhenSecurityChainIsSkipped() {
        spyCtx.setInternalAttribute(ATTR_INTERNAL_SECURITY_SKIP, true);
        spyCtx.setAttribute(ATTR_PLAN, PLAN_ID);
        spyCtx.setAttribute(ATTR_APPLICATION, APPLICATION_ID);
        spyCtx.setAttribute(ATTR_SUBSCRIPTION_ID, SUBSCRIPTION_ID);

        final TestObserver<Void> obs = cut.execute(spyCtx).test();
        obs.assertResult();

        verify(mockMetrics).setPlan(PLAN_ID);
        verify(mockMetrics).setApplication(APPLICATION_ID);
        verify(mockMetrics).setSubscription(SUBSCRIPTION_ID);
    }

    @Test
    void shouldSetUnknownAttributeWhenSecurityChainIsSkipped() {
        String remoteAddress = "remoteAddress";
        when(mockRequest.remoteAddress()).thenReturn(remoteAddress);
        spyCtx.setInternalAttribute(ATTR_INTERNAL_SECURITY_SKIP, true);

        final TestObserver<Void> obs = cut.execute(spyCtx).test();
        obs.assertResult();
        assertThat(spyCtx.<String>getAttribute(ATTR_PLAN)).isEqualTo(PLAN_ANONYMOUS);
        assertThat(spyCtx.<String>getAttribute(ATTR_APPLICATION)).isEqualTo(APPLICATION_ANONYMOUS);
        assertThat(spyCtx.<String>getAttribute(ATTR_SUBSCRIPTION_ID)).isEqualTo(remoteAddress);

        verify(mockMetrics).setPlan(PLAN_ANONYMOUS);
        verify(mockMetrics).setApplication(APPLICATION_ANONYMOUS);
        verify(mockMetrics).setSubscription(remoteAddress);
    }

    @Test
    void shouldAddSubscriptionVariableProviderWithCtxSubscription() {
        spyCtx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION, new Subscription());

        cut.execute(spyCtx).test().assertComplete();

        verify(spyCtx).templateVariableProviders(providersCaptor.capture());
        verify(spyCtx).setInternalAttribute(eq(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION), any());

        List<TemplateVariableProvider> providers = new ArrayList<>(providersCaptor.getValue());
        assertThat(providers).hasSize(1);
        TemplateVariableProvider templateVariableProvider = providers.get(0);
        assertThat(templateVariableProvider).isInstanceOf(SubscriptionTemplateVariableProvider.class);
    }

    @Test
    void shouldAddSubscriptionVariableProviderWithNewSubscription() {
        cut.execute(spyCtx).test().assertComplete();

        verify(spyCtx).templateVariableProviders(providersCaptor.capture());
        verify(spyCtx).setInternalAttribute(eq(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION), any());

        List<TemplateVariableProvider> providers = new ArrayList<>(providersCaptor.getValue());
        assertThat(providers).hasSize(1);
        TemplateVariableProvider templateVariableProvider = providers.get(0);
        assertThat(templateVariableProvider).isInstanceOf(SubscriptionTemplateVariableProvider.class);
    }

    @Test
    void shouldUseSubscriptionIdWhenClientIdentifierHeaderIsNullAndSubscriptionNotNull() {
        String subscriptionId = "1234";
        spyCtx.setAttribute(ContextAttributes.ATTR_SUBSCRIPTION_ID, subscriptionId);

        cut.execute(spyCtx).test().assertComplete();

        assertThat(spyCtx.<String>getAttribute(ATTR_CLIENT_IDENTIFIER)).isEqualTo(subscriptionId);
        assertThat(spyRequestHeaders.get(DEFAULT_CLIENT_IDENTIFIER_HEADER)).isEqualTo(subscriptionId);
        assertThat(spyResponseHeaders.get(DEFAULT_CLIENT_IDENTIFIER_HEADER)).isEqualTo(subscriptionId);

        verify(mockRequest).clientIdentifier(subscriptionId);
        verify(mockMetrics).setClientIdentifier(subscriptionId);
    }

    @Test
    void shouldUseTransactionIdWhenClientIdentifierHeaderIsNullAndSubscriptionNotNull() {
        String transactionId = "1234";
        when(mockRequest.transactionId()).thenReturn(transactionId);

        cut.execute(spyCtx).test().assertComplete();

        assertThat(spyCtx.<String>getAttribute(ATTR_CLIENT_IDENTIFIER)).isEqualTo(transactionId);
        assertThat(spyRequestHeaders.get(DEFAULT_CLIENT_IDENTIFIER_HEADER)).isEqualTo(transactionId);
        assertThat(spyResponseHeaders.get(DEFAULT_CLIENT_IDENTIFIER_HEADER)).isEqualTo(transactionId);

        verify(mockRequest).clientIdentifier(transactionId);
        verify(mockMetrics).setClientIdentifier(transactionId);
    }

    @Test
    void shouldUseTransactionIdWhenClientIdentifierHeaderIsNullAndSubscriptionEqualsRemoteAddress() {
        String transactionId = "1234";
        when(mockRequest.transactionId()).thenReturn(transactionId);
        String remoteAddress = "remoteAddress";
        when(mockRequest.remoteAddress()).thenReturn(remoteAddress);
        spyCtx.setAttribute(ContextAttributes.ATTR_SUBSCRIPTION_ID, remoteAddress);

        cut.execute(spyCtx).test().assertComplete();

        assertThat(spyCtx.<String>getAttribute(ATTR_CLIENT_IDENTIFIER)).isEqualTo(transactionId);
        assertThat(spyRequestHeaders.get(DEFAULT_CLIENT_IDENTIFIER_HEADER)).isEqualTo(transactionId);
        assertThat(spyResponseHeaders.get(DEFAULT_CLIENT_IDENTIFIER_HEADER)).isEqualTo(transactionId);

        verify(mockRequest).clientIdentifier(transactionId);
        verify(mockMetrics).setClientIdentifier(transactionId);
    }

    @Test
    void shouldUseClientIdentifierHeader() {
        String clientIdentifier = "1234";
        spyRequestHeaders.set(DEFAULT_CLIENT_IDENTIFIER_HEADER, clientIdentifier);

        cut.execute(spyCtx).test().assertComplete();

        assertThat(spyCtx.<String>getAttribute(ATTR_CLIENT_IDENTIFIER)).isEqualTo(clientIdentifier);
        assertThat(spyRequestHeaders.get(DEFAULT_CLIENT_IDENTIFIER_HEADER)).isEqualTo(clientIdentifier);
        assertThat(spyResponseHeaders.get(DEFAULT_CLIENT_IDENTIFIER_HEADER)).isEqualTo(clientIdentifier);

        verify(mockRequest).clientIdentifier(clientIdentifier);
        verify(mockMetrics).setClientIdentifier(clientIdentifier);
    }
}
