/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.notification;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public enum AlertHook implements Hook {
    CUSTOMER_HTTP_STATUS(
        "HTTP status code",
        "Email sent to all members of an application when an \"HTTP Status\" consumer alert has been triggered.",
        "CUSTOMER"
    ),
    CUSTOMER_RESPONSE_TIME(
        "Average response time",
        "Email sent to all members of an application when a \"Response time\" consumer alert has been triggered.",
        "CUSTOMER"
    );

    private String label;
    private String description;
    private String category;
    private boolean hidden;

    AlertHook(String label, String description, String category) {
        this(label, description, category, false);
    }

    AlertHook(String label, String description, String category, boolean hidden) {
        this.label = label;
        this.description = description;
        this.category = category;
        this.hidden = hidden;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public HookScope getScope() {
        return HookScope.TEMPLATES_FOR_ALERT;
    }

    @Override
    public boolean isHidden() {
        return hidden;
    }
}
