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
package io.gravitee.rest.api.service;

import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface RatingService {
    RatingEntity create(ExecutionContext executionContext, NewRatingEntity rating);

    RatingEntity createAnswer(ExecutionContext executionContext, NewRatingAnswerEntity answer);

    RatingEntity findById(ExecutionContext executionContext, String id);

    RatingAnswerEntity findAnswerById(ExecutionContext executionContext, String answerId);

    Page<RatingEntity> findByApi(ExecutionContext executionContext, String api, Pageable pageable);

    List<RatingEntity> findByApi(ExecutionContext executionContext, String api);

    RatingSummaryEntity findSummaryByApi(ExecutionContext executionContext, String api);

    RatingEntity findByApiForConnectedUser(ExecutionContext executionContext, String api);

    RatingEntity update(ExecutionContext executionContext, UpdateRatingEntity rating);

    void delete(ExecutionContext executionContext, String id);

    void deleteAnswer(ExecutionContext executionContext, String ratingId, String answerId);

    boolean isEnabled(ExecutionContext executionContext);

    Set<String> findReferenceIdsOrderByRate(ExecutionContext executionContext, Collection<String> apis);
}
