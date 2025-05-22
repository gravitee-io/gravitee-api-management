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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.SharedPolicyGroupRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.SharedPolicyGroup;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SharedPolicyGroupHRIDUpgraderTest {

    @Mock
    private SharedPolicyGroupRepository sharedPolicyGroupRepository;

    @Mock
    private EnvironmentRepository environmentRepository;

    @InjectMocks
    private final SharedPolicyGroupHRIDUpgrader upgrader = new SharedPolicyGroupHRIDUpgrader();

    @BeforeEach
    void setUp() throws TechnicalException {
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT));

        when(
            sharedPolicyGroupRepository.search(
                argThat(criteria -> Environment.DEFAULT.getId().equals(criteria.getEnvironmentId())),
                any(Pageable.class),
                any()
            )
        )
            .thenReturn(new Page<>(List.of(SharedPolicyGroup.builder().crossId("spg-cross-id").build()), 0, 1, 1));
    }

    @Test
    void should_set_hrid_to_cross_id() throws TechnicalException {
        assertThat(upgrader.upgrade()).isTrue();
        verify(sharedPolicyGroupRepository).update(argThat(spg -> "spg-cross-id".equals(spg.getHrid())));
    }
}
