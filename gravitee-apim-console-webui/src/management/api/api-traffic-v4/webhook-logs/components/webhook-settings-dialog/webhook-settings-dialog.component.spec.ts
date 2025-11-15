/// <reference types="jest" />
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
import { of } from 'rxjs';

import { WebhookSettingsDialogComponent } from './webhook-settings-dialog.component';
import { WebhookSettingsDialogHarness } from './webhook-settings-dialog.harness';

import { ApiV2Service } from '../../../../../../services-ngx/api-v2.service';
import { SnackBarService } from '../../../../../../services-ngx/snack-bar.service';
import { ApiV4 } from '../../../../../../entities/management-api-v2';

declare const describe: (...args: any[]) => void;
declare const beforeEach: (...args: any[]) => void;
declare const it: (...args: any[]) => void;
declare const expect: (...args: any[]) => any;
declare const jest: { fn: (...args: any[]) => any };

describe('WebhookSettingsDialogComponent', () => {
  let fixture: ComponentFixture<WebhookSettingsDialogComponent>;
  let harness: WebhookSettingsDialogHarness;

  const dialogRefMock = { close: jest.fn() };
  const apiGetMock = jest.fn();
  const apiServiceMock: Pick<ApiV2Service, 'get'> = { get: apiGetMock };
  const snackBarServiceMock: Pick<SnackBarService, 'error'> = { error: jest.fn() };

  beforeEach(async () => {
    dialogRefMock.close.mockClear();
    apiGetMock.mockReturnValue(of(createApiV4()));

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
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, WebhookSettingsDialogHarness);
  });

  it('should render the dialog content once the API is loaded', async () => {
    expect(await harness.isLoading()).toBe(false);
    expect(await harness.getTitleText()).toBe('Webhook Logs reporting settings');
  });

  it('should close the dialog when close is clicked', async () => {
    expect(dialogRefMock.close).not.toHaveBeenCalled();
    await harness.clickClose();
    expect(dialogRefMock.close).toHaveBeenCalled();
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
