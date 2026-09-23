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
package io.gravitee.gateway.dictionary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.model.dictionary.DictionaryProperty;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.dictionary.model.Dictionary;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MultiEnvironmentDictionaryManagerTest {

    private static final String ENV = "DEFAULT";
    private static final String OTHER_ENV = "OTHER";

    private static final DataEncryptor DATA_ENCRYPTOR = new DataEncryptor(
        new StandardEnvironment(),
        "api.properties.encryption.secret",
        "vvLJ4Q8Khvv9tm2tIPdkGEdmgKUruAL6"
    );

    private MultiEnvironmentDictionaryManager cut;

    @BeforeEach
    void setUp() {
        cut = new MultiEnvironmentDictionaryManager(DATA_ENCRYPTOR);
    }

    @Nested
    class DeployTest {

        @Test
        void should_index_console_dictionary_by_id_when_key_is_null() {
            cut.deploy(dictionary("idp-server-details", null, ENV, "first-value", 1L));

            assertThat(property(ENV, "idp-server-details")).isEqualTo("first-value");
        }

        @Test
        void should_keep_both_dictionaries_when_runtime_keys_differ() {
            cut.deploy(dictionary("idp-server-details", null, ENV, "first-value", 1L));
            cut.deploy(dictionary("uuid-tf", "tf_idp-server-details", ENV, "tf-value", 2L));

            assertThat(property(ENV, "idp-server-details")).isEqualTo("first-value");
            assertThat(property(ENV, "tf_idp-server-details")).isEqualTo("tf-value");
        }

        @Test
        void should_keep_incumbent_when_another_dictionary_collides_on_runtime_key() {
            cut.deploy(dictionary("idp-server-details", null, ENV, "first-value", 1L));
            cut.deploy(dictionary("uuid-tf", "idp-server-details", ENV, "second-value", 2L));

            assertThat(property(ENV, "idp-server-details")).isEqualTo("first-value");
        }

        @Test
        void should_replace_properties_when_same_dictionary_is_redeployed() {
            cut.deploy(dictionary("idp-server-details", null, ENV, "first-value", 1L));
            cut.deploy(dictionary("idp-server-details", null, ENV, "updated-value", 2L));

            assertThat(property(ENV, "idp-server-details")).isEqualTo("updated-value");
        }

        @Test
        void should_not_overwrite_when_same_dictionary_deployed_at_is_older() {
            cut.deploy(dictionary("idp-server-details", null, ENV, "first-value", 2L));
            cut.deploy(dictionary("idp-server-details", null, ENV, "second-value", 1L));

            assertThat(property(ENV, "idp-server-details")).isEqualTo("first-value");
        }

        @Test
        void should_isolate_dictionaries_per_environment() {
            cut.deploy(dictionary("idp-server-details", null, ENV, "default-value", 1L));
            cut.deploy(dictionary("idp-server-details", null, OTHER_ENV, "other-value", 1L));

            assertThat(property(ENV, "idp-server-details")).isEqualTo("default-value");
            assertThat(property(OTHER_ENV, "idp-server-details")).isEqualTo("other-value");
        }

        @Test
        void should_skip_a_property_whose_value_is_null() {
            Dictionary dictionary = dictionary("idp-server-details", null, ENV, "first-value", 1L);
            Map<String, DictionaryProperty> properties = new HashMap<>(dictionary.getProperties());
            properties.put("VALUELESS_PROP", new DictionaryProperty(null, false));
            dictionary.setProperties(properties);

            cut.deploy(dictionary);

            assertThat(property(ENV, "idp-server-details")).isEqualTo("first-value");
            assertThat(cut.getDictionaries(ENV).get("idp-server-details")).doesNotContainKey("VALUELESS_PROP");
        }

        @Test
        void should_deploy_without_throwing_when_a_property_value_is_null() {
            Dictionary dictionary = dictionary("idp-server-details", null, ENV, "first-value", 1L);
            Map<String, DictionaryProperty> properties = new HashMap<>(dictionary.getProperties());
            properties.put("NULL_PROP", null);
            dictionary.setProperties(properties);

            cut.deploy(dictionary);

            assertThat(property(ENV, "idp-server-details")).isEqualTo("first-value");
            assertThat(cut.getDictionaries(ENV).get("idp-server-details")).doesNotContainKey("NULL_PROP");
        }
    }

    @Nested
    class EncryptionTest {

        @Test
        void should_decrypt_an_encrypted_property_so_that_el_sees_plaintext() throws GeneralSecurityException {
            String ciphertext = DATA_ENCRYPTOR.encrypt("s3cr3t-api-key");

            cut.deploy(dictionary(Map.of("MY_PROP", new DictionaryProperty(ciphertext, true))));

            assertThat(property(ENV, "idp-server-details")).isEqualTo("s3cr3t-api-key");
        }

        @Test
        void should_leave_a_plaintext_property_untouched() {
            cut.deploy(dictionary(Map.of("MY_PROP", new DictionaryProperty("not-a-secret", false))));

            assertThat(property(ENV, "idp-server-details")).isEqualTo("not-a-secret");
        }

        @Test
        void should_keep_the_stored_value_when_a_property_cannot_be_decrypted() {
            cut.deploy(dictionary(Map.of("MY_PROP", new DictionaryProperty("***", true))));

            assertThat(property(ENV, "idp-server-details")).isEqualTo("***");
        }

        @Test
        void should_resolve_a_decrypted_value_through_the_dictionaries_el_variable() throws GeneralSecurityException {
            cut.deploy(dictionary(Map.of("MY_PROP", new DictionaryProperty(DATA_ENCRYPTOR.encrypt("s3cr3t-api-key"), true))));

            TemplateEngine engine = TemplateEngine.templateEngine();
            new EnvironmentDictionaryTemplateVariableProvider(ENV, cut).provide(engine.getTemplateContext());

            assertThat(engine.getValue("{#dictionaries['idp-server-details']['MY_PROP']}", String.class)).isEqualTo("s3cr3t-api-key");
        }

        @Test
        void should_deploy_the_other_properties_when_one_cannot_be_decrypted() throws GeneralSecurityException {
            Map<String, DictionaryProperty> properties = new HashMap<>();
            properties.put("MY_PROP", new DictionaryProperty(DATA_ENCRYPTOR.encrypt("s3cr3t-api-key"), true));
            properties.put("BROKEN", new DictionaryProperty("***", true));
            properties.put("PLAIN", new DictionaryProperty("not-a-secret", false));

            cut.deploy(dictionary(properties));

            assertThat(cut.getDictionaries(ENV).get("idp-server-details")).containsOnly(
                entry("MY_PROP", "s3cr3t-api-key"),
                entry("BROKEN", "***"),
                entry("PLAIN", "not-a-secret")
            );
        }
    }

    @Nested
    class UndeployTest {

        @Test
        void should_undeploy_when_occupant_id_matches() {
            Dictionary console = dictionary("idp-server-details", null, ENV, "first-value", 1L);
            cut.deploy(console);

            cut.undeploy(console);

            assertThat(cut.getDictionaries(ENV)).isNullOrEmpty();
        }

        @Test
        void should_keep_incumbent_when_rejected_dictionary_is_undeployed() {
            cut.deploy(dictionary("idp-server-details", null, ENV, "first-value", 1L));
            Dictionary terraform = dictionary("uuid-tf", "idp-server-details", ENV, "second-value", 2L);
            cut.deploy(terraform);

            cut.undeploy(terraform);

            assertThat(property(ENV, "idp-server-details")).isEqualTo("first-value");
        }

        @Test
        void should_keep_occupant_when_undeploy_targets_a_different_dictionary() {
            cut.deploy(dictionary("uuid-tf", "idp-server-details", ENV, "tf-value", 1L));

            cut.undeploy(dictionary("idp-server-details", null, ENV, "first-value", 1L));

            assertThat(property(ENV, "idp-server-details")).isEqualTo("tf-value");
        }

        @Test
        void should_keep_the_other_dictionary_when_one_of_two_is_undeployed() {
            cut.deploy(dictionary("idp-server-details", null, ENV, "first-value", 1L));
            Dictionary terraform = dictionary("uuid-tf", "tf_idp-server-details", ENV, "tf-value", 2L);
            cut.deploy(terraform);

            cut.undeploy(terraform);

            assertThat(property(ENV, "idp-server-details")).isEqualTo("first-value");
            assertThat(cut.getDictionaries(ENV)).doesNotContainKey("tf_idp-server-details");
        }
    }

    private String property(String environmentId, String runtimeKey) {
        Map<String, Map<String, String>> envValues = cut.getDictionaries(environmentId);
        assertThat(envValues).isNotNull();
        assertThat(envValues).containsKey(runtimeKey);
        return envValues.get(runtimeKey).get("MY_PROP");
    }

    private static Dictionary dictionary(Map<String, DictionaryProperty> properties) {
        Dictionary dictionary = dictionary("idp-server-details", null, ENV, "unused", 1L);
        dictionary.setProperties(properties);
        return dictionary;
    }

    private static Dictionary dictionary(String id, String key, String environmentId, String propertyValue, long deployedAt) {
        Dictionary dictionary = new Dictionary();
        dictionary.setId(id);
        dictionary.setKey(key);
        dictionary.setEnvironmentId(environmentId);
        dictionary.setName(id);
        dictionary.setDeployedAt(new Date(deployedAt));
        dictionary.setProperties(Map.of("MY_PROP", new DictionaryProperty(propertyValue, false)));
        return dictionary;
    }
}
