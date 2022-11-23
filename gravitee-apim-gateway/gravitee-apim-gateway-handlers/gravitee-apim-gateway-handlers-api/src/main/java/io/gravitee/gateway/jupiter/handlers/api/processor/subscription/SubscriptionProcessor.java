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

import static io.gravitee.gateway.api.ExecutionContext.ATTR_API;
import static io.gravitee.gateway.api.ExecutionContext.ATTR_APPLICATION;
import static io.gravitee.gateway.api.ExecutionContext.ATTR_PLAN;
import static io.gravitee.gateway.api.ExecutionContext.ATTR_SUBSCRIPTION_ID;
import static io.gravitee.repository.management.model.Subscription.Status.ACCEPTED;

import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.jupiter.api.context.InternalContextAttributes;
import io.gravitee.gateway.jupiter.core.context.MutableExecutionContext;
import io.gravitee.gateway.jupiter.core.processor.Processor;
import io.gravitee.gateway.jupiter.handlers.api.context.SubscriptionTemplateVariableProvider;
import io.reactivex.rxjava3.core.Completable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * This processor will add template variable provide for the resolved {@link Subscription} if any.
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionProcessor implements Processor {

    public static final String ID = "processor-subscription";

    public static SubscriptionProcessor instance() {
        return SubscriptionProcessor.Holder.INSTANCE;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Completable execute(final MutableExecutionContext ctx) {
        return Completable.fromRunnable(
            () -> {
                Subscription subscription = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION);
                if (subscription == null) {
                    subscription = new Subscription();
                    subscription.setId(ctx.getAttribute(ATTR_SUBSCRIPTION_ID));
                    subscription.setApi(ctx.getAttribute(ATTR_API));
                    subscription.setPlan(ctx.getAttribute(ATTR_PLAN));
                    subscription.setApplication(ctx.getAttribute(ATTR_APPLICATION));
                    subscription.setStatus(ACCEPTED.name());
                    ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION, subscription);
                }
                Collection<TemplateVariableProvider> templateVariableProviders = ctx.templateVariableProviders();
                if (templateVariableProviders == null) {
                    templateVariableProviders = new ArrayList<>();
                }
                templateVariableProviders.add(new SubscriptionTemplateVariableProvider(subscription));
                ctx.templateVariableProviders(templateVariableProviders);
            }
        );
    }

    private static class Holder {

        private static final SubscriptionProcessor INSTANCE = new SubscriptionProcessor();
    }
}
