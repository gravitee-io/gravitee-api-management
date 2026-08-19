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
package io.gravitee.apim.plugin.gamma.api.automation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class UpsertResultTest {

    @Test
    void should_report_severe_when_any_issue_is_severe() {
        var result = new UpsertResult<>("view", List.of(AutomationIssue.warning("minor"), AutomationIssue.severe("blocking")));

        assertThat(result.hasSevere()).isTrue();
    }

    @Test
    void should_not_report_severe_for_warnings_only() {
        var result = new UpsertResult<>("view", List.of(AutomationIssue.warning("minor")));

        assertThat(result.hasSevere()).isFalse();
    }

    @Test
    void should_not_report_severe_without_issues() {
        var result = UpsertResult.of("view");

        assertThat(result.hasSevere()).isFalse();
        assertThat(result.issues()).isEmpty();
    }

    @Test
    void should_reject_missing_view() {
        assertThatThrownBy(() -> new UpsertResult<>(null, List.of())).isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_default_missing_issues_to_empty_list() {
        var result = new UpsertResult<>("view", null);

        assertThat(result.issues()).isEmpty();
    }
}
