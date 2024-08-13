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
package io.gravitee.repository.mongodb.management.internal.sharedpolicygrouphistory;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.SharedPolicyGroupHistoryCriteria;
import io.gravitee.repository.mongodb.management.internal.model.SharedPolicyGroupHistoryMongo;
import org.springframework.data.domain.PageRequest;

public interface SharedPolicyGroupHistoryMongoRepositoryCustom {
    Page<SharedPolicyGroupHistoryMongo> search(SharedPolicyGroupHistoryCriteria sharedPolicyGroupHistoryCriteria, PageRequest pageRequest);
    Page<SharedPolicyGroupHistoryMongo> searchLatestBySharedPolicyGroupId(String environmentId, int page, int size);
}
