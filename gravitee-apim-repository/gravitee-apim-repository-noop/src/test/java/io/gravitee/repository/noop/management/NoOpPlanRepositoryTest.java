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
package io.gravitee.repository.noop.management;

import static org.junit.Assert.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.repository.noop.AbstractNoOpRepositoryTest;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NoOpPlanRepositoryTest extends AbstractNoOpRepositoryTest {

    @Autowired
    private PlanRepository cut;

    @Test
    public void findByApis() throws TechnicalException {
        List<Plan> plans = cut.findByApis(List.of("test_id"));

        assertNotNull(plans);
        assertTrue(plans.isEmpty());
    }

    @Test
    public void findByApi() throws TechnicalException {
        Set<Plan> plans = cut.findByApi("test_id");

        assertNotNull(plans);
        assertTrue(plans.isEmpty());
    }

    @Test
    public void findByIdIn() throws TechnicalException {
        Set<Plan> plans = cut.findByIdIn(List.of("test_id"));

        assertNotNull(plans);
        assertTrue(plans.isEmpty());
    }
}
