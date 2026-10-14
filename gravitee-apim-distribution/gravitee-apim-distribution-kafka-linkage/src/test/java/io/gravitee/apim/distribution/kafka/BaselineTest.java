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
package io.gravitee.apim.distribution.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.distribution.kafka.KafkaLinkageCheck.Unresolved;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BaselineTest {

    private static final String REFERENCE = "org/apache/kafka/common/Retriever.<init>(Ljava/lang/String;)V";
    private static final Unresolved UNREACHABLE = new Unresolved(
        "gravitee-policy-protobuf-2.0.0.zip",
        "lib/schema-registry-client-7.8.0.jar",
        REFERENCE,
        "constructor-signature-changed",
        Set.of("io/confluent/Provider")
    );

    private final Baseline baseline = Baseline.parse(
        List.of(
            "# Accepted unresolved references",
            "",
            "gravitee-policy-protobuf " + REFERENCE + "  # Unreachable: the policy only uses ProtobufSchema"
        )
    );

    @Test
    void should_accept_a_listed_reference_whatever_the_plugin_version() {
        var verdict = baseline.evaluate(List.of(UNREACHABLE), Set.of("gravitee-policy-protobuf-2.0.0.zip"));

        assertThat(verdict.unexpected()).isEmpty();
        assertThat(verdict.stale()).isEmpty();
    }

    @Test
    void should_keep_a_reference_that_is_not_listed() {
        Unresolved other = new Unresolved("gravitee-policy-acl-3.0.3.zip", "acl.jar", REFERENCE, "class-gone", Set.of("Acl"));

        var verdict = baseline.evaluate(
            List.of(UNREACHABLE, other),
            Set.of("gravitee-policy-protobuf-2.0.0.zip", "gravitee-policy-acl-3.0.3.zip")
        );

        assertThat(verdict.unexpected()).containsExactly(other);
    }

    @Test
    void should_report_an_entry_that_no_longer_matches_once_the_plugin_links() {
        var verdict = baseline.evaluate(List.of(), Set.of("gravitee-policy-protobuf-2.1.0.zip"));

        assertThat(verdict.stale()).singleElement().asString().contains("gravitee-policy-protobuf").contains(REFERENCE);
    }

    @Test
    void should_not_report_an_entry_whose_plugin_this_bundle_does_not_ship() {
        var verdict = baseline.evaluate(List.of(), Set.of("gravitee-policy-acl-3.0.3.zip"));

        assertThat(verdict.stale()).isEmpty();
    }

    @Test
    void should_refuse_an_entry_without_a_justification() {
        assertThatThrownBy(() -> Baseline.parse(List.of("gravitee-policy-protobuf " + REFERENCE)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("justification");
    }
}
