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

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import java.util.List;

/**
 * Listener path of the workspace LLM proxy, returned as stored.
 */
public final class AiWorkspaceEndpointReader {

    private AiWorkspaceEndpointReader() {}

    public static String read(String environmentId, List<Api> apis) {
        if (apis == null) {
            return null;
        }
        for (Api api : apis) {
            if (api == null || api.getType() != ApiType.LLM_PROXY || !sameEnvironment(environmentId, api)) {
                continue;
            }
            for (var listener : api.getApiListeners()) {
                if (!(listener instanceof HttpListener http) || http.getPaths() == null) {
                    continue;
                }
                for (Path path : http.getPaths()) {
                    if (path != null && path.getPath() != null && !path.getPath().isBlank()) {
                        return path.getPath();
                    }
                }
            }
        }
        return null;
    }

    private static boolean sameEnvironment(String environmentId, Api api) {
        return environmentId == null || api.getEnvironmentId() == null || environmentId.equals(api.getEnvironmentId());
    }
}
