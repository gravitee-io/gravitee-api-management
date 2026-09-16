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
package io.gravitee.gamma.rest.core.observability.filter.port.service_provider;

import java.util.List;

/**
 * Reads the observability entrypoint registry, which records what each entrypoint's traffic means for the
 * signals, for a core layer that cannot import it.
 *
 * <p>A port rather than a constant because the canonical declaration is shared with the analytics
 * engine and therefore lives in the platform's analytics-engine query package, which core must not
 * reach (AGENTS.md §5). The core decides <em>whether</em> a search is entrypoint-scoped; infra
 * says <em>which</em> ids that means.
 *
 * @author GraviteeSource Team
 */
public interface EntrypointScopeProvider {
    /**
     * The entrypoint ids an unfiltered logs search leaves out. The analytics default is built by the engine
     * itself; the logs one is built here because the platform logs query keeps its legacy predicate for the
     * Console and only Gamma asks for the exclusion.
     */
    List<String> excludedFromLogs();
}
