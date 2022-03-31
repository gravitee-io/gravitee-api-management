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

import static io.gravitee.repository.management.model.Audit.AuditProperties.PARAMETER;
import static io.gravitee.repository.management.model.Parameter.AuditEvent.PARAMETER_CREATED;
import static io.gravitee.repository.management.model.Parameter.AuditEvent.PARAMETER_UPDATED;
import static java.lang.String.join;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import io.gravitee.common.event.EventManager;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ParameterRepository;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.KeyScope;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ParameterServiceImpl extends TransactionalService implements ParameterService {

    public static final String SEPARATOR = ";";
    public static final String KV_SEPARATOR = "@";

    private static final Logger LOGGER = LoggerFactory.getLogger(ParameterServiceImpl.class);

    @Inject
    private ParameterRepository parameterRepository;

    @Inject
    private AuditService auditService;

    @Inject
    private ConfigurableEnvironment environment;

    @Inject
    private EventManager eventManager;

    @Inject
    @Lazy
    private EnvironmentService environmentService;

    // Current context
    @Override
    public String find(
        ExecutionContext executionContext,
        Key key,
        io.gravitee.rest.api.model.parameters.ParameterReferenceType referenceType
    ) {
        return find(executionContext, key, null, referenceType);
    }

    @Override
    public boolean findAsBoolean(
        ExecutionContext executionContext,
        final Key key,
        final io.gravitee.rest.api.model.parameters.ParameterReferenceType referenceType
    ) {
        return findAsBoolean(executionContext, key, null, referenceType);
    }

    @Override
    public List<String> findAll(
        ExecutionContext executionContext,
        final Key key,
        final io.gravitee.rest.api.model.parameters.ParameterReferenceType referenceType
    ) {
        return findAll(executionContext, key, Function.identity(), null, null, referenceType);
    }

    @Override
    public Map<String, List<String>> findAll(
        ExecutionContext executionContext,
        final List<Key> keys,
        final io.gravitee.rest.api.model.parameters.ParameterReferenceType referenceType
    ) {
        return findAll(executionContext, keys, Function.identity(), null, null, referenceType);
    }

    @Override
    public <T> List<T> findAll(
        ExecutionContext executionContext,
        final Key key,
        final Function<String, T> mapper,
        final io.gravitee.rest.api.model.parameters.ParameterReferenceType referenceType
    ) {
        return findAll(executionContext, key, mapper, null, null, referenceType);
    }

    @Override
    public <T> Map<String, List<T>> findAll(
        ExecutionContext executionContext,
        final List<Key> keys,
        final Function<String, T> mapper,
        final io.gravitee.rest.api.model.parameters.ParameterReferenceType referenceType
    ) {
        return findAll(executionContext, keys, mapper, null, null, referenceType);
    }

    @Override
    public <T> List<T> findAll(
        ExecutionContext executionContext,
        final Key key,
        final Function<String, T> mapper,
        final Predicate<String> filter,
        final io.gravitee.rest.api.model.parameters.ParameterReferenceType referenceType
    ) {
        return findAll(executionContext, key, mapper, filter, null, referenceType);
    }

    @Override
    public <T> Map<String, List<T>> findAll(
        ExecutionContext executionContext,
        List<Key> keys,
        Function<String, T> mapper,
        Predicate<String> filter,
        final io.gravitee.rest.api.model.parameters.ParameterReferenceType referenceType
    ) {
        return findAll(executionContext, keys, mapper, filter, null, referenceType);
    }

    @Override
    public Parameter save(
        ExecutionContext executionContext,
        final Key key,
        final String value,
        final io.gravitee.rest.api.model.parameters.ParameterReferenceType referenceType
    ) {
        return save(executionContext, key, value, null, referenceType);
    }

    @Override
    public Parameter save(
        ExecutionContext executionContext,
        final Key key,
        final List<String> values,
        final io.gravitee.rest.api.model.parameters.ParameterReferenceType referenceType
    ) {
        return save(executionContext, key, values, null, referenceType);
    }

    @Override
    public Parameter save(
        ExecutionContext executionContext,
        final Key key,
        final Map<String, String> values,
        final io.gravitee.rest.api.model.parameters.ParameterReferenceType referenceType
    ) {
        return save(executionContext, key, values, null, referenceType);
    }

    // Specific context
    @Override
    public String find(
        ExecutionContext executionContext,
        final Key key,
        final String referenceId,
        final io.gravitee.rest.api.model.parameters.ParameterReferenceType referenceType
    ) {
        return GraviteeContext
            .getCurrentParameters()
            .computeIfAbsent(
                key,
                k -> {
                    final List<String> values = findAll(k, referenceId, referenceType, executionContext);
                    final String value;
                    if (values == null || values.isEmpty()) {
                        value = k.defaultValue();
                    } else {
                        value = String.join(SEPARATOR, values);
                    }
                    return value;
                }
            );
    }

    @Override
    public boolean findAsBoolean(
        ExecutionContext executionContext,
        final Key key,
        final String referenceId,
        final io.gravitee.rest.api.model.parameters.ParameterReferenceType referenceType
    ) {
        return Boolean.parseBoolean(find(executionContext, key, referenceId, referenceType));
    }

    @Override
    public List<String> findAll(
        final Key key,
        final String referenceId,
        final io.gravitee.rest.api.model.parameters.ParameterReferenceType referenceType,
        ExecutionContext executionContext
    ) {
        return findAll(executionContext, key, Function.identity(), null, referenceId, referenceType);
    }

    @Override
    public Map<String, List<String>> findAll(
        final List<Key> keys,
        final String referenceId,
        final io.gravitee.rest.api.model.parameters.ParameterReferenceType referenceType,
        ExecutionContext executionContext
    ) {
        return findAll(executionContext, keys, Function.identity(), null, referenceId, referenceType);
    }

    @Override
    public <T> List<T> findAll(
        ExecutionContext executionContext,
        final Key key,
        final Function<String, T> mapper,
        final String referenceId,
        final io.gravitee.rest.api.model.parameters.ParameterReferenceType referenceType
    ) {
        return findAll(executionContext, key, mapper, null, referenceId, referenceType);
    }

    @Override
    public <T> Map<String, List<T>> findAll(
        ExecutionContext executionContext,
        final List<Key> keys,
        final Function<String, T> mapper,
        final String referenceId,
        final io.gravitee.rest.api.model.parameters.ParameterReferenceType referenceType
    ) {
        return findAll(executionContext, keys, mapper, null, referenceId, referenceType);
    }

    @Override
    public <T> List<T> findAll(
        ExecutionContext executionContext,
        final Key key,
        final Function<String, T> mapper,
        final Predicate<String> filter,
        final String referenceId,
        final io.gravitee.rest.api.model.parameters.ParameterReferenceType referenceType
    ) {
        String refIdToUse = getEffectiveReferenceId(executionContext, referenceId, referenceType);
        try {
            Optional<Parameter> optionalParameter = this.getSystemParameter(key);
            if (optionalParameter.isPresent()) {
                return splitValue(optionalParameter.get().getValue(), mapper, filter);
            }
            switch (referenceType) {
                case ENVIRONMENT:
                    optionalParameter = this.getEnvParameter(key, refIdToUse);
                    if (optionalParameter.isPresent()) {
                        return splitValue(optionalParameter.get().getValue(), mapper, filter);
                    }
                    //String organizationId = "DEFAULT";
                    String organizationId = environmentService.findById(refIdToUse).getOrganizationId();
                    optionalParameter = this.getOrgParameter(key, organizationId);
                    if (optionalParameter.isPresent()) {
                        return splitValue(optionalParameter.get().getValue(), mapper, filter);
                    }
                case ORGANIZATION:
                    optionalParameter = this.getOrgParameter(key, refIdToUse);
                    if (optionalParameter.isPresent()) {
                        return splitValue(optionalParameter.get().getValue(), mapper, filter);
                    }
            }
            return splitValue(this.getDefaultParameterValue(key), mapper, filter);
        } catch (final TechnicalException ex) {
            final String message = "An error occurs while trying to find parameter values with key: " + key;
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    @Override
    public <T> Map<String, List<T>> findAll(
        ExecutionContext executionContext,
        List<Key> keys,
        Function<String, T> mapper,
        Predicate<String> filter,
        final String referenceId,
        final io.gravitee.rest.api.model.parameters.ParameterReferenceType referenceType
    ) {
        String refIdToUse = getEffectiveReferenceId(executionContext, referenceId, referenceType);
        try {
            List<Key> keysToFind = new ArrayList<>(keys);
            Map<String, List<T>> result = new HashMap<>();

            // Get System parameters
            for (Key keyToFind : keys) {
                this.getSystemParameter(keyToFind)
                    .ifPresent(
                        p -> {
                            result.put(p.getKey(), splitValue(p.getValue(), mapper, filter));
                            keysToFind.remove(keyToFind);
                        }
                    );
            }

            if (!keysToFind.isEmpty()) {
                switch (referenceType) {
                    case ENVIRONMENT:
                        this.getEnvParameters(keysToFind, refIdToUse)
                            .forEach(
                                p -> {
                                    result.put(p.getKey(), splitValue(p.getValue(), mapper, filter));
                                    keysToFind.remove(Key.findByKey(p.getKey()));
                                }
                            );
                        if (!keysToFind.isEmpty()) {
                            //String organizationId = "DEFAULT";//environmentService.findById(referenceId).getOrganizationId();
                            String organizationId = environmentService.findById(refIdToUse).getOrganizationId();
                            this.getOrgParameters(keysToFind, organizationId)
                                .forEach(
                                    p -> {
                                        result.put(p.getKey(), splitValue(p.getValue(), mapper, filter));
                                        keysToFind.remove(Key.findByKey(p.getKey()));
                                    }
                                );
                            if (!keysToFind.isEmpty()) {
                                keysToFind.forEach(k -> result.put(k.key(), splitValue(k.defaultValue(), mapper, filter)));
                            }
                        }
                        break;
                    case ORGANIZATION:
                        this.getOrgParameters(keysToFind, refIdToUse)
                            .forEach(
                                p -> {
                                    result.put(p.getKey(), splitValue(p.getValue(), mapper, filter));
                                    keysToFind.remove(Key.findByKey(p.getKey()));
                                }
                            );
                        if (!keysToFind.isEmpty()) {
                            keysToFind.forEach(k -> result.put(k.key(), splitValue(k.defaultValue(), mapper, filter)));
                        }
                        break;
                    default:
                        keysToFind.forEach(k -> result.put(k.key(), splitValue(k.defaultValue(), mapper, filter)));
                        break;
                }
            }

            return result;
        } catch (final TechnicalException ex) {
            final String message = "An error occurs while trying to find parameter values with keys: " + keys;
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    private String getEffectiveReferenceId(
        ExecutionContext executionContext,
        String referenceId,
        io.gravitee.rest.api.model.parameters.ParameterReferenceType referenceType
    ) {
        String refIdToUse = referenceId;
        if (refIdToUse == null) {
            if (referenceType == io.gravitee.rest.api.model.parameters.ParameterReferenceType.ORGANIZATION) {
                refIdToUse = executionContext.getOrganizationId();
            } else {
                refIdToUse = executionContext.getEnvironmentId();
            }
        }
        return refIdToUse;
    }

    private <T> List<T> splitValue(final String value, final Function<String, T> mapper, final Predicate<String> filter) {
        if (value == null) {
            return emptyList();
        }
        Stream<String> stream = stream(value.split(SEPARATOR));
        if (filter != null) {
            stream = stream.filter(filter);
        }
        return stream.map(mapper).collect(toList());
    }

    @Override
    public Parameter save(
        ExecutionContext executionContext,
        final Key key,
        final String value,
        final String referenceId,
        final io.gravitee.rest.api.model.parameters.ParameterReferenceType referenceType
    ) {
        String refIdToUse = getEffectiveReferenceId(executionContext, referenceId, referenceType);
        try {
            Optional<Parameter> optionalParameter = parameterRepository.findById(
                key.key(),
                refIdToUse,
                ParameterReferenceType.valueOf(referenceType.name())
            );
            final boolean updateMode = optionalParameter.isPresent();

            final Parameter parameter = new Parameter();
            parameter.setKey(key.key());
            parameter.setReferenceId(refIdToUse);
            parameter.setReferenceType(ParameterReferenceType.valueOf(referenceType.name()));
            parameter.setValue(value);

            if (environment.containsProperty(key.key()) && key.isOverridable()) {
                parameter.setValue(toSemicolonSeparatedString(key, environment.getProperty(key.key())));
                return parameter;
            }

            if (updateMode) {
                if (value == null) {
                    parameterRepository.delete(key.key(), refIdToUse, ParameterReferenceType.valueOf(referenceType.name()));
                    return null;
                } else if (!value.equals(optionalParameter.get().getValue())) {
                    final Parameter updatedParameter = parameterRepository.update(parameter);
                    auditService.createAuditLog(
                        executionContext,
                        singletonMap(PARAMETER, updatedParameter.getKey()),
                        PARAMETER_UPDATED,
                        new Date(),
                        optionalParameter.get(),
                        updatedParameter
                    );
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
                auditService.createAuditLog(
                    executionContext,
                    singletonMap(PARAMETER, savedParameter.getKey()),
                    PARAMETER_CREATED,
                    new Date(),
                    null,
                    savedParameter
                );
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
    public Parameter save(
        ExecutionContext executionContext,
        final Key key,
        final List<String> values,
        final String referenceId,
        final io.gravitee.rest.api.model.parameters.ParameterReferenceType referenceType
    ) {
        return save(
            executionContext,
            key,
            (values == null || values.isEmpty()) ? null : join(SEPARATOR, values),
            referenceId,
            referenceType
        );
    }

    @Override
    public Parameter save(
        ExecutionContext executionContext,
        final Key key,
        final Map<String, String> values,
        final String referenceId,
        final io.gravitee.rest.api.model.parameters.ParameterReferenceType referenceType
    ) {
        return save(
            executionContext,
            key,
            values == null
                ? null
                : values.entrySet().stream().map(entry -> entry.getKey() + KV_SEPARATOR + entry.getValue()).collect(joining(SEPARATOR)),
            referenceId,
            referenceType
        );
    }

    private String toSemicolonSeparatedString(Key key, String value) {
        if (key.type() != null && List.class.isAssignableFrom(key.type())) {
            value = value.replace(",", SEPARATOR);
        }
        return value;
    }

    private Optional<Parameter> getEnvParameter(Key key, String environmentId) throws TechnicalException {
        if (key.scopes().contains(KeyScope.ENVIRONMENT)) {
            return parameterRepository.findById(key.key(), environmentId, ParameterReferenceType.ENVIRONMENT);
        }
        return Optional.empty();
    }

    private List<Parameter> getEnvParameters(List<Key> keys, String environmentId) throws TechnicalException {
        List<Key> keysToFind = keys.stream().filter(k -> k.scopes().contains(KeyScope.ENVIRONMENT)).collect(toList());
        if (!keysToFind.isEmpty()) {
            return parameterRepository
                .findByKeys(keysToFind.stream().map(Key::key).collect(toList()), environmentId, ParameterReferenceType.ENVIRONMENT)
                .stream()
                .filter(Objects::nonNull)
                .collect(toList());
        }
        return Collections.emptyList();
    }

    private Optional<Parameter> getOrgParameter(Key key, String organizationId) throws TechnicalException {
        if (key.scopes().contains(KeyScope.ORGANIZATION)) {
            return parameterRepository.findById(key.key(), organizationId, ParameterReferenceType.ORGANIZATION);
        }
        return Optional.empty();
    }

    private List<Parameter> getOrgParameters(List<Key> keys, String organizationId) throws TechnicalException {
        List<Key> keysToFind = keys.stream().filter(k -> k.scopes().contains(KeyScope.ORGANIZATION)).collect(toList());
        if (!keysToFind.isEmpty()) {
            return parameterRepository
                .findByKeys(keysToFind.stream().map(Key::key).collect(toList()), organizationId, ParameterReferenceType.ORGANIZATION)
                .stream()
                .filter(Objects::nonNull)
                .collect(toList());
        }
        return Collections.emptyList();
    }

    private Optional<Parameter> getSystemParameter(Key key) {
        if (environment.containsProperty(key.key()) && key.isOverridable()) {
            final Parameter parameter = new Parameter();
            parameter.setKey(key.key());
            parameter.setValue(toSemicolonSeparatedString(key, environment.getProperty(key.key())));
            return Optional.of(parameter);
        }
        return Optional.empty();
    }

    private String getDefaultParameterValue(Key key) {
        return key.defaultValue();
    }
}
