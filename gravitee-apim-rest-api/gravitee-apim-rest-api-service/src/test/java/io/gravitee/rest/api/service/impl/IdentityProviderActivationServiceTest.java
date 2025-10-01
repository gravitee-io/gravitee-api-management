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

import static io.gravitee.repository.management.model.IdentityProvider.AuditEvent.IDENTITY_PROVIDER_ACTIVATED;
import static io.gravitee.repository.management.model.IdentityProvider.AuditEvent.IDENTITY_PROVIDER_DEACTIVATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.util.collections.Sets.newSet;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.IdentityProviderActivationRepository;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.management.model.IdentityProviderActivation;
import io.gravitee.repository.management.model.IdentityProviderActivationReferenceType;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService.ActivationTarget;
import io.gravitee.rest.api.service.impl.configuration.identity.IdentityProviderActivationNotFoundException;
import io.gravitee.rest.api.service.impl.configuration.identity.IdentityProviderActivationServiceImpl;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class IdentityProviderActivationServiceTest {

    private static final String IDENTITY_PROVIDER_ID = "my-identity-provider-id";
    private static final String TARGET_REFERENCE_ID = "my-reference-id";
    private static final IdentityProviderActivationReferenceType TARGET_REFERENCE_TYPE =
        IdentityProviderActivationReferenceType.ENVIRONMENT;
    private static final String ANOTHER_IDENTITY_PROVIDER_ID = "another-identity-provider-id";
    private static final String ANOTHER_TARGET_REFERENCE_ID = "another-reference-id";
    private static final IdentityProviderActivationReferenceType ANOTHER_TARGET_REFERENCE_TYPE =
        IdentityProviderActivationReferenceType.ORGANIZATION;

    @InjectMocks
    private IdentityProviderActivationService identityProviderActivationService = new IdentityProviderActivationServiceImpl();

    @Mock
    private IdentityProviderActivationRepository identityProviderActivationRepository;

    @Mock
    private AuditService auditService;

    @Test
    public void shouldActivateIdpOnTargets() throws TechnicalException {
        // Given
        final Date now = new Date();
        IdentityProviderActivation createdIPA = new IdentityProviderActivation();
        createdIPA.setIdentityProviderId(IDENTITY_PROVIDER_ID);
        createdIPA.setReferenceId(TARGET_REFERENCE_ID);
        createdIPA.setReferenceType(TARGET_REFERENCE_TYPE);
        createdIPA.setCreatedAt(now);

        IdentityProviderActivation anotherCreatedIPA = new IdentityProviderActivation();
        anotherCreatedIPA.setIdentityProviderId(IDENTITY_PROVIDER_ID);
        anotherCreatedIPA.setReferenceId(ANOTHER_TARGET_REFERENCE_ID);
        anotherCreatedIPA.setReferenceType(ANOTHER_TARGET_REFERENCE_TYPE);
        anotherCreatedIPA.setCreatedAt(now);

        doReturn(createdIPA)
            .when(identityProviderActivationRepository)
            .create(
                argThat(
                    (IdentityProviderActivation ipa) ->
                        IDENTITY_PROVIDER_ID.equals(ipa.getIdentityProviderId()) &&
                        TARGET_REFERENCE_ID.equals(ipa.getReferenceId()) &&
                        TARGET_REFERENCE_TYPE.equals(ipa.getReferenceType())
                )
            );
        doReturn(anotherCreatedIPA)
            .when(identityProviderActivationRepository)
            .create(
                argThat(
                    (IdentityProviderActivation ipa) ->
                        IDENTITY_PROVIDER_ID.equals(ipa.getIdentityProviderId()) &&
                        ANOTHER_TARGET_REFERENCE_ID.equals(ipa.getReferenceId()) &&
                        ANOTHER_TARGET_REFERENCE_TYPE.equals(ipa.getReferenceType())
                )
            );

        // When
        Set<IdentityProviderActivationEntity> activatedIdentityProviders = this.identityProviderActivationService.activateIdpOnTargets(
            GraviteeContext.getExecutionContext(),
            IDENTITY_PROVIDER_ID,
            new ActivationTarget(
                TARGET_REFERENCE_ID,
                io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType.valueOf(
                    TARGET_REFERENCE_TYPE.name()
                )
            ),
            new ActivationTarget(
                ANOTHER_TARGET_REFERENCE_ID,
                io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType.valueOf(
                    ANOTHER_TARGET_REFERENCE_TYPE.name()
                )
            )
        );

        // Then
        assertNotNull(activatedIdentityProviders);
        assertEquals(2, activatedIdentityProviders.size());

        verify(identityProviderActivationRepository).create(
            argThat(
                (IdentityProviderActivation ipa) ->
                    IDENTITY_PROVIDER_ID.equals(ipa.getIdentityProviderId()) &&
                    TARGET_REFERENCE_ID.equals(ipa.getReferenceId()) &&
                    TARGET_REFERENCE_TYPE.equals(ipa.getReferenceType())
            )
        );
        verify(identityProviderActivationRepository).create(
            argThat(
                (IdentityProviderActivation ipa) ->
                    IDENTITY_PROVIDER_ID.equals(ipa.getIdentityProviderId()) &&
                    ANOTHER_TARGET_REFERENCE_ID.equals(ipa.getReferenceId()) &&
                    ANOTHER_TARGET_REFERENCE_TYPE.equals(ipa.getReferenceType())
            )
        );

        verify(auditService).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getReferenceType().equals(Audit.AuditReferenceType.valueOf(TARGET_REFERENCE_TYPE.name())) &&
                    auditLogData.getReferenceId().equals(TARGET_REFERENCE_ID) &&
                    auditLogData.getEvent().equals(IDENTITY_PROVIDER_ACTIVATED) &&
                    auditLogData.getCreatedAt().equals(now) &&
                    auditLogData.getOldValue() == null &&
                    auditLogData.getNewValue().equals(createdIPA)
            )
        );

        verify(auditService).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getReferenceType().equals(Audit.AuditReferenceType.valueOf(ANOTHER_TARGET_REFERENCE_TYPE.name())) &&
                    auditLogData.getReferenceId().equals(ANOTHER_TARGET_REFERENCE_ID) &&
                    auditLogData.getEvent().equals(IDENTITY_PROVIDER_ACTIVATED) &&
                    auditLogData.getCreatedAt().equals(now) &&
                    auditLogData.getOldValue() == null &&
                    auditLogData.getNewValue().equals(anotherCreatedIPA)
            )
        );
    }

    @Test
    public void shouldAddIdpsOnTarget() throws TechnicalException {
        // Given
        final Date now = new Date();
        IdentityProviderActivation createdIPA = new IdentityProviderActivation();
        createdIPA.setIdentityProviderId(IDENTITY_PROVIDER_ID);
        createdIPA.setReferenceId(TARGET_REFERENCE_ID);
        createdIPA.setReferenceType(TARGET_REFERENCE_TYPE);
        createdIPA.setCreatedAt(now);

        IdentityProviderActivation anotherCreatedIPA = new IdentityProviderActivation();
        anotherCreatedIPA.setIdentityProviderId(ANOTHER_IDENTITY_PROVIDER_ID);
        anotherCreatedIPA.setReferenceId(TARGET_REFERENCE_ID);
        anotherCreatedIPA.setReferenceType(TARGET_REFERENCE_TYPE);
        anotherCreatedIPA.setCreatedAt(now);

        doReturn(createdIPA)
            .when(identityProviderActivationRepository)
            .create(
                argThat(
                    (IdentityProviderActivation ipa) ->
                        IDENTITY_PROVIDER_ID.equals(ipa.getIdentityProviderId()) &&
                        TARGET_REFERENCE_ID.equals(ipa.getReferenceId()) &&
                        TARGET_REFERENCE_TYPE.equals(ipa.getReferenceType())
                )
            );
        doReturn(anotherCreatedIPA)
            .when(identityProviderActivationRepository)
            .create(
                argThat(
                    (IdentityProviderActivation ipa) ->
                        ANOTHER_IDENTITY_PROVIDER_ID.equals(ipa.getIdentityProviderId()) &&
                        TARGET_REFERENCE_ID.equals(ipa.getReferenceId()) &&
                        TARGET_REFERENCE_TYPE.equals(ipa.getReferenceType())
                )
            );

        // When
        Set<IdentityProviderActivationEntity> activatedIdentityProviders = this.identityProviderActivationService.addIdpsOnTarget(
            GraviteeContext.getExecutionContext(),
            new ActivationTarget(
                TARGET_REFERENCE_ID,
                io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType.valueOf(
                    TARGET_REFERENCE_TYPE.name()
                )
            ),
            IDENTITY_PROVIDER_ID,
            ANOTHER_IDENTITY_PROVIDER_ID
        );

        // Then
        assertNotNull(activatedIdentityProviders);
        assertEquals(2, activatedIdentityProviders.size());

        verify(identityProviderActivationRepository).create(
            argThat(
                (IdentityProviderActivation ipa) ->
                    IDENTITY_PROVIDER_ID.equals(ipa.getIdentityProviderId()) &&
                    TARGET_REFERENCE_ID.equals(ipa.getReferenceId()) &&
                    TARGET_REFERENCE_TYPE.equals(ipa.getReferenceType())
            )
        );
        verify(identityProviderActivationRepository).create(
            argThat(
                (IdentityProviderActivation ipa) ->
                    ANOTHER_IDENTITY_PROVIDER_ID.equals(ipa.getIdentityProviderId()) &&
                    TARGET_REFERENCE_ID.equals(ipa.getReferenceId()) &&
                    TARGET_REFERENCE_TYPE.equals(ipa.getReferenceType())
            )
        );

        verify(auditService).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getReferenceType().equals(Audit.AuditReferenceType.valueOf(TARGET_REFERENCE_TYPE.name())) &&
                    auditLogData.getReferenceId().equals(TARGET_REFERENCE_ID) &&
                    auditLogData.getEvent().equals(IDENTITY_PROVIDER_ACTIVATED) &&
                    auditLogData.getCreatedAt().equals(now) &&
                    auditLogData.getOldValue() == null &&
                    auditLogData.getNewValue().equals(createdIPA)
            )
        );

        verify(auditService).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getReferenceType().equals(Audit.AuditReferenceType.valueOf(TARGET_REFERENCE_TYPE.name())) &&
                    auditLogData.getReferenceId().equals(TARGET_REFERENCE_ID) &&
                    auditLogData.getEvent().equals(IDENTITY_PROVIDER_ACTIVATED) &&
                    auditLogData.getCreatedAt().equals(now) &&
                    auditLogData.getOldValue() == null &&
                    auditLogData.getNewValue().equals(anotherCreatedIPA)
            )
        );
    }

    @Test
    public void shouldFindAllByIdentityProviderId() throws TechnicalException {
        // Given
        final Date now = new Date();
        IdentityProviderActivation ipa = new IdentityProviderActivation();
        ipa.setIdentityProviderId(IDENTITY_PROVIDER_ID);
        ipa.setReferenceId(TARGET_REFERENCE_ID);
        ipa.setReferenceType(TARGET_REFERENCE_TYPE);
        ipa.setCreatedAt(now);

        IdentityProviderActivation anotherIpa = new IdentityProviderActivation();
        anotherIpa.setIdentityProviderId(IDENTITY_PROVIDER_ID);
        anotherIpa.setReferenceId(ANOTHER_TARGET_REFERENCE_ID);
        anotherIpa.setReferenceType(ANOTHER_TARGET_REFERENCE_TYPE);
        anotherIpa.setCreatedAt(now);

        doReturn(newSet(ipa, anotherIpa)).when(identityProviderActivationRepository).findAllByIdentityProviderId(IDENTITY_PROVIDER_ID);

        // When
        Set<IdentityProviderActivationEntity> foundIdentityProviders = this.identityProviderActivationService.findAllByIdentityProviderId(
            IDENTITY_PROVIDER_ID
        );

        // Then
        assertNotNull(foundIdentityProviders);
        assertEquals(2, foundIdentityProviders.size());

        verify(identityProviderActivationRepository).findAllByIdentityProviderId(IDENTITY_PROVIDER_ID);
    }

    @Test
    public void shouldFindAllByTarget() throws TechnicalException {
        // Given
        final Date now = new Date();
        IdentityProviderActivation ipa = new IdentityProviderActivation();
        ipa.setIdentityProviderId(IDENTITY_PROVIDER_ID);
        ipa.setReferenceId(TARGET_REFERENCE_ID);
        ipa.setReferenceType(TARGET_REFERENCE_TYPE);
        ipa.setCreatedAt(now);

        IdentityProviderActivation anotherIpa = new IdentityProviderActivation();
        anotherIpa.setIdentityProviderId(ANOTHER_IDENTITY_PROVIDER_ID);
        anotherIpa.setReferenceId(TARGET_REFERENCE_ID);
        anotherIpa.setReferenceType(TARGET_REFERENCE_TYPE);
        anotherIpa.setCreatedAt(now);

        doReturn(newSet(ipa, anotherIpa))
            .when(identityProviderActivationRepository)
            .findAllByReferenceIdAndReferenceType(TARGET_REFERENCE_ID, TARGET_REFERENCE_TYPE);

        // When
        Set<IdentityProviderActivationEntity> foundIdentityProviders = this.identityProviderActivationService.findAllByTarget(
            new ActivationTarget(
                TARGET_REFERENCE_ID,
                io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType.valueOf(
                    TARGET_REFERENCE_TYPE.name()
                )
            )
        );

        // Then
        assertNotNull(foundIdentityProviders);
        assertEquals(2, foundIdentityProviders.size());

        verify(identityProviderActivationRepository).findAllByReferenceIdAndReferenceType(TARGET_REFERENCE_ID, TARGET_REFERENCE_TYPE);
    }

    @Test
    public void shouldDeactivateIdpOnTargets() throws TechnicalException {
        // Given
        final Date now = new Date();
        IdentityProviderActivation ipaToRemove = new IdentityProviderActivation();
        ipaToRemove.setIdentityProviderId(IDENTITY_PROVIDER_ID);
        ipaToRemove.setReferenceId(TARGET_REFERENCE_ID);
        ipaToRemove.setReferenceType(TARGET_REFERENCE_TYPE);
        ipaToRemove.setCreatedAt(now);

        IdentityProviderActivation anotherIpaToRemove = new IdentityProviderActivation();
        anotherIpaToRemove.setIdentityProviderId(IDENTITY_PROVIDER_ID);
        anotherIpaToRemove.setReferenceId(ANOTHER_TARGET_REFERENCE_ID);
        anotherIpaToRemove.setReferenceType(ANOTHER_TARGET_REFERENCE_TYPE);
        anotherIpaToRemove.setCreatedAt(now);

        doReturn(Optional.of(ipaToRemove))
            .when(identityProviderActivationRepository)
            .findById(IDENTITY_PROVIDER_ID, TARGET_REFERENCE_ID, TARGET_REFERENCE_TYPE);
        doReturn(Optional.of(anotherIpaToRemove))
            .when(identityProviderActivationRepository)
            .findById(IDENTITY_PROVIDER_ID, ANOTHER_TARGET_REFERENCE_ID, ANOTHER_TARGET_REFERENCE_TYPE);

        // When
        this.identityProviderActivationService.deactivateIdpOnTargets(
            GraviteeContext.getExecutionContext(),
            IDENTITY_PROVIDER_ID,
            new ActivationTarget(
                TARGET_REFERENCE_ID,
                io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType.valueOf(
                    TARGET_REFERENCE_TYPE.name()
                )
            ),
            new ActivationTarget(
                ANOTHER_TARGET_REFERENCE_ID,
                io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType.valueOf(
                    ANOTHER_TARGET_REFERENCE_TYPE.name()
                )
            )
        );

        // Then
        verify(identityProviderActivationRepository).findById(IDENTITY_PROVIDER_ID, TARGET_REFERENCE_ID, TARGET_REFERENCE_TYPE);
        verify(identityProviderActivationRepository).findById(
            IDENTITY_PROVIDER_ID,
            ANOTHER_TARGET_REFERENCE_ID,
            ANOTHER_TARGET_REFERENCE_TYPE
        );

        verify(identityProviderActivationRepository).delete(IDENTITY_PROVIDER_ID, TARGET_REFERENCE_ID, TARGET_REFERENCE_TYPE);
        verify(identityProviderActivationRepository).delete(
            IDENTITY_PROVIDER_ID,
            ANOTHER_TARGET_REFERENCE_ID,
            ANOTHER_TARGET_REFERENCE_TYPE
        );

        verify(auditService).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getReferenceType().equals(Audit.AuditReferenceType.valueOf(TARGET_REFERENCE_TYPE.name())) &&
                    auditLogData.getReferenceId().equals(TARGET_REFERENCE_ID) &&
                    auditLogData.getEvent().equals(IDENTITY_PROVIDER_DEACTIVATED) &&
                    auditLogData.getOldValue().equals(ipaToRemove) &&
                    auditLogData.getNewValue() == null
            )
        );

        verify(auditService).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getReferenceType().equals(Audit.AuditReferenceType.valueOf(ANOTHER_TARGET_REFERENCE_TYPE.name())) &&
                    auditLogData.getReferenceId().equals(ANOTHER_TARGET_REFERENCE_ID) &&
                    auditLogData.getEvent().equals(IDENTITY_PROVIDER_DEACTIVATED) &&
                    auditLogData.getOldValue().equals(anotherIpaToRemove) &&
                    auditLogData.getNewValue() == null
            )
        );
    }

    @Test
    public void shouldRemoveIdpsFromTarget() throws TechnicalException {
        // Given
        final Date now = new Date();
        IdentityProviderActivation ipaToRemove = new IdentityProviderActivation();
        ipaToRemove.setIdentityProviderId(IDENTITY_PROVIDER_ID);
        ipaToRemove.setReferenceId(TARGET_REFERENCE_ID);
        ipaToRemove.setReferenceType(TARGET_REFERENCE_TYPE);
        ipaToRemove.setCreatedAt(now);

        IdentityProviderActivation anotherIpaToRemove = new IdentityProviderActivation();
        anotherIpaToRemove.setIdentityProviderId(ANOTHER_IDENTITY_PROVIDER_ID);
        anotherIpaToRemove.setReferenceId(TARGET_REFERENCE_ID);
        anotherIpaToRemove.setReferenceType(TARGET_REFERENCE_TYPE);
        anotherIpaToRemove.setCreatedAt(now);

        doReturn(Optional.of(ipaToRemove))
            .when(identityProviderActivationRepository)
            .findById(IDENTITY_PROVIDER_ID, TARGET_REFERENCE_ID, TARGET_REFERENCE_TYPE);
        doReturn(Optional.of(anotherIpaToRemove))
            .when(identityProviderActivationRepository)
            .findById(ANOTHER_IDENTITY_PROVIDER_ID, TARGET_REFERENCE_ID, TARGET_REFERENCE_TYPE);

        // When
        this.identityProviderActivationService.removeIdpsFromTarget(
            GraviteeContext.getExecutionContext(),
            new ActivationTarget(
                TARGET_REFERENCE_ID,
                io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType.valueOf(
                    TARGET_REFERENCE_TYPE.name()
                )
            ),
            IDENTITY_PROVIDER_ID,
            ANOTHER_IDENTITY_PROVIDER_ID
        );

        // Then
        verify(identityProviderActivationRepository).findById(IDENTITY_PROVIDER_ID, TARGET_REFERENCE_ID, TARGET_REFERENCE_TYPE);
        verify(identityProviderActivationRepository).findById(ANOTHER_IDENTITY_PROVIDER_ID, TARGET_REFERENCE_ID, TARGET_REFERENCE_TYPE);

        verify(identityProviderActivationRepository).delete(IDENTITY_PROVIDER_ID, TARGET_REFERENCE_ID, TARGET_REFERENCE_TYPE);
        verify(identityProviderActivationRepository).delete(ANOTHER_IDENTITY_PROVIDER_ID, TARGET_REFERENCE_ID, TARGET_REFERENCE_TYPE);

        verify(auditService).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getReferenceType().equals(Audit.AuditReferenceType.valueOf(TARGET_REFERENCE_TYPE.name())) &&
                    auditLogData.getReferenceId().equals(TARGET_REFERENCE_ID) &&
                    auditLogData.getEvent().equals(IDENTITY_PROVIDER_DEACTIVATED) &&
                    auditLogData.getOldValue().equals(ipaToRemove) &&
                    auditLogData.getNewValue() == null
            )
        );

        verify(auditService).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getReferenceType().equals(Audit.AuditReferenceType.valueOf(TARGET_REFERENCE_TYPE.name())) &&
                    auditLogData.getReferenceId().equals(TARGET_REFERENCE_ID) &&
                    auditLogData.getEvent().equals(IDENTITY_PROVIDER_DEACTIVATED) &&
                    auditLogData.getOldValue().equals(anotherIpaToRemove) &&
                    auditLogData.getNewValue() == null
            )
        );
    }

    @Test
    public void shouldDeactivateIdpOnAllTargets() throws TechnicalException {
        // Given
        final Date now = new Date();
        IdentityProviderActivation ipaToRemove = new IdentityProviderActivation();
        ipaToRemove.setIdentityProviderId(IDENTITY_PROVIDER_ID);
        ipaToRemove.setReferenceId(TARGET_REFERENCE_ID);
        ipaToRemove.setReferenceType(TARGET_REFERENCE_TYPE);
        ipaToRemove.setCreatedAt(now);

        IdentityProviderActivation anotherIpaToRemove = new IdentityProviderActivation();
        anotherIpaToRemove.setIdentityProviderId(IDENTITY_PROVIDER_ID);
        anotherIpaToRemove.setReferenceId(ANOTHER_TARGET_REFERENCE_ID);
        anotherIpaToRemove.setReferenceType(ANOTHER_TARGET_REFERENCE_TYPE);
        anotherIpaToRemove.setCreatedAt(now);

        doReturn(newSet(ipaToRemove, anotherIpaToRemove))
            .when(identityProviderActivationRepository)
            .findAllByIdentityProviderId(IDENTITY_PROVIDER_ID);

        // When
        this.identityProviderActivationService.deactivateIdpOnAllTargets(GraviteeContext.getExecutionContext(), IDENTITY_PROVIDER_ID);

        // Then
        verify(identityProviderActivationRepository).findAllByIdentityProviderId(IDENTITY_PROVIDER_ID);

        verify(identityProviderActivationRepository).deleteByIdentityProviderId(IDENTITY_PROVIDER_ID);

        verify(auditService).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getReferenceType().equals(Audit.AuditReferenceType.valueOf(TARGET_REFERENCE_TYPE.name())) &&
                    auditLogData.getReferenceId().equals(TARGET_REFERENCE_ID) &&
                    auditLogData.getEvent().equals(IDENTITY_PROVIDER_DEACTIVATED) &&
                    auditLogData.getOldValue().equals(ipaToRemove) &&
                    auditLogData.getNewValue() == null
            )
        );

        verify(auditService).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getReferenceType().equals(Audit.AuditReferenceType.valueOf(ANOTHER_TARGET_REFERENCE_TYPE.name())) &&
                    auditLogData.getReferenceId().equals(ANOTHER_TARGET_REFERENCE_ID) &&
                    auditLogData.getEvent().equals(IDENTITY_PROVIDER_DEACTIVATED) &&
                    auditLogData.getOldValue().equals(anotherIpaToRemove) &&
                    auditLogData.getNewValue() == null
            )
        );
    }

    @Test
    public void shouldRemoveAllIdpsFromTarget() throws TechnicalException {
        // Given
        final Date now = new Date();
        IdentityProviderActivation ipaToRemove = new IdentityProviderActivation();
        ipaToRemove.setIdentityProviderId(IDENTITY_PROVIDER_ID);
        ipaToRemove.setReferenceId(TARGET_REFERENCE_ID);
        ipaToRemove.setReferenceType(TARGET_REFERENCE_TYPE);
        ipaToRemove.setCreatedAt(now);

        IdentityProviderActivation anotherIpaToRemove = new IdentityProviderActivation();
        anotherIpaToRemove.setIdentityProviderId(ANOTHER_IDENTITY_PROVIDER_ID);
        anotherIpaToRemove.setReferenceId(TARGET_REFERENCE_ID);
        anotherIpaToRemove.setReferenceType(TARGET_REFERENCE_TYPE);
        anotherIpaToRemove.setCreatedAt(now);

        doReturn(newSet(ipaToRemove, anotherIpaToRemove))
            .when(identityProviderActivationRepository)
            .findAllByReferenceIdAndReferenceType(TARGET_REFERENCE_ID, TARGET_REFERENCE_TYPE);

        // When
        this.identityProviderActivationService.removeAllIdpsFromTarget(
            GraviteeContext.getExecutionContext(),
            new ActivationTarget(
                TARGET_REFERENCE_ID,
                io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType.valueOf(
                    TARGET_REFERENCE_TYPE.name()
                )
            )
        );

        // Then
        verify(identityProviderActivationRepository).findAllByReferenceIdAndReferenceType(TARGET_REFERENCE_ID, TARGET_REFERENCE_TYPE);

        verify(identityProviderActivationRepository).deleteByReferenceIdAndReferenceType(TARGET_REFERENCE_ID, TARGET_REFERENCE_TYPE);

        verify(auditService).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getReferenceType().equals(Audit.AuditReferenceType.valueOf(TARGET_REFERENCE_TYPE.name())) &&
                    auditLogData.getReferenceId().equals(TARGET_REFERENCE_ID) &&
                    auditLogData.getEvent().equals(IDENTITY_PROVIDER_DEACTIVATED) &&
                    auditLogData.getOldValue().equals(ipaToRemove) &&
                    auditLogData.getNewValue() == null
            )
        );

        verify(auditService).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getReferenceType().equals(Audit.AuditReferenceType.valueOf(TARGET_REFERENCE_TYPE.name())) &&
                    auditLogData.getReferenceId().equals(TARGET_REFERENCE_ID) &&
                    auditLogData.getEvent().equals(IDENTITY_PROVIDER_DEACTIVATED) &&
                    auditLogData.getOldValue().equals(anotherIpaToRemove) &&
                    auditLogData.getNewValue() == null
            )
        );
    }

    @Test(expected = IdentityProviderActivationNotFoundException.class)
    public void shouldNotDeactivateIdpOnTargetWhenNotActivated() throws TechnicalException {
        // Given
        doReturn(Optional.empty())
            .when(identityProviderActivationRepository)
            .findById(IDENTITY_PROVIDER_ID, TARGET_REFERENCE_ID, TARGET_REFERENCE_TYPE);

        // When
        this.identityProviderActivationService.deactivateIdpOnTargets(
            GraviteeContext.getExecutionContext(),
            IDENTITY_PROVIDER_ID,
            new ActivationTarget(
                TARGET_REFERENCE_ID,
                io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType.valueOf(
                    TARGET_REFERENCE_TYPE.name()
                )
            )
        );
    }

    @Test(expected = IdentityProviderActivationNotFoundException.class)
    public void shouldNotRemoveIdpsFromTargetWhenNotActivated() throws TechnicalException {
        // Given
        doReturn(Optional.empty())
            .when(identityProviderActivationRepository)
            .findById(IDENTITY_PROVIDER_ID, TARGET_REFERENCE_ID, TARGET_REFERENCE_TYPE);

        // When
        this.identityProviderActivationService.removeIdpsFromTarget(
            GraviteeContext.getExecutionContext(),
            new ActivationTarget(
                TARGET_REFERENCE_ID,
                io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType.valueOf(
                    TARGET_REFERENCE_TYPE.name()
                )
            ),
            IDENTITY_PROVIDER_ID
        );
    }
}
