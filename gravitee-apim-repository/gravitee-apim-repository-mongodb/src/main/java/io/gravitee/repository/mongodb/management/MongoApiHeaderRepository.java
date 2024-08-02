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
package io.gravitee.repository.mongodb.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiHeaderRepository;
import io.gravitee.repository.management.model.ApiHeader;
import io.gravitee.repository.mongodb.management.internal.api.ApiHeaderMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.ApiHeaderMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoApiHeaderRepository implements ApiHeaderRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoApiHeaderRepository.class);

    @Autowired
    private ApiHeaderMongoRepository internalApiHeaderRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<ApiHeader> findById(String apiHeaderId) throws TechnicalException {
        LOGGER.debug("Find apiHeader by ID [{}]", apiHeaderId);

        final ApiHeaderMongo apiHeader = internalApiHeaderRepo.findById(apiHeaderId).orElse(null);

        LOGGER.debug("Find apiHeader by ID [{}] - Done", apiHeaderId);
        return Optional.ofNullable(mapper.map(apiHeader));
    }

    @Override
    public ApiHeader create(ApiHeader apiHeader) throws TechnicalException {
        LOGGER.debug("Create apiHeader [{}]", apiHeader.getName());

        ApiHeaderMongo apiHeaderMongo = mapper.map(apiHeader);
        ApiHeaderMongo createdApiHeaderMongo = internalApiHeaderRepo.insert(apiHeaderMongo);

        ApiHeader res = mapper.map(createdApiHeaderMongo);

        LOGGER.debug("Create apiHeader [{}] - Done", apiHeader.getName());

        return res;
    }

    @Override
    public ApiHeader update(ApiHeader apiHeader) throws TechnicalException {
        if (apiHeader == null || apiHeader.getName() == null) {
            throw new IllegalStateException("ApiHeader to update must have a name");
        }

        final ApiHeaderMongo apiHeaderMongo = internalApiHeaderRepo.findById(apiHeader.getId()).orElse(null);

        if (apiHeaderMongo == null) {
            throw new IllegalStateException(String.format("No apiHeader found with name [%s]", apiHeader.getId()));
        }

        try {
            ApiHeaderMongo apiHeaderMongoUpdated = internalApiHeaderRepo.save(mapper.map(apiHeader));
            return mapper.map(apiHeaderMongoUpdated);
        } catch (Exception e) {
            LOGGER.error("An error occured when updating apiHeader", e);
            throw new TechnicalException("An error occured when updating apiHeader");
        }
    }

    @Override
    public void delete(String apiHeaderId) throws TechnicalException {
        try {
            internalApiHeaderRepo.deleteById(apiHeaderId);
        } catch (Exception e) {
            LOGGER.error("An error occured when deleting apiHeader [{}]", apiHeaderId, e);
            throw new TechnicalException("An error occured when deleting apiHeader");
        }
    }

    @Override
    public Set<ApiHeader> findAll() throws TechnicalException {
        final List<ApiHeaderMongo> apiHeaders = internalApiHeaderRepo.findAll();
        return apiHeaders.stream().map(apiHeaderMongo -> mapper.map(apiHeaderMongo)).collect(Collectors.toSet());
    }

    @Override
    public Set<ApiHeader> findAllByEnvironment(String environmentId) throws TechnicalException {
        final List<ApiHeaderMongo> apiHeaders = internalApiHeaderRepo.findByEnvironmentId(environmentId);
        return apiHeaders.stream().map(apiHeaderMongo -> mapper.map(apiHeaderMongo)).collect(Collectors.toSet());
    }

    @Override
    public List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException {
        LOGGER.debug("Delete by environmentId [{}]", environmentId);
        try {
            final var apiHeaders = internalApiHeaderRepo.deleteByEnvironmentId(environmentId).stream().map(ApiHeaderMongo::getId).toList();
            LOGGER.debug("Delete by environmentId [{}] - Done", environmentId);
            return apiHeaders;
        } catch (Exception ex) {
            LOGGER.error("Failed to delete apiHeader by envId: {}", environmentId, ex);
            throw new TechnicalException("Failed to delete apiHeader by envId");
        }
    }
}
