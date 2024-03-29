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

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MembershipListItem {

    private final MemberEntity member;

    public MembershipListItem(MemberEntity member) {
        this.member = member;
    }

    public String getId() {
        return member.getId();
    }

    public String getDisplayName() {
        return member.getDisplayName();
    }

    public String getRole() {
        RoleEntity roleEntity = member.getRoles().get(0);
        return roleEntity == null ? null : roleEntity.getName();
    }
}
