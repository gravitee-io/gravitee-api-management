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

public class EntrypointModelFixtures {

    private EntrypointModelFixtures() {}

    private static final io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint.EntrypointBuilder BASE_MODEL_ENTRYPOINT_V4 =
        io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint.builder()
            .type("Entrypoint type")
            .qos(io.gravitee.definition.model.v4.listener.entrypoint.Qos.AT_LEAST_ONCE)
            .dlq(new io.gravitee.definition.model.v4.listener.entrypoint.Dlq("my-endpoint"))
            .configuration("{\"nice\": \"configuration\"}");

    public static io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint aModelEntrypointV4() {
        return BASE_MODEL_ENTRYPOINT_V4.build();
    }
}
