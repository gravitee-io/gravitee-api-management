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
package io.gravitee.gamma.definition.approval;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ApprovalEventKindsTest {

    // The shape the control plane requires of a generic event kind: <feature>.<type>, lowercase.
    private static final Pattern KIND = Pattern.compile("[a-z0-9-]+(\\.[a-z0-9-]+)+");

    @Test
    void the_kinds_are_valid_generic_event_kinds_of_one_feature() {
        assertThat(ApprovalEventKinds.RULE).matches(KIND).isEqualTo("approval.rule");
        assertThat(ApprovalEventKinds.STATE).matches(KIND).isEqualTo("approval.state");
        assertThat(ApprovalEventKinds.FEATURE).isEqualTo("approval");
    }
}
