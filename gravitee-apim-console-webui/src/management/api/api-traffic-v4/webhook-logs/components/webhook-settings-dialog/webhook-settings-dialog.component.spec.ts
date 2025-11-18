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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { of } from 'rxjs';

import { WebhookSettingsDialogComponent } from './webhook-settings-dialog.component';
import { WebhookSettingsDialogHarness } from './webhook-settings-dialog.harness';

import { ApiV2Service } from '../../../../../../services-ngx/api-v2.service';
import { SnackBarService } from '../../../../../../services-ngx/snack-bar.service';
import { ApiV4 } from '../../../../../../entities/management-api-v2';

describe('WebhookSettingsDialogComponent', () => {
  let fixture: ComponentFixture<WebhookSettingsDialogComponent>;
  let harness: WebhookSettingsDialogHarness;
  let loader: ReturnType<typeof TestbedHarnessEnvironment.documentRootLoader>;

  const dialogRefMock = { close: jest.fn() };
  const apiGetMock = jest.fn();
  const apiUpdateMock = jest.fn();
  const apiServiceMock: Pick<ApiV2Service, 'get' | 'update'> = { get: apiGetMock, update: apiUpdateMock };
  const snackBarErrorMock = jest.fn();
  const snackBarSuccessMock = jest.fn();
  const snackBarServiceMock: Pick<SnackBarService, 'error' | 'success'> = { error: snackBarErrorMock, success: snackBarSuccessMock };

  beforeEach(async () => {
    dialogRefMock.close.mockClear();
    apiGetMock.mockClear();
    apiUpdateMock.mockClear();
    snackBarErrorMock.mockClear();
    snackBarSuccessMock.mockClear();
    apiGetMock.mockReturnValue(of(createApiV4()));
    apiUpdateMock.mockImplementation((id, api) => of(api));

    await TestBed.configureTestingModule({
      imports: [WebhookSettingsDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefMock },
        { provide: MAT_DIALOG_DATA, useValue: 'api-id' },
        { provide: ApiV2Service, useValue: apiServiceMock },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              params: { apiId: 'api-id' },
              queryParams: {},
              queryParamMap: convertToParamMap({}),
            },
            paramMap: of(convertToParamMap({ apiId: 'api-id' })),
            params: of({ apiId: 'api-id' }),
            queryParamMap: of(convertToParamMap({})),
            queryParams: of({}),
            fragment: of(null),
            data: of({}),
          } as unknown as ActivatedRoute,
        },
        { provide: SnackBarService, useValue: snackBarServiceMock },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(WebhookSettingsDialogComponent);
    loader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, WebhookSettingsDialogHarness);
  });

  it('should render the dialog content once the API is loaded', async () => {
    expect(await harness.isLoading()).toBe(false);
    expect(await harness.getTitleText()).toBe('Webhook Logs reporting settings');
  });

  it('should close the dialog when discard is clicked', async () => {
    expect(dialogRefMock.close).not.toHaveBeenCalled();
    // Make a change to ensure the save bar is visible (it only appears when form has unsaved changes)
    const enabledToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' }));
    await enabledToggle.toggle();
    await fixture.whenStable();
    fixture.detectChanges();

    // Verify save bar is visible before trying to click discard
    const saveBar = await loader.getHarness(GioSaveBarHarness);
    expect(await saveBar.isVisible()).toBe(true);

    // Click discard button (which triggers resetClicked event and closes the dialog)
    await harness.clickClose();
    expect(dialogRefMock.close).toHaveBeenCalled();
  });

  describe('when analytics is enabled initially', () => {
    it('should have top toggle enabled and checked', async () => {
      const enabledToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' }));
      expect(await enabledToggle.isChecked()).toBe(true);
      expect(await enabledToggle.isDisabled()).toBe(false);
    });

    it('should enable content data toggles when top toggle is turned on', async () => {
      const enabledToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' }));
      const requestBodyToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="requestBody"]' }));

      // Turn off and then back on
      await enabledToggle.toggle();
      expect(await requestBodyToggle.isDisabled()).toBe(true);

      await enabledToggle.toggle();
      expect(await requestBodyToggle.isDisabled()).toBe(false);
    });

    it('should have content data toggles enabled', async () => {
      const requestBodyToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="requestBody"]' }));
      const requestHeadersToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="requestHeaders"]' }));
      const responseBodyToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="responseBody"]' }));
      const responseHeadersToggle = await loader.getHarness(
        MatSlideToggleHarness.with({ selector: '[formControlName="responseHeaders"]' }),
      );

      expect(await requestBodyToggle.isDisabled()).toBe(false);
      expect(await requestHeadersToggle.isDisabled()).toBe(false);
      expect(await responseBodyToggle.isDisabled()).toBe(false);
      expect(await responseHeadersToggle.isDisabled()).toBe(false);
    });

    it('should disable and set content data toggles to false when top toggle is turned off', async () => {
      const enabledToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' }));
      const requestBodyToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="requestBody"]' }));
      const requestHeadersToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="requestHeaders"]' }));
      const responseBodyToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="responseBody"]' }));
      const responseHeadersToggle = await loader.getHarness(
        MatSlideToggleHarness.with({ selector: '[formControlName="responseHeaders"]' }),
      );

      // Set some content toggles to true first
      await requestBodyToggle.toggle();
      await requestHeadersToggle.toggle();
      expect(await requestBodyToggle.isChecked()).toBe(true);
      expect(await requestHeadersToggle.isChecked()).toBe(true);

      // Turn off the top toggle
      await enabledToggle.toggle();

      // All content toggles should be disabled and false
      expect(await requestBodyToggle.isChecked()).toBe(false);
      expect(await requestBodyToggle.isDisabled()).toBe(true);
      expect(await requestHeadersToggle.isChecked()).toBe(false);
      expect(await requestHeadersToggle.isDisabled()).toBe(true);
      expect(await responseBodyToggle.isChecked()).toBe(false);
      expect(await responseBodyToggle.isDisabled()).toBe(true);
      expect(await responseHeadersToggle.isChecked()).toBe(false);
      expect(await responseHeadersToggle.isDisabled()).toBe(true);
    });
  });

  describe('when analytics is disabled initially', () => {
    beforeEach(async () => {
      apiGetMock.mockReturnValue(
        of(
          createApiV4({
            analytics: {
              enabled: false,
              sampling: { type: 'COUNT', value: '100' },
              logging: {
                content: {
                  messagePayload: false,
                  messageHeaders: false,
                  payload: false,
                  headers: false,
                },
              },
            },
          }),
        ),
      );
      fixture = TestBed.createComponent(WebhookSettingsDialogComponent);
      loader = TestbedHarnessEnvironment.documentRootLoader(fixture);
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();
      harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, WebhookSettingsDialogHarness);
    });

    it('should have top toggle enabled and unchecked', async () => {
      const enabledToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' }));
      expect(await enabledToggle.isChecked()).toBe(false);
      expect(await enabledToggle.isDisabled()).toBe(false);
    });

    it('should have content data toggles disabled', async () => {
      const requestBodyToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="requestBody"]' }));
      const requestHeadersToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="requestHeaders"]' }));
      const responseBodyToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="responseBody"]' }));
      const responseHeadersToggle = await loader.getHarness(
        MatSlideToggleHarness.with({ selector: '[formControlName="responseHeaders"]' }),
      );

      expect(await requestBodyToggle.isDisabled()).toBe(true);
      expect(await requestHeadersToggle.isDisabled()).toBe(true);
      expect(await responseBodyToggle.isDisabled()).toBe(true);
      expect(await responseHeadersToggle.isDisabled()).toBe(true);
    });
  });

  function createApiV4(overrides: Partial<ApiV4> = {}): ApiV4 {
    return {
      id: 'api-id',
      name: 'Webhook API',
      apiVersion: '1.0.0',
      definitionVersion: 'V4',
      type: 'PROXY',
      originContext: { origin: 'MANAGEMENT' },
      analytics: {
        enabled: true,
        sampling: { type: 'COUNT', value: '100' },
        logging: {
          content: {
            messagePayload: false,
            messageHeaders: false,
            payload: false,
            headers: false,
          },
        },
      },
      ...overrides,
    };
  }
});
