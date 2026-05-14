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
package io.gravitee.apim.infra.search;

import io.gravitee.apim.core.api.domain_service.ApiPathExtractor;
import io.gravitee.apim.core.api.domain_service.ApiPathIndex;
import io.gravitee.apim.core.api.model.Path;
import io.gravitee.apim.core.search.IndexationObserver;
import io.gravitee.apim.core.search.model.IndexableApi;
import io.gravitee.rest.api.model.search.Indexable;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import java.util.List;
import java.util.Optional;
import lombok.CustomLog;
import org.springframework.stereotype.Service;

@CustomLog
@Service
public class ApiPathIndexationObserver implements IndexationObserver {

    private final ApiPathIndex apiPathIndex;

    public ApiPathIndexationObserver(ApiPathIndex apiPathIndex) {
        this.apiPathIndex = apiPathIndex;
    }

    @Override
    public void onIndex(Indexable source) {
        var snapshot = extract(source).orElse(null);
        if (snapshot == null) {
            return;
        }
        try {
            if (snapshot.paths().isEmpty()) {
                apiPathIndex.remove(snapshot.environmentId(), snapshot.apiId());
            } else {
                apiPathIndex.index(snapshot.environmentId(), snapshot.apiId(), snapshot.paths());
            }
        } catch (RuntimeException e) {
            apiPathIndex.invalidate(snapshot.environmentId());
            throw e;
        }
    }

    @Override
    public void onDelete(Indexable source) {
        // The SearchEngineService broadcast cron rebuilds the Indexable via reflection with only id set
        // (see SearchEngineServiceImpl.process() ACTION_DELETE branch). We have no env/proxy/listeners to look at,
        // so route through the env-agnostic removeForApi which walks every snapshot.
        if (!isApiIndexable(source)) {
            return;
        }
        var apiId = source.getId();
        if (apiId == null) {
            log.warn("onDelete received api Indexable with null id (class=[{}]); skipping", source.getClass().getName());
            return;
        }
        apiPathIndex.removeForApi(apiId);
    }

    private static boolean isApiIndexable(Indexable source) {
        return (
            source instanceof IndexableApi || source instanceof io.gravitee.rest.api.model.api.ApiEntity || source instanceof ApiEntity
        );
    }

    private static Optional<ApiPathSnapshot> extract(Indexable source) {
        return switch (source) {
            case IndexableApi indexable -> {
                var api = indexable.getApi();
                yield Optional.of(new ApiPathSnapshot(api.getEnvironmentId(), api.getId(), ApiPathExtractor.extractPaths(api)));
            }
            case io.gravitee.rest.api.model.api.ApiEntity v3 -> Optional.of(
                new ApiPathSnapshot(
                    v3.getReferenceId(),
                    v3.getId(),
                    v3.getProxy() == null ? List.of() : ApiPathExtractor.extractPathsFromVirtualHosts(v3.getProxy().getVirtualHosts())
                )
            );
            case ApiEntity v4 -> Optional.of(
                new ApiPathSnapshot(v4.getReferenceId(), v4.getId(), ApiPathExtractor.extractPathsFromV4Listeners(v4.getListeners()))
            );
            default -> {
                log.debug("onIndex ignoring non-API Indexable class=[{}]", source.getClass().getName());
                yield Optional.empty();
            }
        };
    }

    private record ApiPathSnapshot(String environmentId, String apiId, List<Path> paths) {}
}
