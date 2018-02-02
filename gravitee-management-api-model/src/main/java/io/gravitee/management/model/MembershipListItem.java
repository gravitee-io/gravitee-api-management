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
package io.gravitee.management.model;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MembershipListItem {

    private final String id;

    private final String displayName;

    private final String username;

    private final String role;

    public MembershipListItem(MemberEntity member) {
        this.id = member.getId();
        this.role = member.getRole();
        this.username = member.getUsername();

        if (member.getFirstname() != null && member.getLastname() != null) {
            this.displayName = member.getFirstname() + ' ' + member.getLastname();
        } else {
            this.displayName = username;
        }
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRole() {
        return role;
    }

    public String getUsername() {
        return username;
    }
}
