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
package fixtures;

import io.gravitee.rest.api.management.v2.rest.model.MembershipMemberType;
import io.gravitee.rest.api.management.v2.rest.model.PrimaryOwner;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;

@SuppressWarnings("ALL")
public class PrimaryOwnerFixtures {

    private PrimaryOwnerFixtures() {}

    private static final PrimaryOwner.PrimaryOwnerBuilder BASE_PRIMARY_OWNER = PrimaryOwner.builder()
        .id("primary-owner-id")
        .displayName("primary-owner-displayName")
        .email("primary-owner@email.com")
        .type(MembershipMemberType.USER);

    public static PrimaryOwner aPrimaryOwner() {
        return BASE_PRIMARY_OWNER.build();
    }

    public static PrimaryOwnerEntity aPrimaryOwnerEntity() {
        return PrimaryOwnerModelFixtures.aPrimaryOwnerEntity();
    }
}
