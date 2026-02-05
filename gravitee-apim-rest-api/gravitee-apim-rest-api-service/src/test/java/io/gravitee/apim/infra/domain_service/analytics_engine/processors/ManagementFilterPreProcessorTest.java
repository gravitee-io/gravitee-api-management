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
package io.gravitee.apim.infra.domain_service.analytics_engine.processors;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.analytics_engine.domain_service.FilterPreProcessor;
import io.gravitee.apim.core.analytics_engine.model.MetricsContext;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ManagementFilterPreProcessorTest {

    private final FilterPreProcessor filterPreProcessor = new ManagementFilterPreProcessor();

    AuditInfo buildAuditInfo(String userId) {
        var actor = AuditActor.builder().userId(userId).build();
        return AuditInfo.builder().organizationId("DEFAULT").environmentId("DEFAULT").actor(actor).build();
    }

    @Test
    public void should_return_allowed_apis() {
        var auditInfo = buildAuditInfo(UUID.randomUUID().toString());

        List<Api> adminApis = List.of(
            Api.builder().id("id1").name("api1").build(),
            Api.builder().id("id2").name("api2").build(),
            Api.builder().id("id3").name("api3").build()
        );

        MetricsContext context = new MetricsContext(auditInfo).withApis(adminApis);
        var filters = filterPreProcessor.buildFilters(context);

        assertThat(filters).size().isEqualTo(1);

        var value = filters.getFirst().value();
        assertThat(value).asInstanceOf(InstanceOfAssertFactories.SET).containsExactlyInAnyOrderElementsOf(apiIds(adminApis));
    }

    private static List<String> apiIds(List<Api> apis) {
        return apis.stream().map(Api::getId).toList();
    }
}
