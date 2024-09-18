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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Integration;
import io.gravitee.repository.mongodb.management.internal.integration.IntegrationMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.IntegrationMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MongoIntegrationRepositoryTest {

    @Mock
    IntegrationMongoRepository internalRepository;

    GraviteeMapper mapper = Mappers.getMapper(GraviteeMapper.class);

    MongoIntegrationRepository sut;

    @BeforeEach
    void setup() {
        sut = new MongoIntegrationRepository(internalRepository, mapper);
    }

    @Test
    void findAllByEnvironmentAndGroups() {
        // Given
        String environmentId = "env-1";
        Collection<String> groups = Set.of();
        Pageable pageable = new PageableBuilder().pageNumber(0).pageSize(5).build();

        IntegrationMongo integration = new IntegrationMongo();
        integration.setId("MyIntegration");
        Page<IntegrationMongo> foo = new Page<>(List.of(integration), 0, 0, 0);
        given(internalRepository.findAllByEnvironmentIdAndGroups(anyString(), any(), any(), any())).willReturn(foo);

        // When
        Page<Integration> result = sut.findAllByEnvironmentAndGroups(environmentId, Set.of(), groups, pageable);

        // Then
        assertThat(result.getContent()).map(Integration::getId).containsOnly("MyIntegration");
    }
}
