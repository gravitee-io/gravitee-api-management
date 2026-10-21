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
package io.gravitee.apim.core.api.model.property;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.definition.ApiDefinitionFixtures;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.definition.model.ApiDefinition;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.property.Property;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EncryptedPropertyAuditMarkerTest {

    private static final Map<AuditProperties, String> BASE = Map.of(AuditProperties.API, "api-id");
    private static final Map<AuditProperties, String> MARKED = Map.of(AuditProperties.API, "api-id", AuditProperties.ENCRYPTED, "true");

    @Test
    void should_mark_when_an_encrypted_property_is_added() {
        assertThat(EncryptedPropertyAuditMarker.mark(BASE, httpApi(), httpApi(encrypted("secret")))).isEqualTo(MARKED);
    }

    @Test
    void should_not_mark_when_an_encrypted_property_is_removed() {
        assertThat(
            EncryptedPropertyAuditMarker.mark(BASE, httpApi(encrypted("secret"), plain("p", "v")), httpApi(plain("p", "v")))
        ).isEqualTo(BASE);
    }

    @Test
    void should_mark_when_an_encrypted_property_is_renamed() {
        assertThat(EncryptedPropertyAuditMarker.mark(BASE, httpApi(encrypted("old-key")), httpApi(encrypted("new-key")))).isEqualTo(MARKED);
    }

    @Test
    void should_mark_when_an_encrypted_value_is_renewed() {
        var marked = EncryptedPropertyAuditMarker.mark(
            BASE,
            httpApi(encrypted("secret", "old-ciphertext")),
            httpApi(encrypted("secret", "new-ciphertext"))
        );

        assertThat(marked).isEqualTo(MARKED);
    }

    @Test
    void should_mark_when_a_plain_property_becomes_encrypted() {
        assertThat(EncryptedPropertyAuditMarker.mark(BASE, httpApi(plain("secret", "value")), httpApi(encrypted("secret")))).isEqualTo(
            MARKED
        );
    }

    @Test
    void should_mark_a_creation() {
        assertThat(EncryptedPropertyAuditMarker.mark(Map.of(), null, nativeApi(encrypted("secret")))).containsEntry(
            AuditProperties.ENCRYPTED,
            "true"
        );
    }

    @Test
    void should_not_mark_a_deletion() {
        assertThat(EncryptedPropertyAuditMarker.mark(Map.of(), nativeApi(encrypted("secret")), null)).isEmpty();
    }

    @Test
    void should_mark_a_renewal_beside_a_removal() {
        var marked = EncryptedPropertyAuditMarker.mark(
            BASE,
            httpApi(encrypted("removed"), encrypted("secret", "old-ciphertext")),
            httpApi(encrypted("secret", "new-ciphertext"))
        );

        assertThat(marked).isEqualTo(MARKED);
    }

    @Test
    void should_not_mark_plain_property_changes_beside_an_unchanged_encrypted_property() {
        var marked = EncryptedPropertyAuditMarker.mark(
            BASE,
            httpApi(encrypted("secret"), plain("edited", "v1"), plain("removed", "v1")),
            httpApi(encrypted("secret"), plain("edited", "v2"), plain("plain-c", "plain-c"))
        );

        assertThat(marked).isEqualTo(BASE);
    }

    @Test
    void should_not_mark_a_change_that_leaves_the_properties_untouched() {
        var renamed = httpApi(encrypted("secret"));
        renamed.setName("renamed");

        assertThat(EncryptedPropertyAuditMarker.mark(BASE, httpApi(encrypted("secret")), renamed)).isEqualTo(BASE);
    }

    @Test
    void should_not_mark_a_dynamic_toggle_of_an_unchanged_encrypted_property() {
        var dynamic = Property.builder().key("secret").value("ciphertext").encrypted(true).dynamic(true).build();

        assertThat(EncryptedPropertyAuditMarker.mark(BASE, httpApi(encrypted("secret")), httpApi(dynamic))).isEqualTo(BASE);
    }

    @Test
    void should_not_mark_when_no_property_is_encrypted() {
        var marked = EncryptedPropertyAuditMarker.mark(BASE, httpApi(plain("p", "value")), nativeApi(plain("p", "value")));

        assertThat(marked).isEqualTo(BASE);
    }

    @Test
    void should_not_mark_when_the_new_definition_has_no_properties() {
        var withoutProperties = ApiDefinitionFixtures.anApiV4();
        withoutProperties.setProperties(null);

        assertThat(EncryptedPropertyAuditMarker.mark(BASE, ApiDefinitionFixtures.anApiV2(), withoutProperties)).isEqualTo(BASE);
    }

    @Test
    void should_not_mark_a_migration_that_carries_the_encrypted_properties_over() {
        assertThat(EncryptedPropertyAuditMarker.mark(BASE, v2Api("secret", "ciphertext"), httpApi(encrypted("secret")))).isEqualTo(BASE);
    }

    @Test
    void should_mark_a_migration_that_changes_an_encrypted_value() {
        var marked = EncryptedPropertyAuditMarker.mark(
            BASE,
            v2Api("secret", "old-ciphertext"),
            httpApi(encrypted("secret", "new-ciphertext"))
        );

        assertThat(marked).isEqualTo(MARKED);
    }

    @Test
    void should_detect_an_encrypted_value_written_between_property_lists() {
        assertThat(EncryptedPropertyAuditMarker.writesEncryptedValue(null, List.of(encrypted("secret")))).isTrue();
        assertThat(EncryptedPropertyAuditMarker.writesEncryptedValue(List.of(encrypted("secret")), null)).isFalse();
        assertThat(EncryptedPropertyAuditMarker.writesEncryptedValue(List.of(plain("p", "v1")), List.of(plain("p", "v2")))).isFalse();
        assertThat(
            EncryptedPropertyAuditMarker.writesEncryptedValue(
                List.of(encrypted("secret")),
                List.of(encrypted("secret"), plain("plain-c", "plain-c"))
            )
        ).isFalse();
    }

    @Test
    void should_ignore_null_properties_when_comparing() {
        assertThat(EncryptedPropertyAuditMarker.writesEncryptedValue(null, null)).isFalse();
        assertThat(
            EncryptedPropertyAuditMarker.writesEncryptedValue(Arrays.asList(null, encrypted("secret")), List.of(encrypted("secret")))
        ).isFalse();
        assertThat(EncryptedPropertyAuditMarker.writesEncryptedValue(null, Arrays.asList(null, encrypted("secret")))).isTrue();
    }

    @Test
    void should_not_count_an_encrypted_property_without_value_as_written() {
        var withoutValue = Property.builder().key("secret").encrypted(true).build();

        assertThat(EncryptedPropertyAuditMarker.writesEncryptedValue(List.of(withoutValue), List.of(withoutValue))).isFalse();
        assertThat(EncryptedPropertyAuditMarker.writesEncryptedValue(List.of(encrypted("secret")), List.of(withoutValue))).isFalse();
        assertThat(EncryptedPropertyAuditMarker.writesEncryptedValue(List.of(withoutValue), List.of(encrypted("secret")))).isTrue();
    }

    @Test
    void should_map_each_encrypted_key_to_its_ciphertext_ignoring_null_and_plain_properties() {
        assertThat(EncryptedPropertyAuditMarker.ciphertextByKey(null)).isEmpty();
        assertThat(
            EncryptedPropertyAuditMarker.ciphertextByKey(Arrays.asList(null, encrypted("secret", "secret-ciphertext"), plain("p", "value")))
        ).isEqualTo(Map.of("secret", "secret-ciphertext"));
    }

    @Test
    void should_tell_whether_properties_hold_an_encrypted_one() {
        assertThat(EncryptedPropertyAuditMarker.holdsEncryptedProperty(null)).isFalse();
        assertThat(EncryptedPropertyAuditMarker.holdsEncryptedProperty(List.of(plain("p", "value")))).isFalse();
        assertThat(EncryptedPropertyAuditMarker.holdsEncryptedProperty(Arrays.asList(null, encrypted("secret")))).isTrue();
    }

    private static Api httpApi(Property... properties) {
        var definition = ApiDefinitionFixtures.anApiV4();
        definition.setProperties(List.of(properties));
        return definition;
    }

    private static ApiDefinition nativeApi(Property... properties) {
        var definition = ApiDefinitionFixtures.aNativeApiV4();
        definition.setProperties(List.of(properties));
        return definition;
    }

    private static ApiDefinition v2Api(String key, String ciphertext) {
        var definition = ApiDefinitionFixtures.anApiV2();
        definition.setProperties(new Properties(List.of(new io.gravitee.definition.model.Property(key, ciphertext, true))));
        return definition;
    }

    private static Property encrypted(String key) {
        return encrypted(key, "ciphertext");
    }

    private static Property encrypted(String key, String ciphertext) {
        return Property.builder().key(key).value(ciphertext).encrypted(true).build();
    }

    private static Property plain(String key, String value) {
        return Property.builder().key(key).value(value).build();
    }
}
