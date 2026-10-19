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
package io.gravitee.definition.model.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SaslMechanismTest {

    @Test
    void should_not_print_the_password() {
        assertThat(new SaslMechanism("PLAIN", "alice", "s3cr3t"))
            .hasToString("SaslMechanism[type=PLAIN, username=alice, password=******]")
            .extracting(SaslMechanism::password)
            .isEqualTo("s3cr3t");
    }

    @Test
    void should_print_a_missing_password_as_missing() {
        assertThat(new SaslMechanism("GSSAPI", null, null)).hasToString("SaslMechanism[type=GSSAPI, username=null, password=null]");
    }
}
