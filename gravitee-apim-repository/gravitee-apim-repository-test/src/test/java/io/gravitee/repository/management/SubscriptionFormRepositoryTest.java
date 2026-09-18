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
        assertThat(form.getGmdContent()).contains("gmd-grid");
        assertThat(form.isEnabled()).isTrue();
        assertThat(form.getValidationConstraints()).isEqualTo("{\"email\":[{\"type\":\"required\"}]}");
    }

    @Test
    public void shouldNotFindByIdWhenNotExists() throws Exception {
        Optional<SubscriptionForm> optional = subscriptionFormRepository.findById("unknown-id");

        assertThat(optional).isNotPresent();
    }

    @Test
    public void shouldFindAllByEnvironmentIdSortedByName() throws Exception {
        List<SubscriptionForm> forms = subscriptionFormRepository.findAllByEnvironmentId("env-1");

        assertThat(forms).extracting(SubscriptionForm::getName).containsExactly("API products", "Default", "Partner onboarding");
    }

    @Test
    public void shouldFindNoneByEnvironmentIdWhenNotExists() throws Exception {
        assertThat(subscriptionFormRepository.findAllByEnvironmentId("unknown-env")).isEmpty();
    }

    @Test
    public void shouldLoadApiIdsWithTheForm() throws Exception {
        Optional<SubscriptionForm> optional = subscriptionFormRepository.findById("sub-form-partner");

        assertThat(optional).isPresent();
        assertThat(optional.get().getApiIds()).containsExactly("api-partner-1", "api-partner-2");
        assertThat(subscriptionFormRepository.findById("sub-form-find-by-id"))
            .get()
            .extracting(SubscriptionForm::getApiIds)
            .isEqualTo(List.of());
    }

    @Test
    public void shouldFindByEnvironmentIdAndApiId() throws Exception {
        Optional<SubscriptionForm> optional = subscriptionFormRepository.findByEnvironmentIdAndApiId("env-1", "api-partner-2");

        assertThat(optional).isPresent();
        assertThat(optional.get().getId()).isEqualTo("sub-form-partner");
    }

    @Test
    public void shouldNotFindByEnvironmentIdAndApiIdWhenNotMapped() throws Exception {
        assertThat(subscriptionFormRepository.findByEnvironmentIdAndApiId("env-1", "unknown-api")).isNotPresent();
        assertThat(subscriptionFormRepository.findByEnvironmentIdAndApiId("other-env", "api-partner-1")).isNotPresent();
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
    public void shouldCreate() throws Exception {
        SubscriptionForm form = SubscriptionForm.builder()
            .id("sub-form-new")
            .environmentId("env-new")
            .name("New form")
            .gmdContent("<gmd-card><gmd-input name=\"field\" label=\"Field\" fieldKey=\"field\"/></gmd-card>")
            .apiIds(List.of("api-new-1"))
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
        assertThat(saved.getApiIds()).containsExactly("api-new-1");
        assertThat(saved.getGmdContent()).contains("gmd-input");
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
            .gmdContent("<gmd-card><gmd-input name=\"updated\" label=\"Updated\" fieldKey=\"updated\"/></gmd-card>")
            .apiIds(List.of("api-updated"))
            .enabled(true)
            .validationConstraints("{\"updated\":[]}")
            .build();

        SubscriptionForm result = subscriptionFormRepository.update(updated);

        assertThat(result.getName()).isEqualTo("Renamed");
        assertThat(result.getApiIds()).containsExactly("api-updated");
        assertThat(result.getGmdContent()).contains("updated");
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getValidationConstraints()).contains("updated");

        Optional<SubscriptionForm> reloaded = subscriptionFormRepository.findById("sub-form-update");
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getGmdContent()).contains("updated");
        assertThat(reloaded.get().getApiIds()).containsExactly("api-updated");
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
    public void shouldRejectAnApiAlreadyMappedToAnotherForm() throws Exception {
        SubscriptionForm competing = SubscriptionForm.builder()
            .id("sub-form-competing")
            .environmentId("env-1")
            .name("Competing")
            .apiIds(List.of("api-partner-1"))
            .gmdContent("<gmd-card><gmd-input name=\"field\" label=\"Field\"/></gmd-card>")
            .enabled(false)
            .validationConstraints("{}")
            .build();

        assertThatThrownBy(() -> subscriptionFormRepository.create(competing)).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    public void shouldRejectMappingAnApiAlreadyMappedToAnotherForm() throws Exception {
        SubscriptionForm unmapped = subscriptionFormRepository.findById("sub-form-find-by-id").orElseThrow();
        SubscriptionForm remapped = unmapped.toBuilder().apiIds(List.of("api-partner-1")).build();

        assertThatThrownBy(() -> subscriptionFormRepository.update(remapped)).isInstanceOf(DuplicateKeyException.class);
        assertThat(subscriptionFormRepository.findById("sub-form-find-by-id"))
            .get()
            .extracting(SubscriptionForm::getApiIds)
            .isEqualTo(List.of());
    }

    @Test
    public void shouldFindAll() throws Exception {
        Set<SubscriptionForm> all = subscriptionFormRepository.findAll();

        assertThat(all).hasSize(7);
    }

    private static SubscriptionForm aFormNamed(String id, String environmentId, String name) {
        return SubscriptionForm.builder()
            .id(id)
            .environmentId(environmentId)
            .name(name)
            .gmdContent("<gmd-card><gmd-input name=\"field\" label=\"Field\"/></gmd-card>")
            .enabled(false)
            .validationConstraints("{}")
            .build();
    }
}
