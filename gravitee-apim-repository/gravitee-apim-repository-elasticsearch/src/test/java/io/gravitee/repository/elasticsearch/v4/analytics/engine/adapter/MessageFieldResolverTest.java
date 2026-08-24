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
package io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.repository.analytics.engine.api.query.Facet;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MessageFieldResolverTest {

    private final MessageFieldResolver resolver = new MessageFieldResolver();

    @Test
    void should_resolve_the_dimensions_a_message_document_carries() {
        assertThat(resolver.fromFacet(Facet.MESSAGE_OPERATION_TYPE)).isEqualTo("operation");
        assertThat(resolver.fromFacet(Facet.MESSAGE_CONNECTOR_TYPE)).isEqualTo("connector-type");
        assertThat(resolver.fromFacet(Facet.MESSAGE_CONNECTOR_ID)).isEqualTo("connector-id");
        assertThat(resolver.fromFacet(Facet.API)).isEqualTo("api-id");
        assertThat(resolver.fromFacet(Facet.GATEWAY)).isEqualTo("gateway");
    }

    /**
     * These live on the connection document, not on the message. Resolving them to their HTTP field
     * names would aggregate on a field this index does not have — which Elasticsearch answers with
     * no buckets rather than an error, turning a broken breakdown into an empty chart.
     */
    @ParameterizedTest
    @EnumSource(value = Facet.class, names = { "APPLICATION", "PLAN", "TENANT", "ZONE" })
    void should_refuse_a_dimension_the_message_index_does_not_carry(Facet facet) {
        assertThatThrownBy(() -> resolver.fromFacet(facet))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining(facet.name());
    }
}
