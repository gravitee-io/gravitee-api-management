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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.SubscriptionForm;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;

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
        assertThat(form.getGmdContent()).contains("gmd-grid");
        assertThat(form.isEnabled()).isTrue();
    }

    @Test
    public void shouldNotFindByIdWhenNotExists() throws Exception {
        Optional<SubscriptionForm> optional = subscriptionFormRepository.findById("unknown-id");

        assertThat(optional).isNotPresent();
    }

    @Test
    public void shouldFindByEnvironmentId() throws Exception {
        Optional<SubscriptionForm> optional = subscriptionFormRepository.findByEnvironmentId("env-find");

        assertThat(optional).isPresent();

        SubscriptionForm form = optional.get();
        assertThat(form.getId()).isEqualTo("sub-form-find-by-env");
        assertThat(form.getEnvironmentId()).isEqualTo("env-find");
        assertThat(form.getGmdContent()).contains("gmd-textarea");
        assertThat(form.isEnabled()).isTrue();
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
    public void shouldNotFindByEnvironmentIdWhenNotExists() throws Exception {
        Optional<SubscriptionForm> optional = subscriptionFormRepository.findByEnvironmentId("unknown-env");

        assertThat(optional).isNotPresent();
    }

    @Test
    public void shouldCreate() throws Exception {
        SubscriptionForm form = SubscriptionForm.builder()
            .id("sub-form-new")
            .environmentId("env-new")
            .gmdContent("<gmd-card><gmd-input name=\"field\" label=\"Field\"/></gmd-card>")
            .enabled(false)
            .build();

        Set<SubscriptionForm> allBefore = subscriptionFormRepository.findAll();
        subscriptionFormRepository.create(form);
        Set<SubscriptionForm> allAfter = subscriptionFormRepository.findAll();

        assertThat(allAfter).hasSize(allBefore.size() + 1);

        Optional<SubscriptionForm> optional = subscriptionFormRepository.findById("sub-form-new");
        assertThat(optional).isPresent();

        SubscriptionForm saved = optional.get();
        assertThat(saved.getEnvironmentId()).isEqualTo("env-new");
        assertThat(saved.getGmdContent()).contains("gmd-input");
        assertThat(saved.isEnabled()).isFalse();
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<SubscriptionForm> optional = subscriptionFormRepository.findById("sub-form-update");
        assertThat(optional).as("Subscription form to update not found").isPresent();

        SubscriptionForm existing = optional.get();
        assertThat(existing.isEnabled()).isFalse();

        SubscriptionForm updated = existing
            .toBuilder()
            .gmdContent("<gmd-card><gmd-input name=\"updated\" label=\"Updated\"/></gmd-card>")
            .enabled(true)
            .build();

        SubscriptionForm result = subscriptionFormRepository.update(updated);

        assertThat(result.getGmdContent()).contains("updated");
        assertThat(result.isEnabled()).isTrue();

        Optional<SubscriptionForm> reloaded = subscriptionFormRepository.findById("sub-form-update");
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getGmdContent()).contains("updated");
        assertThat(reloaded.get().isEnabled()).isTrue();
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
        Optional<SubscriptionForm> before = subscriptionFormRepository.findByEnvironmentId("env-delete-by-env");
        assertThat(before).as("Subscription form to delete not found").isPresent();

        subscriptionFormRepository.deleteByEnvironmentId("env-delete-by-env");

        Optional<SubscriptionForm> after = subscriptionFormRepository.findByEnvironmentId("env-delete-by-env");
        assertThat(after).as("Subscription form should have been deleted").isNotPresent();
    }

    @Test
    public void shouldFindAll() throws Exception {
        Set<SubscriptionForm> all = subscriptionFormRepository.findAll();

        assertThat(all).hasSize(5);
    }
}
