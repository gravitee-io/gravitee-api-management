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
package io.gravitee.rest.api.model.notification;

import java.util.Objects;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NewPortalNotificationEntity {

    private String title;
    private String message;
    private String user;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NewPortalNotificationEntity)) return false;
        NewPortalNotificationEntity that = (NewPortalNotificationEntity) o;
        return Objects.equals(title, that.title) && Objects.equals(message, that.message) && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, message, user);
    }

    @Override
    public String toString() {
        return "NewNotificationEntity{" + ", title='" + title + '\'' + ", message='" + message + '\'' + ", user='" + user + '\'' + '}';
    }
}
