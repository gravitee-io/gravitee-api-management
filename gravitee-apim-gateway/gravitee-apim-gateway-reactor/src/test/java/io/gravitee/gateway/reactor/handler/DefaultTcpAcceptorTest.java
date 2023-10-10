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
package io.gravitee.gateway.reactor.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DefaultTcpAcceptorTest {

    public static Stream<Arguments> combinations() {
        return Stream.of(
            arguments("acme.com", List.of("1"), "acme.com", "1", true),
            arguments("acme.com", List.of("1", "2"), "acme.com", "1", true),
            arguments("acme.com", List.of(), "acme.com", "1", true),
            arguments("acme.com", null, "acme.com", "1", true),
            arguments("acme.com", List.of("1"), "acme.com", "2", false),
            arguments("acme.com", List.of("1"), "acme.net", "1", false),
            arguments("acme.com", List.of(), "acme.net", "1", false),
            arguments("acme.com", null, "acme.net", "1", false)
        );
    }

    @ParameterizedTest
    @MethodSource("combinations")
    void should_match_without_serverIds(
        String acceptedHost,
        List<String> acceptedServerIds,
        String apiHost,
        String apiServerId,
        boolean match
    ) {
        assertThat(new DefaultTcpAcceptor(null, acceptedHost, acceptedServerIds).accept(apiHost, apiServerId)).isEqualTo(match);
    }

    // repeat it to make sure the shuffle don't actually shuffle
    @RepeatedTest(5)
    void should_sort_acceptors_on_host() {
        var inOrder = List.of(
            new DefaultTcpAcceptor(null, "0.org", null),
            new DefaultTcpAcceptor(null, "1.org", null),
            new DefaultTcpAcceptor(null, "2.org", null),
            // mind that comparing host is done in lowercase
            new DefaultTcpAcceptor(null, "A.com", null),
            new DefaultTcpAcceptor(null, "a.net", null),
            new DefaultTcpAcceptor(null, "B.com", null),
            new DefaultTcpAcceptor(null, "b.net", null),
            new DefaultTcpAcceptor(null, "C.com", null),
            new DefaultTcpAcceptor(null, "c.net", null)
        );
        var test = new ArrayList<>(inOrder);
        assertThat(test).isEqualTo(inOrder);
        Collections.shuffle(test);
        assertThat(test).isNotEqualTo(inOrder);
        Collections.sort(test);
        assertThat(test).isEqualTo(inOrder);
    }
}
