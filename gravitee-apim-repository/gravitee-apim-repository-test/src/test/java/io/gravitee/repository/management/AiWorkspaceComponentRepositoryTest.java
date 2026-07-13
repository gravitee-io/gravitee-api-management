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
package io.gravitee.repository.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.AiWorkspaceComponent;
import io.gravitee.repository.management.model.AiWorkspaceComponentType;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
public class AiWorkspaceComponentRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/ai-workspace-component-tests/";
    }

    @Test
    public void shouldFindById() throws TechnicalException {
        final Optional<AiWorkspaceComponent> found = aiWorkspaceComponentRepository.findById("comp-1");

        assertThat(found).isPresent();
        assertSoftly(softly -> {
            AiWorkspaceComponent c = found.get();
            softly.assertThat(c.getId()).isEqualTo("comp-1");
            softly.assertThat(c.getApiProductId()).isEqualTo("prod-1");
            softly.assertThat(c.getComponentType()).isEqualTo(AiWorkspaceComponentType.LLM_PROXY);
            softly.assertThat(c.getRefId()).isEqualTo("api-llm-1");
            softly.assertThat(c.getCreatedAt()).isNotNull();
        });
    }

    @Test
    public void shouldReturnEmptyWhenNotFound() throws TechnicalException {
        assertThat(aiWorkspaceComponentRepository.findById("not-existing")).isEmpty();
    }

    @Test
    public void shouldFindByApiProductId() throws TechnicalException {
        final List<AiWorkspaceComponent> components = aiWorkspaceComponentRepository.findByApiProductId("prod-1");

        assertThat(components).extracting(AiWorkspaceComponent::getId).containsExactlyInAnyOrder("comp-1", "comp-2", "comp-3");
    }

    @Test
    public void shouldReturnEmptyListWhenApiProductHasNoComponents() throws TechnicalException {
        assertThat(aiWorkspaceComponentRepository.findByApiProductId("unknown-product")).isEmpty();
    }

    @Test
    public void shouldFindByApiProductIdAndComponentType() throws TechnicalException {
        final List<AiWorkspaceComponent> models = aiWorkspaceComponentRepository.findByApiProductIdAndComponentType(
            "prod-1",
            AiWorkspaceComponentType.MODEL
        );

        assertThat(models).extracting(AiWorkspaceComponent::getId).containsExactly("comp-2");
        assertThat(models.get(0).getRefId()).isEqualTo("gpt-4o");
    }

    @Test
    public void shouldFindByRefIdAcrossProducts() throws TechnicalException {
        final List<AiWorkspaceComponent> byRef = aiWorkspaceComponentRepository.findByRefId("gpt-4o");

        assertThat(byRef).extracting(AiWorkspaceComponent::getId).containsExactlyInAnyOrder("comp-2", "comp-5");
    }

    @Test
    public void shouldCreate() throws TechnicalException {
        var date = new Date(1_470_157_767_000L);
        AiWorkspaceComponent component = AiWorkspaceComponent.builder()
            .id("new-comp")
            .apiProductId("prod-3")
            .componentType(AiWorkspaceComponentType.LLM_PROXY)
            .refId("api-llm-3")
            .createdAt(date)
            .updatedAt(date)
            .build();

        AiWorkspaceComponent created = aiWorkspaceComponentRepository.create(component);

        assertThat(created).isNotNull();
        AiWorkspaceComponent found = aiWorkspaceComponentRepository.findById("new-comp").orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(found.getApiProductId()).isEqualTo("prod-3");
            softly.assertThat(found.getComponentType()).isEqualTo(AiWorkspaceComponentType.LLM_PROXY);
            softly.assertThat(found.getRefId()).isEqualTo("api-llm-3");
        });
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        var date = new Date(1_470_157_767_000L);
        AiWorkspaceComponent component = AiWorkspaceComponent.builder()
            .id("comp-3")
            .apiProductId("prod-1")
            .componentType(AiWorkspaceComponentType.PROVIDER)
            .refId("anthropic")
            .createdAt(date)
            .updatedAt(date)
            .build();

        aiWorkspaceComponentRepository.update(component);

        AiWorkspaceComponent found = aiWorkspaceComponentRepository.findById("comp-3").orElseThrow();
        assertThat(found.getRefId()).isEqualTo("anthropic");
    }

    @Test
    public void shouldDelete() throws TechnicalException {
        aiWorkspaceComponentRepository.delete("comp-1");

        assertThat(aiWorkspaceComponentRepository.findById("comp-1")).isEmpty();
        assertThat(aiWorkspaceComponentRepository.findByApiProductId("prod-1")).extracting(AiWorkspaceComponent::getId).contains("comp-2");
    }

    @Test
    public void shouldDeleteByApiProductId() throws TechnicalException {
        aiWorkspaceComponentRepository.deleteByApiProductId("prod-1");

        assertThat(aiWorkspaceComponentRepository.findByApiProductId("prod-1")).isEmpty();
        assertThat(aiWorkspaceComponentRepository.findByApiProductId("prod-2"))
            .extracting(AiWorkspaceComponent::getId)
            .contains("comp-4", "comp-5");
    }
}
