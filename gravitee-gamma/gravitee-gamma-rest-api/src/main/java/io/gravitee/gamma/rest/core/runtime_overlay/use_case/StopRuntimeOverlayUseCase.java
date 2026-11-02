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
package io.gravitee.gamma.rest.core.runtime_overlay.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.gamma.rest.core.runtime_overlay.port.service_provider.RuntimeOverlayEventPort;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/** Ends a runtime overlay before its end: a new event for the same id, with status STOPPED. */
@UseCase
@RequiredArgsConstructor
public class StopRuntimeOverlayUseCase {

    private final RuntimeOverlayEventPort runtimeOverlayEventPort;

    public record Input(String organizationId, String environmentId, String userId, String apiId, String runtimeOverlayId) {}

    public record Output(String eventId) {}

    public Output execute(final Input input) {
        final String eventId = runtimeOverlayEventPort.publish(
            input.organizationId(),
            input.environmentId(),
            input.apiId(),
            input.userId(),
            input.runtimeOverlayId(),
            RuntimeOverlayEventPort.ACTION_REMOVE,
            Map.of("id", input.runtimeOverlayId(), "apiId", input.apiId(), "status", "STOPPED")
        );
        return new Output(eventId);
    }
}
