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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.authz;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NoopAuthzEnginePortTest {

    private final NoopAuthzEnginePort port = new NoopAuthzEnginePort();

    @Test
    void every_op_completes_without_error_so_deployer_chains_run_silently_when_gamma_disabled() {
        port.addOrUpdateEntity("Resource::\"api.x\"", Map.of(), List.of()).test().assertComplete();
        port.removeEntity("Resource::\"api.x\"").test().assertComplete();
        port.addOrUpdatePolicy("doc-1", "n", "permit(...);").test().assertComplete();
        port.removePolicy("doc-1").test().assertComplete();
        port.commit().test().assertComplete();
    }
}
