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
package io.gravitee.apim.core.user.model;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class BaseUserEntity {

    private String id;
    private String organizationId;
    private String firstname;
    private String lastname;
    private String email;
    private Date createdAt;
    private Date updatedAt;
    /** The source when user is coming from an external system (LDAP, ...) */
    private String source;
    /** The user reference in the external source */
    private String sourceId;

    public String displayName() {
        if (isNotBlank(firstname)) {
            if (isNotBlank(lastname)) {
                return firstname + ' ' + lastname;
            }
            return firstname;
        } else if (isNotBlank(lastname)) {
            return lastname;
        } else if (isNotBlank(email) && !"memory".equals(source)) {
            return email;
        }
        return sourceId;
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.isEmpty();
    }
}
