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
package io.gravitee.rest.api.service.impl.configuration.identity;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.IdentityProviderActivationRepository;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.management.model.IdentityProvider;
import io.gravitee.repository.management.model.IdentityProviderActivation;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationEntity;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import java.util.*;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class IdentityProviderActivationServiceImpl extends AbstractService implements IdentityProviderActivationService {

    private final Logger LOGGER = LoggerFactory.getLogger(IdentityProviderActivationServiceImpl.class);

    @Lazy
    @Autowired
    private IdentityProviderActivationRepository identityProviderActivationRepository;

    @Autowired
    private AuditService auditService;

    @Override
    public Set<IdentityProviderActivationEntity> activateIdpOnTargets(
        ExecutionContext executionContext,
        String identityProviderId,
        ActivationTarget... targetsToAdd
    ) {
        LOGGER.debug("Activate identity provider {} on targets {} ", identityProviderId, targetsToAdd);

        try {
            Set<IdentityProviderActivationEntity> createdActivations = new HashSet<>();
            for (ActivationTarget target : targetsToAdd) {
                IdentityProviderActivation createdIdentityProviderActivation = createIdentityProviderActivation(
                    executionContext,
                    identityProviderId,
                    target
                );
                createdActivations.add(convert(createdIdentityProviderActivation));
            }
            return createdActivations;
        } catch (TechnicalException ex) {
            LOGGER.error(
                "An error occurs while trying to Activate identity provider {} on targets {}",
                identityProviderId,
                targetsToAdd,
                ex
            );
            throw new TechnicalManagementException(
                "An error occurs while trying to Activate identity provider " +
                identityProviderId +
                " on targets " +
                Arrays.toString(targetsToAdd),
                ex
            );
        }
    }

    @Override
    public Set<IdentityProviderActivationEntity> addIdpsOnTarget(
        ExecutionContext executionContext,
        ActivationTarget target,
        String... identityProviderIdsToAdd
    ) {
        LOGGER.debug("Add identity providers {} on target {} ", identityProviderIdsToAdd, target);

        try {
            Set<IdentityProviderActivationEntity> createdActivations = new HashSet<>();
            for (String identityProviderId : identityProviderIdsToAdd) {
                IdentityProviderActivation createdIdentityProviderActivation = createIdentityProviderActivation(
                    executionContext,
                    identityProviderId,
                    target
                );
                createdActivations.add(convert(createdIdentityProviderActivation));
            }
            return createdActivations;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to add identity providers {} on target {}", identityProviderIdsToAdd, target, ex);
            throw new TechnicalManagementException(
                "An error occurs while trying to add identity providers " +
                Arrays.toString(identityProviderIdsToAdd) +
                " on target " +
                target,
                ex
            );
        }
    }

    @Override
    public Set<IdentityProviderActivationEntity> findAllByIdentityProviderId(String identityProviderId) {
        LOGGER.debug("Find all activations for identity provider {}", identityProviderId);

        try {
            return identityProviderActivationRepository
                .findAllByIdentityProviderId(identityProviderId)
                .stream()
                .map(this::convert)
                .collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all activations for identity provider {}", identityProviderId, ex);
            throw new TechnicalManagementException(
                "An error occurs while trying to find all activations for identity provider " + identityProviderId,
                ex
            );
        }
    }

    @Override
    public Set<IdentityProviderActivationEntity> findAllByTarget(ActivationTarget target) {
        LOGGER.debug("Find all activations for target {}", target);

        try {
            return identityProviderActivationRepository
                .findAllByReferenceIdAndReferenceType(target.getReferenceId(), convert(target.getReferenceType()))
                .stream()
                .map(this::convert)
                .collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all activations for target {}", target, ex);
            throw new TechnicalManagementException("An error occurs while trying to find all activations for target " + target, ex);
        }
    }

    @Override
    public void deactivateIdpOnTargets(ExecutionContext executionContext, String identityProviderId, ActivationTarget... targetsToRemove) {
        LOGGER.debug("Deactivate identity provider {} on targets {} ", identityProviderId, targetsToRemove);

        try {
            for (ActivationTarget target : targetsToRemove) {
                deleteIdentityProviderActivation(executionContext, identityProviderId, target);
            }
        } catch (TechnicalException ex) {
            LOGGER.error(
                "An error occurs while trying to deactivate identity provider {} on targets {}",
                identityProviderId,
                targetsToRemove,
                ex
            );
            throw new TechnicalManagementException(
                "An error occurs while trying to deactivate identity provider " +
                identityProviderId +
                " on targets " +
                Arrays.toString(targetsToRemove),
                ex
            );
        }
    }

    @Override
    public void removeIdpsFromTarget(ExecutionContext executionContext, ActivationTarget target, String... identityProviderIdsToRemove) {
        LOGGER.debug("Remove identity providers {} from target {} ", identityProviderIdsToRemove, target);

        try {
            for (String identityProviderId : identityProviderIdsToRemove) {
                deleteIdentityProviderActivation(executionContext, identityProviderId, target);
            }
        } catch (TechnicalException ex) {
            LOGGER.error(
                "An error occurs while trying to remove identity providers {} from target {}",
                identityProviderIdsToRemove,
                target,
                ex
            );
            throw new TechnicalManagementException(
                "An error occurs while trying to remove identity providers " +
                Arrays.toString(identityProviderIdsToRemove) +
                " from target " +
                target,
                ex
            );
        }
    }

    @Override
    public void deactivateIdpOnAllTargets(ExecutionContext executionContext, String identityProviderId) {
        LOGGER.debug("Deactivate identity provider {} on all targets", identityProviderId);

        try {
            Set<IdentityProviderActivation> iPAsToRemove = identityProviderActivationRepository.findAllByIdentityProviderId(
                identityProviderId
            );

            identityProviderActivationRepository.deleteByIdentityProviderId(identityProviderId);

            for (IdentityProviderActivation ipa : iPAsToRemove) {
                auditService.createAuditLog(
                    executionContext,
                    Audit.AuditReferenceType.valueOf(ipa.getReferenceType().name()),
                    ipa.getReferenceId(),
                    Collections.singletonMap(Audit.AuditProperties.IDENTITY_PROVIDER, ipa.getIdentityProviderId()),
                    IdentityProvider.AuditEvent.IDENTITY_PROVIDER_DEACTIVATED,
                    new Date(),
                    ipa,
                    null
                );
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to deactivate identity provider {} on all targets", identityProviderId, ex);
            throw new TechnicalManagementException(
                "An error occurs while trying to deactivate identity provider " + identityProviderId + " on all targets",
                ex
            );
        }
    }

    @Override
    public void removeAllIdpsFromTarget(ExecutionContext executionContext, ActivationTarget target) {
        LOGGER.debug("Remove all identity providers from target {} ", target);

        try {
            Set<IdentityProviderActivation> iPAsToRemove = identityProviderActivationRepository.findAllByReferenceIdAndReferenceType(
                target.getReferenceId(),
                convert(target.getReferenceType())
            );

            identityProviderActivationRepository.deleteByReferenceIdAndReferenceType(
                target.getReferenceId(),
                convert(target.getReferenceType())
            );

            for (IdentityProviderActivation ipa : iPAsToRemove) {
                auditService.createAuditLog(
                    executionContext,
                    Audit.AuditReferenceType.valueOf(ipa.getReferenceType().name()),
                    ipa.getReferenceId(),
                    Collections.singletonMap(Audit.AuditProperties.IDENTITY_PROVIDER, ipa.getReferenceId()),
                    IdentityProvider.AuditEvent.IDENTITY_PROVIDER_DEACTIVATED,
                    new Date(),
                    ipa,
                    null
                );
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to remove all identity providers from target {}", target, ex);
            throw new TechnicalManagementException(
                "An error occurs while trying to remove all identity providers from target " + target,
                ex
            );
        }
    }

    @Override
    public void updateTargetIdp(ExecutionContext executionContext, ActivationTarget target, List<String> identityProviderIds) {
        final Set<IdentityProviderActivationEntity> allTargetActivations = this.findAllByTarget(target);
        allTargetActivations.forEach(
            ipa -> {
                if (!identityProviderIds.contains(ipa.getIdentityProvider())) {
                    this.removeIdpsFromTarget(executionContext, target, ipa.getIdentityProvider());
                } else {
                    identityProviderIds.remove(ipa.getIdentityProvider());
                }
            }
        );
        if (!identityProviderIds.isEmpty()) {
            this.addIdpsOnTarget(executionContext, target, identityProviderIds.toArray(new String[identityProviderIds.size()]));
        }
    }

    @NotNull
    private IdentityProviderActivation createIdentityProviderActivation(
        ExecutionContext executionContext,
        String identityProviderId,
        ActivationTarget target
    ) throws TechnicalException {
        IdentityProviderActivation createdIdentityProviderActivation = identityProviderActivationRepository.create(
            convert(identityProviderId, target, new Date())
        );

        auditService.createAuditLog(
            executionContext,
            Audit.AuditReferenceType.valueOf(target.getReferenceType().name()),
            target.getReferenceId(),
            Collections.singletonMap(Audit.AuditProperties.IDENTITY_PROVIDER, createdIdentityProviderActivation.getIdentityProviderId()),
            IdentityProvider.AuditEvent.IDENTITY_PROVIDER_ACTIVATED,
            createdIdentityProviderActivation.getCreatedAt(),
            null,
            createdIdentityProviderActivation
        );

        return createdIdentityProviderActivation;
    }

    private void deleteIdentityProviderActivation(ExecutionContext executionContext, String identityProviderId, ActivationTarget target)
        throws TechnicalException {
        Optional<IdentityProviderActivation> optIPAToRemove = identityProviderActivationRepository.findById(
            identityProviderId,
            target.getReferenceId(),
            convert(target.getReferenceType())
        );
        if (!optIPAToRemove.isPresent()) {
            throw new IdentityProviderActivationNotFoundException(identityProviderId, target.getReferenceId(), target.getReferenceType());
        }

        identityProviderActivationRepository.delete(identityProviderId, target.getReferenceId(), convert(target.getReferenceType()));

        auditService.createAuditLog(
            executionContext,
            Audit.AuditReferenceType.valueOf(target.getReferenceType().name()),
            target.getReferenceId(),
            Collections.singletonMap(Audit.AuditProperties.IDENTITY_PROVIDER, identityProviderId),
            IdentityProvider.AuditEvent.IDENTITY_PROVIDER_DEACTIVATED,
            new Date(),
            optIPAToRemove.get(),
            null
        );
    }

    private IdentityProviderActivation convert(String identityProviderId, ActivationTarget target, Date createdAt) {
        IdentityProviderActivation identityProviderActivation = new IdentityProviderActivation();

        identityProviderActivation.setIdentityProviderId(identityProviderId);
        identityProviderActivation.setReferenceId(target.getReferenceId());
        identityProviderActivation.setReferenceType(convert(target.getReferenceType()));
        identityProviderActivation.setCreatedAt(createdAt);

        return identityProviderActivation;
    }

    private IdentityProviderActivationEntity convert(IdentityProviderActivation identityProviderActivation) {
        IdentityProviderActivationEntity identityProviderActivationEntity = new IdentityProviderActivationEntity();

        identityProviderActivationEntity.setIdentityProvider(identityProviderActivation.getIdentityProviderId());
        identityProviderActivationEntity.setReferenceId(identityProviderActivation.getReferenceId());
        identityProviderActivationEntity.setReferenceType(convert(identityProviderActivation.getReferenceType()));
        identityProviderActivationEntity.setCreatedAt(identityProviderActivation.getCreatedAt());

        return identityProviderActivationEntity;
    }

    private io.gravitee.repository.management.model.IdentityProviderActivationReferenceType convert(
        IdentityProviderActivationReferenceType referenceType
    ) {
        return io.gravitee.repository.management.model.IdentityProviderActivationReferenceType.valueOf(referenceType.name());
    }

    private IdentityProviderActivationReferenceType convert(
        io.gravitee.repository.management.model.IdentityProviderActivationReferenceType referenceType
    ) {
        return IdentityProviderActivationReferenceType.valueOf(referenceType.name());
    }
}
