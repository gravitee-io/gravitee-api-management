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
package io.gravitee.rest.api.model.configuration.identity;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GroupMappingEntity {

    /**
     * A template the expression language engine evaluates, anywhere in the value: {@code {#...}}, {@code { #...}},
     * {@code {T(...)}} or {@code {(...)}}. Text around it makes a composite template resolving to a single name, e.g.
     * {@code team-{#profile['department']}}.
     */
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\{ *(?:#|T\\(|\\()");

    private String condition;

    @NotEmpty
    private List<String> groups;

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    /**
     * A group of a mapping is either a group id or an expression resolving to group names.
     */
    public static boolean isExpression(String group) {
        return group != null && EXPRESSION_PATTERN.matcher(group).find();
    }
}
