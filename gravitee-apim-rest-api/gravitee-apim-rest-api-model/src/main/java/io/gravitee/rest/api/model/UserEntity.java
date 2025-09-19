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
package io.gravitee.rest.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.rest.api.model.search.Indexable;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import lombok.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UserEntity implements Indexable {

    /**
     * User identifier
     */
    private String id;

    /**
     * The user organization id of the user
     */
    private String organizationId;

    /**
     * The user first name
     */
    private String firstname;

    /**
     * The user last name
     */
    private String lastname;

    /**
     * The user password
     */
    private String password;

    /**
     * The user email
     */
    private String email;

    /**
     * The user 'organization' roles
     */
    private Set<UserRoleEntity> roles;

    /**
     * The user 'environment' roles
     */
    private Map<String, Set<UserRoleEntity>> envRoles;

    /**
     * The user creation date
     */
    @JsonProperty("created_at")
    private Date createdAt;

    /**
     * The user last updated date
     */
    @JsonProperty("updated_at")
    private Date updatedAt;

    /**
     * The user picture
     */
    private String picture;

    /**
     * The source when user is coming from an external system (LDAP, ...)
     */
    private String source;

    /**
     * The user reference in the external source
     */
    private String sourceId;

    /**
     * The user last connection date
     */
    private Date lastConnectionAt;

    /**
     * The user first connection date
     */
    private Date firstConnectionAt;

    @JsonProperty("primary_owner")
    private boolean primaryOwner;

    private String status;

    private long loginCount;

    @JsonProperty("number_of_active_tokens")
    private int nbActiveTokens;

    private Boolean newsletterSubscribed;

    private Map<String, Object> customFields;

    @JsonIgnore
    private String referenceType;

    @JsonIgnore
    private String referenceId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getDisplayName() {
        String displayName;

        if ((firstname != null && !firstname.isEmpty()) || (lastname != null && !lastname.isEmpty())) {
            if (firstname != null && !firstname.isEmpty()) {
                displayName = firstname + ((lastname != null && !lastname.isEmpty()) ? ' ' + lastname : "");
            } else {
                displayName = lastname;
            }
        } else if (email != null && !email.isEmpty() && !"memory".equals(source)) {
            displayName = email;
        } else {
            displayName = sourceId;
        }

        return displayName;
    }

    @JsonIgnore
    public boolean optedIn() {
        return ("ACTIVE".equalsIgnoreCase(status) && password != null) || "memory".equalsIgnoreCase(source);
    }
}
