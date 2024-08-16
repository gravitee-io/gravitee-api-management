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
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Document(collection = "#{@environment.getProperty('management.mongodb.prefix')}plans")
@Getter
@Setter
@EqualsAndHashCode(of = { "id" }, callSuper = false)
public class PlanMongo extends Auditable {

    @Id
    private String id;

    private String definitionVersion;

    /**
     * The plan crossId uniquely identifies a plan across environments.
     * Plans promoted between environments will share the same crossId.
     */
    private String crossId;

    private String name;

    private String description;

    /**
     * The way to validate subscriptions
     */
    private String validation;

    private String type;

    private String mode;

    private String status;

    private String security;

    private int order;

    /**
     * The API used by this plan.
     */
    private String api;

    /**
     * The environment related to this plan
     */
    private String environmentId;

    /**
     * The JSON payload of all policies to apply for this plan
     */
    private String definition;

    private List<String> characteristics;

    /**
     * Plan publication date
     */
    private Date publishedAt;

    /**
     * Plan closing date
     */
    private Date closedAt;

    private List<String> excludedGroups;

    private String securityDefinition;

    private Date needRedeployAt;

    private boolean commentRequired;

    private String commentMessage;

    private String generalConditions;

    private Set<String> tags;

    private String selectionRule;
}
