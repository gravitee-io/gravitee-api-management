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
package io.gravitee.repository.redis.management.internal;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.model.Rating;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Set;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface RatingRedisRepository {

    Set<Rating> findAll();
    Rating findById(String ratingId);
    Rating saveOrUpdate(Rating rating);
    void delete(String rating);
    List<Rating> findByApi(String api);
    Rating findByApiAndUser(String api, String user);
    Page<Rating> findByApi(String api, PageRequest pageRequest);
}
