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
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.model.PortalListing;
import io.gravitee.repository.mongodb.management.internal.model.PortalListingMongo;
import io.gravitee.repository.mongodb.management.internal.portallisting.PortalListingMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MongoPortalListingRepositoryTest {

    @Mock
    PortalListingMongoRepository internalPortalListingRepo;

    @Mock
    GraviteeMapper mapper;

    @InjectMocks
    MongoPortalListingRepository repository;

    private PortalListingMongo mongoA;
    private PortalListingMongo mongoB;
    private PortalListing mappedA;
    private PortalListing mappedB;

    @BeforeEach
    void init() {
        mongoA = new PortalListingMongo();
        mongoA.setId("a");
        mongoB = new PortalListingMongo();
        mongoB.setId("b");
        mappedA = PortalListing.builder().id("a").build();
        mappedB = PortalListing.builder().id("b").build();
    }

    @Test
    void should_find_all_by_portal_and_environment_and_map() throws Exception {
        when(internalPortalListingRepo.findAllByPortalIdAndEnvironmentId("portal1", "env1")).thenReturn(List.of(mongoA, mongoB));
        when(mapper.map(mongoA)).thenReturn(mappedA);
        when(mapper.map(mongoB)).thenReturn(mappedB);

        var result = repository.findAllByPortalIdAndEnvironmentId("portal1", "env1");

        assertThat(result).extracting(PortalListing::getId).containsExactly("a", "b");
    }

    @Test
    void should_return_empty_list_when_no_listings_match() throws Exception {
        when(internalPortalListingRepo.findAllByPortalIdAndEnvironmentId("portal1", "env1")).thenReturn(List.of());

        var result = repository.findAllByPortalIdAndEnvironmentId("portal1", "env1");

        assertThat(result).isEmpty();
    }
}
