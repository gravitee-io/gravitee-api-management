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
package io.gravitee.management.service.impl;

import io.gravitee.management.service.AuditService;
import io.gravitee.management.service.ParameterService;
import io.gravitee.management.service.exceptions.ParameterAlreadyExistsException;
import io.gravitee.management.service.exceptions.ParameterNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ParameterRepository;
import io.gravitee.repository.management.model.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.gravitee.repository.management.model.Audit.AuditProperties.PARAMETER;
import static io.gravitee.repository.management.model.Parameter.AuditEvent.PARAMETER_CREATED;
import static io.gravitee.repository.management.model.Parameter.AuditEvent.PARAMETER_UPDATED;
import static java.lang.String.join;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ParameterServiceImpl extends TransactionalService implements ParameterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParameterServiceImpl.class);

    private static final String SEPARATOR = ";";

    @Inject
    private ParameterRepository parameterRepository;
    @Inject
    private AuditService auditService;

    @Override
    public List<String> findAll(final String key) {
        return findAll(key, value -> value, null);
    }

    @Override
    public <T> List<T> findAll(final String key, final Function<String, T> mapper) {
        return findAll(key, mapper, null);
    }

    @Override
    public <T> List<T> findAll(final String key, final Function<String, T> mapper, final Predicate<String> filter) {
        try {
            final Optional<Parameter> optionalParameter = parameterRepository.findById(key);
            if (optionalParameter.isPresent()) {
                final String value = optionalParameter.get().getValue();
                if (!value.isEmpty()) {
                    Stream<String> stream = stream(value.split(SEPARATOR));
                    if (filter != null) {
                        stream = stream.filter(filter);
                    }
                    return stream.map(mapper).collect(toList());
                }
            }
            return emptyList();
        } catch (final TechnicalException ex) {
            final String message = "An error occurs while trying to find parameter values with key: " + key;
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    @Override
    public Parameter create(final String key, final String value) {
        try {
            final Optional<Parameter> optionalParameter = parameterRepository.findById(key);
            if (optionalParameter.isPresent()) {
                throw new ParameterAlreadyExistsException(key);
            }
            final Parameter parameter = new Parameter();
            parameter.setKey(key);
            parameter.setValue(value);
            final Parameter savedParameter = parameterRepository.create(parameter);
            auditService.createPortalAuditLog(
                    singletonMap(PARAMETER, savedParameter.getKey()),
                    PARAMETER_CREATED,
                    new Date(),
                    null,
                    savedParameter);
            return savedParameter;
        } catch (final TechnicalException ex) {
            final String message = "An error occurs while trying to create parameter for key/value: " + key + '/' + value;
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    @Override
    public Parameter update(final String key, final String value) {
        try {
            final Optional<Parameter> optionalParameter = parameterRepository.findById(key);
            if (!optionalParameter.isPresent()) {
                throw new ParameterNotFoundException(key);
            }
            final Parameter parameter = new Parameter();
            parameter.setKey(key);
            parameter.setValue(value);
            final Parameter updatedParameter = parameterRepository.update(parameter);
            auditService.createPortalAuditLog(
                    singletonMap(PARAMETER, updatedParameter.getKey()),
                    PARAMETER_UPDATED,
                    new Date(),
                    optionalParameter.get(),
                    updatedParameter);
            return updatedParameter;
        } catch (final TechnicalException ex) {
            final String message = "An error occurs while trying to update parameter for key/value: " + key + '/' + value;
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    @Override
    public void createMultipleValue(final String key, final String value) {
        try {
            final Optional<Parameter> optionalParameter = parameterRepository.findById(key);
            final Parameter parameter = new Parameter();
            parameter.setKey(key);
            if (optionalParameter.isPresent()) {
                final String existingValue = optionalParameter.get().getValue();
                if (existingValue.isEmpty()) {
                    parameter.setValue(value);
                } else {
                    parameter.setValue(existingValue + SEPARATOR + value);
                }
                update(parameter.getKey(), parameter.getValue());
            } else {
                parameter.setValue(value);
                create(parameter.getKey(), parameter.getValue());
            }
        } catch (final TechnicalException ex) {
            final String message = "An error occurs while trying to update parameter for key/value: " + key + '/' + value;
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    @Override
    public void updateMultipleValue(final String key, final List<String> values) {
        update(key, join(SEPARATOR, values));
    }
}
