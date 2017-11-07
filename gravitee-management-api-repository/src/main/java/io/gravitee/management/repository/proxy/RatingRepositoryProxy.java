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
package io.gravitee.management.repository.proxy;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.RatingRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Rating;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RatingRepositoryProxy extends AbstractProxy<RatingRepository> implements RatingRepository {

    @Override
    public Rating create(Rating rating) throws TechnicalException {
        return target.create(rating);
    }

    @Override
    public Optional<Rating> findById(String id) throws TechnicalException {
        return target.findById(id);
    }

    @Override
    public Optional<Rating> findByApiAndUser(String api, String user) throws TechnicalException {
        return target.findByApiAndUser(api, user);
    }

    @Override
    public List<Rating> findByApi(String api) throws TechnicalException {
        return target.findByApi(api);
    }

    @Override
    public Page<Rating> findByApiPageable(String api, Pageable pageable) throws TechnicalException {
        return target.findByApiPageable(api, pageable);
    }

    @Override
    public Rating update(Rating rating) throws TechnicalException {
        return target.update(rating);
    }

    @Override
    public void delete(String id) throws TechnicalException {
        target.delete(id);
    }
}
