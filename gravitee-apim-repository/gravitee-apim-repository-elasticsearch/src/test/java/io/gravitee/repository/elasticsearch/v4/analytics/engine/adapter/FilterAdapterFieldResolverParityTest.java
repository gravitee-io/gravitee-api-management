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

import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.api.FieldResolver;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Parity contract between the {@link FilterAdapter} allow-lists and the {@link FieldResolver}
 * implementations. A filter whose name is resolvable to an ES field but absent from the allow-list
 * of its scope is silently dropped by {@code adaptFor*}: the query succeeds and the filter has no
 * effect, corrupting analytics results without any error.
 *
 * @author GraviteeSource Team
 */
class FilterAdapterFieldResolverParityTest {

    private static final FieldResolver HTTP_RESOLVER = new HTTPFieldResolver();
    private static final FieldResolver MESSAGE_RESOLVER = new MessageFieldResolver();
    private static final FieldResolver NATIVE_RESOLVER = new NativeApiFieldResolver();

    // TODO(GMA-932): MESSAGE_SIZE / MESSAGE_COUNT / MESSAGE_ERROR_COUNT are allow-listed but not
    // resolvable by MessageFieldResolver — remove this exclusion once GMA-932 adds their cases.
    private static final Set<Filter.Name> KNOWN_UNRESOLVABLE_MESSAGE_NAMES = Set.of(
        Filter.Name.MESSAGE_SIZE,
        Filter.Name.MESSAGE_COUNT,
        Filter.Name.MESSAGE_ERROR_COUNT
    );

    @Test
    void should_resolve_every_filter_name_allow_listed_for_http() {
        assertThat(unresolvable(FilterAdapter.HTTP_FILTER_NAMES, HTTP_RESOLVER))
            .as("HTTP_FILTER_NAMES entries not resolvable by HTTPFieldResolver (query would fail at ES adaptation)")
            .isEmpty();
    }

    @Test
    void should_resolve_every_filter_name_allow_listed_for_edge() {
        assertThat(unresolvable(FilterAdapter.EDGE_FILTER_NAMES, HTTP_RESOLVER))
            .as("EDGE_FILTER_NAMES entries not resolvable by HTTPFieldResolver (query would fail at ES adaptation)")
            .isEmpty();
    }

    @Test
    void should_resolve_every_filter_name_allow_listed_for_message() {
        assertThat(unresolvable(FilterAdapter.MESSAGE_FILTER_NAMES, MESSAGE_RESOLVER))
            .as("MESSAGE_FILTER_NAMES entries not resolvable by MessageFieldResolver (query would fail at ES adaptation)")
            .isEqualTo(KNOWN_UNRESOLVABLE_MESSAGE_NAMES);
    }

    @Test
    void should_resolve_every_filter_name_allow_listed_for_native() {
        assertThat(unresolvable(FilterAdapter.NATIVE_FILTER_NAMES, NATIVE_RESOLVER))
            .as("NATIVE_FILTER_NAMES entries not resolvable by NativeApiFieldResolver (query would fail at ES adaptation)")
            .isEmpty();
    }

    @Test
    void should_allow_list_every_filter_name_resolvable_by_http_field_resolver() {
        var allowListed = new HashSet<Filter.Name>();
        allowListed.addAll(FilterAdapter.HTTP_FILTER_NAMES);
        allowListed.addAll(FilterAdapter.EDGE_FILTER_NAMES);

        var resolvableButNotAllowListed = Arrays.stream(Filter.Name.values())
            .filter(name -> resolves(HTTP_RESOLVER, name))
            .filter(name -> !allowListed.contains(name))
            .toList();

        assertThat(resolvableButNotAllowListed)
            .as(
                "Filter names resolvable by HTTPFieldResolver but absent from HTTP_FILTER_NAMES and EDGE_FILTER_NAMES: " +
                    "these filters are accepted by the API yet silently dropped from the ES query"
            )
            .isEmpty();
    }

    @Test
    void should_allow_list_every_filter_name_resolvable_by_native_field_resolver() {
        var resolvableButNotAllowListed = Arrays.stream(Filter.Name.values())
            .filter(name -> resolves(NATIVE_RESOLVER, name))
            .filter(name -> !FilterAdapter.NATIVE_FILTER_NAMES.contains(name))
            .toList();

        assertThat(resolvableButNotAllowListed)
            .as(
                "Filter names resolvable by NativeApiFieldResolver but absent from NATIVE_FILTER_NAMES: " +
                    "these filters are accepted by the API yet silently dropped from the ES query"
            )
            .isEmpty();
    }

    private static Set<Filter.Name> unresolvable(List<Filter.Name> allowList, FieldResolver resolver) {
        var unresolvable = new HashSet<Filter.Name>();
        for (var name : allowList) {
            if (!resolves(resolver, name)) {
                unresolvable.add(name);
            }
        }
        return unresolvable;
    }

    private static boolean resolves(FieldResolver resolver, Filter.Name name) {
        try {
            resolver.fromFilter(new Filter(name, Filter.Operator.EQ, "value"));
            return true;
        } catch (UnsupportedOperationException e) {
            // The one legitimate "not resolvable" signal.
            return false;
        } catch (RuntimeException e) {
            // Any other exception means the resolver is broken for this name rather than cleanly
            // rejecting it — surface which resolver/name so the parity failure stays actionable.
            throw new AssertionError(
                "%s threw %s for filter '%s' — resolvers must map a name or reject it with UnsupportedOperationException".formatted(
                    resolver.getClass().getSimpleName(),
                    e.getClass().getSimpleName(),
                    name
                ),
                e
            );
        }
    }
}
