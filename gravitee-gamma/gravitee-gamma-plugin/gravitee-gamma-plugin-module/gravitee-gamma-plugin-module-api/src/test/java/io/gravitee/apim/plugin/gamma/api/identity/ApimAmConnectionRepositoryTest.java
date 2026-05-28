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
package io.gravitee.apim.plugin.gamma.api.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.AmConnectionEntity;
import io.gravitee.rest.api.service.AmConnectionService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApimAmConnectionRepositoryTest {

    @Mock
    AmConnectionService amConnectionService;

    ApimAmConnectionRepository repository() {
        return new ApimAmConnectionRepository(amConnectionService);
    }

    @Test
    void findByOrg_maps_entity_to_model() {
        AmConnectionEntity entity = new AmConnectionEntity();
        entity.setBaseUrl("https://am.example.com");
        entity.setServiceAccountAccessToken("plain-token");
        entity.setDefaultDomainId("dom-1");
        entity.setDefaultDomainHrid("dom-hrid-1");
        entity.setGatewayUrl("https://gw.example.com");
        when(amConnectionService.findByOrganizationId("org-1")).thenReturn(Optional.of(entity));

        Optional<AmConnection> result = repository().findByOrg("org-1");

        assertThat(result).isPresent();
        AmConnection model = result.get();
        assertThat(model.baseUrl()).isEqualTo("https://am.example.com");
        assertThat(model.serviceAccountAccessToken()).isEqualTo("plain-token");
        assertThat(model.defaultDomainId()).isEqualTo("dom-1");
        assertThat(model.defaultDomainHrid()).isEqualTo("dom-hrid-1");
        assertThat(model.gatewayUrl()).isEqualTo("https://gw.example.com");
    }

    @Test
    void findByOrg_returns_empty_when_service_empty() {
        when(amConnectionService.findByOrganizationId("org-1")).thenReturn(Optional.empty());
        assertThat(repository().findByOrg("org-1")).isEmpty();
    }

    @Test
    void hasTokenForOrg_delegates_to_service() {
        when(amConnectionService.hasToken("org-1")).thenReturn(true);
        assertThat(repository().hasTokenForOrg("org-1")).isTrue();
        verify(amConnectionService).hasToken("org-1");
    }

    @Test
    void save_builds_entity_from_model_and_returns_mapped_result() {
        AmConnection connection = new AmConnection(
            "https://am.example.com",
            "plain-token",
            "dom-1",
            "dom-hrid-1",
            "https://gw.example.com"
        );
        AmConnectionEntity returned = new AmConnectionEntity();
        returned.setBaseUrl("https://am.example.com");
        returned.setServiceAccountAccessToken("plain-token");
        when(amConnectionService.save(eq("org-1"), any())).thenReturn(returned);

        AmConnection result = repository().save("org-1", connection);

        ArgumentCaptor<AmConnectionEntity> captor = ArgumentCaptor.forClass(AmConnectionEntity.class);
        verify(amConnectionService).save(eq("org-1"), captor.capture());
        AmConnectionEntity sent = captor.getValue();
        assertThat(sent.getOrganizationId()).isEqualTo("org-1");
        assertThat(sent.getBaseUrl()).isEqualTo("https://am.example.com");
        assertThat(sent.getServiceAccountAccessToken()).isEqualTo("plain-token");
        assertThat(sent.getDefaultDomainId()).isEqualTo("dom-1");
        assertThat(sent.getDefaultDomainHrid()).isEqualTo("dom-hrid-1");
        assertThat(sent.getGatewayUrl()).isEqualTo("https://gw.example.com");

        assertThat(result.baseUrl()).isEqualTo("https://am.example.com");
        assertThat(result.serviceAccountAccessToken()).isEqualTo("plain-token");
    }

    @Test
    void save_passes_null_token_through() {
        AmConnection connection = new AmConnection("https://am.example.com", null, null, null, null);
        when(amConnectionService.save(eq("org-1"), any())).thenReturn(new AmConnectionEntity());

        repository().save("org-1", connection);

        ArgumentCaptor<AmConnectionEntity> captor = ArgumentCaptor.forClass(AmConnectionEntity.class);
        verify(amConnectionService).save(eq("org-1"), captor.capture());
        assertThat(captor.getValue().getServiceAccountAccessToken()).isNull();
    }

    @Test
    void deleteByOrg_delegates_to_service() {
        repository().deleteByOrg("org-1");
        verify(amConnectionService).delete("org-1");
    }
}
