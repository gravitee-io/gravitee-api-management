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
package io.gravitee.rest.api.model.kafka;

import java.util.Objects;

/**
 * Configuration for connecting to a Kafka cluster.
 *
 * @author Gravitee Team
 */
public class KafkaConnectionConfig {

    private String bootstrapServers;
    private String securityProtocol;
    private String saslMechanism;
    private String saslUsername;
    private String saslPassword;

    public KafkaConnectionConfig() {}

    public KafkaConnectionConfig(String bootstrapServers, String securityProtocol) {
        this.bootstrapServers = bootstrapServers;
        this.securityProtocol = securityProtocol;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public String getSecurityProtocol() {
        return securityProtocol;
    }

    public void setSecurityProtocol(String securityProtocol) {
        this.securityProtocol = securityProtocol;
    }

    public String getSaslMechanism() {
        return saslMechanism;
    }

    public void setSaslMechanism(String saslMechanism) {
        this.saslMechanism = saslMechanism;
    }

    public String getSaslUsername() {
        return saslUsername;
    }

    public void setSaslUsername(String saslUsername) {
        this.saslUsername = saslUsername;
    }

    public String getSaslPassword() {
        return saslPassword;
    }

    public void setSaslPassword(String saslPassword) {
        this.saslPassword = saslPassword;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KafkaConnectionConfig that = (KafkaConnectionConfig) o;
        return (
            Objects.equals(bootstrapServers, that.bootstrapServers) &&
            Objects.equals(securityProtocol, that.securityProtocol) &&
            Objects.equals(saslMechanism, that.saslMechanism) &&
            Objects.equals(saslUsername, that.saslUsername)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(bootstrapServers, securityProtocol, saslMechanism, saslUsername);
    }

    @Override
    public String toString() {
        return (
            "KafkaConnectionConfig{" +
            "bootstrapServers='" +
            bootstrapServers +
            '\'' +
            ", securityProtocol='" +
            securityProtocol +
            '\'' +
            ", saslMechanism='" +
            saslMechanism +
            '\'' +
            '}'
        );
    }
}
