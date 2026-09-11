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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.SubscriptionForm;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class SubscriptionFormRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/subscriptionform-tests/";
    }

    @Test
    public void shouldFindById() throws Exception {
        Optional<SubscriptionForm> optional = subscriptionFormRepository.findById("sub-form-find-by-id");

        assertThat(optional).isPresent();

        SubscriptionForm form = optional.get();
        assertThat(form.getId()).isEqualTo("sub-form-find-by-id");
        assertThat(form.getEnvironmentId()).isEqualTo("env-1");
        assertThat(form.getName()).isEqualTo("Default");
        assertThat(form.isDefaultForm()).isTrue();
        assertThat(form.getGmdContent()).isNull();
        assertThat(form.getPortalPageContentId()).isEqualTo("5d1f0c1e-3a3b-4b7e-9c2a-8f6e1d2c3b4a");
        assertThat(form.isEnabled()).isTrue();
        assertThat(form.getValidationConstraints()).isEqualTo("{\"email\":[{\"type\":\"required\"}]}");
    }

    @Test
    public void shouldNotFindByIdWhenNotExists() throws Exception {
        Optional<SubscriptionForm> optional = subscriptionFormRepository.findById("unknown-id");

        assertThat(optional).isNotPresent();
    }

    @Test
    public void shouldFindAllByEnvironmentId() throws Exception {
        List<SubscriptionForm> forms = subscriptionFormRepository.findAllByEnvironmentId("env-1");

        assertThat(forms).extracting(SubscriptionForm::getId).containsExactlyInAnyOrder("sub-form-find-by-id", "sub-form-partner");
    }

    @Test
    public void shouldFindNoneByEnvironmentIdWhenNotExists() throws Exception {
        assertThat(subscriptionFormRepository.findAllByEnvironmentId("unknown-env")).isEmpty();
    }

    @Test
    public void shouldFindDefaultByEnvironmentId() throws Exception {
        Optional<SubscriptionForm> optional = subscriptionFormRepository.findDefaultByEnvironmentId("env-1");

        assertThat(optional).isPresent();

        SubscriptionForm form = optional.get();
        assertThat(form.getId()).isEqualTo("sub-form-find-by-id");
        assertThat(form.getEnvironmentId()).isEqualTo("env-1");
        assertThat(form.isDefaultForm()).isTrue();
    }

    @Test
    public void shouldFindByIdAndEnvironmentId() throws Exception {
        Optional<SubscriptionForm> optional = subscriptionFormRepository.findByIdAndEnvironmentId("sub-form-find-by-id", "env-1");

        assertThat(optional).isPresent();

        SubscriptionForm form = optional.get();
        assertThat(form.getId()).isEqualTo("sub-form-find-by-id");
        assertThat(form.getEnvironmentId()).isEqualTo("env-1");
    }

    @Test
    public void shouldNotFindByIdAndEnvironmentIdWhenEnvironmentMismatch() throws Exception {
        Optional<SubscriptionForm> optional = subscriptionFormRepository.findByIdAndEnvironmentId("sub-form-find-by-id", "unknown-env");

        assertThat(optional).isNotPresent();
    }

    @Test
    public void shouldNotFindDefaultByEnvironmentIdWhenNotExists() throws Exception {
        Optional<SubscriptionForm> optional = subscriptionFormRepository.findDefaultByEnvironmentId("unknown-env");

        assertThat(optional).isNotPresent();
    }

    @Test
    public void shouldCreate() throws Exception {
        SubscriptionForm form = SubscriptionForm.builder()
            .id("sub-form-new")
            .environmentId("env-new")
            .name("New form")
            .portalPageContentId("9b2d7c4e-1f3a-4d5b-8e6f-0a1b2c3d4e5f")
            .enabled(false)
            .validationConstraints("{\"field\":[]}")
            .build();

        Set<SubscriptionForm> allBefore = subscriptionFormRepository.findAll();
        subscriptionFormRepository.create(form);
        Set<SubscriptionForm> allAfter = subscriptionFormRepository.findAll();

        assertThat(allAfter).hasSize(allBefore.size() + 1);

        Optional<SubscriptionForm> optional = subscriptionFormRepository.findById("sub-form-new");
        assertThat(optional).isPresent();

        SubscriptionForm saved = optional.get();
        assertThat(saved.getEnvironmentId()).isEqualTo("env-new");
        assertThat(saved.getName()).isEqualTo("New form");
        assertThat(saved.isDefaultForm()).isFalse();
        assertThat(saved.getGmdContent()).isNull();
        assertThat(saved.getPortalPageContentId()).isEqualTo("9b2d7c4e-1f3a-4d5b-8e6f-0a1b2c3d4e5f");
        assertThat(saved.isEnabled()).isFalse();
        assertThat(saved.getValidationConstraints()).isEqualTo("{\"field\":[]}");
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<SubscriptionForm> optional = subscriptionFormRepository.findById("sub-form-update");
        assertThat(optional).as("Subscription form to update not found").isPresent();

        SubscriptionForm existing = optional.get();
        assertThat(existing.isEnabled()).isFalse();

        SubscriptionForm updated = existing
            .toBuilder()
            .name("Renamed")
            .gmdContent(null)
            .portalPageContentId("3e4d5c6b-7a89-4f01-b2c3-d4e5f6a7b8c9")
            .enabled(true)
            .validationConstraints("{\"updated\":[]}")
            .build();

        SubscriptionForm result = subscriptionFormRepository.update(updated);

        assertThat(result.getName()).isEqualTo("Renamed");
        assertThat(result.getGmdContent()).isNull();
        assertThat(result.getPortalPageContentId()).isEqualTo("3e4d5c6b-7a89-4f01-b2c3-d4e5f6a7b8c9");
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getValidationConstraints()).contains("updated");

        Optional<SubscriptionForm> reloaded = subscriptionFormRepository.findById("sub-form-update");
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getGmdContent()).isNull();
        assertThat(reloaded.get().getPortalPageContentId()).isEqualTo("3e4d5c6b-7a89-4f01-b2c3-d4e5f6a7b8c9");
        assertThat(reloaded.get().isEnabled()).isTrue();
        assertThat(reloaded.get().getValidationConstraints()).contains("updated");
    }

    @Test
    public void shouldNotUpdateUnknownSubscriptionForm() {
        SubscriptionForm unknown = SubscriptionForm.builder().id("unknown-id").environmentId("env").gmdContent("content").build();
        assertThatThrownBy(() -> subscriptionFormRepository.update(unknown))
            .isInstanceOf(TechnicalException.class)
            .hasMessageContaining("Subscription form not found with id");
    }

    @Test
    public void shouldNotUpdateNull() {
        assertThatThrownBy(() -> subscriptionFormRepository.update(null))
            .isInstanceOf(TechnicalException.class)
            .hasMessageContaining("Subscription form must not be null");
    }

    @Test
    public void shouldDelete() throws Exception {
        Optional<SubscriptionForm> before = subscriptionFormRepository.findById("sub-form-delete");
        assertThat(before).as("Subscription form to delete not found").isPresent();

        subscriptionFormRepository.delete("sub-form-delete");

        Optional<SubscriptionForm> after = subscriptionFormRepository.findById("sub-form-delete");
        assertThat(after).as("Subscription form should have been deleted").isNotPresent();
    }

    @Test
    public void shouldDeleteByEnvironmentId() throws Exception {
        assertThat(subscriptionFormRepository.findAllByEnvironmentId("env-delete-by-env"))
            .as("Subscription form to delete not found")
            .isNotEmpty();

        subscriptionFormRepository.deleteByEnvironmentId("env-delete-by-env");

        assertThat(subscriptionFormRepository.findAllByEnvironmentId("env-delete-by-env"))
            .as("Subscription forms should have been deleted")
            .isEmpty();
    }

    @Test
    public void shouldRejectASecondDefaultInTheSameEnvironment() throws Exception {
        SubscriptionForm secondDefault = SubscriptionForm.builder()
            .id("sub-form-second-default")
            .environmentId("env-1")
            .name("Second default")
            .portalPageContentId("1c2d3e4f-5a6b-4c7d-8e9f-0a1b2c3d4e5f")
            .enabled(false)
            .defaultForm(true)
            .validationConstraints("{}")
            .build();

        assertThatThrownBy(() -> subscriptionFormRepository.create(secondDefault)).isInstanceOf(DuplicateKeyException.class);
        assertThat(subscriptionFormRepository.findById("sub-form-second-default")).isNotPresent();
    }

    @Test
    public void shouldRejectPromotingASecondDefaultInTheSameEnvironment() throws Exception {
        SubscriptionForm partner = subscriptionFormRepository.findById("sub-form-partner").orElseThrow();
        SubscriptionForm promoted = partner.toBuilder().defaultForm(true).build();

        assertThatThrownBy(() -> subscriptionFormRepository.update(promoted)).isInstanceOf(DuplicateKeyException.class);
        assertThat(subscriptionFormRepository.findById("sub-form-partner"))
            .get()
            .extracting(SubscriptionForm::isDefaultForm)
            .isEqualTo(false);
    }

    @Test
    public void shouldRejectASecondFormWithTheSameNameInTheSameEnvironment() throws Exception {
        SubscriptionForm sameName = aFormNamed("sub-form-same-name", "env-1", "  partner ONBOARDING  ");

        assertThatThrownBy(() -> subscriptionFormRepository.create(sameName)).isInstanceOf(DuplicateKeyException.class);
        assertThat(subscriptionFormRepository.findById("sub-form-same-name")).isNotPresent();
    }

    @Test
    public void shouldRejectRenamingAFormOntoTheNameOfAnotherFormOfTheEnvironment() throws Exception {
        SubscriptionForm partner = subscriptionFormRepository.findById("sub-form-partner").orElseThrow();
        SubscriptionForm renamed = partner.toBuilder().name("DEFAULT").build();

        assertThatThrownBy(() -> subscriptionFormRepository.update(renamed)).isInstanceOf(DuplicateKeyException.class);
        assertThat(subscriptionFormRepository.findById("sub-form-partner"))
            .get()
            .extracting(SubscriptionForm::getName)
            .isEqualTo("Partner onboarding");
    }

    @Test
    public void shouldAcceptTheSameNameInAnotherEnvironment() throws Exception {
        SubscriptionForm sameNameElsewhere = aFormNamed("sub-form-same-name-other-env", "env-other", "Partner onboarding");

        SubscriptionForm created = subscriptionFormRepository.create(sameNameElsewhere);

        assertThat(created.getName()).isEqualTo("Partner onboarding");
        assertThat(subscriptionFormRepository.findById("sub-form-same-name-other-env")).isPresent();
    }

    @Test
    public void shouldFindAll() throws Exception {
        Set<SubscriptionForm> all = subscriptionFormRepository.findAll();

        assertThat(all).hasSize(6);
    }

    private static SubscriptionForm aFormNamed(String id, String environmentId, String name) {
        return SubscriptionForm.builder()
            .id(id)
            .environmentId(environmentId)
            .name(name)
            .portalPageContentId("1c2d3e4f-5a6b-4c7d-8e9f-0a1b2c3d4e5f")
            .enabled(false)
            .defaultForm(false)
            .validationConstraints("{}")
            .build();
    }
}
