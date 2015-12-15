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
package io.gravitee.management.service.builder;

import io.gravitee.management.service.EmailNotification;

import java.util.Map;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
public class EmailNotificationBuilder {

    private final EmailNotification emailNotification = new EmailNotification();

    public EmailNotificationBuilder from(String from) {
        this.emailNotification.setFrom(from);
        return this;
    }

    public EmailNotificationBuilder to(String to) {
        this.emailNotification.setTo(to);
        return this;
    }

    public EmailNotificationBuilder content(String content) {
        this.emailNotification.setContent(content);
        return this;
    }

    public EmailNotificationBuilder subject(String subject) {
        this.emailNotification.setSubject(subject);
        return this;
    }

    public EmailNotificationBuilder params(Map params) {
        this.emailNotification.setParams(params);
        return this;
    }

    public EmailNotification build() {
        return this.emailNotification;
    }
}