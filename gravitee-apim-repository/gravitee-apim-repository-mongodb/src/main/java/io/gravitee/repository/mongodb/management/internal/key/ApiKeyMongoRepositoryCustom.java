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
package io.gravitee.repository.mongodb.management.internal.key;

import com.mongodb.client.result.UpdateResult;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.mongodb.management.internal.model.ApiKeyMongo;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public interface ApiKeyMongoRepositoryCustom {
    List<ApiKeyMongo> search(ApiKeyCriteria filter, final Sortable sortable);

    List<ApiKeyMongo> findByKeyAndApi(String key, String api);

    List<ApiKeyMongo> findByKeyAndReferenceIdAndReferenceType(String key, String referenceId, String referenceType);

    List<ApiKeyMongo> findByPlan(String plan);

    UpdateResult addSubscription(String id, String subscriptionId);
}
