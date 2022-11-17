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
package io.gravitee.gateway.jupiter.handlers.api.processor.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.jupiter.api.context.InternalContextAttributes;
import io.gravitee.gateway.jupiter.core.context.MutableExecutionContext;
import io.gravitee.gateway.jupiter.handlers.api.context.SubscriptionTemplateVariableProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionProcessorTest {

    @Captor
    ArgumentCaptor<Collection<TemplateVariableProvider>> providersCaptor;

    @Mock
    private MutableExecutionContext ctx;

    private SubscriptionProcessor cut;

    @BeforeEach
    void initProcessor() {
        cut = SubscriptionProcessor.instance();
    }

    @Test
    void shouldReturnId() {
        assertThat(cut.getId()).isEqualTo("processor-subscription");
    }

    @Test
    void shouldAddSubscriptionVariableProviderWithCtxSubscription() {
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION)).thenReturn(new Subscription());

        cut.execute(ctx).test().assertComplete();

        verify(ctx).templateVariableProviders(providersCaptor.capture());
        verify(ctx, times(0)).setInternalAttribute(eq(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION), any());

        List<TemplateVariableProvider> providers = new ArrayList<>(providersCaptor.getValue());
        assertThat(providers).hasSize(1);
        TemplateVariableProvider templateVariableProvider = providers.get(0);
        assertThat(templateVariableProvider).isInstanceOf(SubscriptionTemplateVariableProvider.class);
    }

    @Test
    void shouldAddSubscriptionVariableProviderWithNewSubscription() {
        cut.execute(ctx).test().assertComplete();

        verify(ctx).templateVariableProviders(providersCaptor.capture());
        verify(ctx).setInternalAttribute(eq(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION), any());

        List<TemplateVariableProvider> providers = new ArrayList<>(providersCaptor.getValue());
        assertThat(providers).hasSize(1);
        TemplateVariableProvider templateVariableProvider = providers.get(0);
        assertThat(templateVariableProvider).isInstanceOf(SubscriptionTemplateVariableProvider.class);
    }
}
