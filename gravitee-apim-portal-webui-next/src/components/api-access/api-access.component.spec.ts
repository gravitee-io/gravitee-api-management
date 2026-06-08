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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { Injectable } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatTabGroupHarness } from '@angular/material/tabs/testing';
import { MatTooltipHarness } from '@angular/material/tooltip/testing';
import { By } from '@angular/platform-browser';

import { ApiAccessComponent } from './api-access.component';
import { Configuration } from '../../entities/configuration/configuration';
import { Subscription } from '../../entities/subscription/subscription';
import { ConfigService } from '../../services/config.service';
import { AppTestingModule, TESTING_BASE_URL } from '../../testing/app-testing.module';
import { ConfirmDialogHarness } from '../confirm-dialog/confirm-dialog.harness';
import { CopyCodeHarness } from '../copy-code/copy-code.harness';
import { PaginatedTableHarness } from '../paginated-table/paginated-table.harness';

describe('ApiAccessComponent', () => {
  let component: ApiAccessComponent;
  let fixture: ComponentFixture<ApiAccessComponent>;
  let harnessLoader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const CONFIGURATION_KAFKA_SASL_MECHANISMS = ['PLAIN', 'SCRAM-SHA-256', 'SCRAM-SHA-512'];

  @Injectable()
  class CustomConfigurationServiceStub {
    get baseURL(): string {
      return TESTING_BASE_URL;
    }
    get configuration(): Configuration {
      return {
        portal: {
          apikeyHeader: 'X-My-Apikey',
          kafkaSaslMechanisms: CONFIGURATION_KAFKA_SASL_MECHANISMS,
        },
      };
    }
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiAccessComponent, AppTestingModule, MatIconTestingModule],
      providers: [
        {
          provide: ConfigService,
          useClass: CustomConfigurationServiceStub,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiAccessComponent);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    component = fixture.componentInstance;
    component.canManageApiKey = true;
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('Keyless', () => {
    it('should show base url and command line', async () => {
      component.planSecurity = 'KEY_LESS';
      component.entrypointUrls = ['my-entrypoint-url'];
      fixture.detectChanges();
      expect(apiAccessShellShown()).toBeTruthy();
      expect(await baseUrlShown()).toBeTruthy();
      expect(await commandLineShown()).toBeTruthy();
    });

    it('should show base url and command line with multiple urls', async () => {
      component.planSecurity = 'KEY_LESS';
      component.entrypointUrls = ['my-entrypoint-url', 'my-entrypoint-url-2'];
      fixture.detectChanges();
      expect(await baseUrlShown()).toBeFalsy();

      const selectEntrypoints = await getSelectEntrypoints();
      expect(selectEntrypoints).toBeTruthy();
      expect(await selectEntrypoints?.getValueText()).toContain('my-entrypoint-url');
      expect(apiAccessShellShown()).toBeTruthy();
      expect(await commandLineShown()).toBeTruthy();
    });
  });

  describe('Federated API (empty entrypoints)', () => {
    it('should hide the whole API access card for a keyless plan with empty entrypoints', async () => {
      component.planSecurity = 'KEY_LESS';
      component.entrypointUrls = [];

      fixture.detectChanges();

      expect(apiAccessCardShown()).toBeFalsy();
      expect(apiAccessShellShown()).toBeFalsy();
      expect(await commandLineShown()).toBeFalsy();
    });

    it('should hide the whole API access card for a keyless plan with undefined entrypoints', async () => {
      component.planSecurity = 'KEY_LESS';
      component.entrypointUrls = undefined;

      fixture.detectChanges();

      expect(apiAccessCardShown()).toBeFalsy();
      expect(apiAccessShellShown()).toBeFalsy();
      expect(await commandLineShown()).toBeFalsy();
    });

    it('should still show api keys but hide calling the API content for an api key plan with empty entrypoints', async () => {
      component.planSecurity = 'API_KEY';
      component.subscription = { status: 'ACCEPTED' } as Subscription;
      component.entrypointUrls = [];
      component.apiKeys = [{ key: 'api-key', application: { id: 'app-id', name: 'app-name' } }];

      fixture.detectChanges();

      expect(apiAccessCardShown()).toBeTruthy();
      expect(apiAccessShellShown()).toBeTruthy();
      expect(await apiKeysTableShown()).toBeTruthy();
      expect(await baseUrlShown()).toBeFalsy();
      expect(await commandLineShown()).toBeFalsy();
    });
  });

  describe('Accepted', () => {
    describe('HTTP API', () => {
      describe('API Key', () => {
        it('should show api keys and hide calling api content when all api keys are inactive', async () => {
          component.planSecurity = 'API_KEY';
          component.subscription = { status: 'ACCEPTED' } as Subscription;
          component.entrypointUrls = ['my-entrypoint-url'];
          component.apiKeys = [
            {
              key: 'api-key-1',
              created_at: '2024-04-17T10:33:29Z',
              revoked_at: '2024-04-19T10:33:29Z',
              application: { id: 'app-id', name: 'app-name' },
            },
            {
              key: 'api-key-2',
              created_at: '2024-04-20T10:33:29Z',
              expire_at: expiredApiKeyDate(),
              application: { id: 'app-id', name: 'app-name' },
            },
          ];

          fixture.detectChanges();
          expect(apiAccessShellShown()).toBeTruthy();
          expect(await apiKeysTableShown()).toBeTruthy();
          expect(await baseUrlShown()).toBeFalsy();
          expect(await commandLineShown()).toBeFalsy();
          expect(await revokeApiKeyButtonShown()).toBeFalsy();
        });

        it('should show empty api keys message and hide calling api content when there are no api keys', async () => {
          component.planSecurity = 'API_KEY';
          component.subscription = { status: 'ACCEPTED' } as Subscription;
          component.entrypointUrls = ['my-entrypoint-url'];
          component.apiKeys = [];

          fixture.detectChanges();

          expect(apiAccessShellShown()).toBeTruthy();
          expect(await apiKeysTableShown()).toBeFalsy();
          expect(fixture.nativeElement.textContent).toContain('No API keys available.');
          expect(await revokeApiKeyButtonShown()).toBeFalsy();
        });

        it('should hide command line placeholder when there are no api keys', async () => {
          component.planSecurity = 'API_KEY';
          component.subscription = { status: 'ACCEPTED' } as Subscription;
          component.entrypointUrls = ['my-entrypoint-url'];
          component.apiKeys = [];

          fixture.detectChanges();

          expect(await commandLineShown()).toBeFalsy();
        });

        it('should hide revoke button when canManageApiKey is false', async () => {
          component.planSecurity = 'API_KEY';
          component.subscription = { id: 'subscription-id', status: 'ACCEPTED' } as Subscription;
          component.entrypointUrls = ['my-entrypoint-url'];
          component.apiKeys = [{ key: 'api-key', application: { id: 'app-id', name: 'app-name' } }];
          component.canManageApiKey = false;

          fixture.detectChanges();

          expect(await revokeApiKeyButtonShown()).toBeFalsy();
          expect(await renewApiKeyButtonShown()).toBeFalsy();
        });

        it('should show renew api key button with tooltip', async () => {
          component.planSecurity = 'API_KEY';
          component.subscription = { id: 'subscription-id', status: 'ACCEPTED' } as Subscription;
          component.entrypointUrls = ['my-entrypoint-url'];
          component.apiKeys = [{ key: 'api-key', application: { id: 'app-id', name: 'app-name' } }];

          fixture.detectChanges();

          const renewButton = await getRenewApiKeyButton();
          expect(renewButton).toBeTruthy();
          expect(await renewButton!.getText()).toContain('Renew API Key');

          const tooltip = await getRenewApiKeyTooltip();
          expect(tooltip).toBeTruthy();
          await tooltip!.show();
          expect(await tooltip!.getTooltipText()).toBe('Renew API Key');
        });

        it('should open confirmation dialog and call renew service after confirmation', async () => {
          component.planSecurity = 'API_KEY';
          component.subscription = { id: 'subscription-id', status: 'ACCEPTED' } as Subscription;
          component.entrypointUrls = ['my-entrypoint-url'];
          component.apiKeys = [{ key: 'api-key', application: { id: 'app-id', name: 'app-name' } }];
          const apiKeyRenewedSpy = jest.spyOn(component.apiKeyRenewed, 'emit');

          fixture.detectChanges();

          const renewButton = await getRenewApiKeyButton();
          expect(renewButton).toBeTruthy();

          await renewButton!.click();
          const confirmDialog = await rootLoader.getHarness(ConfirmDialogHarness);
          expect(await confirmDialog.getTitle()).toContain('Renew API Key?');
          expect(await confirmDialog.getContent()).toContain('API Key renewal will eventually deprecate the current key');
          expect(await confirmDialog.getCancelText()).toContain('Cancel');
          expect(await confirmDialog.getConfirmText()).toContain('Yes, renew');

          await confirmDialog.confirm();
          expectRenewApiKeyRequest('subscription-id').flush(null);
          fixture.detectChanges();

          expect(apiKeyRenewedSpy).toHaveBeenCalled();
          expect(getRenewApiKeyFeedbackText()).toContain('API key renewed successfully. You can now use it to access the API.');
          expect(getRenewApiKeyFeedbackAttribute('aria-live')).toBe('polite');
          expect(getRenewApiKeyFeedbackAttribute('role')).toBeNull();
        });

        it('should not call renew service when dialog is canceled', async () => {
          component.planSecurity = 'API_KEY';
          component.subscription = { id: 'subscription-id', status: 'ACCEPTED' } as Subscription;
          component.entrypointUrls = ['my-entrypoint-url'];
          component.apiKeys = [{ key: 'api-key', application: { id: 'app-id', name: 'app-name' } }];

          fixture.detectChanges();

          const renewButton = await getRenewApiKeyButton();
          expect(renewButton).toBeTruthy();

          await renewButton!.click();
          const confirmDialog = await rootLoader.getHarness(ConfirmDialogHarness);
          await confirmDialog.cancel();

          expectNoRenewApiKeyRequest();
        });

        it('should not emit revoke event when renew service fails', async () => {
          component.planSecurity = 'API_KEY';
          component.subscription = { id: 'subscription-id', status: 'ACCEPTED' } as Subscription;
          component.entrypointUrls = ['my-entrypoint-url'];
          component.apiKeys = [{ key: 'api-key', application: { id: 'app-id', name: 'app-name' } }];
          const apiKeyRenewedSpy = jest.spyOn(component.apiKeyRenewed, 'emit');

          fixture.detectChanges();

          const renewButton = await getRenewApiKeyButton();
          expect(renewButton).toBeTruthy();

          await renewButton!.click();
          const confirmDialog = await rootLoader.getHarness(ConfirmDialogHarness);
          await confirmDialog.confirm();

          expectRenewApiKeyRequest('subscription-id').flush('Failed to renew API key', {
            status: 500,
            statusText: 'Server Error',
          });
          fixture.detectChanges();

          expect(apiKeyRenewedSpy).not.toHaveBeenCalled();
          expect(getRenewApiKeyFeedbackText()).toContain('Failed to renew API key. Please try again.');
          expect(getRenewApiKeyFeedbackAttribute('role')).toBe('alert');
          expect(getRenewApiKeyFeedbackAttribute('aria-live')).toBeNull();
        });

        it('should not call renew service when subscription id is missing', async () => {
          component.planSecurity = 'API_KEY';
          component.subscription = { status: 'ACCEPTED' } as Subscription;
          component.entrypointUrls = ['my-entrypoint-url'];
          component.apiKeys = [{ key: 'api-key', application: { id: 'app-id', name: 'app-name' } }];

          fixture.detectChanges();

          const renewButton = await getRenewApiKeyButton();
          expect(renewButton).toBeTruthy();
          await renewButton!.click();

          expectNoRenewApiKeyRequest();
        });

        it('should disable renew button while request is in flight', async () => {
          component.planSecurity = 'API_KEY';
          component.subscription = { id: 'subscription-id', status: 'ACCEPTED' } as Subscription;
          component.entrypointUrls = ['my-entrypoint-url'];
          component.apiKeys = [{ key: 'api-key', application: { id: 'app-id', name: 'app-name' } }];

          fixture.detectChanges();

          const renewButton = await getRenewApiKeyButton();
          expect(renewButton).toBeTruthy();
          expect(await renewButton!.isDisabled()).toBeFalsy();

          await renewButton!.click();
          const confirmDialog = await rootLoader.getHarness(ConfirmDialogHarness);
          await confirmDialog.confirm();
          fixture.detectChanges();

          const request = expectRenewApiKeyRequest('subscription-id');
          const renewButtonWhileLoading = await getRenewApiKeyButton();
          expect(await renewButtonWhileLoading!.isDisabled()).toBeTruthy();

          request.flush(null);
          fixture.detectChanges();

          const renewButtonAfterDone = await getRenewApiKeyButton();
          expect(await renewButtonAfterDone!.isDisabled()).toBeFalsy();
        });

        it('should show revoke button only for active api keys', async () => {
          component.planSecurity = 'API_KEY';
          component.subscription = { id: 'subscription-id', status: 'ACCEPTED' } as Subscription;
          component.entrypointUrls = ['my-entrypoint-url'];
          component.apiKeys = [
            {
              key: 'expired-api-key',
              expire_at: expiredApiKeyDate(),
              application: { id: 'app-id', name: 'app-name' },
            },
            { key: 'active-api-key', application: { id: 'app-id', name: 'app-name' } },
          ];

          fixture.detectChanges();

          const revokeButton = await getRevokeApiKeyButton();
          expect(revokeButton).toBeTruthy();
          expect(await revokeButton!.isDisabled()).toBeFalsy();
          expect(await (await revokeButton!.host()).getAttribute('aria-label')).toBe('Revoke API key active-api-key');
          expect(getApiKeyRevokeButtonApiKeys()).toEqual(['active-api-key']);
          expect(apiAccessShellShown()).toBeTruthy();
          expect(await apiKeysTableShown()).toBeTruthy();
          expect(await baseUrlShown()).toBeTruthy();
          expect(await commandLineShown()).toBeTruthy();
          expect(await commandLineContains('X-My-Apikey: active-api-key')).toBeTruthy();

          await revokeButton!.click();
          const confirmDialog = await rootLoader.getHarness(ConfirmDialogHarness);
          await confirmDialog.confirm();

          expectRevokeApiKeyRequest('subscription-id', 'active-api-key').flush(null);
        });

        it('should display active and inactive api key status icons', async () => {
          component.planSecurity = 'API_KEY';
          component.subscription = { id: 'subscription-id', status: 'ACCEPTED' } as Subscription;
          component.entrypointUrls = ['my-entrypoint-url'];
          component.apiKeys = [
            {
              key: 'active-api-key',
              application: { id: 'app-id', name: 'app-name' },
            },
            {
              key: 'expired-api-key',
              expire_at: expiredApiKeyDate(),
              application: { id: 'app-id', name: 'app-name' },
            },
            {
              key: 'revoked-api-key',
              revoked_at: '2024-04-19T10:33:29Z',
              application: { id: 'app-id', name: 'app-name' },
            },
          ];

          fixture.detectChanges();

          expect(getApiKeyStatusIcons()).toEqual(['gio:check-circled-outline', 'gio:x-circle', 'gio:x-circle']);
          expect(getApiKeyStatusLabels()).toEqual(['Active API key', 'Inactive API key', 'Inactive API key']);
        });

        it('should treat api key with future expire_at as active', async () => {
          component.planSecurity = 'API_KEY';
          component.subscription = { id: 'subscription-id', status: 'ACCEPTED' } as Subscription;
          component.entrypointUrls = ['my-entrypoint-url'];
          component.apiKeys = [
            {
              key: 'future-expiring-api-key',
              expire_at: new Date(Date.now() + 60_000).toISOString(),
              application: { id: 'app-id', name: 'app-name' },
            },
          ];

          fixture.detectChanges();

          expect(getApiKeyStatusIcons()).toEqual(['gio:check-circled-outline']);
          expect(await (await getRevokeApiKeyButton())?.isDisabled()).toBeFalsy();
          expect(getApiKeyRevokeButtonApiKeys()).toEqual(['future-expiring-api-key']);
          expect(await commandLineContains('X-My-Apikey: future-expiring-api-key')).toBeTruthy();
        });

        it('should not show revoke button when all api keys have expire_at', async () => {
          component.planSecurity = 'API_KEY';
          component.subscription = { id: 'subscription-id', status: 'ACCEPTED' } as Subscription;
          component.entrypointUrls = ['my-entrypoint-url'];
          component.apiKeys = [
            {
              key: 'expired-api-key-1',
              expire_at: expiredApiKeyDate(),
              application: { id: 'app-id', name: 'app-name' },
            },
            {
              key: 'expired-api-key-2',
              expire_at: expiredApiKeyDate(),
              application: { id: 'app-id', name: 'app-name' },
            },
          ];

          fixture.detectChanges();

          expect(apiAccessShellShown()).toBeTruthy();
          expect(await revokeApiKeyButtonShown()).toBeFalsy();
        });

        it('should not show revoke button when all api keys have revoked_at', async () => {
          component.planSecurity = 'API_KEY';
          component.subscription = { id: 'subscription-id', status: 'ACCEPTED' } as Subscription;
          component.entrypointUrls = ['my-entrypoint-url'];
          component.apiKeys = [
            {
              key: 'revoked-api-key-1',
              revoked_at: '2024-04-19T10:33:29Z',
              application: { id: 'app-id', name: 'app-name' },
            },
            {
              key: 'revoked-api-key-2',
              revoked_at: '2024-04-20T10:33:29Z',
              application: { id: 'app-id', name: 'app-name' },
            },
          ];

          fixture.detectChanges();

          expect(apiAccessShellShown()).toBeTruthy();
          expect(await revokeApiKeyButtonShown()).toBeFalsy();
        });

        it('should not show revoke button when api keys are mixed but none are active', async () => {
          component.planSecurity = 'API_KEY';
          component.subscription = { id: 'subscription-id', status: 'ACCEPTED' } as Subscription;
          component.entrypointUrls = ['my-entrypoint-url'];
          component.apiKeys = [
            {
              key: 'revoked-api-key',
              revoked_at: '2024-04-19T10:33:29Z',
              application: { id: 'app-id', name: 'app-name' },
            },
            {
              key: 'expired-api-key',
              expire_at: expiredApiKeyDate(),
              application: { id: 'app-id', name: 'app-name' },
            },
          ];

          fixture.detectChanges();

          expect(apiAccessShellShown()).toBeTruthy();
          expect(await revokeApiKeyButtonShown()).toBeFalsy();
        });

        it('should open confirmation dialog and call revoke service after confirmation', async () => {
          component.planSecurity = 'API_KEY';
          component.subscription = { id: 'subscription-id', status: 'ACCEPTED' } as Subscription;
          component.apiKeys = [{ key: 'api-key', application: { id: 'app-id', name: 'app-name' } }];
          const apiKeyRevokedSpy = jest.spyOn(component.apiKeyRevoked, 'emit');

          fixture.detectChanges();

          const revokeButton = await getRevokeApiKeyButton();
          expect(revokeButton).toBeTruthy();
          expect(await revokeButton!.isDisabled()).toBeFalsy();

          await revokeButton!.click();
          const confirmDialog = await rootLoader.getHarness(ConfirmDialogHarness);
          expect(await confirmDialog.getTitle()).toContain('Close this API key?');
          expect(await confirmDialog.getContent()).toContain('Applications using it will no longer be able to access the API.');
          expect(await confirmDialog.getCancelText()).toContain('Cancel');
          expect(await confirmDialog.getConfirmText()).toContain('Yes, revoke');

          await confirmDialog.confirm();
          expectRevokeApiKeyRequest('subscription-id', 'api-key').flush(null);

          expect(apiKeyRevokedSpy).toHaveBeenCalled();
        });

        it('should not call revoke service when dialog is canceled', async () => {
          component.planSecurity = 'API_KEY';
          component.subscription = { id: 'subscription-id', status: 'ACCEPTED' } as Subscription;
          component.apiKeys = [{ key: 'api-key', application: { id: 'app-id', name: 'app-name' } }];

          fixture.detectChanges();

          const revokeButton = await getRevokeApiKeyButton();
          expect(revokeButton).toBeTruthy();

          await revokeButton!.click();
          const confirmDialog = await rootLoader.getHarness(ConfirmDialogHarness);
          await confirmDialog.cancel();

          expectNoRevokeApiKeyRequest();
        });

        it('should not open another confirmation dialog while one is already open', async () => {
          component.planSecurity = 'API_KEY';
          component.subscription = { id: 'subscription-id', status: 'ACCEPTED' } as Subscription;
          component.apiKeys = [
            { key: 'api-key-1', application: { id: 'app-id', name: 'app-name' } },
            { key: 'api-key-2', application: { id: 'app-id', name: 'app-name' } },
          ];
          const matDialogOpenSpy = jest.spyOn(TestBed.inject(MatDialog), 'open');

          fixture.detectChanges();

          const revokeButtons = await getRevokeApiKeyButtons();
          expect(revokeButtons).toHaveLength(2);

          await revokeButtons[0].click();
          fixture.detectChanges();

          expect(matDialogOpenSpy).toHaveBeenCalledTimes(1);
          await expect(revokeButtons[0].isDisabled()).resolves.toBeTruthy();
          await expect(revokeButtons[1].isDisabled()).resolves.toBeTruthy();

          (component as unknown as { revokeApiKeyRow: (apiKey: { isActive: boolean; key: string }) => void }).revokeApiKeyRow({
            isActive: true,
            key: 'api-key-2',
          });

          expect(matDialogOpenSpy).toHaveBeenCalledTimes(1);

          const confirmDialog = await rootLoader.getHarness(ConfirmDialogHarness);
          await confirmDialog.cancel();
          fixture.detectChanges();

          const revokeButtonsAfterCancel = await getRevokeApiKeyButtons();
          await expect(revokeButtonsAfterCancel[0].isDisabled()).resolves.toBeFalsy();
          await expect(revokeButtonsAfterCancel[1].isDisabled()).resolves.toBeFalsy();
        });

        it('should not open another confirmation dialog while one is already open', async () => {
          component.planSecurity = 'API_KEY';
          component.subscription = { id: 'subscription-id', status: 'ACCEPTED' } as Subscription;
          component.apiKeys = [
            { key: 'api-key-1', application: { id: 'app-id', name: 'app-name' } },
            { key: 'api-key-2', application: { id: 'app-id', name: 'app-name' } },
          ];
          const matDialogOpenSpy = jest.spyOn(TestBed.inject(MatDialog), 'open');

          fixture.detectChanges();

          const revokeButtons = await getRevokeApiKeyButtons();
          expect(revokeButtons).toHaveLength(2);

          await revokeButtons[0].click();
          fixture.detectChanges();

          expect(matDialogOpenSpy).toHaveBeenCalledTimes(1);
          await expect(revokeButtons[0].isDisabled()).resolves.toBeTruthy();
          await expect(revokeButtons[1].isDisabled()).resolves.toBeTruthy();

          (component as unknown as { revokeApiKeyRow: (apiKey: { isActive: boolean; key: string }) => void }).revokeApiKeyRow({
            isActive: true,
            key: 'api-key-2',
          });

          expect(matDialogOpenSpy).toHaveBeenCalledTimes(1);

          const confirmDialog = await rootLoader.getHarness(ConfirmDialogHarness);
          await confirmDialog.cancel();
          fixture.detectChanges();

          const revokeButtonsAfterCancel = await getRevokeApiKeyButtons();
          await expect(revokeButtonsAfterCancel[0].isDisabled()).resolves.toBeFalsy();
          await expect(revokeButtonsAfterCancel[1].isDisabled()).resolves.toBeFalsy();
        });

        it('should not emit revoke event when revoke service fails', async () => {
          component.planSecurity = 'API_KEY';
          component.subscription = { id: 'subscription-id', status: 'ACCEPTED' } as Subscription;
          component.apiKeys = [{ key: 'api-key', application: { id: 'app-id', name: 'app-name' } }];
          const apiKeyRevokedSpy = jest.spyOn(component.apiKeyRevoked, 'emit');

          fixture.detectChanges();

          const revokeButton = await getRevokeApiKeyButton();
          expect(revokeButton).toBeTruthy();

          await revokeButton!.click();
          const confirmDialog = await rootLoader.getHarness(ConfirmDialogHarness);
          await confirmDialog.confirm();

          expectRevokeApiKeyRequest('subscription-id', 'api-key').flush('Failed to revoke API key', {
            status: 500,
            statusText: 'Server Error',
          });

          expect(apiKeyRevokedSpy).not.toHaveBeenCalled();
        });

        it('should not call revoke service when subscription id is missing', async () => {
          component.planSecurity = 'API_KEY';
          component.subscription = { status: 'ACCEPTED' } as Subscription;
          component.apiKeys = [{ key: 'api-key', application: { id: 'app-id', name: 'app-name' } }];

          fixture.detectChanges();

          const revokeButton = await getRevokeApiKeyButton();
          expect(revokeButton).toBeTruthy();
          expect(await revokeButton!.isDisabled()).toBeFalsy();
          await revokeButton!.click();
          const confirmDialog = await rootLoader.getHarness(ConfirmDialogHarness);
          await confirmDialog.confirm();

          expectNoRevokeApiKeyRequest();
        });

        it('should disable revoke button while request is in flight', async () => {
          component.planSecurity = 'API_KEY';
          component.subscription = { id: 'subscription-id', status: 'ACCEPTED' } as Subscription;
          component.apiKeys = [{ key: 'api-key', application: { id: 'app-id', name: 'app-name' } }];

          fixture.detectChanges();

          const revokeButton = await getRevokeApiKeyButton();
          expect(revokeButton).toBeTruthy();
          expect(await revokeButton!.isDisabled()).toBeFalsy();

          await revokeButton!.click();
          const confirmDialog = await rootLoader.getHarness(ConfirmDialogHarness);
          await confirmDialog.confirm();
          fixture.detectChanges();

          const request = expectRevokeApiKeyRequest('subscription-id', 'api-key');
          const revokeButtonWhileLoading = await getRevokeApiKeyButton();
          expect(await revokeButtonWhileLoading!.isDisabled()).toBeTruthy();

          request.flush(null);
          fixture.detectChanges();

          const revokeButtonAfterDone = await getRevokeApiKeyButton();
          expect(await revokeButtonAfterDone!.isDisabled()).toBeFalsy();
        });
      });
      describe('OAuth2', () => {
        it('should show client id and secret', async () => {
          component.planSecurity = 'OAUTH2';
          component.subscription = { status: 'ACCEPTED' } as Subscription;
          component.clientId = 'my-client-id';
          component.clientSecret = 'my-client-secret';

          fixture.detectChanges();
          expect(apiAccessShellShown()).toBeTruthy();
          expect(await clientIdShown()).toBeTruthy();
          expect(await clientSecretShown()).toBeTruthy();
          expect(await baseUrlShown()).toBeFalsy();
          expect(await revokeApiKeyButtonShown()).toBeFalsy();
        });
      });
      describe('JWT', () => {
        it('should show client id and secret', async () => {
          component.planSecurity = 'JWT';
          component.subscription = { status: 'ACCEPTED' } as Subscription;
          component.clientId = 'my-client-id';
          component.clientSecret = 'my-client-secret';

          fixture.detectChanges();
          expect(apiAccessShellShown()).toBeTruthy();
          expect(await clientIdShown()).toBeTruthy();
          expect(await clientSecretShown()).toBeTruthy();
          expect(await baseUrlShown()).toBeFalsy();
          expect(await revokeApiKeyButtonShown()).toBeFalsy();
        });
      });
    });
    describe('Native API', () => {
      beforeEach(() => {
        component.apiType = 'NATIVE';
        component.entrypointUrls = ['my-entrypoint-url'];
        component.subscription = { status: 'ACCEPTED' } as Subscription;
      });
      describe('API Key', () => {
        it('should show api key, config, producer and consumer calls', async () => {
          component.planSecurity = 'API_KEY';
          component.apiKeys = [{ key: 'api-key', hash: 'hash-username', application: { id: 'app-id', name: 'app-name' } }];
          component.apiKeyConfigUsername = 'hash-username';

          fixture.detectChanges();
          expect(await apiKeysTableShown()).toBeFalsy();
          expect(await baseUrlShown()).toBeFalsy();
          expect(await commandLineShown()).toBeFalsy();
          expect(await revokeApiKeyButtonShown()).toBeFalsy();

          expect(await apiKeyUsernameShown()).toBeTruthy();
          expect(await apiKeyPasswordShown()).toBeTruthy();
          expect(await plainConfigurationShown()).toBeTruthy();
          expect(await scram256ConfigurationShown()).toBeFalsy();
          expect(await scram512ConfigurationShown()).toBeFalsy();

          const plainConfiguration = await getPlainConfiguration();
          const plainConfigurationContent = await plainConfiguration?.host().then(host => host.text());
          expect(plainConfigurationContent).toContain('api-key');
          expect(plainConfigurationContent).toContain(component.apiKeyConfigUsername);

          const configTabs = await getApiKeyPropertiesConfiguration();
          expect(await configTabs.getTabs().then(tabs => tabs.length)).toEqual(3);
          await configTabs.selectTab({ label: 'SCRAM-SHA-256' });

          expect(await plainConfigurationShown()).toBeFalsy();
          expect(await scram256ConfigurationShown()).toBeTruthy();
          expect(await scram512ConfigurationShown()).toBeFalsy();

          await configTabs.selectTab({ label: 'SCRAM-SHA-512' });

          expect(await plainConfigurationShown()).toBeFalsy();
          expect(await scram256ConfigurationShown()).toBeFalsy();
          expect(await scram512ConfigurationShown()).toBeTruthy();

          const commandExamples = await getCommandExamples();
          expect(await commandExamples.getTabs().then(tabs => tabs.length)).toEqual(2);
          expect(await producerCommandShown()).toBeTruthy();
          expect(await consumerCommandShown()).toBeFalsy();

          await commandExamples.selectTab({ label: 'Consumer' });
          expect(await producerCommandShown()).toBeFalsy();
          expect(await consumerCommandShown()).toBeTruthy();
        });

        describe('Multiple Entrypoint URLs', () => {
          it('should show api key, config, producer and consumer calls', async () => {
            component.planSecurity = 'API_KEY';
            component.apiKeys = [{ key: 'api-key', hash: 'hash-username', application: { id: 'app-id', name: 'app-name' } }];
            component.apiKeyConfigUsername = 'hash-username';
            component.entrypointUrls = ['my-entrypoint-url-1', 'my-entrypoint-url-2'];

            fixture.detectChanges();

            const selectEntrypoints = await getSelectEntrypoints();
            expect(selectEntrypoints).toBeTruthy();

            const commandExamples = await getCommandExamples();
            expect(await (await getCopyCodeHarnessOrNullById('native-kafka-producer'))?.getText()).toContain('my-entrypoint-url-1');

            await selectEntrypoints?.clickOptions({ text: 'my-entrypoint-url-2' });
            fixture.detectChanges();

            await commandExamples.selectTab({ label: 'Consumer' });
            expect(await (await getCopyCodeHarnessOrNullById('native-kafka-consumer'))?.getText()).toContain('my-entrypoint-url-2');
          });
        });
      });
      describe('OAuth2', () => {
        it('should show client id and secret', async () => {
          component.planSecurity = 'OAUTH2';
          component.clientId = 'my-client-id';
          component.clientSecret = 'my-client-secret';

          fixture.detectChanges();
          expect(await clientIdShown()).toBeTruthy();
          expect(await clientSecretShown()).toBeTruthy();
          expect(await baseUrlShown()).toBeFalsy();

          expect(await getCommandExamples()).toBeTruthy();
          expect(await producerCommandShown()).toBeTruthy();
          expect(await consumerCommandShown()).toBeFalsy();
        });
      });
      describe('JWT', () => {
        it('should show client id and secret', async () => {
          component.planSecurity = 'JWT';
          component.clientId = 'my-client-id';
          component.clientSecret = 'my-client-secret';

          fixture.detectChanges();
          expect(await clientIdShown()).toBeTruthy();
          expect(await clientSecretShown()).toBeTruthy();
          expect(await baseUrlShown()).toBeFalsy();

          expect(await getCommandExamples()).toBeTruthy();
          expect(await producerCommandShown()).toBeTruthy();
          expect(await consumerCommandShown()).toBeFalsy();
        });
      });
    });
  });

  describe('Pending', () => {
    it('should show message', async () => {
      component.planSecurity = 'API_KEY';
      component.subscription = { status: 'PENDING' } as Subscription;
      component.entrypointUrls = ['my-entrypoint-url'];
      component.apiKeys = [{ key: 'api-key', application: { id: 'app-id', name: 'app-name' } }];
      fixture.detectChanges();
      await expectMessageTextContains('Subscription in progress');
      await expectMessageTextContains('Your subscription request is being validated. Come back later.');
      expect(await revokeApiKeyButtonShown()).toBeFalsy();
    });
  });
  describe('Rejected', () => {
    it('should show message', async () => {
      component.planSecurity = 'API_KEY';
      component.subscription = { status: 'REJECTED' } as Subscription;
      component.entrypointUrls = ['my-entrypoint-url'];
      component.apiKeys = [{ key: 'api-key', application: { id: 'app-id', name: 'app-name' } }];

      fixture.detectChanges();
      await expectMessageTextContains('Subscription rejected');
      expect(await revokeApiKeyButtonShown()).toBeFalsy();
    });
  });
  describe('Paused', () => {
    it('should show message', async () => {
      component.planSecurity = 'API_KEY';
      component.subscription = { status: 'PAUSED' } as Subscription;
      component.entrypointUrls = ['my-entrypoint-url'];
      component.apiKeys = [{ key: 'api-key', application: { id: 'app-id', name: 'app-name' } }];

      fixture.detectChanges();
      await expectMessageTextContains('Subscription paused');
      expect(await revokeApiKeyButtonShown()).toBeFalsy();
    });
  });
  describe('Closed', () => {
    it('should show message', async () => {
      component.planSecurity = 'API_KEY';
      component.subscription = { status: 'CLOSED' } as Subscription;
      component.entrypointUrls = ['my-entrypoint-url'];
      component.apiKeys = [{ key: 'api-key', application: { id: 'app-id', name: 'app-name' } }];

      fixture.detectChanges();
      await expectMessageTextContains('Subscription closed');
      expect(await revokeApiKeyButtonShown()).toBeFalsy();
    });
  });

  async function apiKeysTableShown() {
    return !!(await harnessLoader.getHarnessOrNull(PaginatedTableHarness));
  }
  function expiredApiKeyDate(): string {
    return new Date(Date.now() - 60_000).toISOString();
  }
  function revokeApiKeyUrl(subscriptionId: string, apiKey: string): string {
    return `${TESTING_BASE_URL}/subscriptions/${subscriptionId}/keys/${apiKey}/_revoke`;
  }
  function expectRevokeApiKeyRequest(subscriptionId: string, apiKey: string) {
    const request = httpTestingController.expectOne(revokeApiKeyUrl(subscriptionId, apiKey));
    expect(request.request.method).toEqual('POST');
    expect(request.request.body).toBeNull();
    return request;
  }
  function expectNoRevokeApiKeyRequest() {
    httpTestingController.expectNone(request => request.url.includes('/_revoke'), 'revoke API key request');
  }
  function renewApiKeyUrl(subscriptionId: string): string {
    return `${TESTING_BASE_URL}/subscriptions/${subscriptionId}/keys/_renew`;
  }
  function expectRenewApiKeyRequest(subscriptionId: string) {
    const request = httpTestingController.expectOne(renewApiKeyUrl(subscriptionId));
    expect(request.request.method).toEqual('POST');
    expect(request.request.body).toBeNull();
    return request;
  }
  function expectNoRenewApiKeyRequest() {
    httpTestingController.expectNone(request => request.url.includes('/_renew'), 'renew API key request');
  }
  function getApiKeyStatusIcons() {
    return fixture.debugElement
      .queryAll(By.css('[data-testid="api-key-status-icon"]'))
      .map(icon => icon.nativeElement.getAttribute('data-status-icon'));
  }
  function getApiKeyStatusLabels() {
    return fixture.debugElement
      .queryAll(By.css('[data-testid="api-key-status-icon"]'))
      .map(icon => icon.nativeElement.getAttribute('aria-label'));
  }
  function apiAccessShellShown() {
    return !!fixture.debugElement.query(By.css('.api-access__copy-code-content'));
  }
  function apiAccessCardShown() {
    return !!fixture.debugElement.query(By.css('.api-access'));
  }
  async function getRevokeApiKeyButton() {
    return await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ selector: '[data-testid="api-key-revoke-button"]' }));
  }
  async function getRevokeApiKeyButtons() {
    return await harnessLoader.getAllHarnesses(MatButtonHarness.with({ selector: '[data-testid="api-key-revoke-button"]' }));
  }
  async function revokeApiKeyButtonShown() {
    return !!(await getRevokeApiKeyButton());
  }
  async function getRenewApiKeyButton() {
    return await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ selector: '[data-testid="renew-api-key-button"]' }));
  }
  async function renewApiKeyButtonShown() {
    return !!(await getRenewApiKeyButton());
  }
  async function getRenewApiKeyTooltip() {
    return await harnessLoader.getHarnessOrNull(MatTooltipHarness.with({ selector: '[data-testid="renew-api-key-button"]' }));
  }
  function getRenewApiKeyFeedback() {
    return fixture.debugElement.query(By.css('[data-testid="renew-api-key-feedback"]'));
  }
  function getRenewApiKeyFeedbackText() {
    const element = getRenewApiKeyFeedback()?.nativeElement as HTMLElement | undefined;
    return element?.textContent?.trim();
  }
  function getRenewApiKeyFeedbackAttribute(attribute: string) {
    const element = getRenewApiKeyFeedback()?.nativeElement as HTMLElement | undefined;
    return element?.getAttribute(attribute) ?? null;
  }
  function getApiKeyRevokeButtonApiKeys() {
    return fixture.debugElement
      .queryAll(By.css('[data-testid="api-key-revoke-button"]'))
      .map(button => button.nativeElement.getAttribute('data-api-key'));
  }
  async function apiKeyUsernameShown() {
    return !!(await getCopyCodeHarnessOrNullByTitle('Username'));
  }
  async function apiKeyPasswordShown() {
    return !!(await getCopyCodeHarnessOrNullByTitle('Password'));
  }
  async function baseUrlShown() {
    return !!(await getCopyCodeHarnessOrNullById('base-url'));
  }
  async function getSelectEntrypoints() {
    return await harnessLoader.getHarnessOrNull(MatSelectHarness.with({ selector: '#select-entrypoints' }));
  }

  async function commandLineShown() {
    return !!(await getCopyCodeHarnessOrNullById('command-line'));
  }
  async function commandLineContains(expectedText: string) {
    return (await (await getCopyCodeHarnessOrNullById('command-line'))?.getText())?.includes(expectedText);
  }
  async function clientIdShown() {
    return !!(await getCopyCodeHarnessOrNullByTitle('Client ID'));
  }
  async function clientSecretShown() {
    return !!(await getCopyCodeHarnessOrNullByTitle('Client Secret'));
  }
  async function getApiKeyPropertiesConfiguration() {
    return await harnessLoader.getHarness(MatTabGroupHarness.with({ selector: '[aria-label="API Key connect properties"]' }));
  }
  async function getPlainConfiguration() {
    return await getCopyCodeHarnessOrNullById('native-kafka-api-key-plain-properties');
  }
  async function plainConfigurationShown() {
    return !!(await getCopyCodeHarnessOrNullById('native-kafka-api-key-plain-properties'));
  }
  async function scram256ConfigurationShown() {
    return !!(await getCopyCodeHarnessOrNullById('native-kafka-api-key-scram-sha-256-properties'));
  }
  async function scram512ConfigurationShown() {
    return !!(await getCopyCodeHarnessOrNullById('native-kafka-api-key-scram-sha-512-properties'));
  }
  async function getCommandExamples() {
    return await harnessLoader.getHarness(MatTabGroupHarness.with({ selector: '[aria-label="Example commands"]' }));
  }
  async function producerCommandShown() {
    return !!(await getCopyCodeHarnessOrNullById('native-kafka-producer'));
  }
  async function consumerCommandShown() {
    return !!(await getCopyCodeHarnessOrNullById('native-kafka-consumer'));
  }
  async function expectMessageTextContains(expectedMessage: string) {
    expect(fixture.debugElement.query(By.css('.api-access__message')).nativeElement.innerHTML).toContain(expectedMessage);
  }

  async function getCopyCodeHarnessOrNullByTitle(title: string): Promise<CopyCodeHarness | null> {
    return await harnessLoader.getHarnessOrNull(CopyCodeHarness.with({ selector: `[title="${title}"]` }));
  }
  async function getCopyCodeHarnessOrNullById(id: string): Promise<CopyCodeHarness | null> {
    return await harnessLoader.getHarnessOrNull(CopyCodeHarness.with({ selector: `#${id}` }));
  }
});
