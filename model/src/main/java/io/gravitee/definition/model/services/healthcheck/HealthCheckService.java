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
package io.gravitee.definition.model.services.healthcheck;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.gravitee.definition.model.services.schedule.ScheduledService;

import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HealthCheckService extends ScheduledService {

    public final static String SERVICE_KEY = "health-check";

    public HealthCheckService() {
        super(SERVICE_KEY);
    }

    private List<Step> steps;
    @JsonIgnore
    private transient Step step;

    public static String getServiceKey() {
        return SERVICE_KEY;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public void setSteps(List<Step> steps) {
        this.steps = steps;
    }

    @JsonSetter // Ensure backward compatibility
    private void setRequest(Request request) {
        initLegacyStep();
        step.setRequest(request);
    }

    @JsonSetter // Ensure backward compatibility
    private void setExpectation(Response response) {
        initLegacyStep();
        step.setResponse(response);
    }

    private void initLegacyStep() {
        if (steps == null) {
            steps = new ArrayList<>();
        }
        if (step == null) {
            step = new Step();
            step.setResponse(Response.DEFAULT_RESPONSE);
            steps.add(step);
        }
    }
}
