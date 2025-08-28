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
package io.gravitee.repository.mongodb.management.internal.clusters;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.ClusterCriteria;
import io.gravitee.repository.mongodb.management.internal.model.ClusterMongo;
import java.util.Set;
import org.springframework.data.domain.PageRequest;

public interface ClusterMongoRepositoryCustom {
    Page<ClusterMongo> search(ClusterCriteria criteria, PageRequest pageRequest);

    /**
     * Updates the groups of the cluster identified by the given id.
     * @param id the cluster id
     * @param groups the new groups list (can be null)
     * @return true if a document was matched and updated, false otherwise
     */
    boolean updateGroups(String id, Set<String> groups);
}
