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
package io.gravitee.gateway.reactive.reactor.path;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Holds {@link RequestPathNormalizer#needsNormalization(String)} and
 * {@link RequestPathNormalizer#normalize(String)} to the same rules.
 *
 * <p>The two answer the same question by different means: one decides in a single scan, the other
 * by rewriting. That is the point — the scan is what makes {@code REJECT} cheap and gives
 * {@code NORMALIZE} its fast path — and it is also the risk, because two implementations of one
 * rule drift as soon as one is touched alone. The drift would be silent and would fail open: a scan
 * that wrongly answers "nothing to do" leaves a path unnormalized and routed on as received.
 *
 * <p>So the agreement is asserted rather than reviewed, over paths assembled from a vocabulary of
 * segment kinds rather than from a hand-written list, which is what makes the coverage broader than
 * anyone's imagination of the edge cases. The seed is fixed, so a failure is reproducible and names
 * the exact path.
 *
 * <p>No property-testing library is used: none is available in this repository, and pulling one in
 * for a single invariant is not worth the dependency. What is lost is shrinking — a failing case is
 * reported as generated rather than reduced — which these paths are short enough to survive.
 *
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RequestPathNormalizerPropertyTest {

    private static final long SEED = 20260814L;
    private static final int GENERATED_PATHS = 20_000;
    private static final int MAX_SEGMENTS = 6;

    /**
     * Every kind of segment worth combining, including the ones that must <em>not</em> trigger a
     * normalization — an ordinary segment carrying a dot, and a reserved character left encoded.
     */
    private static final String[] SEGMENT_KINDS = {
        "orders",
        "12345.json",
        "a.b.c",
        ".hidden",
        "..config",
        ".",
        "..",
        "%2e",
        "%2E",
        "%2e%2e",
        ".%2e",
        "%41",
        "%7e",
        "%2d",
        "%2F",
        "%3F",
        "%23",
        "a%20b",
        "%zz",
        "%2",
        "%",
        "",
    };

    @Test
    void should_agree_with_normalize_on_every_generated_path() {
        final List<String> disagreements = new ArrayList<>();
        int needing = 0;
        int canonical = 0;

        for (final String path : generatePaths()) {
            final String normalized = RequestPathNormalizer.normalize(path);
            final boolean wouldChange = normalized == null || !normalized.equals(path);

            if (wouldChange) {
                needing++;
            } else {
                canonical++;
            }
            if (RequestPathNormalizer.needsNormalization(path) != wouldChange) {
                disagreements.add(path + "  -> normalize=" + normalized + ", needsNormalization=" + !wouldChange);
            }
        }

        assertThat(disagreements).as("needsNormalization(p) must equal !p.equals(normalize(p)) for every path").isEmpty();

        // A corpus that landed on one side only would make the assertion above pass without ever
        // exercising it. Both branches have to be walked for the property to mean anything.
        assertThat(needing).as("paths needing normalization in the corpus").isGreaterThan(1_000);
        assertThat(canonical).as("already canonical paths in the corpus").isGreaterThan(1_000);
    }

    /**
     * The generated corpus is broad but blind; these are the shapes the decision was written for,
     * named one by one so a regression on them is reported as itself rather than counted.
     */
    @ParameterizedTest
    @ValueSource(
        strings = {
            "/",
            "/v1/orders/list",
            "/v1/orders/12345.json",
            "/a/b%2Fc",
            "/alpha/api/../../beta/api/echo",
            "/alpha/api/%2e%2e/%2e%2e/beta/api/echo",
            "/a//b",
            "/a/./b",
            "/a/b/..",
            "/../../x",
            "/a%",
            "/a%zz",
            "relative/path",
            "",
        }
    )
    void should_agree_with_normalize_on_the_paths_that_matter_most(final String path) {
        final String normalized = RequestPathNormalizer.normalize(path);
        final boolean wouldChange = normalized == null || !normalized.equals(path);

        assertThat(RequestPathNormalizer.needsNormalization(path)).as("normalized [%s]", normalized).isEqualTo(wouldChange);
    }

    /**
     * Stated separately from the equivalence, because this is the reason the scan exists: these are
     * the paths a gateway sees all day, and they must cost one pass and nothing more.
     */
    @ParameterizedTest
    @ValueSource(
        strings = { "/v1/orders/list", "/v1/orders/12345.json", "/a/b%2Fc", "/api/v2/customers/8f3a/orders/2026/08/13/items/447/details" }
    )
    void should_leave_the_ordinary_shapes_alone(final String path) {
        assertThat(RequestPathNormalizer.needsNormalization(path)).isFalse();
    }

    private static List<String> generatePaths() {
        final Random random = new Random(SEED);
        final List<String> paths = new ArrayList<>(GENERATED_PATHS);

        for (int i = 0; i < GENERATED_PATHS; i++) {
            final StringBuilder path = new StringBuilder();
            // One path in ten has no leading slash, which is a case of its own.
            if (random.nextInt(10) != 0) {
                path.append('/');
            }
            final int segments = 1 + random.nextInt(MAX_SEGMENTS);
            for (int s = 0; s < segments; s++) {
                if (s > 0) {
                    path.append('/');
                }
                path.append(SEGMENT_KINDS[random.nextInt(SEGMENT_KINDS.length)]);
            }
            // One path in five ends on a separator.
            if (random.nextInt(5) == 0) {
                path.append('/');
            }
            paths.add(path.toString());
        }
        return paths;
    }
}
