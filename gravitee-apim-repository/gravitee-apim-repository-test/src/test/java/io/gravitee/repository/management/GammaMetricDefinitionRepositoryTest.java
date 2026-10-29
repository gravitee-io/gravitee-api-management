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
import io.gravitee.repository.management.model.GammaMetricDefinition;
import java.util.Date;
import org.junit.jupiter.api.Test;

public class GammaMetricDefinitionRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/gamma-metric-definition-tests/";
    }

    @Test
    public void should_list_the_records_of_one_environment_ordered_by_raw_key() throws Exception {
        var records = gammaMetricDefinitionRepository.findByEnvironmentId("DEFAULT");

        assertThat(records)
            .extracting(GammaMetricDefinition::getRawKey)
            .containsExactly("double_checkout_basket-value", "keyword_mcp-proxy_tools/call.v2", "long_fraud-check_duration-ms");
    }

    @Test
    public void should_return_nothing_for_an_environment_without_records() throws Exception {
        assertThat(gammaMetricDefinitionRepository.findByEnvironmentId("UNKNOWN_ENV")).isEmpty();
    }

    @Test
    public void should_find_a_record_by_environment_and_raw_key_with_every_field() throws Exception {
        var found = gammaMetricDefinitionRepository.findByEnvironmentIdAndRawKey("DEFAULT", "long_fraud-check_duration-ms");

        assertThat(found).hasValueSatisfying(record -> {
            assertThat(record.getId()).isEqualTo("gmd-1");
            assertThat(record.getEnvironmentId()).isEqualTo("DEFAULT");
            assertThat(record.getLabel()).isEqualTo("Fraud check time");
            assertThat(record.getDescription()).isEqualTo("Time spent by the fraud check policy");
            assertThat(record.getUnit()).isEqualTo("MILLISECONDS");
            assertThat(record.isExposed()).isTrue();
            assertThat(record.getCreatedAt()).isEqualTo(new Date(1000000000000L));
            assertThat(record.getUpdatedAt()).isEqualTo(new Date(1111111111111L));
        });
    }

    @Test
    public void should_keep_a_raw_key_with_a_slash_and_a_dot_and_absent_optional_fields() throws Exception {
        var found = gammaMetricDefinitionRepository.findByEnvironmentIdAndRawKey("DEFAULT", "keyword_mcp-proxy_tools/call.v2");

        assertThat(found).hasValueSatisfying(record -> {
            assertThat(record.getLabel()).isNull();
            assertThat(record.getDescription()).isNull();
            assertThat(record.getUnit()).isNull();
            assertThat(record.isExposed()).isFalse();
        });
    }

    @Test
    public void should_keep_a_free_unit_suffix_verbatim() throws Exception {
        var found = gammaMetricDefinitionRepository.findByEnvironmentIdAndRawKey("DEFAULT", "double_checkout_basket-value");

        assertThat(found).get().extracting(GammaMetricDefinition::getUnit).isEqualTo("€");
    }

    @Test
    public void should_not_find_a_record_of_another_environment() throws Exception {
        assertThat(gammaMetricDefinitionRepository.findByEnvironmentIdAndRawKey("OTHER_ENV", "double_checkout_basket-value")).isEmpty();
        assertThat(gammaMetricDefinitionRepository.findByEnvironmentIdAndRawKey("OTHER_ENV", "long_fraud-check_duration-ms"))
            .get()
            .extracting(GammaMetricDefinition::getId)
            .isEqualTo("gmd-4");
    }

    @Test
    public void should_match_the_raw_key_case_sensitively() throws Exception {
        assertThat(gammaMetricDefinitionRepository.findByEnvironmentIdAndRawKey("DEFAULT", "LONG_FRAUD-CHECK_DURATION-MS")).isEmpty();
    }

    @Test
    public void should_accept_two_keys_differing_by_case_only() throws Exception {
        gammaMetricDefinitionRepository.create(aRecord("gmd-upper", "DEFAULT", "long_FRAUD-check_duration-ms"));

        assertThat(gammaMetricDefinitionRepository.findByEnvironmentIdAndRawKey("DEFAULT", "long_FRAUD-check_duration-ms"))
            .get()
            .extracting(GammaMetricDefinition::getId)
            .isEqualTo("gmd-upper");
        assertThat(gammaMetricDefinitionRepository.findByEnvironmentIdAndRawKey("DEFAULT", "long_fraud-check_duration-ms"))
            .get()
            .extracting(GammaMetricDefinition::getId)
            .isEqualTo("gmd-1");
    }

    @Test
    public void should_create_a_record() throws Exception {
        var record = aRecord("gmd-new", "DEFAULT", "bool_cache_hit");

        var created = gammaMetricDefinitionRepository.create(record);

        assertThat(created).usingRecursiveComparison().isEqualTo(record);
        assertThat(gammaMetricDefinitionRepository.findById("gmd-new")).get().usingRecursiveComparison().isEqualTo(record);
    }

    @Test
    public void should_refuse_a_second_record_on_the_same_key_of_the_same_environment() throws Exception {
        var duplicate = aRecord("gmd-duplicate", "DEFAULT", "long_fraud-check_duration-ms");

        assertThatThrownBy(() -> gammaMetricDefinitionRepository.create(duplicate)).isInstanceOf(DuplicateKeyException.class);
        assertThat(gammaMetricDefinitionRepository.findById("gmd-duplicate")).isEmpty();
    }

    @Test
    public void should_accept_the_same_key_in_another_environment() throws Exception {
        gammaMetricDefinitionRepository.create(aRecord("gmd-third-env", "THIRD_ENV", "long_fraud-check_duration-ms"));

        assertThat(gammaMetricDefinitionRepository.findByEnvironmentIdAndRawKey("THIRD_ENV", "long_fraud-check_duration-ms")).isPresent();
    }

    @Test
    public void should_update_a_record() throws Exception {
        var existing = gammaMetricDefinitionRepository.findById("gmd-3").orElseThrow();
        var edited = existing.toBuilder().label("Cart value").unit("USD").exposed(false).updatedAt(new Date(1200000000000L)).build();

        var updated = gammaMetricDefinitionRepository.update(edited);

        assertThat(updated).usingRecursiveComparison().isEqualTo(edited);
        assertThat(gammaMetricDefinitionRepository.findById("gmd-3")).get().usingRecursiveComparison().isEqualTo(edited);
    }

    @Test
    public void should_fail_to_update_a_record_that_does_not_exist() {
        var missing = aRecord("gmd-missing", "DEFAULT", "long_missing");

        assertThatThrownBy(() -> gammaMetricDefinitionRepository.update(missing)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void should_delete_a_record() throws Exception {
        gammaMetricDefinitionRepository.delete("gmd-2");

        assertThat(gammaMetricDefinitionRepository.findById("gmd-2")).isEmpty();
        assertThat(gammaMetricDefinitionRepository.findByEnvironmentId("DEFAULT")).hasSize(2);
    }

    @Test
    public void should_delete_every_record_of_one_environment_only() throws Exception {
        gammaMetricDefinitionRepository.deleteByEnvironmentId("DEFAULT");

        assertThat(gammaMetricDefinitionRepository.findByEnvironmentId("DEFAULT")).isEmpty();
        assertThat(gammaMetricDefinitionRepository.findByEnvironmentId("OTHER_ENV")).hasSize(1);
    }

    private static GammaMetricDefinition aRecord(String id, String environmentId, String rawKey) {
        return GammaMetricDefinition.builder()
            .id(id)
            .environmentId(environmentId)
            .rawKey(rawKey)
            .label("A label")
            .description("A description")
            .unit("COUNT")
            .exposed(true)
            .createdAt(new Date(1000000000000L))
            .updatedAt(new Date(1000000000000L))
            .build();
    }
}
