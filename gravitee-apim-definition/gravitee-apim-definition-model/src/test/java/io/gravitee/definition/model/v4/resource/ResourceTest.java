package io.gravitee.definition.model.v4.resource;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ResourceTest {

    @Nested
    class IsValidReferenceOrInline {

        @Test
        void should_be_valid_when_only_reference_id_is_set() {
            var resource = Resource.builder().id("env-resource-id").build();
            assertThat(resource.isValidReferenceOrInline()).isTrue();
        }

        @Test
        void should_be_valid_when_only_inline_fields_are_set() {
            var resource = Resource.builder().name("my-resource").type("cache").configuration("{}").build();
            assertThat(resource.isValidReferenceOrInline()).isTrue();
        }

        @Test
        void should_be_valid_when_both_reference_and_inline_are_set() {
            var resource = Resource.builder().id("env-resource-id").name("my-resource").type("cache").configuration("{}").build();
            assertThat(resource.isValidReferenceOrInline()).isTrue();
        }

        @Test
        void should_be_invalid_when_neither_reference_nor_inline_is_set() {
            var resource = Resource.builder().build();
            assertThat(resource.isValidReferenceOrInline()).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = { "   ", "\t", "\n", "\r" })
        @NullAndEmptySource
        void should_be_invalid_when_id_is_missing(String id) {
            var resource = Resource.builder().id(id).build();
            assertThat(resource.isValidReferenceOrInline()).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = { "   ", "\t", "\n", "\r" })
        @NullAndEmptySource
        void should_be_invalid_when_inline_name_is_missing(String name) {
            var resource = Resource.builder().type("cache").name(name).configuration("{}").build();
            assertThat(resource.isValidReferenceOrInline()).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = { "   ", "\t", "\n", "\r" })
        @NullAndEmptySource
        void should_be_invalid_when_inline_type_is_missing(String type) {
            var resource = Resource.builder().name("my-resource").type(type).configuration("{}").build();
            assertThat(resource.isValidReferenceOrInline()).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = { "   ", "\t", "\n", "\r" })
        @NullAndEmptySource
        void should_be_invalid_when_inline_configuration_is_missing(String configuration) {
            var resource = Resource.builder().name("my-resource").type("cache").configuration(configuration).build();
            assertThat(resource.isValidReferenceOrInline()).isFalse();
        }
    }
}
