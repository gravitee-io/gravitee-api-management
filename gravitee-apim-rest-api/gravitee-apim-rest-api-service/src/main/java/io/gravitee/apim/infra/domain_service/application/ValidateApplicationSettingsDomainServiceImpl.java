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
package io.gravitee.apim.infra.domain_service.application;

import static io.gravitee.repository.management.model.Application.METADATA_CLIENT_ID;
import static io.gravitee.repository.management.model.ApplicationStatus.*;

import io.gravitee.apim.core.application.domain_service.ValidateApplicationSettingsDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.apim.core.utils.StringUtils;
import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.application.OAuthClientSettings;
import io.gravitee.rest.api.model.application.SimpleApplicationSettings;
import io.gravitee.rest.api.model.application.TlsSettings;
import io.gravitee.rest.api.model.clientcertificate.CreateClientCertificate;
import io.gravitee.rest.api.model.configuration.application.ApplicationGrantTypeEntity;
import io.gravitee.rest.api.model.configuration.application.ApplicationTypeEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.configuration.application.ApplicationTypeService;
import java.net.MalformedURLException;
import java.net.URI;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import joptsimple.internal.Strings;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Service
public class ValidateApplicationSettingsDomainServiceImpl implements ValidateApplicationSettingsDomainService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationTypeService applicationTypeService;
    private final ParameterService parameterService;

    public ValidateApplicationSettingsDomainServiceImpl(
        @Lazy ApplicationRepository applicationRepository,
        ApplicationTypeService applicationTypeService,
        ParameterService parameterService
    ) {
        this.applicationRepository = applicationRepository;
        this.applicationTypeService = applicationTypeService;
        this.parameterService = parameterService;
    }

    @Override
    public Result<Input> validateAndSanitize(Input input) {
        if (input.settings() == null) {
            // default to empty simple app
            return Result.ofValue(input.sanitized(ApplicationSettings.builder().app(new SimpleApplicationSettings()).build()));
        }

        var errors = new ArrayList<Error>();
        var sanitizedBuilder = input.settings().toBuilder();

        if (input.settings().getApp() != null) {
            validateSimpleSettings(input, sanitizedBuilder, errors);
        } else if (input.settings().getOauth() != null) {
            validateOAuthSettings(input, sanitizedBuilder, errors);
        }

        if (input.settings().getTls() != null) {
            errors.addAll(validateTlsSettings(input.settings().getTls()));
        }

        return Result.ofBoth(input.sanitized(sanitizedBuilder.build()), errors);
    }

    private void validateSimpleSettings(Input input, ApplicationSettings.ApplicationSettingsBuilder sanitizedBuilder, List<Error> errors) {
        var simpleSettings = input.settings().getApp().toBuilder().build();

        if (StringUtils.isNotEmpty(simpleSettings.getClientId())) {
            errors.addAll(validateClientId(input.auditInfo().environmentId(), input.applicationId(), simpleSettings));
        }

        sanitizedBuilder.app(simpleSettings);
    }

    private void validateOAuthSettings(Input input, ApplicationSettings.ApplicationSettingsBuilder sanitizedBuilder, List<Error> errors) {
        var oauthSettings = input.settings().getOauth().toBuilder().build();

        if (!isClientRegistrationEnabled(input.auditInfo())) {
            errors.add(
                Error.severe(
                    "configuring OAuth requires client registration to be enabled on environment [%s]",
                    input.auditInfo().environmentId()
                )
            );
        }

        var appType = applicationTypeService.getApplicationType(oauthSettings.getApplicationType());
        errors.addAll(validateGrantTypes(appType, oauthSettings));
        errors.addAll(validateRedirectURIs(appType, oauthSettings));
        oauthSettings.setResponseTypes(getResponseTypes(appType, oauthSettings));
        sanitizedBuilder.oauth(oauthSettings);
    }

    private List<Error> validateTlsSettings(TlsSettings tls) {
        var errors = new ArrayList<Error>();
        boolean hasSingle = StringUtils.isNotEmpty(tls.getClientCertificate());
        boolean hasList = CollectionUtils.isNotEmpty(tls.getClientCertificates());

        if (hasSingle && hasList) {
            errors.add(Error.severe("cannot set both clientCertificate and clientCertificates, use clientCertificates only"));
            return errors;
        }

        if (hasSingle) {
            errors.add(Error.warning("clientCertificate is deprecated, use clientCertificates instead"));
            errors.addAll(validateCertificatePem(tls.getClientCertificate()).errors());
        }

        if (hasList) {
            for (var cert : tls.getClientCertificates()) {
                errors.addAll(validateCertificateEntry(cert));
            }
        }

        return errors;
    }

    private List<Error> validateCertificateEntry(CreateClientCertificate cert) {
        var errors = new ArrayList<Error>();
        var parsed = validateCertificatePem(cert.certificate());
        errors.addAll(parsed.errors());

        if (cert.startsAt() != null && cert.endsAt() != null && !cert.startsAt().before(cert.endsAt())) {
            errors.add(Error.severe("certificate [%s]: startsAt must be before endsAt", cert.name()));
        }

        if (cert.endsAt() != null && parsed.certificate() != null) {
            if (cert.endsAt().after(parsed.certificate().getNotAfter())) {
                errors.add(Error.warning("certificate [%s]: endsAt is after certificate expiration date", cert.name()));
            }
        }

        return errors;
    }

    private record CertificateParseResult(List<Error> errors, X509Certificate certificate) {
        static CertificateParseResult ofErrors(List<Error> errors) {
            return new CertificateParseResult(errors, null);
        }

        static CertificateParseResult of(X509Certificate cert) {
            return new CertificateParseResult(List.of(), cert);
        }
    }

    private CertificateParseResult validateCertificatePem(String pem) {
        Certificate[] certificates;
        try {
            certificates = KeyStoreUtils.loadPemCertificates(pem);
        } catch (Exception e) {
            return CertificateParseResult.ofErrors(List.of(Error.severe("certificate is not a valid PEM")));
        }

        if (certificates == null || certificates.length == 0) {
            return CertificateParseResult.ofErrors(List.of(Error.severe("certificate is empty")));
        }

        var x509 = (X509Certificate) certificates[0];
        if (x509.getBasicConstraints() != -1) {
            return CertificateParseResult.ofErrors(List.of(Error.severe("certificate is a CA certificate")));
        }

        return CertificateParseResult.of(x509);
    }

    private List<Error> validateGrantTypes(ApplicationTypeEntity type, OAuthClientSettings settings) {
        var allowedGrantTypes = getAllowedGrantTypes(type);
        var mandatoryGrantTypes = getMandatoryGrantTypes(type);
        var grantTypes = new ArrayList<>(settings.getGrantTypes());
        if (CollectionUtils.isEmpty(grantTypes)) {
            return List.of(
                Error.severe(
                    "OAuth application of type [%s] must have at least one of [%s] as a grant type",
                    type.getName(),
                    Strings.join(allowedGrantTypes, ",")
                )
            );
        }
        if (!grantTypes.containsAll(mandatoryGrantTypes)) {
            return List.of(
                Error.severe(
                    "OAuth application of type [%s] must have at least [%s] as a grant type",
                    type.getName(),
                    Strings.join(mandatoryGrantTypes, ",")
                )
            );
        }
        grantTypes.removeAll(allowedGrantTypes);
        if (CollectionUtils.isNotEmpty(grantTypes)) {
            return List.of(
                Error.severe("unknown grant types [%s] for OAuth application of type [%s]", Strings.join(grantTypes, ","), type.getName())
            );
        }
        return List.of();
    }

    private List<Error> validateRedirectURIs(ApplicationTypeEntity type, OAuthClientSettings settings) {
        var redirectURIs = settings.getRedirectUris();

        if (Boolean.TRUE.equals(type.getRequires_redirect_uris()) && CollectionUtils.isEmpty(redirectURIs)) {
            return List.of(Error.severe("application type [%s] requires redirect URIs to be defined", type.getName()));
        }

        if (CollectionUtils.isEmpty(redirectURIs)) {
            settings.setRedirectUris(List.of());
            return List.of();
        }

        var errors = new ArrayList<Error>();
        for (var uri : redirectURIs) {
            try {
                var validURL = URI.create(uri).toURL();
                log.debug("redirect URI [{}] has been validated", validURL);
            } catch (IllegalArgumentException | MalformedURLException e) {
                errors.add(Error.severe("invalid redirect URI [%s]", uri));
            }
        }

        return errors;
    }

    private List<String> getAllowedGrantTypes(ApplicationTypeEntity type) {
        return type.getAllowed_grant_types() == null
            ? List.of()
            : type.getAllowed_grant_types().stream().map(ApplicationGrantTypeEntity::getType).toList();
    }

    private List<String> getMandatoryGrantTypes(ApplicationTypeEntity type) {
        return type.getMandatory_grant_types() == null
            ? List.of()
            : type.getMandatory_grant_types().stream().map(ApplicationGrantTypeEntity::getType).toList();
    }

    private List<String> getResponseTypes(ApplicationTypeEntity type, OAuthClientSettings settings) {
        return type
            .getAllowed_grant_types()
            .stream()
            .filter(grantType -> settings.getGrantTypes().contains(grantType.getType()))
            .map(ApplicationGrantTypeEntity::getResponse_types)
            .flatMap(Collection::stream)
            .distinct()
            .toList();
    }

    private boolean isClientRegistrationEnabled(AuditInfo auditInfo) {
        return parameterService.findAsBoolean(
            new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId()),
            Key.APPLICATION_REGISTRATION_ENABLED,
            auditInfo.environmentId(),
            ParameterReferenceType.ENVIRONMENT
        );
    }

    private List<Error> validateClientId(String environmentId, String applicationId, SimpleApplicationSettings settings) {
        try {
            var clientId = settings.getClientId();

            var activeApps = applicationRepository
                .findAllByEnvironment(environmentId, ACTIVE)
                .stream()
                .filter(app -> !app.getId().equals(applicationId))
                .toList();

            for (var app : activeApps) {
                if (app.getMetadata() != null && clientId.equals(app.getMetadata().get(METADATA_CLIENT_ID))) {
                    return List.of(
                        Error.severe(
                            "client id [%s] is already defined for application [%s] with id [%s] on environment [%s]",
                            settings.getClientId(),
                            app.getName(),
                            app.getId(),
                            environmentId
                        )
                    );
                }
            }
            return List.of();
        } catch (TechnicalException e) {
            log.error("unable to find active applications", e);
            throw new TechnicalDomainException(e.getMessage());
        }
    }
}
