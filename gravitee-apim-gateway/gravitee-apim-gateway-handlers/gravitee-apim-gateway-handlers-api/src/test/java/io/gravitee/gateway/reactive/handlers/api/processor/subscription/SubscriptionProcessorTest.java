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
package io.gravitee.gateway.reactive.handlers.api.processor.subscription;

import static io.gravitee.gateway.api.ExecutionContext.ATTR_APPLICATION;
import static io.gravitee.gateway.api.ExecutionContext.ATTR_CLIENT_IDENTIFIER;
import static io.gravitee.gateway.api.ExecutionContext.ATTR_PLAN;
import static io.gravitee.gateway.api.ExecutionContext.ATTR_SUBSCRIPTION_ID;
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_SECURITY_SKIP;
import static io.gravitee.gateway.reactive.handlers.api.processor.subscription.SubscriptionProcessor.APPLICATION_ANONYMOUS;
import static io.gravitee.gateway.reactive.handlers.api.processor.subscription.SubscriptionProcessor.DEFAULT_CLIENT_IDENTIFIER_HEADER;
import static io.gravitee.gateway.reactive.handlers.api.processor.subscription.SubscriptionProcessor.PLAN_ANONYMOUS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import io.gravitee.common.util.MultiValueMap;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.http.vertx.VertxHttpHeaders;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.handlers.api.context.SubscriptionTemplateVariableProvider;
import io.gravitee.gateway.reactive.handlers.api.processor.AbstractProcessorTest;
import io.reactivex.rxjava3.observers.TestObserver;
import io.vertx.core.MultiMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubscriptionProcessorTest extends AbstractProcessorTest {

    protected static final String PLAN_ID = "planId";
    protected static final String APPLICATION_ID = "applicationId";
    protected static final String SUBSCRIPTION_ID = "subscriptionId";
    protected static final String TRANSACTION_ID = "transactionId";
    protected static final String REMOTE_ADDRESS = "remoteAddress";

    @Captor
    ArgumentCaptor<Collection<TemplateVariableProvider>> providersCaptor;

    private SubscriptionProcessor cut;
    private MultiValueMap<String, String> requestParams;

    @BeforeEach
    void initProcessor() {
        cut = SubscriptionProcessor.instance(null);
        spyCtx.setAttribute(ATTR_PLAN, PLAN_ID);
        spyCtx.setAttribute(ATTR_APPLICATION, APPLICATION_ID);
        spyCtx.setAttribute(ATTR_SUBSCRIPTION_ID, SUBSCRIPTION_ID);
        lenient().when(mockRequest.remoteAddress()).thenReturn(REMOTE_ADDRESS);
        lenient().when(mockRequest.transactionId()).thenReturn(TRANSACTION_ID);
        requestParams = new VertxHttpHeaders(MultiMap.caseInsensitiveMultiMap());
        lenient().when(mockRequest.parameters()).thenReturn(requestParams);
    }

    @Test
    void shouldReturnId() {
        assertThat(cut.getId()).isEqualTo("processor-subscription");
    }

    @Nested
    class HttpSecurityChain {

        @Test
        void should_set_metrics_when_security_chain_is_not_skipped() {
            spyCtx.setInternalAttribute(ATTR_INTERNAL_SECURITY_SKIP, null);
            final TestObserver<Void> obs = cut.execute(spyCtx).test();
            obs.assertResult();

            assertThat(spyCtx.metrics().getPlanId()).isEqualTo(PLAN_ID);
            assertThat(spyCtx.metrics().getApplicationId()).isEqualTo(APPLICATION_ID);
            assertThat(spyCtx.metrics().getSubscriptionId()).isEqualTo(SUBSCRIPTION_ID);
        }

        @Test
        void should_not_override_attribute_when_security_chain_is_skipped() {
            spyCtx.setInternalAttribute(ATTR_INTERNAL_SECURITY_SKIP, true);
            final TestObserver<Void> obs = cut.execute(spyCtx).test();
            obs.assertResult();

            assertThat(spyCtx.metrics().getPlanId()).isEqualTo(PLAN_ID);
            assertThat(spyCtx.metrics().getApplicationId()).isEqualTo(APPLICATION_ID);
            assertThat(spyCtx.metrics().getSubscriptionId()).isEqualTo(SUBSCRIPTION_ID);
        }

        @Test
        void should_set_unknown_attribute_when_security_chain_is_skipped() {
            spyCtx.setInternalAttribute(ATTR_INTERNAL_SECURITY_SKIP, true);
            spyCtx.setAttribute(ATTR_PLAN, null);
            spyCtx.setAttribute(ATTR_APPLICATION, null);
            spyCtx.setAttribute(ATTR_SUBSCRIPTION_ID, null);

            final TestObserver<Void> obs = cut.execute(spyCtx).test();
            obs.assertResult();
            assertThat(spyCtx.<String>getAttribute(ATTR_PLAN)).isEqualTo(PLAN_ANONYMOUS);
            assertThat(spyCtx.<String>getAttribute(ATTR_APPLICATION)).isEqualTo(APPLICATION_ANONYMOUS);
            assertThat(spyCtx.<String>getAttribute(ATTR_SUBSCRIPTION_ID)).isEqualTo(REMOTE_ADDRESS);

            assertThat(spyCtx.metrics().getPlanId()).isEqualTo(PLAN_ANONYMOUS);
            assertThat(spyCtx.metrics().getApplicationId()).isEqualTo(APPLICATION_ANONYMOUS);
            assertThat(spyCtx.metrics().getSubscriptionId()).isEqualTo(REMOTE_ADDRESS);
        }
    }

    @Nested
    class SubscriptionVariableProvider {

        @Test
        void should_add_subscription_variable_provider_with_ctx_subscription() {
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
        void should_add_subscription_variable_provider_with_new_subscription() {
            cut.execute(spyCtx).test().assertComplete();

            verify(spyCtx).templateVariableProviders(providersCaptor.capture());
            verify(spyCtx).setInternalAttribute(eq(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION), any());

            List<TemplateVariableProvider> providers = new ArrayList<>(providersCaptor.getValue());
            assertThat(providers).hasSize(1);
            TemplateVariableProvider templateVariableProvider = providers.get(0);
            assertThat(templateVariableProvider).isInstanceOf(SubscriptionTemplateVariableProvider.class);
        }
    }

    @Nested
    class ClientIdentifier {

        @Test
        void should_use_subscription_id_when_client_identifier_header_is_null_and_subscription_is_not_null() {
            cut.execute(spyCtx).test().assertComplete();

            assertThat(spyCtx.<String>getAttribute(ATTR_CLIENT_IDENTIFIER)).isEqualTo(SUBSCRIPTION_ID);
            assertThat(spyRequestHeaders.get(DEFAULT_CLIENT_IDENTIFIER_HEADER)).isNull();
            assertThat(spyResponseHeaders.get(DEFAULT_CLIENT_IDENTIFIER_HEADER)).isEqualTo(SUBSCRIPTION_ID);

            verify(mockRequest).clientIdentifier(SUBSCRIPTION_ID);
            assertThat(spyCtx.metrics().getClientIdentifier()).isEqualTo(SUBSCRIPTION_ID);
        }

        @Test
        void should_use_transaction_id_when_client_identifier_header_is_null_and_subscription_is_null() {
            spyCtx.setAttribute(ContextAttributes.ATTR_SUBSCRIPTION_ID, null);

            cut.execute(spyCtx).test().assertComplete();

            assertThat(spyCtx.<String>getAttribute(ATTR_CLIENT_IDENTIFIER)).isEqualTo(TRANSACTION_ID);
            assertThat(spyRequestHeaders.get(DEFAULT_CLIENT_IDENTIFIER_HEADER)).isNull();
            assertThat(spyResponseHeaders.get(DEFAULT_CLIENT_IDENTIFIER_HEADER)).isEqualTo(TRANSACTION_ID);

            verify(mockRequest).clientIdentifier(TRANSACTION_ID);
            assertThat(spyCtx.metrics().getClientIdentifier()).isEqualTo(TRANSACTION_ID);
        }

        @Test
        void should_use_hash_subscription_id_when_client_identifier_header_is_null_and_subscription_equals_remote_address() {
            spyCtx.setAttribute(ContextAttributes.ATTR_SUBSCRIPTION_ID, REMOTE_ADDRESS);

            cut.execute(spyCtx).test().assertComplete();

            assertThat(spyCtx.<String>getAttribute(ATTR_CLIENT_IDENTIFIER)).isNotNull();
            assertThat(spyCtx.<String>getAttribute(ATTR_CLIENT_IDENTIFIER)).doesNotContain(TRANSACTION_ID);
            assertThat(spyCtx.<String>getAttribute(ATTR_CLIENT_IDENTIFIER)).doesNotContain(REMOTE_ADDRESS);
            assertThat(spyCtx.<String>getAttribute(ATTR_CLIENT_IDENTIFIER)).doesNotContain(SUBSCRIPTION_ID);
            assertThat(spyRequestHeaders.get(DEFAULT_CLIENT_IDENTIFIER_HEADER)).isNull();
            assertThat(spyResponseHeaders.get(DEFAULT_CLIENT_IDENTIFIER_HEADER)).isNotNull();
            assertThat(spyResponseHeaders.get(DEFAULT_CLIENT_IDENTIFIER_HEADER)).doesNotContain(TRANSACTION_ID);
            assertThat(spyResponseHeaders.get(DEFAULT_CLIENT_IDENTIFIER_HEADER)).doesNotContain(REMOTE_ADDRESS);
            assertThat(spyResponseHeaders.get(DEFAULT_CLIENT_IDENTIFIER_HEADER)).doesNotContain(SUBSCRIPTION_ID);

            verify(mockRequest).clientIdentifier(AdditionalMatchers.not(eq(SUBSCRIPTION_ID)));
            verify(mockRequest).clientIdentifier(AdditionalMatchers.not(eq(TRANSACTION_ID)));
            verify(mockRequest).clientIdentifier(AdditionalMatchers.not(eq(REMOTE_ADDRESS)));
            assertThat(spyCtx.metrics().getClientIdentifier()).isNotNull();
            assertThat(spyCtx.metrics().getClientIdentifier()).doesNotContain(TRANSACTION_ID);
            assertThat(spyCtx.metrics().getClientIdentifier()).doesNotContain(REMOTE_ADDRESS);
            assertThat(spyCtx.metrics().getClientIdentifier()).doesNotContain(SUBSCRIPTION_ID);
        }

        @Test
        void should_use_client_identifier_header_when_suffix_by_subscription() {
            String clientIdentifier = "1234-" + SUBSCRIPTION_ID;
            spyRequestHeaders.set(DEFAULT_CLIENT_IDENTIFIER_HEADER, clientIdentifier);

            cut.execute(spyCtx).test().assertComplete();

            assertThat(spyCtx.<String>getAttribute(ATTR_CLIENT_IDENTIFIER)).isEqualTo(clientIdentifier);
            assertThat(spyRequestHeaders.get(DEFAULT_CLIENT_IDENTIFIER_HEADER)).isEqualTo(clientIdentifier);
            assertThat(spyResponseHeaders.get(DEFAULT_CLIENT_IDENTIFIER_HEADER)).isEqualTo(clientIdentifier);

            verify(mockRequest).clientIdentifier(clientIdentifier);
            assertThat(spyCtx.metrics().getClientIdentifier()).isEqualTo(clientIdentifier);
        }

        @Test
        void should_suffix_client_identifier_header_with_subscription_when_not_suffix_by_subscription() {
            String clientIdentifier = "1234";
            String ctxClientIdentifier = "1234-" + SUBSCRIPTION_ID;
            spyRequestHeaders.set(DEFAULT_CLIENT_IDENTIFIER_HEADER, clientIdentifier);

            cut.execute(spyCtx).test().assertComplete();

            assertThat(spyCtx.<String>getAttribute(ATTR_CLIENT_IDENTIFIER)).isEqualTo(ctxClientIdentifier);
            assertThat(spyRequestHeaders.get(DEFAULT_CLIENT_IDENTIFIER_HEADER)).isEqualTo(clientIdentifier);
            assertThat(spyResponseHeaders.get(DEFAULT_CLIENT_IDENTIFIER_HEADER)).isEqualTo(clientIdentifier);

            verify(mockRequest).clientIdentifier(ctxClientIdentifier);
            assertThat(spyCtx.metrics().getClientIdentifier()).isEqualTo(clientIdentifier);
        }

        @Test
        void should_use_client_identifier_header_when_suffix_by_transaction() {
            String clientIdentifier = "1234-" + TRANSACTION_ID;
            spyRequestHeaders.set(DEFAULT_CLIENT_IDENTIFIER_HEADER, clientIdentifier);

            cut.execute(spyCtx).test().assertComplete();

            assertThat(spyCtx.<String>getAttribute(ATTR_CLIENT_IDENTIFIER)).isEqualTo(clientIdentifier);
            assertThat(spyRequestHeaders.get(DEFAULT_CLIENT_IDENTIFIER_HEADER)).isEqualTo(clientIdentifier);
            assertThat(spyResponseHeaders.get(DEFAULT_CLIENT_IDENTIFIER_HEADER)).isEqualTo(clientIdentifier);

            verify(mockRequest).clientIdentifier(clientIdentifier);
            assertThat(spyCtx.metrics().getClientIdentifier()).isEqualTo(clientIdentifier);
        }

        @Test
        void should_suffix_client_identifier_header_with_hash_when_subscription_equals_remote_address() {
            spyCtx.setAttribute(ContextAttributes.ATTR_SUBSCRIPTION_ID, REMOTE_ADDRESS);
            String clientIdentifier = "1234";
            String startCtxClientIdentifier = "1234-";
            spyRequestHeaders.set(DEFAULT_CLIENT_IDENTIFIER_HEADER, clientIdentifier);

            cut.execute(spyCtx).test().assertComplete();

            assertThat(spyCtx.<String>getAttribute(ATTR_CLIENT_IDENTIFIER)).startsWith(startCtxClientIdentifier);
            assertThat(spyCtx.<String>getAttribute(ATTR_CLIENT_IDENTIFIER)).doesNotContain(SUBSCRIPTION_ID);
            assertThat(spyCtx.<String>getAttribute(ATTR_CLIENT_IDENTIFIER)).doesNotContain(TRANSACTION_ID);
            assertThat(spyCtx.<String>getAttribute(ATTR_CLIENT_IDENTIFIER)).doesNotContain(REMOTE_ADDRESS);
            assertThat(spyRequestHeaders.get(DEFAULT_CLIENT_IDENTIFIER_HEADER)).isEqualTo(clientIdentifier);
            assertThat(spyResponseHeaders.get(DEFAULT_CLIENT_IDENTIFIER_HEADER)).isEqualTo(clientIdentifier);

            verify(mockRequest).clientIdentifier(startsWith(startCtxClientIdentifier));
            verify(mockRequest).clientIdentifier(AdditionalMatchers.not(eq(SUBSCRIPTION_ID)));
            verify(mockRequest).clientIdentifier(AdditionalMatchers.not(eq(TRANSACTION_ID)));
            verify(mockRequest).clientIdentifier(AdditionalMatchers.not(eq(REMOTE_ADDRESS)));
            assertThat(spyCtx.metrics().getClientIdentifier()).isEqualTo(clientIdentifier);
        }

        @Test
        void should_suffix_client_identifier_param_with_subscription_when_not_suffix_by_subscription() {
            String clientIdentifier = "1234";
            String ctxClientIdentifier = "1234-" + SUBSCRIPTION_ID;
            requestParams.set(DEFAULT_CLIENT_IDENTIFIER_HEADER, clientIdentifier);

            cut.execute(spyCtx).test().assertComplete();

            assertThat(spyCtx.<String>getAttribute(ATTR_CLIENT_IDENTIFIER)).isEqualTo(ctxClientIdentifier);
            assertThat(spyRequestHeaders.get(DEFAULT_CLIENT_IDENTIFIER_HEADER)).isNull();
            assertThat(spyResponseHeaders.get(DEFAULT_CLIENT_IDENTIFIER_HEADER)).isEqualTo(clientIdentifier);

            verify(mockRequest).clientIdentifier(ctxClientIdentifier);
            assertThat(spyCtx.metrics().getClientIdentifier()).isEqualTo(clientIdentifier);
        }
    }
}
