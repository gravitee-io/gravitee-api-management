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
package io.gravitee.rest.api.model.settings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.gravitee.rest.api.model.annotations.ParameterKey;
import io.gravitee.rest.api.model.parameters.Key;
import java.util.List;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ConsoleSettingsEntity extends AbstractCommonSettingsEntity {

    private Alert alert;
    private ConsoleAuthentication authentication;
    private ConsoleCors cors;
    private ConsoleReCaptcha reCaptcha;
    private ConsoleScheduler scheduler;
    private Logging logging;
    private Maintenance maintenance;
    private Management management;
    private Newsletter newsletter;
    private Theme theme;
    private JupiterMode jupiterMode;

    public ConsoleSettingsEntity() {
        super();
        alert = new Alert();
        authentication = new ConsoleAuthentication();
        cors = new ConsoleCors();
        logging = new Logging();
        maintenance = new Maintenance();
        management = new Management();
        newsletter = new Newsletter();
        reCaptcha = new ConsoleReCaptcha();
        scheduler = new ConsoleScheduler();
        theme = new Theme();
        jupiterMode = new JupiterMode();
    }

    // Getters & setters
    public Alert getAlert() {
        return alert;
    }

    public void setAlert(Alert alert) {
        this.alert = alert;
    }

    public ConsoleAuthentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(ConsoleAuthentication authentication) {
        this.authentication = authentication;
    }

    public ConsoleCors getCors() {
        return cors;
    }

    public void setCors(ConsoleCors cors) {
        this.cors = cors;
    }

    public ConsoleReCaptcha getReCaptcha() {
        return reCaptcha;
    }

    public void setReCaptcha(ConsoleReCaptcha reCaptcha) {
        this.reCaptcha = reCaptcha;
    }

    public ConsoleScheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(ConsoleScheduler scheduler) {
        this.scheduler = scheduler;
    }

    public Logging getLogging() {
        return logging;
    }

    public void setLogging(Logging logging) {
        this.logging = logging;
    }

    public Maintenance getMaintenance() {
        return maintenance;
    }

    public void setMaintenance(Maintenance maintenance) {
        this.maintenance = maintenance;
    }

    public Management getManagement() {
        return management;
    }

    public void setManagement(Management management) {
        this.management = management;
    }

    public Newsletter getNewsletter() {
        return newsletter;
    }

    public void setNewsletter(Newsletter newsletter) {
        this.newsletter = newsletter;
    }

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    public JupiterMode getJupiterMode() {
        return jupiterMode;
    }

    public void setJupiterMode(JupiterMode jupiterMode) {
        this.jupiterMode = jupiterMode;
    }

    //Classes
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConsoleCors {

        @ParameterKey(Key.CONSOLE_HTTP_CORS_ALLOW_ORIGIN)
        private List<String> allowOrigin;

        @ParameterKey(Key.CONSOLE_HTTP_CORS_ALLOW_HEADERS)
        private List<String> allowHeaders;

        @ParameterKey(Key.CONSOLE_HTTP_CORS_ALLOW_METHODS)
        private List<String> allowMethods;

        @ParameterKey(Key.CONSOLE_HTTP_CORS_EXPOSED_HEADERS)
        private List<String> exposedHeaders;

        @ParameterKey(Key.CONSOLE_HTTP_CORS_MAX_AGE)
        private Integer maxAge;

        public List<String> getAllowOrigin() {
            return allowOrigin;
        }

        public void setAllowOrigin(List<String> allowOrigin) {
            this.allowOrigin = allowOrigin;
        }

        public List<String> getAllowHeaders() {
            return allowHeaders;
        }

        public void setAllowHeaders(List<String> allowHeaders) {
            this.allowHeaders = allowHeaders;
        }

        public List<String> getAllowMethods() {
            return allowMethods;
        }

        public void setAllowMethods(List<String> allowMethods) {
            this.allowMethods = allowMethods;
        }

        public List<String> getExposedHeaders() {
            return exposedHeaders;
        }

        public void setExposedHeaders(List<String> exposedHeaders) {
            this.exposedHeaders = exposedHeaders;
        }

        public Integer getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(Integer maxAge) {
            this.maxAge = maxAge;
        }
    }
}
