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
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "#{@environment.getProperty('management.mongodb.prefix')}sharedpolicygrouphistories")
public class SharedPolicyGroupHistoryMongo extends Auditable {

    @Id
    private String _id;

    private String id;

    private String organizationId;

    private String environmentId;

    private String crossId;

    private String name;

    private String description;

    private Integer version;

    private String apiType;

    private String phase;

    private String definition;

    private Date deployedAt;

    private String lifecycleState;
}
