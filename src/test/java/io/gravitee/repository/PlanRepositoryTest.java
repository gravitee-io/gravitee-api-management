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
package io.gravitee.repository;

import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.management.model.Plan;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static io.gravitee.repository.utils.DateUtils.parse;
import static org.junit.Assert.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PlanRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/plan-tests/";
    }

    @Test
    public void shouldFindById() throws Exception {
        final Optional<Plan> plan = planRepository.findById("my-plan");

        assertNotNull(plan);
        assertTrue(plan.isPresent());
        assertEquals("my-plan", plan.get().getId());
    }

    @Test
    public void shouldFindByApi() throws Exception {
        final Set<Plan> plans = planRepository.findByApi("api1");

        assertNotNull(plans);
        assertEquals(2, plans.size());
    }

    @Test
    public void shouldCreate() throws Exception {
        String planName = "new-plan";

        final Plan plan = new Plan();
        plan.setId(planName);
        plan.setName("Plan name");
        plan.setDescription("Description for the new plan");
        plan.setValidation(Plan.PlanValidationType.AUTO);
        plan.setType(Plan.PlanType.API);
        plan.setApis(Collections.singleton("my-api"));
        plan.setCreatedAt(parse("11/02/2016"));
        plan.setUpdatedAt(parse("12/02/2016"));

        planRepository.create(plan);

        Optional<Plan> optional = planRepository.findById(planName);
        Assert.assertTrue("New plan not found", optional.isPresent());

        final Plan createdPlan = optional.get();
        Assert.assertEquals("Invalid plan name.", plan.getName(), createdPlan.getName());
        Assert.assertEquals("Invalid plan description.", plan.getDescription(), createdPlan.getDescription());
        Assert.assertEquals("Invalid plan validation.", plan.getValidation(), createdPlan.getValidation());
        Assert.assertEquals("Invalid plan type.", plan.getType(), createdPlan.getType());
        Assert.assertEquals("Invalid plan APIs.", plan.getApis().size(), createdPlan.getApis().size());
        Assert.assertEquals("Invalid plan created date.", plan.getCreatedAt(), createdPlan.getCreatedAt());
        Assert.assertEquals("Invalid plan updated date.", plan.getUpdatedAt(), createdPlan.getUpdatedAt());
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Plan> optional = planRepository.findById("my-plan");
        Assert.assertTrue("Plan to update not found", optional.isPresent());

        final Plan plan = optional.get();
        plan.setName("New plan");

        planRepository.update(plan);

        Optional<Plan> optionalUpdated = planRepository.findById("my-plan");
        Assert.assertTrue("View to update not found", optionalUpdated.isPresent());

        final Plan planUpdated = optionalUpdated.get();
        Assert.assertEquals("Invalid saved plan name.", "New plan", planUpdated.getName());
        Assert.assertEquals("Invalid plan description.", plan.getDescription(), planUpdated.getDescription());
    }

    @Test
    public void shouldDelete() throws Exception {
        planRepository.delete("stores");

        Optional<Plan> optional = planRepository.findById("stores");

        Assert.assertFalse("Plan must not be found", optional.isPresent());
    }
}
