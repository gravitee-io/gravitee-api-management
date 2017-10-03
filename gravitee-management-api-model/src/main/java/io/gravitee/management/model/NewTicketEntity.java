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

import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NewTicketEntity {

    @NotNull
    private String subject;
    @NotNull
    private String content;
    private String application;
    private String api;
    private boolean copyToSender;

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public boolean isCopyToSender() {
        return copyToSender;
    }

    public void setCopyToSender(boolean copyToSender) {
        this.copyToSender = copyToSender;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NewTicketEntity)) return false;
        NewTicketEntity that = (NewTicketEntity) o;
        return Objects.equals(subject, that.subject) &&
                Objects.equals(content, that.content) &&
                Objects.equals(application, that.application) &&
                Objects.equals(api, that.api) &&
                Objects.equals(copyToSender, that.copyToSender);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, content, application, api, copyToSender);
    }

    @Override
    public String toString() {
        return "NewSupportEntity{" +
                "subject='" + subject + '\'' +
                ", content='" + content + '\'' +
                ", application='" + application + '\'' +
                ", api='" + api + '\'' +
                ", copyToSender='" + copyToSender + '\'' +
                '}';
    }
}
