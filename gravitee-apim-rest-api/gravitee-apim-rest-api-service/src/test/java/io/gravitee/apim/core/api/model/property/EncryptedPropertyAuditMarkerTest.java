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
import io.gravitee.definition.model.v4.property.Property;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EncryptedPropertyAuditMarkerTest {

    private static final Map<AuditProperties, String> BASE = Map.of(AuditProperties.API, "api-id");

    @Test
    void should_mark_when_the_new_api_holds_an_encrypted_property() {
        var marked = EncryptedPropertyAuditMarker.mark(BASE, httpApi(), httpApi(encrypted("secret")));

        assertThat(marked).containsExactlyInAnyOrderEntriesOf(Map.of(AuditProperties.API, "api-id", AuditProperties.ENCRYPTED, "true"));
    }

    @Test
    void should_mark_when_only_the_old_api_held_an_encrypted_property() {
        var marked = EncryptedPropertyAuditMarker.mark(BASE, httpApi(encrypted("secret")), httpApi());

        assertThat(marked).containsEntry(AuditProperties.ENCRYPTED, "true");
    }

    @Test
    void should_mark_a_creation_or_a_deletion() {
        assertThat(EncryptedPropertyAuditMarker.mark(Map.of(), null, nativeApi(encrypted("secret")))).containsEntry(
            AuditProperties.ENCRYPTED,
            "true"
        );
        assertThat(EncryptedPropertyAuditMarker.mark(Map.of(), nativeApi(encrypted("secret")), null)).containsEntry(
            AuditProperties.ENCRYPTED,
            "true"
        );
    }

    @Test
    void should_not_mark_when_no_property_is_encrypted() {
        var marked = EncryptedPropertyAuditMarker.mark(BASE, httpApi(plain("p")), nativeApi(plain("p")));

        assertThat(marked).isEqualTo(BASE);
    }

    @Test
    void should_not_mark_apis_without_v4_properties() {
        var withoutProperties = ApiDefinitionFixtures.anApiV4();
        withoutProperties.setProperties(null);

        assertThat(EncryptedPropertyAuditMarker.mark(BASE, ApiDefinitionFixtures.anApiV2(), withoutProperties)).isEqualTo(BASE);
    }

    @Test
    void should_ignore_null_properties_when_looking_for_an_encrypted_one() {
        assertThat(EncryptedPropertyAuditMarker.holdsEncryptedProperty(null)).isFalse();
        assertThat(EncryptedPropertyAuditMarker.holdsEncryptedProperty(Arrays.asList(null, encrypted("secret")))).isTrue();
    }

    private static ApiDefinition httpApi(Property... properties) {
        var definition = ApiDefinitionFixtures.anApiV4();
        definition.setProperties(List.of(properties));
        return definition;
    }

    private static ApiDefinition nativeApi(Property... properties) {
        var definition = ApiDefinitionFixtures.aNativeApiV4();
        definition.setProperties(List.of(properties));
        return definition;
    }

    private static Property encrypted(String key) {
        return Property.builder().key(key).value("ciphertext").encrypted(true).build();
    }

    private static Property plain(String key) {
        return Property.builder().key(key).value("value").build();
    }
}
