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

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Setter
@Getter
@Document(collection = "#{@environment.getProperty('management.mongodb.prefix')}groups")
public class GroupMongo extends DeprecatedAuditable {

    @Id
    private String id;

    private String environmentId;
    private String name;
    private List<String> administrators;
    private List<GroupEventRuleMongo> eventRules;
    private Integer maxInvitation;
    private boolean lockApiRole;
    private boolean lockApplicationRole;
    private boolean systemInvitation;
    private boolean emailInvitation;
    private boolean disableMembershipNotifications;
    private String apiPrimaryOwner;
    private String origin;
}
