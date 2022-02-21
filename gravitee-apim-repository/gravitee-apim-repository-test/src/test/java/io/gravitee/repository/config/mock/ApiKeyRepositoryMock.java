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
package io.gravitee.repository.config.mock;

import static io.gravitee.repository.utils.DateUtils.parse;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.collections.Sets.newSet;

import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.model.ApiKey;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiKeyRepositoryMock extends AbstractRepositoryMock<ApiKeyRepository> {

    public ApiKeyRepositoryMock() {
        super(ApiKeyRepository.class);
    }

    @Override
    void prepare(ApiKeyRepository apiKeyRepository) throws Exception {
        final ApiKey apiKey1 = mockApiKey1();
        final ApiKey apiKey2 = mockApiKey2();
        final ApiKey apiKey3 = mockApiKey3();
        final ApiKey apiKey4 = mockApiKey4();
        final ApiKey apiKey5 = mockApiKey5();
        final ApiKey apiKey6 = mockApiKey6();

        when(apiKey1.getDaysToExpirationOnLastNotification()).thenReturn(30);
        when(apiKeyRepository.findById(anyString())).thenReturn(empty());
        when(apiKeyRepository.findById("id-of-apikey-1")).thenReturn(of(apiKey1));
        when(apiKeyRepository.findById("id-of-new-apikey")).thenReturn(of(apiKey1));
        when(apiKeyRepository.findByKey("d449098d-8c31-4275-ad59-8dd707865a34")).thenReturn(List.of(apiKey1, apiKey2));
        when(apiKeyRepository.findBySubscription("subscription1")).thenReturn(newSet(apiKey1, mock(ApiKey.class)));
        when(apiKeyRepository.findByKeyAndApi("d449098d-8c31-4275-ad59-8dd707865a34", "api2")).thenReturn(of(apiKey2));

        when(apiKeyRepository.update(argThat(o -> o == null || o.getId().equals("unknown_key_id")))).thenThrow(new IllegalStateException());

        ApiKey mockCriteria1 = mock(ApiKey.class);
        ApiKey mockCriteria1Revoked = mock(ApiKey.class);
        ApiKey mockCriteria2 = mock(ApiKey.class);
        when(mockCriteria1.getKey()).thenReturn("findByCriteria1");
        when(mockCriteria1Revoked.getKey()).thenReturn("findByCriteria1Revoked");
        when(mockCriteria2.getKey()).thenReturn("findByCriteria2");
        when(apiKeyRepository.findByCriteria(argThat(o -> o == null || o.getFrom() == 0))).thenReturn(asList(mockCriteria1, mockCriteria2));
        when(apiKeyRepository.findByCriteria(argThat(o -> o == null || o.getTo() == 1486771400000L)))
            .thenReturn(singletonList(mockCriteria1));
        when(apiKeyRepository.findByCriteria(argThat(o -> o == null || o.isIncludeRevoked())))
            .thenReturn(asList(mockCriteria2, mockCriteria1Revoked, mockCriteria1));

        when(apiKeyRepository.findByCriteria(argThat(o -> o != null && o.getExpireAfter() == 30019401755L)))
            .thenReturn(asList(apiKey2, apiKey3));
        when(apiKeyRepository.findByCriteria(argThat(o -> o != null && o.getExpireBefore() == 30019401755L)))
            .thenReturn(asList(mockCriteria2, mockCriteria1));
        when(
            apiKeyRepository.findByCriteria(
                argThat(o -> o != null && o.getExpireAfter() == 1439022010000L && o.getExpireBefore() == 1439022020000L)
            )
        )
            .thenReturn(asList(apiKey2, apiKey3));

        when(apiKeyRepository.findByKeyAndApi("findByCriteria2", "api2")).thenReturn(Optional.of(apiKey6));
    }

    private ApiKey mockApiKey1() {
        ApiKey apiKey = mock(ApiKey.class);
        when(apiKey.getId()).thenReturn("id-of-apikey-1");
        when(apiKey.getKey()).thenReturn("apiKey");
        when(apiKey.getExpireAt()).thenReturn(parse("11/02/2016"));
        when(apiKey.getSubscriptions()).thenReturn(List.of("subscription1"));
        when(apiKey.getSubscription()).thenReturn("subscription1");
        when(apiKey.getApi()).thenReturn("api1");
        when(apiKey.isRevoked()).thenReturn(true);
        when(apiKey.isPaused()).thenReturn(true);
        return apiKey;
    }

    private ApiKey mockApiKey2() {
        ApiKey apiKey = mock(ApiKey.class);
        when(apiKey.getId()).thenReturn("id-of-apikey-2");
        when(apiKey.getKey()).thenReturn("d449098d-8c31-4275-ad59-8dd707865a34");
        when(apiKey.getExpireAt()).thenReturn(parse("11/02/2016"));
        when(apiKey.getSubscriptions()).thenReturn(List.of("subscription2"));
        when(apiKey.isRevoked()).thenReturn(false);
        when(apiKey.isPaused()).thenReturn(false);
        return apiKey;
    }

    private ApiKey mockApiKey3() {
        ApiKey apiKey = mock(ApiKey.class);
        when(apiKey.getKey()).thenReturn("d449098d-8c31-4275-ad59-8dd707865a35");
        when(apiKey.getSubscriptions()).thenReturn(List.of("subscription1"));
        return apiKey;
    }

    private ApiKey mockApiKey4() {
        ApiKey apiKey = mock(ApiKey.class);
        when(apiKey.getId()).thenReturn("id-of-apikey-4");
        when(apiKey.getApplication()).thenReturn("app1");
        when(apiKey.getSubscriptions()).thenReturn(List.of("sub1"));
        return apiKey;
    }

    private ApiKey mockApiKey5() {
        ApiKey apiKey = mock(ApiKey.class);
        when(apiKey.getId()).thenReturn("id-of-apikey-5");
        when(apiKey.getApplication()).thenReturn("app1");
        when(apiKey.getSubscriptions()).thenReturn(List.of("sub1"));
        when(apiKey.isRevoked()).thenReturn(true);
        return apiKey;
    }

    private ApiKey mockApiKey6() {
        ApiKey apiKey = mock(ApiKey.class);
        when(apiKey.getId()).thenReturn("id-of-apikey-6");
        when(apiKey.getApplication()).thenReturn("app2");
        when(apiKey.getSubscriptions()).thenReturn(List.of("sub2"));
        return apiKey;
    }
}
