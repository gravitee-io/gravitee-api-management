/*
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
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { ApiLabelsPipe } from '../../../pipes/api-labels.pipe';
import { ApiStatesPipe } from '../../../pipes/api-states.pipe';
import { ApplicationService } from '../../../../../projects/portal-webclient-sdk/src/lib';
import { NotificationService } from '../../../services/notification.service';
import { ConfigurationService } from '../../../services/configuration.service';
import { SubscriptionService } from '../../../../../projects/portal-webclient-sdk/src/lib';

import { ApplicationCreationComponent } from './application-creation.component';

describe('ApplicationCreationComponent', () => {
  const enabledApplicationTypes = [
    { id: 'type1', name: 'type1' },
    { id: 'type2', name: 'type2' },
  ];

  const mockApplicationService = {
    createApplication: jest.fn(),
  };

  const mockSubscriptionService = {
    createSubscription: jest.fn(),
  };

  const mockNotificationService = {
    reset: jest.fn(),
  };

  const mockConfigurationService = {
    hasFeature: jest.fn(),
  };

  const createComponent = createComponentFactory({
    component: ApplicationCreationComponent,
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    imports: [HttpClientTestingModule, RouterTestingModule, FormsModule, ReactiveFormsModule],
    declarations: [ApiStatesPipe, ApiLabelsPipe],
    providers: [
      ApiStatesPipe,
      ApiLabelsPipe,
      { provide: ActivatedRoute, useValue: { snapshot: { data: { enabledApplicationTypes } } } },
      { provide: ApplicationService, useValue: mockApplicationService },
      { provide: SubscriptionService, useValue: mockSubscriptionService },
      { provide: NotificationService, useValue: mockNotificationService },
      { provide: ConfigurationService, useValue: mockConfigurationService },
    ],
  });

  let spectator: Spectator<ApplicationCreationComponent>;
  let component: ApplicationCreationComponent;

  beforeEach(() => {
    spectator = createComponent();
    component = spectator.component;
    jest.clearAllMocks();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('createApp with OAuth metadata', () => {
    beforeEach(() => {
      // Setup form with OAuth metadata
      component.applicationForm.patchValue({
        name: 'Test App',
        description: 'Test Description',
        settings: {
          oauth: {
            redirect_uris: ['http://localhost'],
            grant_types: ['authorization_code'],
            application_type: 'web',
            additionalClientMetadata: [
              { key: 'custom_key1', value: 'custom_value1' },
              { key: 'custom_key2', value: 'custom_value2' },
            ],
          },
          tls: {
            client_certificate: '',
          },
        },
      });

      component.subscribeList = [];
      component.apiKeyMode = 'UNSPECIFIED';
    });

    it('should convert additionalClientMetadata array to object when creating OAuth application', async () => {
      const mockApplication = { id: 'app-123', name: 'Test App' };
      mockApplicationService.createApplication.mockReturnValue(of(mockApplication));

      await component.createApp();

      expect(mockApplicationService.createApplication).toHaveBeenCalledWith({
        applicationInput: {
          name: 'Test App',
          description: 'Test Description',
          domain: null,
          picture: null,
          settings: {
            oauth: {
              redirect_uris: ['http://localhost'],
              grant_types: ['authorization_code'],
              application_type: 'web',
              additionalClientMetadata: [
                { key: 'custom_key1', value: 'custom_value1' },
                { key: 'custom_key2', value: 'custom_value2' },
              ],
              additional_client_metadata: {
                custom_key1: 'custom_value1',
                custom_key2: 'custom_value2',
              },
            },
            tls: {
              client_certificate: '',
            },
          },
          api_key_mode: 'UNSPECIFIED',
        },
      });
    });

    it('should handle empty additionalClientMetadata array', async () => {
      component.applicationForm.patchValue({
        settings: {
          oauth: {
            redirect_uris: ['http://localhost'],
            grant_types: ['authorization_code'],
            application_type: 'web',
            additionalClientMetadata: [],
          },
          tls: {
            client_certificate: '',
          },
        },
      });

      const mockApplication = { id: 'app-123', name: 'Test App' };
      mockApplicationService.createApplication.mockReturnValue(of(mockApplication));

      await component.createApp();

      expect(mockApplicationService.createApplication).toHaveBeenCalledWith({
        applicationInput: {
          name: 'Test App',
          description: 'Test Description',
          domain: null,
          picture: null,
          settings: {
            oauth: {
              redirect_uris: ['http://localhost'],
              grant_types: ['authorization_code'],
              application_type: 'web',
              additionalClientMetadata: [],
              additional_client_metadata: {},
            },
            tls: {
              client_certificate: '',
            },
          },
          api_key_mode: 'UNSPECIFIED',
        },
      });
    });

    it('should not modify settings when oauth is undefined', async () => {
      component.applicationForm.patchValue({
        settings: {
          app: {
            type: 'simple',
            client_id: 'client123',
          },
          tls: {
            client_certificate: '',
          },
        },
      });

      const mockApplication = { id: 'app-123', name: 'Test App' };
      mockApplicationService.createApplication.mockReturnValue(of(mockApplication));

      await component.createApp();

      expect(mockApplicationService.createApplication).toHaveBeenCalledWith({
        applicationInput: {
          name: 'Test App',
          description: 'Test Description',
          domain: null,
          picture: null,
          settings: {
            app: {
              type: 'simple',
              client_id: 'client123',
            },
            tls: {
              client_certificate: '',
            },
          },
          api_key_mode: 'UNSPECIFIED',
        },
      });
    });
  });
});
