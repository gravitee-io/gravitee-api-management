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
package io.gravitee.apim.core.api.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.gravitee.definition.model.PathMapping;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.listener.subscription.SubscriptionListener;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HttpListenerPathMappingsTest {

    @Test
    void should_return_first_http_listener_mappings() {
        var httpListener = HttpListener.builder()
            .paths(List.of(new Path("path")))
            .pathMappings(new LinkedHashSet<>(List.of("/pets", "/pets/:petId")))
            .build();

        var result = HttpListenerPathMappings.firstHttpListenerMappings(List.of(httpListener));

        assertThat(result).hasValueSatisfying(mappings -> assertThat(mappings).containsExactly("/pets", "/pets/:petId"));
    }

    @Test
    void should_return_empty_when_no_http_listener() {
        var subListener = new SubscriptionListener();
        subListener.setEntrypoints(List.of(Entrypoint.builder().type("webhook").build()));

        var result = HttpListenerPathMappings.firstHttpListenerMappings(List.of(subListener));

        assertThat(result).isEmpty();
    }

    @Test
    void should_return_empty_when_mappings_are_null() {
        var httpListener = HttpListener.builder().paths(List.of(new Path("path"))).build();

        var result = HttpListenerPathMappings.firstHttpListenerMappings(List.of(httpListener));

        assertThat(result).isEmpty();
    }

    @Test
    void should_replace_only_first_http_listener_mappings() {
        var httpListener1 = HttpListener.builder()
            .paths(List.of(new Path("path1")))
            .pathMappings(new LinkedHashSet<>(Set.of("/existing")))
            .build();
        var httpListener2 = HttpListener.builder()
            .paths(List.of(new Path("path2")))
            .pathMappings(new LinkedHashSet<>(Set.of("/keep")))
            .build();
        var subListener = new SubscriptionListener();
        subListener.setEntrypoints(List.of(Entrypoint.builder().type("webhook").build()));

        var result = HttpListenerPathMappings.withFirstHttpListenerMappings(
            List.of(httpListener1, httpListener2, subListener),
            Set.of("/new")
        );

        assertSoftly(softly -> {
            softly.assertThat(result).hasSize(3);
            softly
                .assertThat(result.get(0))
                .isInstanceOfSatisfying(HttpListener.class, hl -> {
                    softly.assertThat(hl.getPathMappings()).containsExactly("/new");
                });
            softly.assertThat(result.get(1)).isSameAs(httpListener2);
            softly.assertThat(result.get(2)).isSameAs(subListener);
        });
    }

    @Test
    void should_keep_paths_cors_and_entrypoints_of_first_http_listener() {
        var httpListener = HttpListener.builder()
            .paths(List.of(new Path("path")))
            .pathMappings(new LinkedHashSet<>(Set.of("/old")))
            .entrypoints(List.of(Entrypoint.builder().type("http-proxy").build()))
            .build();

        var result = HttpListenerPathMappings.withFirstHttpListenerMappings(List.of(httpListener), Set.of("/new"));

        assertThat(result)
            .singleElement()
            .isInstanceOfSatisfying(HttpListener.class, hl -> {
                assertSoftly(softly -> {
                    softly.assertThat(hl.getPaths()).hasSize(1);
                    softly.assertThat(hl.getEntrypoints()).hasSize(1);
                    softly.assertThat(hl.getPathMappings()).containsExactly("/new");
                });
            });
    }

    @Test
    void should_not_mutate_input_list() {
        var httpListener = HttpListener.builder()
            .paths(List.of(new Path("path")))
            .pathMappings(new LinkedHashSet<>(Set.of("/original")))
            .build();
        var inputList = List.<Listener>of(httpListener);

        HttpListenerPathMappings.withFirstHttpListenerMappings(inputList, Set.of("/new"));

        assertThat(((HttpListener) inputList.get(0)).getPathMappings()).containsExactly("/original");
    }

    @Test
    void should_build_patterns_with_runtime_semantics() {
        var httpListener = HttpListener.builder()
            .paths(List.of(new Path("path")))
            .pathMappings(new LinkedHashSet<>(Set.of("/old")))
            .build();

        var result = HttpListenerPathMappings.withFirstHttpListenerMappings(List.of(httpListener), Set.of("/a/:id"));

        assertThat(result)
            .singleElement()
            .isInstanceOfSatisfying(HttpListener.class, hl -> {
                var expected = PathMapping.buildPattern("/a/:id").pattern();
                assertThat(hl.getPathMappingsPattern()).containsKey("/a/:id");
                assertThat(hl.getPathMappingsPattern().get("/a/:id").pattern()).isEqualTo(expected);
            });
    }
}
