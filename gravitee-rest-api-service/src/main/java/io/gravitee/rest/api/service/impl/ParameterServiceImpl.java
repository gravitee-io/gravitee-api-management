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

import io.gravitee.common.event.EventManager;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ParameterRepository;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static io.gravitee.repository.management.model.Audit.AuditProperties.PARAMETER;
import static io.gravitee.repository.management.model.Parameter.AuditEvent.PARAMETER_CREATED;
import static io.gravitee.repository.management.model.Parameter.AuditEvent.PARAMETER_UPDATED;
import static java.lang.String.join;
import static java.util.Arrays.stream;
import static java.util.Collections.*;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ParameterServiceImpl extends TransactionalService implements ParameterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParameterServiceImpl.class);

    public static final String SEPARATOR = ";";
    public static final String KV_SEPARATOR = "@";

    @Inject
    private ParameterRepository parameterRepository;
    @Inject
    private AuditService auditService;
    @Inject
    private ConfigurableEnvironment environment;
    @Inject
    private EventManager eventManager;

    @Override
    public String find(final Key key) {
        final List<String> values = findAll(key);
        final String value;
        if (values == null || values.isEmpty()) {
            value = key.defaultValue();
        } else {
            value = String.join(SEPARATOR, values);
        }
        return value;
    }

    @Override
    public boolean findAsBoolean(final Key key) {
        return Boolean.valueOf(find(key));
    }

    @Override
    public List<String> findAll(final Key key) {
        return findAll(key, value -> value, null);
    }

    @Override
    public Map<String, List<String>> findAll(final List<Key> keys) {
        return findAll(keys, value -> value, null);
    }

    @Override
    public <T> List<T> findAll(final Key key, final Function<String, T> mapper) {
        return findAll(key, mapper, null);
    }

    @Override
    public <T> Map<String, List<T>> findAll(final List<Key> keys, final Function<String, T> mapper) {
        return findAll(keys, mapper, null);
    }

    @Override
    public <T> List<T> findAll(final Key key, final Function<String, T> mapper, final Predicate<String> filter) {
        try {
            Optional<Parameter> optionalParameter;
            if (environment.containsProperty(key.key()) && key.isOverridable()) {
                final Parameter parameter = new Parameter();
                parameter.setKey(key.key());
                parameter.setReferenceId(GraviteeContext.getCurrentEnvironment());
                parameter.setReferenceType(ParameterReferenceType.ENVIRONMENT);
                parameter.setValue(toSemicolonSeparatedString(key, environment.getProperty(key.key())));
                optionalParameter = Optional.of(parameter);
            } else {
                optionalParameter = parameterRepository.findById(key.key());
            }

            if (optionalParameter.isPresent()) {
                return splitValue(optionalParameter.get().getValue(), mapper, filter);
            }
            return emptyList();
        } catch (final TechnicalException ex) {
            final String message = "An error occurs while trying to find parameter values with key: " + key;
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    @Override
    public <T> Map<String, List<T>> findAll(List<Key> keys, Function<String, T> mapper, Predicate<String> filter) {
        try {
            List<Parameter> parameters = parameterRepository.findAllByReferenceIdAndReferenceType(
                    keys.stream().map(Key::key).collect(toList()), 
                    GraviteeContext.getCurrentEnvironment(), 
                    ParameterReferenceType.ENVIRONMENT);

            // Override or add parameters from environment
            keys.forEach(k -> {
                if (environment.containsProperty(k.key()) && k.isOverridable()) {
                    final Optional<Parameter> optionalParameter = parameters.stream().
                            filter(p -> p.getKey().equals(k.key()))
                            .findFirst();
                    if (optionalParameter.isPresent()) {
                        final Parameter p = optionalParameter.get();
                        p.setValue(toSemicolonSeparatedString(Key.findByKey(p.getKey()),
                                environment.getProperty(p.getKey())));
                    } else {
                        final Parameter parameter = new Parameter();
                        parameter.setKey(k.key());
                        parameter.setReferenceId(GraviteeContext.getCurrentEnvironment());
                        parameter.setReferenceType(ParameterReferenceType.ENVIRONMENT);
                        parameter.setValue(toSemicolonSeparatedString(k, environment.getProperty(k.key())));
                        parameters.add(parameter);
                    }
                }
            });

            if (parameters.isEmpty()) {
                return emptyMap();
            }

            Map<String, List<T>> result = new HashMap<>();
            parameters.forEach( p -> {
                result.put(p.getKey(), splitValue(p.getValue(), mapper, filter));
            });
            return result;
        } catch (final TechnicalException ex) {
            final String message = "An error occurs while trying to find parameter values with keys: " + keys;
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    private <T> List<T> splitValue(final String value, final Function<String, T> mapper, final Predicate<String> filter) {
        if (value == null || value.isEmpty()) {
            return emptyList();
        }
        Stream<String> stream = stream(value.split(SEPARATOR));
        if (filter != null) {
            stream = stream.filter(filter);
        }
        return stream.map(mapper).collect(toList());
    }

    @Override
    public Parameter save(final Key key, final String value) {

        try {
            Optional<Parameter> optionalParameter = parameterRepository.findById(key.key());
            final boolean updateMode = optionalParameter.isPresent();

            final Parameter parameter = new Parameter();
            parameter.setKey(key.key());
            parameter.setReferenceId(GraviteeContext.getCurrentEnvironment());
            parameter.setReferenceType(ParameterReferenceType.ENVIRONMENT);
            parameter.setValue(value);

            if (environment.containsProperty(key.key()) && key.isOverridable()) {
                parameter.setValue(toSemicolonSeparatedString(key, environment.getProperty(key.key())));
                return parameter;
            }

            if (updateMode) {
                if (value == null) {
                    parameterRepository.delete(key.key());
                    return null;
                } else if (!value.equals(optionalParameter.get().getValue())) {
                    final Parameter updatedParameter = parameterRepository.update(parameter);
                    auditService.createEnvironmentAuditLog(
                            singletonMap(PARAMETER, updatedParameter.getKey()),
                            PARAMETER_UPDATED,
                            new Date(),
                            optionalParameter.get(),
                            updatedParameter);
                    eventManager.publishEvent(key, parameter);
                    return updatedParameter;
                } else {
                    return optionalParameter.get();
                }
            } else {
                if (value == null) {
                    return null;
                }
                final Parameter savedParameter = parameterRepository.create(parameter);
                auditService.createEnvironmentAuditLog(
                        singletonMap(PARAMETER, savedParameter.getKey()),
                        PARAMETER_CREATED,
                        new Date(),
                        null,
                        savedParameter);
                eventManager.publishEvent(key, parameter);
                return savedParameter;
            }

        } catch (final TechnicalException ex) {
            final String message = "An error occurs while trying to create parameter for key/value: " + key + '/' + value;
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    @Override
    public Parameter save(final Key key, final List<String> values) {
        return save(key, values==null ? null : join(SEPARATOR, values));
    }

    @Override
    public Parameter save(final Key key, final Map<String, String> values) {
        return save(key, values==null ? null : values.entrySet()
                .stream()
                .map(entry -> entry.getKey() + KV_SEPARATOR + entry.getValue())
                .collect(joining(SEPARATOR)));
    }

    private String toSemicolonSeparatedString(Key key, String value) {
        if (key.type() != null && List.class.isAssignableFrom(key.type())) {
            value = value.replace(",", SEPARATOR);
        }
        return value;
    }
}
