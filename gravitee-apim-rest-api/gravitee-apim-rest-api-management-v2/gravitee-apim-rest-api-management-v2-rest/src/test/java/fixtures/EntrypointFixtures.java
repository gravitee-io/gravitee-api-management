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
package fixtures;

import io.gravitee.rest.api.management.v2.rest.model.Dlq;
import io.gravitee.rest.api.management.v2.rest.model.Entrypoint;
import io.gravitee.rest.api.management.v2.rest.model.Qos;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("ALL")
public class EntrypointFixtures {

    private EntrypointFixtures() {}

    private static final Entrypoint.EntrypointBuilder BASE_ENTRYPOINT_V4 = Entrypoint.builder()
        .type("Entrypoint type")
        .qos(Qos.AT_LEAST_ONCE)
        .dlq(new Dlq().endpoint("my-endpoint"))
        .configuration(new LinkedHashMap<>(Map.of("nice", "configuration")));

    public static Entrypoint anEntrypointV4() {
        return BASE_ENTRYPOINT_V4.build();
    }

    public static io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint aModelEntrypointV4() {
        return EntrypointModelFixtures.aModelEntrypointV4();
    }
}
