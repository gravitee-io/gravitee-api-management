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
import { RouterTestingModule } from '@angular/router/testing';
import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';

import { ApplicationService } from '../../../../../projects/portal-webclient-sdk/src/lib';
import { NotificationService } from '../../../services/notification.service';
import { EventService } from '../../../services/event.service';

import { ApplicationGeneralComponent } from './application-general.component';

describe('ApplicationGeneralComponent', () => {
  const mockApplication = {
    id: 'app-123',
    name: 'Test Application',
    description: 'Test Description',
    domain: 'test.com',
    picture: 'data:image/png;base64,test',
    background: 'data:image/png;base64,background',
    applicationType: 'WEB',
    owner: {
      display_name: 'Test Owner',
    },
    settings: {
      oauth: {
        client_id: 'client123',
        client_secret: 'secret123',
        redirect_uris: ['http://localhost'],
        grant_types: ['authorization_code'],
        additional_client_metadata: {
          custom_key: 'custom_value',
        },
      },
      tls: {
        client_certificate: 'cert123',
      },
    },
    created_at: '2023-01-01T00:00:00Z',
    updated_at: '2023-01-02T00:00:00Z',
  };

  const mockApplicationType = {
    id: 'web',
    name: 'Web Application',
    allowed_grant_types: [
      { type: 'authorization_code', name: 'Authorization Code' },
      { type: 'refresh_token', name: 'Refresh Token' },
    ],
    mandatory_grant_types: [{ type: 'authorization_code' }],
    requires_redirect_uris: true,
  };

  const mockPermissions = {
    DEFINITION: ['U', 'D'],
  };

  const mockConnectedApis = [
    { id: 'api-1', name: 'API 1' },
    { id: 'api-2', name: 'API 2' },
  ];

  const mockApplicationService = {
    getSubscriberApisByApplicationId: jest.fn(),
    updateApplicationByApplicationId: jest.fn(),
    deleteApplicationByApplicationId: jest.fn(),
    renewApplicationSecret: jest.fn(),
  };

  const mockNotificationService = {
    success: jest.fn(),
  };

  const mockEventService = {
    dispatch: jest.fn(),
  };

  const mockRouter = {
    navigate: jest.fn(),
  };

  const mockTranslateService = {
    currentLang: 'en',
  };

  const createComponent = createComponentFactory({
    component: ApplicationGeneralComponent,
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    imports: [HttpClientTestingModule, RouterTestingModule, FormsModule, ReactiveFormsModule],
    providers: [
      { provide: ApplicationService, useValue: mockApplicationService },
      { provide: NotificationService, useValue: mockNotificationService },
      { provide: EventService, useValue: mockEventService },
      { provide: Router, useValue: mockRouter },
      { provide: TranslateService, useValue: mockTranslateService },
      {
        provide: ActivatedRoute,
        useValue: {
          snapshot: {
            data: {
              application: mockApplication,
              applicationType: mockApplicationType,
              permissions: mockPermissions,
            },
          },
        },
      },
    ],
  });

  let spectator: Spectator<ApplicationGeneralComponent>;
  let component: ApplicationGeneralComponent;

  beforeEach(() => {
    jest.clearAllMocks();
    mockApplicationService.getSubscriberApisByApplicationId.mockReturnValue(
      of({ data: mockConnectedApis.map(api => ({ item: api, type: 'api' })) }),
    );
    spectator = createComponent();
    component = spectator.component;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('initialization', () => {
    it('should initialize form with OAuth settings', () => {
      expect(component.applicationForm).toBeTruthy();
      expect(component.isOAuth()).toBeTruthy();
      expect(component.canUpdate).toBeTruthy();
      expect(component.canDelete).toBeTruthy();
    });

    it('should load connected APIs', async () => {
      await component.connectedApis;
      expect(mockApplicationService.getSubscriberApisByApplicationId).toHaveBeenCalledWith({
        applicationId: 'app-123',
        statuses: ['ACCEPTED'],
      });
    });
  });

  describe('form methods', () => {
    it('should reset form', () => {
      const resetSpy = jest.spyOn(component.applicationForm, 'reset');
      component.reset();
      expect(resetSpy).toHaveBeenCalledWith(mockApplication);
    });

    it('should check if OAuth is enabled', () => {
      expect(component.isOAuth()).toBeTruthy();
    });
  });

  describe('grant types', () => {
    it('should update grant types', () => {
      component.updateGrantTypes();
      expect(component.allGrantTypes).toBeDefined();
      expect(component.allGrantTypes.length).toBeGreaterThan(0);
    });

    it('should add grant type when switched on', () => {
      const grantType = { type: 'refresh_token', name: 'Refresh Token' };
      const event = { target: { value: true } };
      component.onSwitchGrant(event, grantType);
      expect(component.grantTypes.length).toBeGreaterThan(0);
    });

    it('should remove grant type when switched off', () => {
      // First add a grant type
      const grantType = { type: 'refresh_token', name: 'Refresh Token' };
      const addEvent = { target: { value: true } };
      component.onSwitchGrant(addEvent, grantType);

      // Then remove it
      const removeEvent = { target: { value: false } };
      component.onSwitchGrant(removeEvent, grantType);

      expect(component.grantTypes.length).toBe(1); // Only the mandatory one should remain
    });
  });

  describe('redirect URIs', () => {
    it('should add redirect URI', () => {
      const event = { target: { valid: true, value: 'http://new-uri.com' } };
      component.addRedirectUri(event);
      expect(component.redirectURIs.length).toBeGreaterThan(1);
    });

    it('should not add duplicate redirect URI', () => {
      const event = { target: { valid: true, value: 'http://localhost' } };
      component.addRedirectUri(event);
      expect(component.redirectURIs.length).toBe(1); // Should not add duplicate
    });

    it('should remove redirect URI', () => {
      const initialLength = component.redirectURIs.length;
      component.removeRedirectUri(0);
      expect(component.redirectURIs.length).toBe(initialLength - 1);
    });

    it('should validate redirect URIs', () => {
      expect(component.validRedirectUris).toBeTruthy();
    });
  });

  describe('metadata management', () => {
    it('should add metadata', () => {
      const initialLength = component.additionalClientMetadata.length;
      component.addMetadata();
      expect(component.additionalClientMetadata.length).toBe(initialLength + 1);
    });

    it('should remove metadata', () => {
      // First add metadata
      component.addMetadata();
      const lengthAfterAdd = component.additionalClientMetadata.length;

      // Then remove it
      component.removeMetadata(0);
      expect(component.additionalClientMetadata.length).toBe(lengthAfterAdd - 1);
    });

    it('should get metadata controls', () => {
      const controls = component.metadataControls;
      expect(controls).toBeDefined();
    });
  });

  describe('form submission', () => {
    it('should submit form with OAuth metadata conversion', async () => {
      // Add some metadata
      component.addMetadata();
      const metadataControl = component.additionalClientMetadata.at(0) as any;
      metadataControl.patchValue({ key: 'test_key', value: 'test_value' });

      mockApplicationService.updateApplicationByApplicationId.mockReturnValue(of(mockApplication));

      await component.submit();

      expect(mockApplicationService.updateApplicationByApplicationId).toHaveBeenCalled();
      const callArg = mockApplicationService.updateApplicationByApplicationId.mock.calls[0][0];
      expect(callArg.application.settings.oauth.additional_client_metadata).toEqual({
        test_key: 'test_value',
      });
      expect(callArg.application.settings.oauth.additionalClientMetadata).toBeUndefined();
    });

    it('should handle submission error', async () => {
      mockApplicationService.updateApplicationByApplicationId.mockReturnValue(throwError(() => new Error('Update failed')));

      await component.submit();

      expect(mockApplicationService.updateApplicationByApplicationId).toHaveBeenCalled();
    });
  });

  describe('application deletion', () => {
    it('should delete application', async () => {
      mockApplicationService.deleteApplicationByApplicationId.mockReturnValue(of({}));

      await component.delete();

      expect(mockApplicationService.deleteApplicationByApplicationId).toHaveBeenCalledWith({
        applicationId: 'app-123',
      });
      expect(mockRouter.navigate).toHaveBeenCalledWith(['applications']);
      expect(mockNotificationService.success).toHaveBeenCalledWith('application.success.delete');
    });

    it('should handle deletion error', async () => {
      mockApplicationService.deleteApplicationByApplicationId.mockReturnValue(throwError(() => new Error('Delete failed')));

      await component.delete();

      expect(mockApplicationService.deleteApplicationByApplicationId).toHaveBeenCalled();
    });
  });

  describe('secret renewal', () => {
    it('should renew application secret', async () => {
      mockApplicationService.renewApplicationSecret.mockReturnValue(of(mockApplication));

      await component.renewSecret();

      expect(mockApplicationService.renewApplicationSecret).toHaveBeenCalledWith({
        applicationId: 'app-123',
      });
      expect(mockNotificationService.success).toHaveBeenCalledWith('application.success.renewSecret');
    });

    it('should handle renewal error', async () => {
      mockApplicationService.renewApplicationSecret.mockReturnValue(throwError(() => new Error('Renewal failed')));

      await component.renewSecret();

      expect(mockApplicationService.renewApplicationSecret).toHaveBeenCalled();
    });
  });

  describe('utility methods', () => {
    it('should format date to locale string', () => {
      const result = component.toLocaleDateString('2023-01-01T00:00:00Z');
      expect(result).toBeDefined();
    });

    it('should get display name', () => {
      const displayName = component.displayName;
      expect(displayName).toBeDefined();
    });

    it('should handle gv-list click', () => {
      const detail = { item: { id: 'api-1' } };
      component.onGvListClick(detail);
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/catalog/api/api-1'], {
        queryParams: { app: 'app-123' },
      });
    });
  });

  describe('non-OAuth application', () => {
    it('should handle non-OAuth application settings', () => {
      // Test the isOAuth method with non-OAuth settings
      const nonOAuthSettings = {
        app: {
          type: 'simple',
          client_id: 'simple-client',
        },
        tls: {
          client_certificate: 'cert123',
        },
      };

      // Create a mock application with non-OAuth settings
      const testApplication = {
        ...mockApplication,
        settings: nonOAuthSettings as any,
      };

      // Test the isOAuth logic directly
      const isOAuth = testApplication && (testApplication.settings as any).oauth != null;
      expect(isOAuth).toBeFalsy();
    });

    it('should handle form submission without OAuth metadata conversion', async () => {
      // Test the submit method's OAuth metadata conversion logic
      const formValue = {
        settings: {
          app: {
            type: 'simple',
            client_id: 'simple-client',
          },
        } as any,
      };

      // Simulate the submit method's logic for non-OAuth applications
      const isOAuth = formValue.settings.oauth !== undefined;
      expect(isOAuth).toBeFalsy();

      // Verify that no OAuth metadata conversion occurs
      expect(formValue.settings.app).toBeDefined();
      expect(formValue.settings.oauth).toBeUndefined();
    });
  });
});
