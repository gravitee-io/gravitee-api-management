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
package io.gravitee.rest.api.model.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.rest.api.model.annotations.ParameterKey;
import io.gravitee.rest.api.model.parameters.Key;
import java.util.List;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Email {

    @ParameterKey(Key.EMAIL_ENABLED)
    private Boolean enabled;

    @ParameterKey(Key.EMAIL_HOST)
    private String host;

    @ParameterKey(Key.EMAIL_PORT)
    private Integer port;

    @ParameterKey(Key.EMAIL_USERNAME)
    private String username;

    @ParameterKey(value = Key.EMAIL_PASSWORD, sensitive = true)
    private String password;

    @ParameterKey(Key.EMAIL_PROTOCOL)
    private String protocol;

    @ParameterKey(Key.EMAIL_SUBJECT)
    private String subject;

    @ParameterKey(Key.EMAIL_FROM)
    private String from;

    /**
     * Raw JSON storage for the branded-sender configurations, persisted via {@link ParameterKey}
     * under {@code email.branded_senders}. Exposed to the API as the typed {@code brandedSenders}
     * list (see {@link #getBrandedSenders()}); this raw form is internal and kept out of the JSON
     * surface.
     */
    @ParameterKey(Key.EMAIL_BRANDED_SENDERS)
    @JsonIgnore
    private String brandedSendersRaw = "[]";

    private EmailProperties properties;

    public Email() {
        properties = new EmailProperties();
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    @JsonIgnore
    public String getBrandedSendersRaw() {
        return brandedSendersRaw;
    }

    @JsonIgnore
    public void setBrandedSendersRaw(String brandedSendersRaw) {
        this.brandedSendersRaw = brandedSendersRaw;
    }

    /**
     * Returns the branded-sender configurations deserialised from the {@code email.branded_senders}
     * parameter. Each entry maps a set of recipient domains to a {@code From} address and subject
     * prefix (see {@link BrandedSenderConfig}). Never {@code null}; malformed stored values degrade to
     * an empty list (see {@link BrandedSenders#parse(String)}).
     */
    @JsonProperty("brandedSenders")
    public List<BrandedSenderConfig> getBrandedSenders() {
        return BrandedSenders.parse(brandedSendersRaw);
    }

    /**
     * Serialises the branded-sender configurations into the {@code email.branded_senders} parameter
     * value; invalid or oversized input is rejected (see {@link BrandedSenders#write(List)}).
     */
    @JsonProperty("brandedSenders")
    public void setBrandedSenders(List<BrandedSenderConfig> brandedSenders) {
        this.brandedSendersRaw = BrandedSenders.write(brandedSenders);
    }

    public EmailProperties getProperties() {
        return properties;
    }

    public void setProperties(EmailProperties properties) {
        this.properties = properties;
    }

    public static class EmailProperties {

        @ParameterKey(Key.EMAIL_PROPERTIES_AUTH_ENABLED)
        private Boolean auth;

        @ParameterKey(Key.EMAIL_PROPERTIES_STARTTLS_ENABLE)
        private Boolean startTlsEnable;

        @ParameterKey(Key.EMAIL_PROPERTIES_SSL_TRUST)
        private String sslTrust;

        public Boolean getAuth() {
            return auth;
        }

        public void setAuth(Boolean auth) {
            this.auth = auth;
        }

        public Boolean getStartTlsEnable() {
            return startTlsEnable;
        }

        public void setStartTlsEnable(Boolean startTlsEnable) {
            this.startTlsEnable = startTlsEnable;
        }

        public String getSslTrust() {
            return sslTrust;
        }

        public void setSslTrust(String sslTrust) {
            this.sslTrust = sslTrust;
        }
    }
}
