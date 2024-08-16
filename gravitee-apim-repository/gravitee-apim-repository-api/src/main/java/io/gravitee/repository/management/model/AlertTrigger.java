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
package io.gravitee.repository.management.model;

import java.util.Date;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Setter
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AlertTrigger {

    public enum AuditEvent implements Audit.AuditEvent {
        ALERT_TRIGGER_CREATED,
        ALERT_TRIGGER_UPDATED,
        ALERT_TRIGGER_DELETED,
    }

    @EqualsAndHashCode.Include
    private String id;

    private String name;

    private String description;

    private String type;

    private String definition;

    private boolean enabled;

    private String referenceType;

    private String referenceId;

    private Date createdAt;

    private Date updatedAt;

    private String severity;

    private String parentId;

    private List<AlertEventRule> eventRules;

    private boolean template;

    private String environmentId;
}
