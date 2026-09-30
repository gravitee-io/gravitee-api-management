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
package io.gravitee.apim.core.ai_workspace.domain_service;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import java.util.Optional;

/** The listener path of the workspace proxy, returned as stored. */
public final class AiWorkspaceEndpointReader {

    private AiWorkspaceEndpointReader() {}

    public static Optional<String> read(Api definition) {
        if (definition == null || definition.getListeners() == null) {
            return Optional.empty();
        }
        for (var listener : definition.getListeners()) {
            if (listener instanceof HttpListener http && http.getPaths() != null) {
                for (Path path : http.getPaths()) {
                    if (path.getPath() != null && !path.getPath().isBlank()) {
                        return Optional.of(path.getPath());
                    }
                }
            }
        }
        return Optional.empty();
    }
}
