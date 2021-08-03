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
package io.gravitee.rest.api.model.promotion;

import java.util.Objects;

public class PromotionEntityAuthor {

    private String userId;
    private String displayName;
    private String email;
    private String picture;
    private String source;
    private String sourceId;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PromotionEntityAuthor that = (PromotionEntityAuthor) o;
        return (
            Objects.equals(userId, that.userId) &&
            Objects.equals(displayName, that.displayName) &&
            Objects.equals(email, that.email) &&
            Objects.equals(picture, that.picture) &&
            Objects.equals(source, that.source) &&
            Objects.equals(sourceId, that.sourceId)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, displayName, email, picture, source, sourceId);
    }

    @Override
    public String toString() {
        return (
            "PromotionEntityAuthor{" +
            "userId='" +
            userId +
            '\'' +
            ", displayName='" +
            displayName +
            '\'' +
            ", email='" +
            email +
            '\'' +
            ", picture='" +
            picture +
            '\'' +
            ", source='" +
            source +
            '\'' +
            ", sourceId='" +
            sourceId +
            '\'' +
            '}'
        );
    }
}
