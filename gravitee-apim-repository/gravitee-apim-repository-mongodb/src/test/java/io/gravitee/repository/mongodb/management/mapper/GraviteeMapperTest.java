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
package io.gravitee.repository.mongodb.management.mapper;

import static org.junit.Assert.assertEquals;

import io.gravitee.repository.management.model.*;
import io.gravitee.repository.management.model.flow.Flow;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.repository.management.model.flow.FlowStep;
import io.gravitee.repository.management.model.flow.selector.FlowChannelSelector;
import io.gravitee.repository.management.model.flow.selector.FlowConditionSelector;
import io.gravitee.repository.management.model.flow.selector.FlowHttpSelector;
import io.gravitee.repository.management.model.flow.selector.FlowSelector;
import io.gravitee.repository.mongodb.management.internal.model.*;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.mapstruct.factory.Mappers;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GraviteeMapperTest {

    GraviteeMapper mapper = Mappers.getMapper(GraviteeMapper.class);

    @Test
    public void should_map_api_quality_rule_with_proper_ids() {
        final ApiQualityRule source = new ApiQualityRule();
        source.setApi("api");
        source.setQualityRule("qualityRule");
        source.setChecked(true);
        source.setCreatedAt(new Date());
        source.setUpdatedAt(new Date());

        final ApiQualityRuleMongo target = mapper.map(source);

        assertEquals(source.getApi(), target.getId().getApi());
        assertEquals(source.getQualityRule(), target.getId().getQualityRule());
        assertEquals(source.isChecked(), target.isChecked());
        assertEquals(source.getCreatedAt(), target.getCreatedAt());
        assertEquals(source.getUpdatedAt(), target.getUpdatedAt());

        final ApiQualityRule sourceBack = mapper.map(target);

        assertEquals(source.getApi(), sourceBack.getApi());
        assertEquals(source.getQualityRule(), sourceBack.getQualityRule());
        assertEquals(source.isChecked(), sourceBack.isChecked());
        assertEquals(source.getCreatedAt(), sourceBack.getCreatedAt());
        assertEquals(source.getUpdatedAt(), sourceBack.getUpdatedAt());
    }

    @Test
    public void should_map_page_with_proper_visibility() {
        final Page source = new Page();
        final PageSource pageSource = new PageSource();
        pageSource.setConfiguration("configuration");
        pageSource.setType("type");

        source.setId("id");
        source.setReferenceType(PageReferenceType.API);
        source.setReferenceId("referenceId");
        source.setSource(pageSource);
        source.setVisibility("PRIVATE");
        source.setOrder(1);
        source.setCreatedAt(new Date());
        source.setUpdatedAt(new Date());

        final PageMongo target = mapper.map(source);

        assertEquals(source.getId(), target.getId());
        assertEquals(source.getReferenceType().name(), target.getReferenceType());
        assertEquals(source.getReferenceId(), target.getReferenceId());
        assertEquals(source.getSource().getType(), target.getSource().getType());
        assertEquals(source.getSource().getConfiguration(), target.getSource().getConfiguration());
        assertEquals(source.getVisibility(), target.getVisibility());
        assertEquals(source.getOrder(), target.getOrder());
        assertEquals(source.getCreatedAt(), target.getCreatedAt());
        assertEquals(source.getUpdatedAt(), target.getUpdatedAt());

        final Page sourceBack = mapper.map(target);

        assertEquals(source.getId(), sourceBack.getId());
        assertEquals(source.getReferenceType(), sourceBack.getReferenceType());
        assertEquals(source.getReferenceId(), sourceBack.getReferenceId());
        assertEquals(source.getSource().getType(), sourceBack.getSource().getType());
        assertEquals(source.getSource().getConfiguration(), sourceBack.getSource().getConfiguration());
        assertEquals(source.getVisibility(), sourceBack.getVisibility());
        assertEquals(source.getOrder(), sourceBack.getOrder());
        assertEquals(source.getCreatedAt(), sourceBack.getCreatedAt());
        assertEquals(source.getUpdatedAt(), sourceBack.getUpdatedAt());
    }

    @Test
    public void should_map_page_with_public_visibility_when_null() {
        final Page source = new Page();
        source.setVisibility(null);

        final PageMongo target = mapper.map(source);

        assertEquals("PUBLIC", target.getVisibility());
    }

    @Test
    public void should_map_page_revision_with_proper_ids() {
        final PageRevision source = new PageRevision();
        source.setRevision(2);
        source.setContent("content");
        source.setPageId("pageId");
        source.setHash("hash");
        source.setContributor("contributor");
        source.setCreatedAt(new Date());

        final PageRevisionMongo target = mapper.map(source);

        assertEquals(source.getRevision(), target.getId().getRevision());
        assertEquals(source.getContent(), target.getContent());
        assertEquals(source.getPageId(), target.getId().getPageId());
        assertEquals(source.getHash(), target.getHash());
        assertEquals(source.getContributor(), target.getContributor());
        assertEquals(source.getCreatedAt(), target.getCreatedAt());

        final PageRevision sourceBack = mapper.map(target);

        assertEquals(source.getRevision(), sourceBack.getRevision());
        assertEquals(source.getContent(), sourceBack.getContent());
        assertEquals(source.getPageId(), sourceBack.getPageId());
        assertEquals(source.getHash(), sourceBack.getHash());
        assertEquals(source.getContributor(), sourceBack.getContributor());
        assertEquals(source.getCreatedAt(), sourceBack.getCreatedAt());
    }

    @Test
    public void should_map_subscription_with_proper_ids() {
        final Subscription source = new Subscription();
        source.setApplication("application");
        source.setApi("api");
        source.setStatus(Subscription.Status.PENDING);
        source.setId("id");
        source.setConsumerStatus(Subscription.ConsumerStatus.STARTED);
        source.setGeneralConditionsContentPageId("pageId");
        source.setGeneralConditionsContentRevision(2);
        source.setCreatedAt(new Date());
        source.setUpdatedAt(new Date());

        final SubscriptionMongo target = mapper.map(source);

        assertEquals(source.getApplication(), target.getApplication());
        assertEquals(source.getApi(), target.getApi());
        assertEquals(source.getStatus().name(), target.getStatus());
        assertEquals(source.getId(), target.getId());
        assertEquals(source.getConsumerStatus().name(), target.getConsumerStatus());
        assertEquals(source.getGeneralConditionsContentPageId(), target.getGeneralConditionsContentRevision().getPageId());
        assertEquals(source.getGeneralConditionsContentRevision().intValue(), target.getGeneralConditionsContentRevision().getRevision());
        assertEquals(source.getCreatedAt(), target.getCreatedAt());
        assertEquals(source.getUpdatedAt(), target.getUpdatedAt());

        final Subscription sourceBack = mapper.map(target);

        assertEquals(source.getApplication(), sourceBack.getApplication());
        assertEquals(source.getApi(), sourceBack.getApi());
        assertEquals(source.getStatus(), sourceBack.getStatus());
        assertEquals(source.getId(), sourceBack.getId());
        assertEquals(source.getConsumerStatus(), sourceBack.getConsumerStatus());
        assertEquals(source.getGeneralConditionsContentPageId(), sourceBack.getGeneralConditionsContentPageId());
        assertEquals(source.getGeneralConditionsContentRevision(), sourceBack.getGeneralConditionsContentRevision());
        assertEquals(source.getCreatedAt(), sourceBack.getCreatedAt());
        assertEquals(source.getUpdatedAt(), sourceBack.getUpdatedAt());
    }

    @Test
    public void should_map_custom_user_field_with_proper_ids() {
        final CustomUserField source = new CustomUserField();
        source.setKey("key");
        source.setReferenceId("referenceId");
        source.setReferenceType(CustomUserFieldReferenceType.ENVIRONMENT);
        source.setFormat(MetadataFormat.STRING);
        source.setRequired(true);
        source.setCreatedAt(new Date());
        source.setUpdatedAt(new Date());

        final CustomUserFieldMongo target = mapper.map(source);

        assertEquals(source.getKey(), target.getId().getKey());
        assertEquals(source.getReferenceId(), target.getId().getReferenceId());
        assertEquals(source.getReferenceType().name(), target.getId().getReferenceType());
        assertEquals(source.getFormat().name(), target.getFormat());
        assertEquals(source.isRequired(), target.isRequired());
        assertEquals(source.getCreatedAt(), target.getCreatedAt());
        assertEquals(source.getUpdatedAt(), target.getUpdatedAt());

        final CustomUserField sourceBack = mapper.map(target);

        assertEquals(source.getKey(), sourceBack.getKey());
        assertEquals(source.getReferenceId(), sourceBack.getReferenceId());
        assertEquals(source.getReferenceType(), sourceBack.getReferenceType());
        assertEquals(source.getFormat(), sourceBack.getFormat());
        assertEquals(source.isRequired(), sourceBack.isRequired());
        assertEquals(source.getCreatedAt(), sourceBack.getCreatedAt());
        assertEquals(source.getUpdatedAt(), sourceBack.getUpdatedAt());
    }

    @Test
    public void should_map_flow_with_selectors() {
        final Flow source = new Flow();
        final Set<String> tags = Set.of("a", "b", "c", "d");
        final FlowStep step = new FlowStep();
        step.setName("step");

        final List<FlowStep> requestSteps = List.of(step, step);
        final List<FlowStep> publishSteps = List.of(step);
        final List<FlowStep> responseSteps = List.of(step, step);
        final List<FlowStep> subscribeSteps = List.of(step);
        final List<FlowSelector> selectors = List.of(new FlowHttpSelector(), new FlowConditionSelector(), new FlowChannelSelector());

        source.setName("name");
        source.setEnabled(true);
        source.setOrder(2);
        source.setReferenceType(FlowReferenceType.API);
        source.setReferenceId("referenceId");
        source.setTags(tags);
        source.setSelectors(selectors);
        source.setRequest(requestSteps);
        source.setPublish(publishSteps);
        source.setResponse(responseSteps);
        source.setSubscribe(subscribeSteps);

        final FlowMongo target = mapper.map(source);

        assertEquals(source.getName(), target.getName());
        assertEquals(source.isEnabled(), target.isEnabled());
        assertEquals(source.getOrder(), target.getOrder());
        assertEquals(source.getReferenceType(), target.getReferenceType());
        assertEquals(source.getReferenceId(), target.getReferenceId());
        assertEquals(source.getName(), target.getName());
        assertEquals(source.getTags(), new HashSet<>(target.getTags()));
        assertEquals(source.getSelectors(), target.getSelectors());
        assertEquals(source.getRequest(), target.getRequest());
        assertEquals(source.getPublish(), target.getPublish());
        assertEquals(source.getResponse(), target.getResponse());
        assertEquals(source.getSubscribe(), target.getSubscribe());

        final Flow sourceBack = mapper.map(target);

        assertEquals(source.getName(), sourceBack.getName());
        assertEquals(source.isEnabled(), sourceBack.isEnabled());
        assertEquals(source.getOrder(), sourceBack.getOrder());
        assertEquals(source.getReferenceType(), target.getReferenceType());
        assertEquals(source.getReferenceId(), target.getReferenceId());
        assertEquals(source.getTags(), sourceBack.getTags());
        assertEquals(source.getSelectors(), sourceBack.getSelectors());
        assertEquals(source.getRequest(), sourceBack.getRequest());
        assertEquals(source.getPublish(), sourceBack.getPublish());
        assertEquals(source.getResponse(), sourceBack.getResponse());
        assertEquals(source.getSubscribe(), sourceBack.getSubscribe());
    }
}
