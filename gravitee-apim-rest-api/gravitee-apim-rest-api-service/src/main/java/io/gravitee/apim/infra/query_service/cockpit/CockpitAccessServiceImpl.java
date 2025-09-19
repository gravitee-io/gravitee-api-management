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
package io.gravitee.apim.infra.query_service.cockpit; /*

import com.google.common.base.Strings;
import com.google.common.net.InternetDomainName;
import io.gravitee.apim.core.cockpit.model.AccessPointTemplate;
import io.gravitee.apim.core.cockpit.query_service.CockpitAccessService;
import io.gravitee.apim.core.installation.domain_service.InstallationTypeDomainService;
import io.gravitee.apim.core.installation.model.InstallationType;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Service;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */

import com.google.common.base.Strings;
import com.google.common.net.InternetDomainName;
import io.gravitee.apim.core.cockpit.model.AccessPointTemplate;
import io.gravitee.apim.core.cockpit.query_service.CockpitAccessService;
import io.gravitee.apim.core.installation.domain_service.InstallationTypeDomainService;
import io.gravitee.apim.core.installation.model.InstallationType;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class CockpitAccessServiceImpl implements CockpitAccessService {

    private static final String MULTI_TENANT_ACCESS_POINT_PROPERTY =
        "installation." + InstallationType.Labels.MULTI_TENANT + ".accessPoints";

    private final InstallationTypeDomainService installationTypeDomainService;
    private final ConfigurableEnvironment environment;

    private final Map<AccessPointTemplate.Type, List<AccessPointTemplate>> accessPointsTemplate = new EnumMap<>(
        AccessPointTemplate.Type.class
    );

    @PostConstruct
    public void afterPropertiesSet() {
        if (installationTypeDomainService.isMultiTenant()) {
            for (AccessPointTemplate.Type type : AccessPointTemplate.Type.values()) {
                List<AccessPointTemplate> accessPoints = new ArrayList<>();
                for (AccessPointTemplate.Target target : AccessPointTemplate.Target.values()) {
                    String host = environment.getProperty(
                        MULTI_TENANT_ACCESS_POINT_PROPERTY + "." + type.getLabel() + "." + target.getLabel() + ".host"
                    );
                    if (host != null) {
                        validateHost(host);
                        Boolean secured = environment.getProperty(
                            MULTI_TENANT_ACCESS_POINT_PROPERTY + "." + type.getLabel() + "." + target.getLabel() + ".secured",
                            Boolean.class,
                            true
                        );
                        accessPoints.add(AccessPointTemplate.builder().target(target).host(host).secured(secured).build());
                    }
                }
                if (!accessPoints.isEmpty()) {
                    accessPointsTemplate.put(type, accessPoints);
                }
            }
            if (accessPointsTemplate.isEmpty()) {
                throw new InvalidAccessPointException("Installation is multi-tenant but doesn't configure any access points.");
            }
        }
    }

    private void validateHost(String value) {
        String toValidate = value.replaceAll("[{}]", "").replaceAll(":\\d*", "");
        if (!isValidDomainName(toValidate)) {
            throw new InvalidAccessPointException("Installation access point '%s' is malformed.".formatted(value));
        }
    }

    public static boolean isValidDomainName(String domain) {
        if (Strings.isNullOrEmpty(domain)) {
            return false;
        }
        return InternetDomainName.isValid(domain);
    }

    @Override
    public Map<AccessPointTemplate.Type, List<AccessPointTemplate>> getAccessPointsTemplate() {
        return accessPointsTemplate;
    }

    static class InvalidAccessPointException extends RuntimeException {

        public InvalidAccessPointException(String message) {
            super(message);
        }
    }
}
