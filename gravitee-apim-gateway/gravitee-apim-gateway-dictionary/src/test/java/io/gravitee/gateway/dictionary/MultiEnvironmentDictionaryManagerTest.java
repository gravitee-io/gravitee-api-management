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

import io.gravitee.gateway.dictionary.model.Dictionary;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MultiEnvironmentDictionaryManagerTest {

    private static final String ENV = "DEFAULT";
    private static final String OTHER_ENV = "OTHER";

    private MultiEnvironmentDictionaryManager cut;

    @BeforeEach
    void setUp() {
        cut = new MultiEnvironmentDictionaryManager();
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

    private static Dictionary dictionary(String id, String key, String environmentId, String propertyValue, long deployedAt) {
        Dictionary dictionary = new Dictionary();
        dictionary.setId(id);
        dictionary.setKey(key);
        dictionary.setEnvironmentId(environmentId);
        dictionary.setName(id);
        dictionary.setDeployedAt(new Date(deployedAt));
        dictionary.setProperties(Map.of("MY_PROP", propertyValue));
        return dictionary;
    }
}
