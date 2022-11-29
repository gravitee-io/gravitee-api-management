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
package io.gravitee.rest.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.gravitee.repository.log.api.LogRepository;
import io.gravitee.repository.log.model.ExtendedLog;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.log.ApiRequest;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.LogsService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import java.time.Instant;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class LogsServiceITest extends TestCase {

    private static final ExecutionContext EXECUTION_CONTEXT = GraviteeContext.getExecutionContext();
    private static final String LOG_ID = "2560f540-c0f1-4c12-a76e-0da89ace6911";
    private static final String API_ID = "e7fa7f51-1540-490a-8134-31e965efdc09";
    private static final String APPLICATION_ID = "ca7ea3dc-1434-4ee5-a25f-cc57327d28cf";
    private static final String APPLICATION_NAME = "default";
    private static final String PLAN_ID = "81d3dc39-0e5f-4c1c-94cc-ec48c3609b5f";
    private static final String LOG_URI = "/echo";
    private static final Long LOG_TIMESTAMP = Instant.now().toEpochMilli();

    @Mock
    private LogRepository logRepository;

    @Mock
    private PlanService planService;

    @Mock
    ApplicationService applicationService;

    @Mock
    ParameterService parameterService;

    @InjectMocks
    private final LogsService logService = new LogsServiceImpl();

    @Test
    public void findApiLogShouldNotFailOnPlanNotFoundException() throws Exception {
        when(logRepository.findById(LOG_ID, LOG_TIMESTAMP)).thenReturn(newLog());
        when(applicationService.findById(EXECUTION_CONTEXT, APPLICATION_ID)).thenReturn(newApplication());

        when(planService.findById(EXECUTION_CONTEXT, PLAN_ID)).thenThrow(new PlanNotFoundException(PLAN_ID));

        ApiRequest apiLog = logService.findApiLog(EXECUTION_CONTEXT, LOG_ID, LOG_TIMESTAMP);

        assertThat(apiLog).isNotNull();
        assertThat(apiLog.getApi()).isEqualTo(API_ID);
        assertThat(apiLog.getApplication()).isEqualTo(APPLICATION_ID);
        assertThat(apiLog.getPlan()).isEqualTo(PLAN_ID);
    }

    private static ExtendedLog newLog() {
        ExtendedLog log = new ExtendedLog();
        log.setApi(API_ID);
        log.setApplication(APPLICATION_ID);
        log.setPlan(PLAN_ID);
        log.setUri(LOG_URI);
        return log;
    }

    private static ApplicationEntity newApplication() {
        ApplicationEntity application = new ApplicationEntity();
        application.setName(APPLICATION_NAME);
        return application;
    }
}
