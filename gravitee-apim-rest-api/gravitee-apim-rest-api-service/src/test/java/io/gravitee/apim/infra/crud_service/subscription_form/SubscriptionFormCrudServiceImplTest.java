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
package io.gravitee.apim.infra.crud_service.subscription_form;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.SubscriptionFormFixtures;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.infra.adapter.SubscriptionFormAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionFormRepository;
import io.gravitee.repository.management.model.SubscriptionForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubscriptionFormCrudServiceImplTest {

    @Mock
    SubscriptionFormRepository repository;

    SubscriptionFormCrudServiceImpl service;

    SubscriptionFormAdapter subscriptionFormAdapter = SubscriptionFormAdapter.INSTANCE;

    @Captor
    ArgumentCaptor<SubscriptionForm> captor;

    @BeforeEach
    void setUp() {
        service = new SubscriptionFormCrudServiceImpl(repository);
    }

    @Nested
    class Create {

        @BeforeEach
        void setUp() throws TechnicalException {
            when(repository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        void should_create_a_subscription_form() throws TechnicalException {
            var subscriptionForm = SubscriptionFormFixtures.aSubscriptionForm();

            service.create(subscriptionForm);

            verify(repository).create(captor.capture());
            assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(subscriptionFormAdapter.toRepository(subscriptionForm));
        }

        @Test
        void should_return_the_created_subscription_form() {
            var subscriptionForm = SubscriptionFormFixtures.aSubscriptionForm();

            var result = service.create(subscriptionForm);

            assertThat(result).usingRecursiveComparison().isEqualTo(subscriptionForm);
        }

        @Test
        void should_generate_id_when_null_and_return_created_form_with_it() throws TechnicalException {
            var subscriptionForm = SubscriptionFormFixtures.aSubscriptionFormWithNullId();

            var result = service.create(subscriptionForm);

            assertThat(result.getId()).isNotNull();
            verify(repository).create(captor.capture());
            assertThat(captor.getValue().getId()).isEqualTo(result.getId().toString());
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            when(repository.create(any())).thenThrow(TechnicalException.class);
            var subscriptionForm = SubscriptionFormFixtures.aSubscriptionForm();

            assertThatThrownBy(() -> service.create(subscriptionForm))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage(
                    "An error occurred while trying to create a SubscriptionForm for env: " + SubscriptionFormFixtures.ENVIRONMENT_ID
                );
        }
    }

    @Nested
    class Update {

        @BeforeEach
        void setUp() throws TechnicalException {
            when(repository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        void should_update_a_subscription_form() throws TechnicalException {
            var subscriptionForm = SubscriptionFormFixtures.aSubscriptionForm();

            service.update(subscriptionForm);

            verify(repository).update(captor.capture());
            assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(subscriptionFormAdapter.toRepository(subscriptionForm));
        }

        @Test
        void should_return_the_updated_subscription_form() {
            var subscriptionForm = SubscriptionFormFixtures.aSubscriptionForm();

            var result = service.update(subscriptionForm);

            assertThat(result).usingRecursiveComparison().isEqualTo(subscriptionForm);
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            when(repository.update(any())).thenThrow(TechnicalException.class);
            var subscriptionForm = SubscriptionFormFixtures.aSubscriptionForm();

            assertThatThrownBy(() -> service.update(subscriptionForm))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while trying to update a SubscriptionForm with id: " + SubscriptionFormFixtures.FORM_ID);
        }
    }
}
