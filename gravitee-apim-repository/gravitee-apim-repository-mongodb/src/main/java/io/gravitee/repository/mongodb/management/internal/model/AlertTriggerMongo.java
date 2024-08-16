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
package io.gravitee.repository.mongodb.management.internal.model;

import java.util.Date;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Setter
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@Document(collection = "#{@environment.getProperty('management.mongodb.prefix')}alert_triggers")
public class AlertTriggerMongo {

    @Id
    @EqualsAndHashCode.Include
    private String id;

    private String name;
    private String type;
    private String description;
    private String referenceType;
    private String referenceId;
    private String definition;
    private String severity;
    private boolean enabled;
    private String parentId;
    private Date createdAt;
    private Date updatedAt;
    private boolean template;
    private List<AlertEventRuleMongo> eventRules;
    private String environmentId;
}
