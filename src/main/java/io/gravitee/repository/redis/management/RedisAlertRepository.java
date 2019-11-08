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
package io.gravitee.repository.redis.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AlertTriggerRepository;
import io.gravitee.repository.management.model.AlertTrigger;
import io.gravitee.repository.redis.management.internal.AlertRedisRepository;
import io.gravitee.repository.redis.management.model.RedisAlertTrigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisAlertRepository implements AlertTriggerRepository {

    @Autowired
    private AlertRedisRepository alertRedisRepository;

    @Override
    public Optional<AlertTrigger> findById(final String alertId) throws TechnicalException {
        final RedisAlertTrigger redisAlertTrigger = alertRedisRepository.findById(alertId);
        return Optional.ofNullable(convert(redisAlertTrigger));
    }

    @Override
    public AlertTrigger create(final AlertTrigger alert) throws TechnicalException {
        final RedisAlertTrigger redisAlertTrigger = alertRedisRepository.saveOrUpdate(convert(alert));
        return convert(redisAlertTrigger);
    }

    @Override
    public AlertTrigger update(final AlertTrigger alert) throws TechnicalException {
        if (alert == null || alert.getName() == null) {
            throw new IllegalStateException("Alert to update must have a name");
        }

        final RedisAlertTrigger redisAlertTrigger = alertRedisRepository.findById(alert.getId());

        if (redisAlertTrigger == null) {
            throw new IllegalStateException(String.format("No alert found with id [%s]", alert.getId()));
        }

        final RedisAlertTrigger redisAlertTriggerUpdated = alertRedisRepository.saveOrUpdate(convert(alert));
        return convert(redisAlertTriggerUpdated);
    }

    @Override
    public Set<AlertTrigger> findAll() {
        final Set<RedisAlertTrigger> alerts = alertRedisRepository.findAll();

        return alerts.stream()
                .map(this::convert)
                .collect(Collectors.toSet());
    }

    @Override
    public List<AlertTrigger> findByReference(String referenceType, String referenceId) {
        final List<RedisAlertTrigger> alerts = alertRedisRepository.findByReference(referenceType, referenceId);
        return alerts.stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(final String alertId) throws TechnicalException {
        alertRedisRepository.delete(alertId);
    }

    private AlertTrigger convert(final RedisAlertTrigger redisAlertTrigger) {
        final AlertTrigger alert = new AlertTrigger();
        alert.setId(redisAlertTrigger.getId());
        alert.setReferenceType(redisAlertTrigger.getReferenceType());
        alert.setReferenceId(redisAlertTrigger.getReferenceId());
        alert.setName(redisAlertTrigger.getName());
        alert.setSeverity(redisAlertTrigger.getSeverity());
        alert.setType(redisAlertTrigger.getType());
        alert.setDescription(redisAlertTrigger.getDescription());
        alert.setDefinition(redisAlertTrigger.getDefinition());
        alert.setEnabled(redisAlertTrigger.isEnabled());
        alert.setCreatedAt(new Date(redisAlertTrigger.getCreatedAt()));
        alert.setUpdatedAt(new Date(redisAlertTrigger.getUpdatedAt()));
        return alert;
    }

    private RedisAlertTrigger convert(final AlertTrigger alert) {
        final RedisAlertTrigger redisAlertTrigger = new RedisAlertTrigger();
        redisAlertTrigger.setId(alert.getId());
        redisAlertTrigger.setReferenceType(alert.getReferenceType());
        redisAlertTrigger.setReferenceId(alert.getReferenceId());
        redisAlertTrigger.setName(alert.getName());
        redisAlertTrigger.setSeverity(alert.getSeverity());
        redisAlertTrigger.setType(alert.getType());
        redisAlertTrigger.setDescription(alert.getDescription());
        redisAlertTrigger.setDefinition(alert.getDefinition());
        redisAlertTrigger.setEnabled(alert.isEnabled());
        redisAlertTrigger.setCreatedAt(alert.getCreatedAt().getTime());
        if (alert.getUpdatedAt() != null) {
            redisAlertTrigger.setUpdatedAt(alert.getUpdatedAt().getTime());
        }
        return redisAlertTrigger;
    }
}
