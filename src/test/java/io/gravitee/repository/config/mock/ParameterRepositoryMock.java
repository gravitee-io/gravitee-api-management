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
package io.gravitee.repository.config.mock;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;

import java.util.Arrays;

import io.gravitee.repository.management.api.ParameterRepository;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ParameterRepositoryMock extends AbstractRepositoryMock<ParameterRepository> {

    public ParameterRepositoryMock() {
        super(ParameterRepository.class);
    }

    @Override
    void prepare(ParameterRepository parameterRepository) throws Exception {
        final Parameter parameter = mock(Parameter.class);
        when(parameter.getValue()).thenReturn("Parameter value");
        when(parameter.getReferenceId()).thenReturn("DEFAULT");
        when(parameter.getReferenceType()).thenReturn(ParameterReferenceType.ENVIRONMENT);
        
        final Parameter parameter2 = mock(Parameter.class);
        when(parameter2.getKey()).thenReturn("portal.top-apis");
        when(parameter2.getValue()).thenReturn("api1;api2;api2");
        when(parameter2.getReferenceId()).thenReturn("DEFAULT");
        when(parameter2.getReferenceType()).thenReturn(ParameterReferenceType.ENVIRONMENT);

        final Parameter parameter2Updated = mock(Parameter.class);
        when(parameter2Updated.getValue()).thenReturn("New value");
        when(parameter2Updated.getReferenceId()).thenReturn("DEFAULT");
        when(parameter2Updated.getReferenceType()).thenReturn(ParameterReferenceType.ENVIRONMENT);
        
        when(parameterRepository.create(any(Parameter.class))).thenReturn(parameter);

        when(parameterRepository.findById("new-parameter")).thenReturn(empty(), of(parameter));
        when(parameterRepository.findById("management.oAuth.clientId")).thenReturn(of(parameter2), empty());
        when(parameterRepository.findById("portal.top-apis")).thenReturn(of(parameter2), of(parameter2Updated));

        when(parameterRepository.update(argThat(o -> o == null || o.getKey().equals("unknown")))).thenThrow(new IllegalStateException());

        when(parameterRepository.findAll(any())).thenReturn(Arrays.asList(mock(Parameter.class), mock(Parameter.class)));
        when(parameterRepository.findAllByReferenceIdAndReferenceType(any(), eq("DEFAULT"), eq(ParameterReferenceType.ENVIRONMENT))).thenReturn(Arrays.asList(parameter, parameter2, parameter2Updated));

    }
}
