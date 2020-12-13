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
package io.gravitee.rest.api.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.rest.api.model.annotations.ParameterKey;
import io.gravitee.rest.api.model.parameters.Key;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Application {

    private ClientRegistration registration = new ClientRegistration();
    private ApplicationTypes types = new ApplicationTypes();

    public ClientRegistration getRegistration() {
        return registration;
    }

    public void setRegistration(ClientRegistration registration) {
        this.registration = registration;
    }

    public ApplicationTypes getTypes() {
        return types;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public class ApplicationTypes {
        @JsonProperty("simple")
        @ParameterKey(Key.APPLICATION_TYPE_SIMPLE_ENABLED)
        private ConsoleConfigEntity.Enabled simpleType;

        @JsonProperty("browser")
        @ParameterKey(Key.APPLICATION_TYPE_BROWSER_ENABLED)
        private ConsoleConfigEntity.Enabled browserType;

        @JsonProperty("web")
        @ParameterKey(Key.APPLICATION_TYPE_WEB_ENABLED)
        private ConsoleConfigEntity.Enabled webType;

        @JsonProperty("native")
        @ParameterKey(Key.APPLICATION_TYPE_NATIVE_ENABLED)
        private ConsoleConfigEntity.Enabled nativeType;

        @JsonProperty("backend_to_backend")
        @ParameterKey(Key.APPLICATION_TYPE_BACKEND_TO_BACKEND_ENABLED)
        private ConsoleConfigEntity.Enabled backendToBackendType;

        public ConsoleConfigEntity.Enabled getSimpleType() {
            return simpleType;
        }

        public void setSimpleType(ConsoleConfigEntity.Enabled simpleType) {
            this.simpleType = simpleType;
        }

        public ConsoleConfigEntity.Enabled getBrowserType() {
            return browserType;
        }

        public void setBrowserType(ConsoleConfigEntity.Enabled browserType) {
            this.browserType = browserType;
        }

        public ConsoleConfigEntity.Enabled getWebType() {
            return webType;
        }

        public void setWebType(ConsoleConfigEntity.Enabled webType) {
            this.webType = webType;
        }

        public ConsoleConfigEntity.Enabled getNativeType() {
            return nativeType;
        }

        public void setNativeType(ConsoleConfigEntity.Enabled nativeType) {
            this.nativeType = nativeType;
        }

        public ConsoleConfigEntity.Enabled getBackendToBackendType() {
            return backendToBackendType;
        }

        public void setBackendToBackendType(ConsoleConfigEntity.Enabled backendToBackendType) {
            this.backendToBackendType = backendToBackendType;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public class ClientRegistration {
        @ParameterKey(Key.APPLICATION_REGISTRATION_ENABLED)
        private Boolean enabled;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }
}
