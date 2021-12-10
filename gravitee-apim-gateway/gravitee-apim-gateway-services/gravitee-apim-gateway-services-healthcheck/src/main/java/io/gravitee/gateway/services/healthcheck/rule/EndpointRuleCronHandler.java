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
package io.gravitee.gateway.services.healthcheck.rule;

import io.gravitee.definition.model.Endpoint;
import io.gravitee.gateway.services.healthcheck.EndpointRule;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.io.Serializable;
import java.util.Date;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointRuleCronHandler implements Handler<Long>, Serializable {

    private final transient Vertx vertx;
    private final CronTrigger expression;
    private final Endpoint endpoint;
    private transient EndpointRuleHandler handler;
    private long timerId;

    public EndpointRuleCronHandler(Vertx vertx, EndpointRule rule) {
        this.vertx = vertx;
        this.expression = new CronTrigger(rule.schedule());
        this.endpoint = rule.endpoint();
    }

    public EndpointRuleCronHandler schedule(EndpointRuleHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Handler is null.");
        }
        this.handler = handler;
        final long delay = getDelay(expression);
        timerId = vertx.setTimer(delay, this);
        return this;
    }

    private long getDelay(CronTrigger expression) {
        final Date next = expression.nextExecutionTime(new SimpleTriggerContext());
        return next.getTime() - new Date().getTime();
    }

    @Override
    public void handle(final Long timerId) {
        final long delay = getDelay(expression);
        this.timerId = vertx.setTimer(delay, this);
        handler.handle(timerId);
    }

    public void cancel() {
        vertx.cancelTimer(timerId);
        handler.close();
    }

    public long getTimerId() {
        return timerId;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }
}
