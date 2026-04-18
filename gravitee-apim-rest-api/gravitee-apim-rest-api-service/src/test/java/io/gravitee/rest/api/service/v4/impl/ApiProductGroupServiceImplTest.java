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
package io.gravitee.rest.api.service.v4.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiProductsRepository;
import io.gravitee.repository.management.model.ApiProduct;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.notification.ApiHook;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiProductGroupServiceImplTest {

    private static final String ORGANIZATION_ID = "org-1";
    private static final String ENVIRONMENT_ID = "env-1";
    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);

    @InjectMocks
    private ApiProductGroupServiceImpl service;

    @Mock
    private ApiProductsRepository apiProductsRepository;

    @Mock
    private NotifierService notifierService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private UserService userService;

    @Test
    void should_add_group_to_api_product() throws TechnicalException {
        ApiProduct apiProduct = new ApiProduct();
        apiProduct.setId("api-product-1");
        apiProduct.setEnvironmentId(ENVIRONMENT_ID);

        when(apiProductsRepository.findById("api-product-1")).thenReturn(Optional.of(apiProduct));

        service.addGroup(EXECUTION_CONTEXT, "api-product-1", "group-1");

        assertThat(apiProduct.getGroups()).contains("group-1");
        verify(apiProductsRepository).update(apiProduct);
        verify(notifierService).trigger(
            eq(EXECUTION_CONTEXT),
            eq(ApiHook.API_UPDATED),
            eq(NotificationReferenceType.API_PRODUCT),
            eq("api-product-1"),
            any()
        );
    }

    @Test
    void should_add_group_to_api_product_with_existing_groups() throws TechnicalException {
        ApiProduct apiProduct = new ApiProduct();
        apiProduct.setId("api-product-1");
        apiProduct.setEnvironmentId(ENVIRONMENT_ID);
        apiProduct.setGroups(new HashSet<>(Set.of("existing-group")));

        when(apiProductsRepository.findById("api-product-1")).thenReturn(Optional.of(apiProduct));

        service.addGroup(EXECUTION_CONTEXT, "api-product-1", "group-1");

        assertThat(apiProduct.getGroups()).containsExactlyInAnyOrder("existing-group", "group-1");
        verify(apiProductsRepository).update(apiProduct);
    }

    @Test
    void should_remove_group_from_api_product() throws TechnicalException {
        ApiProduct apiProduct = new ApiProduct();
        apiProduct.setId("api-product-1");
        apiProduct.setEnvironmentId(ENVIRONMENT_ID);
        apiProduct.setGroups(new HashSet<>(Set.of("group-1", "group-2")));

        when(apiProductsRepository.findById("api-product-1")).thenReturn(Optional.of(apiProduct));

        service.removeGroup(EXECUTION_CONTEXT, "api-product-1", "group-1");

        assertThat(apiProduct.getGroups()).containsExactly("group-2");
        verify(apiProductsRepository).update(apiProduct);
        verify(notifierService).trigger(
            eq(EXECUTION_CONTEXT),
            eq(ApiHook.API_UPDATED),
            eq(NotificationReferenceType.API_PRODUCT),
            eq("api-product-1"),
            any()
        );
    }

    @Test
    void should_not_update_when_group_not_in_set() throws TechnicalException {
        ApiProduct apiProduct = new ApiProduct();
        apiProduct.setId("api-product-1");
        apiProduct.setEnvironmentId(ENVIRONMENT_ID);
        apiProduct.setGroups(new HashSet<>(Set.of("group-2")));

        when(apiProductsRepository.findById("api-product-1")).thenReturn(Optional.of(apiProduct));

        service.removeGroup(EXECUTION_CONTEXT, "api-product-1", "group-1");

        assertThat(apiProduct.getGroups()).containsExactly("group-2");
        verify(apiProductsRepository, never()).update(any());
        verify(notifierService, never()).trigger(any(), any(ApiHook.class), any(NotificationReferenceType.class), any(), any());
    }

    @Test
    void should_throw_when_api_product_not_found_on_add() throws TechnicalException {
        when(apiProductsRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addGroup(EXECUTION_CONTEXT, "missing", "group-1")).isInstanceOf(ApiProductNotFoundException.class);
    }

    @Test
    void should_throw_when_api_product_not_found_on_remove() throws TechnicalException {
        when(apiProductsRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeGroup(EXECUTION_CONTEXT, "missing", "group-1")).isInstanceOf(
            ApiProductNotFoundException.class
        );
    }

    @Test
    void should_throw_when_api_product_in_different_environment() throws TechnicalException {
        ApiProduct apiProduct = new ApiProduct();
        apiProduct.setId("api-product-1");
        apiProduct.setEnvironmentId("other-env");

        when(apiProductsRepository.findById("api-product-1")).thenReturn(Optional.of(apiProduct));

        assertThatThrownBy(() -> service.addGroup(EXECUTION_CONTEXT, "api-product-1", "group-1")).isInstanceOf(
            ApiProductNotFoundException.class
        );

        verify(apiProductsRepository, never()).update(any());
    }

    @Test
    void should_wrap_technical_exception_on_add() throws TechnicalException {
        when(apiProductsRepository.findById("api-product-1")).thenThrow(new TechnicalException("db error"));

        assertThatThrownBy(() -> service.addGroup(EXECUTION_CONTEXT, "api-product-1", "group-1"))
            .isInstanceOf(TechnicalManagementException.class)
            .hasCauseInstanceOf(TechnicalException.class);
    }

    @Test
    void should_wrap_technical_exception_on_remove() throws TechnicalException {
        when(apiProductsRepository.findById("api-product-1")).thenThrow(new TechnicalException("db error"));

        assertThatThrownBy(() -> service.removeGroup(EXECUTION_CONTEXT, "api-product-1", "group-1"))
            .isInstanceOf(TechnicalManagementException.class)
            .hasCauseInstanceOf(TechnicalException.class);
    }
}
