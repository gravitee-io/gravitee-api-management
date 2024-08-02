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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.gravitee.rest.api.model.annotations.ParameterKey;
import io.gravitee.rest.api.model.parameters.Key;
import jakarta.validation.Valid;
import java.util.List;
import lombok.Data;

/**
 * @author GraviteeSource Team
 */
@Data
public class ConsoleSettingsEntity extends AbstractCommonSettingsEntity {

    private Alert alert;
    private ConsoleAuthentication authentication;
    private ConsoleCors cors;
    private ConsoleReCaptcha reCaptcha;
    private ConsoleScheduler scheduler;

    private ConsoleAnalyticsPendo analyticsPendo;

    @Valid
    private Logging logging;

    private Maintenance maintenance;
    private Management management;
    private Newsletter newsletter;
    private V4EmulationEngine v4EmulationEngine;
    private AlertEngine alertEngine;
    private LicenseExpirationNotification licenseExpirationNotification;
    private TrialInstance trialInstance;
    private Federation federation;

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
        analyticsPendo = new ConsoleAnalyticsPendo();
        v4EmulationEngine = new V4EmulationEngine();
        alertEngine = new AlertEngine();
        licenseExpirationNotification = new LicenseExpirationNotification();
        trialInstance = new TrialInstance();
        federation = new Federation();
    }

    //Classes
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
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
    }
}
