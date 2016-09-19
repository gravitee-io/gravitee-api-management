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
package io.gravitee.repository.mongodb.management.internal.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
public class MembershipPkMongo implements Serializable {
    private String userId;
    private String referenceId;
    private String referenceType;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MembershipPkMongo)) return false;
        MembershipPkMongo membershipPkMongo = (MembershipPkMongo) o;
        return Objects.equals(userId, membershipPkMongo.userId) &&
                Objects.equals(referenceId, membershipPkMongo.referenceId) &&
                Objects.equals(referenceType, membershipPkMongo.referenceType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, referenceId, referenceType);
    }

    @Override
    public String toString() {
        return "MembershipPkMongo{" +
                "userId='" + userId + '\'' +
                ", referenceId ='" + referenceId + '\'' +
                ", referenceType ='" + referenceType + '\'' +
                '}';
    }
}
