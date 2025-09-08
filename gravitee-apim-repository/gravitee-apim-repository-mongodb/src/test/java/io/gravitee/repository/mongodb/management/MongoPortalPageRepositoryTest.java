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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.model.ExpandsViewContext;
import io.gravitee.repository.management.model.PortalPage;
import io.gravitee.repository.mongodb.management.internal.model.PortalPageMongo;
import io.gravitee.repository.mongodb.management.internal.portalpage.PortalPageMongoRepository;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MongoPortalPageRepositoryTest {

    @Mock
    PortalPageMongoRepository internalRepo;

    @InjectMocks
    MongoPortalPageRepository repository;

    PortalPageMongo p1;
    PortalPageMongo p2;

    @BeforeEach
    void init() {
        p1 = new PortalPageMongo();
        p1.setId("a");
        p1.setEnvironmentId("env");
        p1.setName("n1");
        p1.setContent("c1");

        p2 = new PortalPageMongo();
        p2.setId("b");
        p2.setEnvironmentId("env");
        p2.setName("n2");
        p2.setContent(null);
    }

    @Test
    void should_return_empty_list_when_ids_empty() {
        List<PortalPage> res = repository.findByIdsWithExpand(List.of(), Collections.singletonList(ExpandsViewContext.CONTENT));
        assertThat(res).isEmpty();
    }

    @Test
    void should_map_projections_with_expand() {
        when(internalRepo.findPortalPagesByIdWithExpand(eq(List.of("a", "b")), eq(List.of(ExpandsViewContext.CONTENT)))).thenReturn(
            List.of(p1, p2)
        );

        List<PortalPage> res = repository.findByIdsWithExpand(List.of("a", "b"), Collections.singletonList(ExpandsViewContext.CONTENT));

        verify(internalRepo).findPortalPagesByIdWithExpand(eq(List.of("a", "b")), eq(List.of(ExpandsViewContext.CONTENT)));
        assertThat(res).hasSize(2);
        PortalPage rp1 = res.get(0);
        PortalPage rp2 = res.get(1);
        assertThat(rp1.getId()).isEqualTo("a");
        assertThat(rp1.getContent()).isEqualTo("c1");
        assertThat(rp2.getId()).isEqualTo("b");
        assertThat(rp2.getContent()).isNull();
    }
}
