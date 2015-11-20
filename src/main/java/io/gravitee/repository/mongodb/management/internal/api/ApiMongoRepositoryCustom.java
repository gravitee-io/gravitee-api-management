/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.mongodb.management.internal.api;

import io.gravitee.repository.management.model.Visibility;
import io.gravitee.repository.management.model.MembershipType;
import io.gravitee.repository.mongodb.management.internal.model.ApiMongo;

import java.util.Collection;
import java.util.List;

public interface ApiMongoRepositoryCustom {
    
	/**
	 * Find apis by user
	 * @param username
	 * @return
	 */
    Collection<ApiMongo> findByMember(String username, MembershipType membershipType, Visibility visibility);
    
    /**
     * Count api by user
     * @param username
     * @return
     */
	int countByUser(String username, String membershipType);
}
