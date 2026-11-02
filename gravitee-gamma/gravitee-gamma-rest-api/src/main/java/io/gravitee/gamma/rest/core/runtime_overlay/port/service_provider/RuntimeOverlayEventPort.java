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
package io.gravitee.gamma.rest.core.runtime_overlay.port.service_provider;

import java.util.List;
import java.util.Map;

/**
 * Runtime overlays travel to gateways as generic events of kind {@code runtime-overlay}: the sync is their only channel.
 */
public interface RuntimeOverlayEventPort {
    String ACTION_APPLY = "apply";
    String ACTION_REMOVE = "remove";

    /** @return the id of the event written. */
    String publish(
        String organizationId,
        String environmentId,
        String apiId,
        String userId,
        String runtimeOverlayId,
        String action,
        Map<String, Object> payload
    );

    /** The payloads of the runtime overlay events of the environment written in [from, to], oldest first. */
    List<Map<String, Object>> payloads(String environmentId, long from, long to);
}
