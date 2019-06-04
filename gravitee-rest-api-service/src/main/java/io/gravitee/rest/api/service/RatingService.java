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
package io.gravitee.rest.api.service;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.rest.api.model.*;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface RatingService {

    RatingEntity create(NewRatingEntity rating);

    RatingEntity createAnswer(NewRatingAnswerEntity answer);

    Page<RatingEntity> findByApi(String api, Pageable pageable);

    RatingSummaryEntity findSummaryByApi(String api);

    RatingEntity findByApiForConnectedUser(String api);

    RatingEntity update(UpdateRatingEntity rating);

    void delete(String id);

    void deleteAnswer(String ratingId, String answerId);

    boolean isEnabled();
}