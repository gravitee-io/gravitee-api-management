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
package io.gravitee.rest.api.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;

/**
 * @author Azize Elamrani (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
public class EmailNotification {

    private String from;
    private String fromName;
    private String[] to;
    private String[] bcc;
    private String template;
    private Map<String, Object> params = new HashMap<>();
    private boolean copyToSender;
    private String replyTo;

    public boolean hasRecipients() {
        return (getTo() != null && getTo().length > 0) || (getBcc() != null && getBcc().length > 0);
    }

    public Integer recipientsCount() {
        return (getTo() != null ? getTo().length : 0) + (getBcc() != null ? getBcc().length : 0);
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setTo(String... to) {
        this.to = to;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public void setCopyToSender(boolean copyToSender) {
        this.copyToSender = copyToSender;
    }

    public void setBcc(String[] bcc) {
        this.bcc = bcc;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmailNotification that)) return false;
        return (
            Objects.equals(from, that.from) &&
            Objects.equals(fromName, that.fromName) &&
            Arrays.equals(to, that.to) &&
            Objects.equals(template, that.template) &&
            Objects.equals(params, that.params) &&
            Arrays.equals(bcc, that.bcc) &&
            Objects.equals(copyToSender, that.copyToSender) &&
            Objects.equals(replyTo, that.replyTo)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, fromName, Arrays.hashCode(to), template, params, copyToSender, Arrays.hashCode(bcc), replyTo);
    }

    @Override
    public String toString() {
        return (
            "EmailNotification{" +
            "from='" +
            from +
            '\'' +
            ", reply-to='" +
            replyTo +
            '\'' +
            ", fromName='" +
            fromName +
            '\'' +
            ", to=" +
            Arrays.toString(to) +
            ", template='" +
            template +
            '\'' +
            ", params=" +
            params +
            ", copyToSender=" +
            copyToSender +
            ", bcc=" +
            Arrays.toString(bcc) +
            '}'
        );
    }
}
