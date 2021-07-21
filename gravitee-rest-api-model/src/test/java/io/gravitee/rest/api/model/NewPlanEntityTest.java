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
package io.gravitee.rest.api.model;

import static org.junit.Assert.*;

import io.gravitee.rest.api.model.settings.Plan;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NewPlanEntityTest {

    @Test
    public void from() {
        final PlanEntity actual = getAPlanEntity();
        final NewPlanEntity result = NewPlanEntity.from(actual);

        Assert.assertEquals(result.getId(), actual.getId());
        Assert.assertEquals(result.getApi(), actual.getApi());
        Assert.assertEquals(result.getName(), actual.getName());
        Assert.assertEquals(result.getDescription(), actual.getDescription());
        Assert.assertEquals(result.getValidation(), actual.getValidation());
        Assert.assertEquals(result.getSecurity(), actual.getSecurity());
        Assert.assertEquals(result.getSecurityDefinition(), actual.getSecurityDefinition());
        Assert.assertEquals(result.getType(), actual.getType());
        Assert.assertEquals(result.getStatus(), actual.getStatus());
        Assert.assertEquals(result.getApi(), actual.getApi());
        Assert.assertEquals(result.getPaths(), actual.getPaths());
        Assert.assertEquals(result.getFlows(), actual.getFlows());
        Assert.assertEquals(result.getCharacteristics(), actual.getCharacteristics());
        Assert.assertEquals(result.getExcludedGroups(), actual.getExcludedGroups());
        Assert.assertEquals(result.isCommentRequired(), actual.isCommentRequired());
        Assert.assertEquals(result.getCommentMessage(), actual.getCommentMessage());
        Assert.assertEquals(result.getGeneralConditions(), actual.getGeneralConditions());
        Assert.assertEquals(result.getTags(), actual.getTags());
        Assert.assertEquals(result.getSelectionRule(), actual.getSelectionRule());
    }

    private PlanEntity getAPlanEntity() {
        final PlanEntity planEntity = new PlanEntity();
        planEntity.setId("plan-id");
        planEntity.setApi("api-id");
        planEntity.setName("plan-name");
        planEntity.setDescription("description");
        planEntity.setValidation(PlanValidationType.AUTO);
        planEntity.setSecurity(PlanSecurityType.JWT);
        planEntity.setSecurityDefinition("definition");
        planEntity.setType(PlanType.API);
        planEntity.setStatus(PlanStatus.STAGING);
        planEntity.setPaths(new HashMap<>());
        planEntity.setFlows(new ArrayList<>());
        planEntity.setCharacteristics(new ArrayList<>());
        planEntity.setExcludedGroups(new ArrayList<>());
        planEntity.setCommentRequired(true);
        planEntity.setCommentMessage("comment-message");
        planEntity.setGeneralConditions("conditions");
        planEntity.setTags(new HashSet<>());
        planEntity.setSelectionRule("selection-rule");

        return planEntity;
    }
}
