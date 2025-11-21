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

import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { HarnessLoader } from '@angular/cdk/testing';

import { WebhookLogsComponent } from './webhook-logs.component';
import { WebhookLogsHarness } from './webhook-logs.harness';
import { WebhookSettingsDialogComponent } from './components/webhook-settings-dialog/webhook-settings-dialog.component';
import { WebhookSettingsDialogHarness } from './components/webhook-settings-dialog/webhook-settings-dialog.harness';

import { ApiV4 } from '../../../../entities/management-api-v2';
import { Constants } from '../../../../entities/Constants';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';

const API_ID = 'api-test-id';
const defaultApi = {
  id: API_ID,
  analytics: { enabled: true, logging: { mode: { endpoint: true } } },
  definitionVersion: 'V4',
} as ApiV4;

describe('WebhookLogsComponent', () => {
  let fixture: ComponentFixture<WebhookLogsComponent>;
  let harness: WebhookLogsHarness;
  let routerNavigateSpy: jest.SpyInstance;
  let httpTestingController: HttpTestingController;
  let activatedRoute: ActivatedRoute;
  let rootLoader: HarnessLoader;

  const setupComponent = async (options?: { queryParams?: Record<string, string | undefined>; api?: ApiV4 }) => {
    const { queryParams = {}, api = defaultApi } = options ?? {};

    TestBed.configureTestingModule({
      imports: [WebhookLogsComponent, WebhookSettingsDialogComponent, NoopAnimationsModule, MatIconTestingModule, GioTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              params: { apiId: API_ID },
              queryParams: queryParams,
              queryParamMap: convertToParamMap(queryParams),
            },
            params: of({ apiId: API_ID }),
            queryParams: of(queryParams),
          },
        },
        { provide: Constants, useValue: CONSTANTS_TESTING },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });

    await TestBed.compileComponents();

    fixture = TestBed.createComponent(WebhookLogsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    activatedRoute = TestBed.inject(ActivatedRoute);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    const router = TestBed.inject(Router);
    routerNavigateSpy = jest.spyOn(router, 'navigate').mockResolvedValue(true as any);

    fixture.detectChanges();

    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
      method: 'GET',
    });
    req.flush(api || defaultApi);

    await fixture.whenStable();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, WebhookLogsHarness);
  };

  afterEach(() => {
    httpTestingController?.verify();
    routerNavigateSpy?.mockRestore();
  });

  it('should render demo logs and navigate to the details page when clicking the details action', async () => {
    await setupComponent();

    const logsListHarness = await harness.getLogsList();
    expect(logsListHarness).not.toBeNull();
    expect(await logsListHarness!.countRows()).toBe(3);

    await logsListHarness!.clickDetailsButtonAtRow(0);

    expect(routerNavigateSpy).toHaveBeenCalledWith(['./', 'req-1'], {
      relativeTo: activatedRoute,
    });
  });

  it('should filter logs when quick filters change', async () => {
    await setupComponent();

    fixture.componentInstance.onFiltersChanged({ statuses: [500] });
    fixture.detectChanges();

    const logsListHarness = await harness.getLogsList();
    expect(await logsListHarness!.countRows()).toBe(1);

    fixture.componentInstance.onFiltersChanged({
      statuses: [500],
      searchTerm: 'monitoring',
    });
    fixture.detectChanges();

    expect(await logsListHarness!.countRows()).toBe(1);

    fixture.componentInstance.onFiltersChanged({
      applications: [{ value: '8ff0178d-9326-465c-b017-8d9326a65cb7', label: 'Acme Monitoring' }],
    });
    fixture.detectChanges();

    expect(await logsListHarness!.countRows()).toBe(3);
  });

  it('should open the settings dialog when clicking the Configure Reporting button', async () => {
    await setupComponent();

    await harness.clickConfigureReporting();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const dialog = await rootLoader.getHarness(WebhookSettingsDialogHarness);
    expect(dialog).not.toBeNull();
  });

  it('should show the reporting disabled banner when analytics are disabled', async () => {
    const disabledApi = {
      ...defaultApi,
      analytics: { enabled: false },
    } as ApiV4;

    await setupComponent({ api: disabledApi });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('reporting-disabled-banner')).not.toBeNull();
  });
});
