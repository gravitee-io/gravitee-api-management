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
package io.gravitee.gateway.reactor.handler.index;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.common.event.EventManager;
import io.gravitee.common.event.impl.EventManagerImpl;
import io.gravitee.gateway.reactor.accesspoint.ReactableAccessPoint;
import io.gravitee.gateway.reactor.handler.DefaultHttpAcceptor;
import io.gravitee.gateway.reactor.handler.HttpAcceptor;
import io.gravitee.gateway.reactor.handler.HttpAcceptorFactory;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.reactor.handler.http.AccessPointHttpAcceptor;
import io.gravitee.gateway.reactor.handler.index.HttpAcceptorCorpus.Mode;
import io.gravitee.gateway.reactor.handler.index.HttpAcceptorCorpus.Request;
import io.gravitee.gateway.reactor.handler.index.HttpAcceptorCorpus.Shape;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Confronts the index with the scan it replaces, on a generated population, and requires that they
 * elect the same API for every request.
 *
 * <p>Agreement is judged on the reactor and the context path of the winner rather than on its identity.
 * Those two are what the dispatcher consumes, and they are what tells an operator which API answered.
 * The distinction matters for access point acceptors: the scan returns the composite, the index returns
 * the inner acceptor that actually matched, and both carry the same reactor and the same path.
 *
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HttpAcceptorResolutionDifferentialTest {

    private static final long SEED = 20260817L;
    private static final int POPULATION = 500;

    private static HttpAcceptorIndex indexOf(HttpAcceptorCorpus corpus, Mode mode) {
        HttpAcceptorIndex index = mode.overlapping ? new OverlappingHttpAcceptorIndex() : new DefaultHttpAcceptorIndex();
        corpus.acceptors().forEach(index::add);
        return index;
    }

    private static String describe(HttpAcceptor acceptor) {
        return acceptor == null ? "no acceptor" : acceptor.reactor() + " on " + acceptor.path();
    }

    private static void assertAgreementOn(HttpAcceptorCorpus corpus, Mode mode) {
        LinearHttpAcceptorResolver linear = new LinearHttpAcceptorResolver(corpus.acceptors());
        HttpAcceptorIndex index = indexOf(corpus, mode);
        List<String> divergences = new ArrayList<>();

        for (Request request : corpus.requests()) {
            HttpAcceptor expected = linear.resolve(request.host(), request.path(), request.serverId());
            HttpAcceptor actual = index.resolve(request.host(), request.path(), request.serverId());
            if (!describe(expected).equals(describe(actual))) {
                divergences.add(
                    "host=%s path=%s serverId=%s: scan elected %s, index elected %s".formatted(
                        request.host(),
                        request.path(),
                        request.serverId(),
                        describe(expected),
                        describe(actual)
                    )
                );
            }
        }

        assertThat(divergences)
            .describedAs("mode=%s seed=%d acceptors=%d requests=%d", mode, corpus.seed(), index.size(), corpus.requests().size())
            .isEmpty();
    }

    @Nested
    class Non_overlapping_mode {

        @ParameterizedTest
        @EnumSource(value = Shape.class, names = { "NO_HOST", "VHOST", "MIXED" })
        void should_elect_the_same_acceptor_as_the_scan(Shape shape) {
            // Given
            HttpAcceptorCorpus corpus = new HttpAcceptorCorpus(SEED, Mode.DEFAULT, shape, POPULATION);

            // When / Then
            assertAgreementOn(corpus, Mode.DEFAULT);
        }
    }

    @Nested
    class Overlapping_mode {

        @ParameterizedTest
        @EnumSource(Shape.class)
        void should_elect_the_same_acceptor_as_the_scan(Shape shape) {
            // Given
            HttpAcceptorCorpus corpus = new HttpAcceptorCorpus(SEED, Mode.OVERLAPPING, shape, POPULATION);

            // When / Then
            assertAgreementOn(corpus, Mode.OVERLAPPING);
        }
    }

    @Nested
    class Across_several_seeds {

        @Test
        void should_agree_on_every_generated_population() {
            // Given / When / Then
            for (long seed = 1; seed <= 20; seed++) {
                for (Mode mode : Mode.values()) {
                    assertAgreementOn(new HttpAcceptorCorpus(seed, mode, Shape.MIXED, 200), mode);
                }
            }
        }
    }

    /**
     * Behaviours the index does not reproduce. They are pinned here rather than left to be discovered,
     * and each is a pre-existing defect of the scan rather than a shortcut taken by the index.
     */
    @Nested
    class Known_divergences {

        @Test
        void should_not_reproduce_the_exact_match_on_an_unnormalised_context_path() {
            // Given an acceptor declared with a duplicated slash. AbstractHttpAcceptor derives
            // pathWithoutTrailingSlash from the raw declaration but path from the normalised one, so the
            // acceptor ends up answering for "/a/b/..." and for the literal "/a//b", yet not for "/a/b".
            HttpAcceptor acceptor = new DefaultHttpAcceptor("/a//b");
            LinearHttpAcceptorResolver linear = new LinearHttpAcceptorResolver(List.of(acceptor));
            HttpAcceptorIndex index = new DefaultHttpAcceptorIndex();
            index.add(acceptor);

            // When / Then: the index only knows the normalised path, so it loses the literal match
            assertThat(acceptor.path()).isEqualTo("/a/b/");
            assertThat(linear.resolve(null, "/a//b", null)).isSameAs(acceptor);
            assertThat(index.resolve(null, "/a//b", null)).isNull();

            // Everywhere else the two agree, including on the exact form the acceptor already rejects
            assertThat(linear.resolve(null, "/a/b/c", null)).isSameAs(acceptor);
            assertThat(index.resolve(null, "/a/b/c", null)).isSameAs(acceptor);
            assertThat(linear.resolve(null, "/a/b", null)).isNull();
            assertThat(index.resolve(null, "/a/b", null)).isNull();
        }

        @Test
        void should_honour_the_most_specific_context_path_where_the_scan_misorders_a_multi_host_composite() {
            // Given an API exposed on two access points, deployed on a longer context path than a second
            // API sharing one of those hosts. The composite is sorted by its FIRST host but accepts for
            // both, so the scan reaches the other API first and returns the shorter context path.
            EventManager eventManager = new EventManagerImpl();
            HttpAcceptorFactory factory = new HttpAcceptorFactory(true);
            ReactorHandler onTwoAccessPoints = new HttpAcceptorCorpus.NamedReactorHandler("api-on-two-access-points");
            ReactorHandler onSharedHost = new HttpAcceptorCorpus.NamedReactorHandler("api-on-shared-host");

            HttpAcceptor composite = new AccessPointHttpAcceptor(
                eventManager,
                factory,
                "environment",
                List.of(accessPoint("aaa.acme.com"), accessPoint("zzz.acme.com")),
                "/deep/path",
                onTwoAccessPoints,
                null
            );
            HttpAcceptor shorter = factory.create("zzz.acme.com", "/deep", onSharedHost, null);

            LinearHttpAcceptorResolver linear = new LinearHttpAcceptorResolver(List.of(composite, shorter));
            HttpAcceptorIndex index = new OverlappingHttpAcceptorIndex();
            index.add(composite);
            index.add(shorter);

            // When / Then
            assertThat(linear.resolve("zzz.acme.com", "/deep/path/orders", null)).isSameAs(shorter);
            assertThat(index.resolve("zzz.acme.com", "/deep/path/orders", null))
                .describedAs("the index places each inner acceptor under the host it answers for")
                .extracting(HttpAcceptor::reactor)
                .isSameAs(onTwoAccessPoints);
        }

        private ReactableAccessPoint accessPoint(String host) {
            return ReactableAccessPoint.builder()
                .id("access-point-" + host)
                .environmentId("environment")
                .host(host)
                .target(ReactableAccessPoint.Target.GATEWAY)
                .build();
        }

        @Test
        void should_answer_no_acceptor_instead_of_throwing_on_a_null_path() {
            // Given
            HttpAcceptor acceptor = new DefaultHttpAcceptor("/store");
            HttpAcceptorIndex index = new DefaultHttpAcceptorIndex();
            index.add(acceptor);

            // When / Then
            assertThat(index.resolve(null, null, null)).isNull();
        }
    }
}
