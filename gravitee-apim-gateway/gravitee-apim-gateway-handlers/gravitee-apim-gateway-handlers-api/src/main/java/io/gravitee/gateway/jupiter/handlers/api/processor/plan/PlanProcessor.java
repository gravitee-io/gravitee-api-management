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
package io.gravitee.gateway.jupiter.handlers.api.processor.plan;

import static io.gravitee.gateway.api.ExecutionContext.*;

import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.jupiter.core.processor.Processor;
import io.gravitee.gateway.jupiter.handlers.api.security.SecurityChain;
import io.gravitee.reporter.api.http.Metrics;
import io.reactivex.Completable;
import java.util.Objects;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PlanProcessor implements Processor {

    static final String APPLICATION_NAME_ANONYMOUS = "1";
    static final String PLAN_NAME_ANONYMOUS = "1";
    public static final String ID = "processor-plan";

    private PlanProcessor() {}

    public static PlanProcessor instance() {
        return Holder.INSTANCE;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Completable execute(RequestExecutionContext ctx) {
        return Completable.fromRunnable(
            () -> {
                final Metrics metrics = ctx.request().metrics();

                if (Objects.equals(true, ctx.getAttribute(SecurityChain.SKIP_SECURITY_CHAIN))) {
                    final String remoteAddress = ctx.request().remoteAddress();

                    // Fixes consuming application and subscription which are data that can be used by policies (ie. rate-limit).
                    ctx.setAttribute(ATTR_APPLICATION, APPLICATION_NAME_ANONYMOUS);
                    ctx.setAttribute(ATTR_PLAN, PLAN_NAME_ANONYMOUS);
                    ctx.setAttribute(ATTR_SUBSCRIPTION_ID, remoteAddress);

                    metrics.setApplication(APPLICATION_NAME_ANONYMOUS);
                    metrics.setPlan(PLAN_NAME_ANONYMOUS);
                    metrics.setSubscription(remoteAddress);
                } else {
                    // Stores information about the resolved plan (according to the incoming request)
                    metrics.setPlan(ctx.getAttribute(ATTR_PLAN));
                    metrics.setApplication(ctx.getAttribute(ATTR_APPLICATION));
                    metrics.setSubscription(ctx.getAttribute(ATTR_SUBSCRIPTION_ID));
                }
            }
        );
    }

    private static class Holder {

        private static final PlanProcessor INSTANCE = new PlanProcessor();
    }
}
