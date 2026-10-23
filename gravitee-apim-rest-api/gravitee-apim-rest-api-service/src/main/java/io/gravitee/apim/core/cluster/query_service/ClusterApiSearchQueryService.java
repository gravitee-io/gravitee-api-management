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
package io.gravitee.apim.core.cluster.query_service;

import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import jakarta.annotation.Nullable;
import java.util.Collection;

/** Searches the APIs using clusters, by the cluster cross ids their endpoints name. */
public interface ClusterApiSearchQueryService {
    /**
     * A page of the ids of the v4 APIs of an environment whose endpoints use one of {@code clusterCrossIds}, sorted by
     * name then id.
     *
     * @param name narrows the APIs to those whose name contains it, ignoring case; none when blank
     */
    Page<String> search(String environmentId, Collection<String> clusterCrossIds, @Nullable String name, Pageable pageable);
}
