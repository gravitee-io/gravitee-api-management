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
package io.gravitee.apim.core.observability.model;

import java.util.Set;

/**
 * Observability signal kind. A {@link io.gravitee.apim.core.analytics_engine.model.FilterSpec} declares
 * which signals it applies to, so that each consumer surface (the logs table, the analytics dashboards)
 * only ever advertises filters it can actually honour.
 *
 * <p>Before this axis existed, {@code GET /observability/filters/definition} returned the whole analytics
 * catalog to every consumer. The logs screen therefore offered filters its search engine cannot translate,
 * and dropped them silently — see APIM-14817.
 *
 * <p>The vocabulary mirrors {@code io.gravitee.gamma.rest.core.observability.filter.model.Signal} so both
 * observability backends narrow their catalog along the same axis. {@code TRACES} is declared for that
 * parity but no filter carries it yet: the trace explorer keeps its own separate registry.
 *
 * @author GraviteeSource Team
 */
public enum Signal {
    LOGS,
    ANALYTICS,
    TRACES;

    /**
     * Applied to any filter that does not declare {@code signals} in the definition file. Analytics is the
     * historical scope of that catalog, so defaulting there keeps an unannotated entry exactly where it was
     * and makes every new {@code LOGS} exposure an explicit, reviewable opt-in.
     */
    public static final Set<Signal> DEFAULT = Set.of(ANALYTICS);
}
