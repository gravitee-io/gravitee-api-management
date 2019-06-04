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
package io.gravitee.rest.api.portal.rest.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.Path;
import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.Rule;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PlanSecurityType;
import io.gravitee.rest.api.model.PlanStatus;
import io.gravitee.rest.api.model.PlanType;
import io.gravitee.rest.api.model.PlanValidationType;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.portal.rest.model.Plan;
import io.gravitee.rest.api.portal.rest.model.Plan.SecurityEnum;
import io.gravitee.rest.api.portal.rest.model.Plan.ValidationEnum;
import io.gravitee.rest.api.service.SubscriptionService;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PlanMapperTest {

    private static final String API = "my-api";
    private static final String PLAN = "my-plan";

    private PlanEntity planEntity;

    @Mock
    private SubscriptionService subscriptionService;
    
    @InjectMocks
    private PlanMapper planMapper;
    
    @Test
    public void testConvert() {
        Instant now = Instant.now();
        Date nowDate = Date.from(now);

        //init
        planEntity = new PlanEntity();
       
        planEntity.setApi(API);
        planEntity.setCharacteristics(Arrays.asList(PLAN));
        planEntity.setClosedAt(nowDate);
        planEntity.setCommentMessage(PLAN);
        planEntity.setCommentRequired(true);
        planEntity.setCreatedAt(nowDate);
        planEntity.setDescription(PLAN);
        planEntity.setExcludedGroups(Arrays.asList(PLAN));
        planEntity.setId(PLAN);
        planEntity.setName(PLAN);
        planEntity.setNeedRedeployAt(nowDate);
        planEntity.setOrder(1);
        
        Policy policy = new Policy();
        policy.setConfiguration(PLAN);
        policy.setName(PLAN);
        Rule rule = new Rule();
        rule.setDescription(PLAN);
        rule.setEnabled(true);
        rule.setMethods(new HashSet<HttpMethod>(Arrays.asList(HttpMethod.GET)));
        rule.setPolicy(policy);
        Path path = new Path();
        path.setPath(PLAN);
        path.setRules(Arrays.asList(rule));
        Map<String, Path> paths = new HashMap<>();
        paths.put(PLAN, path);
        planEntity.setPaths(paths);
        
        planEntity.setPublishedAt(nowDate);
        planEntity.setSecurity(PlanSecurityType.API_KEY);
        planEntity.setSecurityDefinition(PLAN);
        planEntity.setSelectionRule(PLAN);
        planEntity.setStatus(PlanStatus.PUBLISHED);
        planEntity.setTags(new HashSet<String>(Arrays.asList(PLAN)));
        planEntity.setType(PlanType.API);
        planEntity.setUpdatedAt(nowDate);
        planEntity.setValidation(PlanValidationType.AUTO);
        
        SubscriptionEntity mockSubscription = new SubscriptionEntity();
        mockSubscription.setPlan(PLAN);
        mockSubscription.setStatus(SubscriptionStatus.ACCEPTED);
        Collection<SubscriptionEntity> subscriptions = Arrays.asList(mockSubscription);
        doReturn(subscriptions).when(subscriptionService).search(any());
        
        //Test
        Plan responsePlan = planMapper.convert(planEntity);
        assertNotNull(responsePlan);
        
        List<String> characteristics = responsePlan.getCharacteristics();
        assertNotNull(characteristics);
        assertEquals(1, characteristics.size());
        assertEquals(PLAN, characteristics.get(0));
        assertEquals(PLAN, responsePlan.getCommentQuestion());
        assertTrue(responsePlan.getCommentRequired());
        assertEquals(PLAN, responsePlan.getDescription());
        assertEquals(PLAN, responsePlan.getId());
        assertEquals(PLAN, responsePlan.getName());
        assertEquals(1, responsePlan.getOrder().intValue());
        assertEquals(SecurityEnum.API_KEY, responsePlan.getSecurity());
        assertTrue(responsePlan.getSubscribed());
        assertEquals(ValidationEnum.AUTO, responsePlan.getValidation());
        
        
    }
 
    @Test
    public void testNoSubscription() {
        planEntity = new PlanEntity();
        planEntity.setSecurity(PlanSecurityType.API_KEY);
        planEntity.setValidation(PlanValidationType.AUTO);
        
        //Empty list of subscription
        Collection<SubscriptionEntity> noSubscriptions = Arrays.asList();
        doReturn(noSubscriptions).when(subscriptionService).search(any());
        
        Plan responsePlan = planMapper.convert(planEntity);
        assertNotNull(responsePlan);
        assertFalse(responsePlan.getSubscribed());
        
        //Null list of subscription
        doReturn(null).when(subscriptionService).search(any());
        responsePlan = planMapper.convert(planEntity);
        assertNotNull(responsePlan);
        assertFalse(responsePlan.getSubscribed());
    }
    
}
