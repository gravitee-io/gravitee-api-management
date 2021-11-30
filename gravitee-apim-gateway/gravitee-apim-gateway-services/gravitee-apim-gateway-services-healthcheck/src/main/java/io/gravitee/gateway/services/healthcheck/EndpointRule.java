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
package io.gravitee.gateway.services.healthcheck;

import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.services.healthcheck.Step;
import io.gravitee.definition.model.services.schedule.Trigger;
import io.gravitee.gateway.services.healthcheck.rule.EndpointRuleHandler;
import io.vertx.core.Vertx;
import io.vertx.core.net.ProxyOptions;
import org.springframework.core.env.Environment;

import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface EndpointRule<T extends Endpoint> {

    String api();

    T endpoint();

    Trigger trigger();

    List<Step> steps();

    ProxyOptions getSystemProxyOptions();

    EndpointRuleHandler<T> createRunner(Vertx vertx, EndpointRule<T> rule, Environment environment) throws Exception;
}
