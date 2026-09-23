/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { enrichPortalNotification } from './portal-notification.mapper';
import { fakePortalNotification } from '../../../entities/notification/portal-notification.fixture';

describe('enrichPortalNotification', () => {
  it('maps an API subscription notification from the default portal template', () => {
    const enriched = enrichPortalNotification(
      fakePortalNotification({
        title: '[Echo API] Subscription Accepted',
        message: 'The subscription request for the application "Petstore App" to the plan "Premium" was accepted.',
      }),
    );

    expect(enriched).toEqual(
      expect.objectContaining({
        source: 'API',
        notificationType: 'Subscription Accepted',
        resourceName: 'Echo API',
        apiName: 'Echo API',
        applicationName: 'Petstore App',
        read: false,
      }),
    );
  });

  it('maps an application subscription notification from the default portal template', () => {
    const enriched = enrichPortalNotification(
      fakePortalNotification({
        title: '[Petstore App] Subscription Accepted',
        message: 'The subscription request to the plan "Premium" of the api "Echo API" was accepted.',
      }),
      true,
    );

    expect(enriched).toEqual(
      expect.objectContaining({
        source: 'APPLICATION',
        notificationType: 'Subscription Accepted',
        resourceName: 'Petstore App',
        apiName: 'Echo API',
        applicationName: 'Petstore App',
        read: true,
      }),
    );
  });

  it('maps API lifecycle notifications that quote the API name in the message', () => {
    const enriched = enrichPortalNotification(
      fakePortalNotification({
        title: 'API started',
        message: '"Planets" was started by Admin.',
      }),
    );

    expect(enriched).toEqual(
      expect.objectContaining({
        source: 'API',
        notificationType: 'API started',
        resourceName: 'Planets',
        apiName: 'Planets',
      }),
    );
  });
});
