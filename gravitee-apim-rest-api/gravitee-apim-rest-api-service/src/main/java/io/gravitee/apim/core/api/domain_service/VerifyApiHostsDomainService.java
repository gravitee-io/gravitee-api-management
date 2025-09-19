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
package io.gravitee.apim.core.api.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.exception.DuplicatedHostException;
import io.gravitee.apim.core.api.exception.HostAlreadyExistsException;
import io.gravitee.apim.core.api.exception.InvalidHostException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@DomainService
public class VerifyApiHostsDomainService {

    /**
     * According to <a href="https://www.rfc-editor.org/rfc/rfc1123">RFC-1123</a> and <a href="https://www.rfc-editor.org/rfc/rfc952">RFC-952</a>
     * - hostname label can contain lowercase and digits characters.
     * - hostname label can contain dash or underscores, but not starts or ends with these characters
     * - each hostname label must have a max length of 63 characters
     */
    @SuppressWarnings("squid:S5998") // A max host size validation is done before the regexp to avoid applying it
    private static final Pattern HOST_PATTERN = Pattern.compile(
        "^([a-z0-9]|[a-z0-9][a-z0-9\\-_]{0,61}[a-z0-9])(\\.([a-z0-9]|[a-z0-9][a-z0-9\\-_]{0,61}[a-z0-9]))*$"
    );

    // Kafka host must have a first segment that is less than 50 characters
    private static final Pattern FIRST_SEGMENT_LENGTH_INVALID_PATTERN = Pattern.compile("^[^.]{50,}");

    private final ApiQueryService apiQueryService;

    public boolean checkApiHosts(String environmentId, String apiId, List<String> hosts, ListenerType listenerType) {
        checkHostsListIsNotEmpty(hosts);

        checkHostsAreNotBlank(hosts);

        checkHostsAreNotLongerThan255(hosts);

        checkHostsAreRfc1123Compliant(hosts);

        checkNoDuplicate(hosts);

        checkHostsAreAvailable(environmentId, apiId, hosts);

        checkHostFirstSegment(hosts, listenerType);

        return true;
    }

    private static void checkHostFirstSegment(List<String> hosts, ListenerType listenerType) {
        if (ListenerType.KAFKA.equals(listenerType)) {
            if (hosts.stream().anyMatch(host -> FIRST_SEGMENT_LENGTH_INVALID_PATTERN.matcher(host).lookingAt())) {
                throw new InvalidHostException("The first segment must be less than 50 characters.");
            }
        }
    }

    private static void checkHostsListIsNotEmpty(List<String> hosts) {
        if (hosts == null || hosts.isEmpty()) {
            throw new InvalidHostException("At least one host is required for the TCP listener.");
        }
    }

    private static void checkHostsAreNotBlank(List<String> hosts) {
        if (hosts.stream().anyMatch(host -> host == null || host.isBlank())) {
            throw new InvalidHostException("The hosts should not be null or blank.");
        }
    }

    private static void checkHostsAreNotLongerThan255(List<String> hosts) {
        if (hosts.stream().anyMatch(host -> host.length() > 255)) {
            throw new InvalidHostException("The hosts should not be greater than 255 characters.");
        }
    }

    private static void checkHostsAreRfc1123Compliant(List<String> hosts) {
        if (hosts.stream().anyMatch(host -> !HOST_PATTERN.matcher(host).lookingAt())) {
            throw new InvalidHostException("The hosts should be valid.");
        }
    }

    private void checkNoDuplicate(List<String> hosts) throws DuplicatedHostException {
        var set = new HashSet<>();
        var duplicates = hosts
            .stream()
            .filter(n -> !set.add(n))
            .toList();

        if (!duplicates.isEmpty()) {
            throw new DuplicatedHostException(String.join(", ", duplicates));
        }
    }

    private void checkHostsAreAvailable(String environmentId, String apiId, List<String> hosts) {
        var existingHosts = listHostsByEnvironmentId(environmentId, apiId);
        var duplicates = hosts.stream().filter(existingHosts::contains).toList();
        if (!duplicates.isEmpty()) {
            throw new HostAlreadyExistsException(String.join(", ", duplicates));
        }
    }

    private Set<String> listHostsByEnvironmentId(String environmentId, String apiId) {
        return apiQueryService
            .search(
                ApiSearchCriteria.builder().environmentId(environmentId).build(),
                null,
                ApiFieldFilter.builder().pictureExcluded(true).build()
            )
            .filter(
                api ->
                    !api.getId().equals(apiId) &&
                    DefinitionVersion.V4.equals(api.getDefinitionVersion()) &&
                    ((ApiType.PROXY.equals(api.getType()) && null != api.getApiDefinitionHttpV4()) ||
                        (ApiType.NATIVE.equals(api.getType()) && null != api.getApiDefinitionNativeV4()))
            )
            .flatMap(this::extractHostsFromListeners)
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
    }

    private Stream<String> extractHostsFromListeners(Api api) {
        if (api.getType() == ApiType.PROXY && api.getApiDefinitionHttpV4() != null) {
            return api
                .getApiDefinitionHttpV4()
                .getListeners()
                .stream()
                .filter(TcpListener.class::isInstance)
                .map(TcpListener.class::cast)
                .map(TcpListener::getHosts)
                .filter(extractedHosts -> !extractedHosts.isEmpty())
                .flatMap(List::stream);
        } else if (api.getType() == ApiType.NATIVE && api.getApiDefinitionNativeV4() != null) {
            return api
                .getApiDefinitionNativeV4()
                .getListeners()
                .stream()
                .filter(KafkaListener.class::isInstance)
                .map(KafkaListener.class::cast)
                .map(KafkaListener::getHost);
        }
        return Stream.of();
    }
}
