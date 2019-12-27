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
package io.gravitee.gateway.core.logging;

public class LogConfiguration {
    private boolean logSSLInformation;
    private boolean logCertificateChains;
    private int maxSizeLogMessage = -1;

    public LogConfiguration() {
    }

    public boolean isLogSSLInformation() {
        return logSSLInformation;
    }

    public void setLogSSLInformation(boolean logSSLInformation) {
        this.logSSLInformation = logSSLInformation;
    }

    public boolean isLogCertificateChains() {
        return logCertificateChains;
    }

    public void setLogCertificateChains(boolean logCertificateChains) {
        this.logCertificateChains = logCertificateChains;
    }

    public int getMaxSizeLogMessage() {
        return maxSizeLogMessage;
    }

    public void setMaxSizeLogMessage(int maxSizeLogMessage) {
        this.maxSizeLogMessage = maxSizeLogMessage;
    }

    public static class Builder {

        private final LogConfiguration logConfiguration;

        public Builder() {
            this.logConfiguration = new LogConfiguration();
        }

        public Builder maxSizeLogMessage(int maxSizeLogMessage) {
            this.logConfiguration.setMaxSizeLogMessage(Math.max(maxSizeLogMessage, -1));
            return this;
        }

        public Builder withSsl(boolean logSslInfo, boolean logCerts) {
            this.logConfiguration.setLogSSLInformation(logSslInfo);
            this.logConfiguration.setLogCertificateChains(logCerts);
            return this;
        }

        public LogConfiguration build() {
            return this.logConfiguration;
        }
    }
}
