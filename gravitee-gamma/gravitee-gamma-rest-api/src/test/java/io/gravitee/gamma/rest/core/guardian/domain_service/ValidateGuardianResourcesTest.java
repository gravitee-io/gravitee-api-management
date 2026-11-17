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
package io.gravitee.gamma.rest.core.guardian.domain_service;

import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.ENVIRONMENT_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.INSTALLED_RESOURCE_TYPE;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aResourceCatalog;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aResourcePluginProvider;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aResourceType;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aResourceValidator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import io.gravitee.apim.core.validation.Validator;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.gamma.rest.core.gravitee_plugin.inmemory.InMemoryResourceValidation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The resources a Guardian holds are judged as an API's are — each enabled one against the schema of its
 * plugin, a disabled one not at all — with two rules of the Guardian's own: the type has to be one the
 * node installed, and no two enabled resources share a name.
 */
class ValidateGuardianResourcesTest {

    private static final String TYPE = INSTALLED_RESOURCE_TYPE;

    /** A second resource type the node has, so two resources can be told apart by what their plugin answers. */
    private static final String OTHER_TYPE = "ai-model-text-classification";

    private static final String CORRECTED = "{\"corrected\":true}";
    private static final String REQUIRED = "$.model: is missing but it is required";
    private static final String ONLY_CORRECTED =
        "Resource [%s] has a configuration its plugin only accepts corrected: edit and save the Guardian to store the configuration the plugin accepts.";

    /** The answers of the resource plugins, which a test installs to make a resource conforming, corrected or refused. */
    private final InMemoryResourceValidation resourceValidation = new InMemoryResourceValidation();

    private final ValidateGuardianResourcesDomainService validator = aResourceValidator(
        aResourcePluginProvider(() -> Set.of(TYPE, OTHER_TYPE)),
        resourceValidation
    );

    @Test
    void should_accept_a_resource_the_plugin_schema_configures() {
        assertThat(refusalsOf(aResource("classifier"))).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(Correction.class)
    void should_accept_a_guardian_holding_no_resource(Correction correction) {
        assertThat(validator.validate(List.of(), ENVIRONMENT_ID, correction)).isEmpty();
        assertThat(validator.validate(null, ENVIRONMENT_ID, correction)).isEmpty();
        assertThat(resourceValidation.checks()).isEmpty();
    }

    /** As for a step: the schema error is what tells the user which property to fix. */
    @Test
    void should_name_a_resource_whose_configuration_the_schema_refuses_and_say_why() {
        resourceValidation.givenRefuses(TYPE, REQUIRED);

        assertThat(refusalsOf(aResource("classifier"))).containsExactly(
            "Resource [classifier] has an invalid configuration: $.model: is missing but it is required."
        );
    }

    /** Mirrors what a step gets for an uninstalled policy: named, and refused before deployment. */
    @Test
    void should_refuse_a_resource_whose_type_the_node_does_not_have() {
        assertThat(refusalsOf(aResource("classifier", "absent-from-the-node"))).containsExactly(
            "Resource [classifier] references resource type [absent-from-the-node], which is not installed."
        );
        assertThat(resourceValidation.checks()).isEmpty();
    }

    /**
     * The catalog lists such a type with {@code deployed: false} and the console does not offer it: the
     * validation refuses what the catalog says cannot be picked.
     */
    @Test
    void should_refuse_a_resource_whose_type_the_node_or_the_license_holds_back() {
        var heldBack = aResourceValidator(aResourceCatalog(() -> Set.of(aResourceType(TYPE, false))), resourceValidation);

        var refusals = messages(heldBack.validate(List.of(aResource("classifier")), ENVIRONMENT_ID, Correction.REFUSE));

        assertThat(refusals).containsExactly(
            "Resource [classifier] references resource type [" +
                TYPE +
                "], which is not available: the node did not deploy it, or the license of the organization does not enable it."
        );
        assertThat(resourceValidation.checks()).isEmpty();
    }

    /** Nothing is asked of the catalog, nor of the environment, for a Guardian whose resources are all switched off. */
    @Test
    void should_not_read_the_catalog_when_no_resource_is_enabled() {
        var catalog = aResourcePluginProvider(() -> Set.of(TYPE));
        var onlyDisabled = aResourceValidator(catalog, resourceValidation);

        onlyDisabled.validate(List.of(aDisabledResource("classifier", TYPE)), ENVIRONMENT_ID, Correction.REFUSE);

        verifyNoInteractions(catalog);
    }

    /** Only a write goes through the bean validation, so a stored resource may carry no type at all. */
    @Test
    void should_refuse_a_stored_resource_that_carries_no_type() {
        assertThat(refusalsOf(new Resource("classifier", null, "{}", true))).containsExactly("Resource [classifier] has no type.");
    }

    /** As a null step is: silently dropping it would store an entry the definition model cannot read. */
    @Test
    void should_refuse_a_null_entry_among_the_resources() {
        var resources = new ArrayList<Resource>();
        resources.add(null);

        assertThat(messages(validator.validate(resources, ENVIRONMENT_ID, Correction.REFUSE))).containsExactly(
            "Guardian resources must not contain null entries."
        );
    }

    /** The gateway builds a resource from an object, and a plugin with no schema would let anything else through. */
    @ParameterizedTest
    @ValueSource(strings = { "[1]", "\"text\"", "42", "not json" })
    void should_refuse_a_configuration_that_is_not_a_json_object(String configuration) {
        assertThat(refusalsOf(new Resource("classifier", TYPE, configuration, true))).containsExactly(
            "Resource [classifier] has an invalid configuration: it must be a JSON object."
        );
        assertThat(resourceValidation.checks()).isEmpty();
    }

    /** The gateway keeps the resources of a Guardian by name: the second would silently replace the first. */
    @Nested
    class A_name_shared_by_enabled_resources {

        @Test
        void should_be_refused_once_however_many_share_it() {
            var refusals = refusalsOf(aResource("classifier"), aResource("classifier", OTHER_TYPE), aResource("classifier"));

            assertThat(refusals).containsExactly("Resource name [classifier] is used by more than one enabled resource.");
        }

        @Test
        void should_not_count_a_disabled_resource() {
            assertThat(refusalsOf(aResource("classifier"), aDisabledResource("classifier", OTHER_TYPE))).isEmpty();
        }

        /** The gateway compares names exactly, so these are two resources to it. */
        @Test
        void should_tell_apart_names_that_differ_by_case() {
            assertThat(refusalsOf(aResource("classifier"), aResource("Classifier"))).isEmpty();
        }
    }

    @Nested
    class Disabled {

        /** Parity with an API, which filters on isEnabled before validating anything. */
        @Test
        void should_not_read_the_configuration_of_a_disabled_resource() {
            resourceValidation.givenRefuses(TYPE, REQUIRED);

            assertThat(refusalsOf(aDisabledResource("classifier", TYPE))).isEmpty();
            assertThat(resourceValidation.checks()).isEmpty();
        }

        @Test
        void should_not_read_the_type_of_a_disabled_resource() {
            assertThat(refusalsOf(aDisabledResource("classifier", "absent-from-the-node"))).isEmpty();
        }
    }

    @Nested
    class A_configuration_the_plugin_only_accepts_corrected {

        /** A path that publishes or restores a stored definition may not rewrite it. */
        @Test
        void should_be_refused_and_left_as_it_is_stored_when_it_may_not_be_rewritten() {
            resourceValidation.givenRewrites(TYPE, CORRECTED);
            var resource = aResource("classifier");

            assertThat(refusalsOf(resource)).containsExactly(ONLY_CORRECTED.formatted("classifier"));
            assertThat(resource.getConfiguration()).isEqualTo("{}");
        }

        /** On a write path the opposite holds: what is stored has to be what the plugin validated. */
        @Test
        void should_be_stored_corrected_on_a_write_path() {
            resourceValidation.givenRewrites(TYPE, CORRECTED);
            var resource = aResource("classifier");

            assertThat(validator.validate(List.of(resource), ENVIRONMENT_ID, Correction.STORE)).isEmpty();
            assertThat(resource.getConfiguration()).isEqualTo(CORRECTED);
        }

        /** The same document written another way is not a correction, as for a step. */
        @Test
        void should_not_be_one_when_only_its_writing_differs() {
            resourceValidation.givenRewrites(TYPE, "{\"threshold\":0.8}");

            assertThat(refusalsOf(new Resource("classifier", TYPE, "{ \"threshold\" : 0.8 }", true))).isEmpty();
        }

        /** One reason per resource: a refusal is not also reported as a correction. */
        @Test
        void should_not_be_reported_beside_a_refusal_of_the_same_resource() {
            resourceValidation.givenRefuses(TYPE, REQUIRED).givenRewrites(TYPE, CORRECTED);

            assertThat(refusalsOf(aResource("classifier"))).singleElement().asString().contains(REQUIRED);
        }
    }

    @Nested
    class No_configuration {

        /** A resource with nothing in it still has a schema to answer to, as a step does. */
        @ParameterizedTest
        @ValueSource(strings = { "", "  " })
        void should_be_judged_as_an_empty_object_when_blank(String configuration) {
            refusalsOf(new Resource("classifier", TYPE, configuration, true));

            assertThat(resourceValidation.checks()).containsExactly(new InMemoryResourceValidation.Check(TYPE, "{}"));
        }

        @Test
        void should_be_judged_as_an_empty_object_when_absent() {
            refusalsOf(aResourceWithoutConfiguration("classifier"));

            assertThat(resourceValidation.checks()).containsExactly(new InMemoryResourceValidation.Check(TYPE, "{}"));
        }

        @Test
        void should_be_refused_once_by_a_schema_that_requires_something() {
            resourceValidation.givenRefuses(TYPE, REQUIRED);

            assertThat(refusalsOf(aResourceWithoutConfiguration("classifier"))).hasSize(1);
        }

        /** A stored resource that never carried one deploys: the plugin accepting {@code {}} corrected nothing. */
        @Test
        void should_pass_when_the_plugin_accepts_an_empty_object() {
            assertThat(refusalsOf(aResourceWithoutConfiguration("classifier"))).isEmpty();
        }

        @Test
        void should_stay_absent_on_a_write_path_when_the_plugin_accepts_an_empty_object() {
            var resource = aResourceWithoutConfiguration("classifier");

            validator.validate(List.of(resource), ENVIRONMENT_ID, Correction.STORE);

            assertThat(resource.getConfiguration()).isNull();
        }
    }

    @Nested
    class Several {

        @Test
        void should_report_each_resource_for_its_own_reason() {
            resourceValidation.givenRewrites(TYPE, CORRECTED);

            assertThat(refusalsOf(aResource("absent", "absent-from-the-node"), aResource("classifier"))).containsExactly(
                "Resource [absent] references resource type [absent-from-the-node], which is not installed.",
                ONLY_CORRECTED.formatted("classifier")
            );
        }

        /** Each resource is judged by its own plugin: a correction lands on the one it was made for. */
        @Test
        void should_store_a_correction_on_the_resource_it_was_made_for() {
            resourceValidation.givenRewrites(TYPE, CORRECTED);
            var untouched = aResource("embedder", OTHER_TYPE);
            var corrected = aResource("classifier");

            validator.validate(List.of(untouched, corrected), ENVIRONMENT_ID, Correction.STORE);

            assertThat(untouched.getConfiguration()).isEqualTo("{}");
            assertThat(corrected.getConfiguration()).isEqualTo(CORRECTED);
        }
    }

    /** What a path that may not rewrite the stored definition refuses these resources for. */
    private List<String> refusalsOf(Resource... resources) {
        return messages(validator.validate(List.of(resources), ENVIRONMENT_ID, Correction.REFUSE));
    }

    private static List<String> messages(List<Validator.Error> errors) {
        return errors.stream().map(Validator.Error::getMessage).toList();
    }

    private static Resource aResource(String name) {
        return aResource(name, TYPE);
    }

    private static Resource aResource(String name, String type) {
        return new Resource(name, type, "{}", true);
    }

    private static Resource aDisabledResource(String name, String type) {
        return new Resource(name, type, "{}", false);
    }

    private static Resource aResourceWithoutConfiguration(String name) {
        return new Resource(name, TYPE, null, true);
    }
}
