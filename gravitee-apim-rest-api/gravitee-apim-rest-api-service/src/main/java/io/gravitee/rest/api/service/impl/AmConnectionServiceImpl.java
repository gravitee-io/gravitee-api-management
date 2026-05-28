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
package io.gravitee.rest.api.service.impl;

import io.gravitee.common.util.DataEncryptor;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AmConnectionRepository;
import io.gravitee.repository.management.model.AmConnection;
import io.gravitee.rest.api.model.AmConnectionEntity;
import io.gravitee.rest.api.service.AmConnectionService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.Optional;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class AmConnectionServiceImpl implements AmConnectionService {

    private final AmConnectionRepository amConnectionRepository;
    private final DataEncryptor dataEncryptor;

    public AmConnectionServiceImpl(
        AmConnectionRepository amConnectionRepository,
        @Qualifier("apiPropertiesEncryptor") DataEncryptor dataEncryptor
    ) {
        this.amConnectionRepository = amConnectionRepository;
        this.dataEncryptor = dataEncryptor;
    }

    @Override
    public Optional<AmConnectionEntity> findByOrganizationId(String organizationId) {
        try {
            return amConnectionRepository.findByOrganizationId(organizationId).map(this::toEntity);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Failed to find am connection for organization " + organizationId, e);
        }
    }

    @Override
    public boolean hasToken(String organizationId) {
        try {
            return amConnectionRepository
                .findByOrganizationId(organizationId)
                .map(AmConnection::getServiceAccountAccessTokenEncrypted)
                .filter(StringUtils::hasText)
                .isPresent();
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Failed to read am connection for organization " + organizationId, e);
        }
    }

    @Override
    public AmConnectionEntity save(String organizationId, AmConnectionEntity entity) {
        try {
            Optional<AmConnection> existing = amConnectionRepository.findByOrganizationId(organizationId);

            AmConnection model = new AmConnection();
            model.setOrganizationId(organizationId);
            model.setBaseUrl(stripTrailingSlashes(entity.getBaseUrl()));
            model.setDefaultDomainId(entity.getDefaultDomainId());
            model.setDefaultDomainHrid(entity.getDefaultDomainHrid());
            model.setGatewayUrl(entity.getGatewayUrl());
            model.setServiceAccountAccessTokenEncrypted(encryptOrPreserve(entity.getServiceAccountAccessToken(), existing.orElse(null)));
            model.setUpdatedAt(new Date());

            AmConnection saved = existing.isPresent() ? amConnectionRepository.update(model) : amConnectionRepository.create(model);
            return toEntity(saved);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Failed to save am connection for organization " + organizationId, e);
        }
    }

    @Override
    public void delete(String organizationId) {
        try {
            amConnectionRepository.delete(organizationId);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Failed to delete am connection for organization " + organizationId, e);
        }
    }

    private String encryptOrPreserve(String plaintextToken, AmConnection existing) {
        if (plaintextToken == null) {
            return existing == null ? null : existing.getServiceAccountAccessTokenEncrypted();
        }
        if (!StringUtils.hasText(plaintextToken)) {
            return null;
        }
        try {
            return dataEncryptor.encrypt(plaintextToken);
        } catch (GeneralSecurityException e) {
            throw new TechnicalManagementException("Failed to encrypt am service account access token", e);
        }
    }

    private AmConnectionEntity toEntity(AmConnection model) {
        AmConnectionEntity entity = new AmConnectionEntity();
        entity.setOrganizationId(model.getOrganizationId());
        entity.setBaseUrl(model.getBaseUrl());
        entity.setServiceAccountAccessToken(decryptOrNull(model.getServiceAccountAccessTokenEncrypted()));
        entity.setDefaultDomainId(model.getDefaultDomainId());
        entity.setDefaultDomainHrid(model.getDefaultDomainHrid());
        entity.setGatewayUrl(model.getGatewayUrl());
        entity.setUpdatedAt(model.getUpdatedAt());
        return entity;
    }

    private String decryptOrNull(String ciphertext) {
        if (!StringUtils.hasText(ciphertext)) {
            return null;
        }
        try {
            return dataEncryptor.decrypt(ciphertext);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            log.warn("Failed to decrypt am service account access token, treating it as absent", e);
            return null;
        }
    }

    private static String stripTrailingSlashes(String baseUrl) {
        return baseUrl == null ? null : baseUrl.replaceAll("/+$", "");
    }
}
