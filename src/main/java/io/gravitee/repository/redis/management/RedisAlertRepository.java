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
import io.gravitee.repository.management.api.AlertRepository;
import io.gravitee.repository.management.model.Alert;
import io.gravitee.repository.redis.management.internal.AlertRedisRepository;
import io.gravitee.repository.redis.management.model.RedisAlert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisAlertRepository implements AlertRepository {

    @Autowired
    private AlertRedisRepository alertRedisRepository;

    @Override
    public Optional<Alert> findById(final String alertId) throws TechnicalException {
        final RedisAlert redisAlert = alertRedisRepository.findById(alertId);
        return Optional.ofNullable(convert(redisAlert));
    }

    @Override
    public Alert create(final Alert alert) throws TechnicalException {
        final RedisAlert redisAlert = alertRedisRepository.saveOrUpdate(convert(alert));
        return convert(redisAlert);
    }

    @Override
    public Alert update(final Alert alert) throws TechnicalException {
        if (alert == null || alert.getName() == null) {
            throw new IllegalStateException("Alert to update must have a name");
        }

        final RedisAlert redisAlert = alertRedisRepository.findById(alert.getId());

        if (redisAlert == null) {
            throw new IllegalStateException(String.format("No alert found with name [%s]", alert.getId()));
        }

        final RedisAlert redisAlertUpdated = alertRedisRepository.saveOrUpdate(convert(alert));
        return convert(redisAlertUpdated);
    }

    @Override
    public Set<Alert> findAll() {
        final Set<RedisAlert> alerts = alertRedisRepository.findAll();

        return alerts.stream()
                .map(this::convert)
                .collect(Collectors.toSet());
    }

    @Override
    public List<Alert> findByReference(String referenceType, String referenceId) {
        final List<RedisAlert> alerts = alertRedisRepository.findByReference(referenceType, referenceId);
        return alerts.stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(final String alertId) throws TechnicalException {
        alertRedisRepository.delete(alertId);
    }

    private Alert convert(final RedisAlert redisAlert) {
        final Alert alert = new Alert();
        alert.setId(redisAlert.getId());
        alert.setName(redisAlert.getName());
        alert.setDescription(redisAlert.getDescription());
        alert.setReferenceType(redisAlert.getReferenceType());
        alert.setReferenceId(redisAlert.getReferenceId());
        alert.setMetricType(redisAlert.getMetricType());
        alert.setMetric(redisAlert.getMetric());
        alert.setType(redisAlert.getType());
        alert.setPlan(redisAlert.getPlan());
        alert.setThresholdType(redisAlert.getThresholdType());
        alert.setThreshold(redisAlert.getThreshold());
        alert.setEnabled(redisAlert.isEnabled());
        alert.setCreatedAt(redisAlert.getCreatedAt());
        alert.setUpdatedAt(redisAlert.getUpdatedAt());
        return alert;
    }

    private RedisAlert convert(final Alert alert) {
        final RedisAlert redisAlert = new RedisAlert();
        redisAlert.setId(alert.getId());
        redisAlert.setName(alert.getName());
        redisAlert.setDescription(alert.getDescription());
        redisAlert.setReferenceType(alert.getReferenceType());
        redisAlert.setReferenceId(alert.getReferenceId());
        redisAlert.setMetricType(alert.getMetricType());
        redisAlert.setMetric(alert.getMetric());
        redisAlert.setType(alert.getType());
        redisAlert.setPlan(alert.getPlan());
        redisAlert.setThresholdType(alert.getThresholdType());
        redisAlert.setThreshold(alert.getThreshold());
        redisAlert.setEnabled(alert.isEnabled());
        redisAlert.setCreatedAt(alert.getCreatedAt());
        redisAlert.setUpdatedAt(alert.getUpdatedAt());
        return redisAlert;
    }
}
