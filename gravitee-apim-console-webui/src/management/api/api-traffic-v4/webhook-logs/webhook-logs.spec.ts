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

import { provideHttpClient } from '@angular/common/http';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';

import { WebhookLogsComponent } from './webhook-logs.component';
import { WebhookLogsHarness } from './webhook-logs.harness';
import { WebhookSettingsDialogComponent } from './components/webhook-settings-dialog';

import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { ApiV4 } from '../../../../entities/management-api-v2';
import { Constants } from '../../../../entities/Constants';
import { CONSTANTS_TESTING } from '../../../../shared/testing';

const API_ID = 'api-test-id';
const defaultApi = {
  id: API_ID,
  analytics: { enabled: true, logging: { mode: { endpoint: true } } },
} as ApiV4;

type RouterNavigateSpy = {
  mockRestore: () => void;
  mock: { calls: unknown[] };
};

type JestMockFn = ((...args: unknown[]) => unknown) & {
  mockReturnValue: (...args: unknown[]) => JestMockFn;
  mock: { calls: unknown[] };
};

describe('WebhookLogsComponent', () => {
  let fixture: ComponentFixture<WebhookLogsComponent>;
  let harness: WebhookLogsHarness;
  let routerNavigateSpy: RouterNavigateSpy | null = null;
  let dialogOpenSpy: JestMockFn;

  const activatedRouteMock = {
    snapshot: {
      params: { apiId: API_ID },
      queryParams: {},
      queryParamMap: convertToParamMap({}),
    },
  } as unknown as ActivatedRoute;

  const apiServiceMock = {
    get: jest.fn(),
  } as unknown as ApiV2Service;

  const updateRouteSnapshot = (queryParams: Record<string, string | undefined>) => {
    (activatedRouteMock.snapshot as any).queryParams = queryParams;
    (activatedRouteMock.snapshot as any).queryParamMap = convertToParamMap(queryParams);
  };

  const setupComponent = async (options?: { queryParams?: Record<string, string | undefined>; api?: ApiV4 }) => {
    const { queryParams = {}, api = defaultApi } = options ?? {};
    updateRouteSnapshot(queryParams);
    (activatedRouteMock.snapshot as any).params = { apiId: API_ID };

    apiServiceMock.get = jest.fn().mockReturnValue(of(api));

    fixture = TestBed.createComponent(WebhookLogsComponent);

    const router = TestBed.inject(Router);
    routerNavigateSpy = jest.spyOn(router, 'navigate').mockResolvedValue(true as any);

    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, WebhookLogsHarness);
  };

  beforeEach(async () => {
    dialogOpenSpy = jest.fn().mockReturnValue({
      afterClosed: () => of(undefined),
    });

    TestBed.configureTestingModule({
      imports: [WebhookLogsComponent, NoopAnimationsModule, RouterTestingModule, MatIconTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: activatedRouteMock },
        { provide: ApiV2Service, useValue: apiServiceMock },
        { provide: Constants, useValue: CONSTANTS_TESTING },
        provideHttpClient(),
      ],
    });

    TestBed.overrideProvider(MatDialog, {
      useValue: {
        open: dialogOpenSpy,
      } as Partial<MatDialog>,
    });

    await TestBed.compileComponents();
  });

  afterEach(() => {
    if (routerNavigateSpy) {
      routerNavigateSpy.mockRestore();
    }
  });

  it('should render demo logs and navigate to the details page when clicking the details action', async () => {
    await setupComponent();

    const logsListHarness = await harness.getLogsList();
    expect(logsListHarness).not.toBeNull();
    expect(await logsListHarness!.countRows()).toBe(3);

    await logsListHarness!.clickDetailsButtonAtRow(0);

    expect(routerNavigateSpy).toHaveBeenCalledWith(['./', 'req-1'], {
      relativeTo: activatedRouteMock,
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

    expect(dialogOpenSpy).toHaveBeenCalledTimes(1);
    expect(dialogOpenSpy).toHaveBeenCalledWith(WebhookSettingsDialogComponent, {
      width: '750px',
      data: API_ID,
    });
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
