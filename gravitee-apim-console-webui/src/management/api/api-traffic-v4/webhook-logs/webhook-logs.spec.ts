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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { of } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { MatIconRegistry } from '@angular/material/icon';
import { provideHttpClient } from '@angular/common/http';

import { WebhookLogsComponent } from './webhook-logs.component';
import { WebhookLogsHarness } from './webhook-logs.harness';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { ApiV4 } from '../../../../entities/management-api-v2';
import { WebhookSettingsDialogComponent } from './components/webhook-settings-dialog';

declare const describe: (...args: any[]) => void;
declare const beforeEach: (...args: any[]) => void;
declare const afterEach: (...args: any[]) => void;
declare const it: (...args: any[]) => void;
declare const expect: (...args: any[]) => any;
declare const jest: {
  fn: (...args: any[]) => any;
  spyOn: (...args: any[]) => any;
};

const API_ID = 'api-test-id';
const defaultApi = {
  id: API_ID,
  analytics: { enabled: true, logging: { mode: { endpoint: true } } },
} as ApiV4;

class MatIconRegistryMock {
  addSvgIconSetInNamespace(): this {
    return this;
  }
  addSvgIconLiteralInNamespace(): this {
    return this;
  }
  addSvgIconSet(): this {
    return this;
  }
  addSvgIcon(): this {
    return this;
  }
  getNamedSvgIcon(): any {
    return of(document.createElement('svg'));
  }
}

describe('WebhookLogsComponent', () => {
  let fixture: ComponentFixture<WebhookLogsComponent>;
  let harness: WebhookLogsHarness;
  let routerNavigateSpy: ReturnType<typeof jest.spyOn>;
  let dialogOpenSpy: jest.Mock;

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
      imports: [WebhookLogsComponent, NoopAnimationsModule, RouterTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: activatedRouteMock },
        { provide: ApiV2Service, useValue: apiServiceMock },
        { provide: MatIconRegistry, useClass: MatIconRegistryMock },
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
    expect(await logsListHarness!.countRows()).toBe(5);

    await logsListHarness!.clickDetailsButtonAtRow(0);

    expect(routerNavigateSpy).toHaveBeenCalledWith(['./', 'req-1'], {
      relativeTo: activatedRouteMock,
    });
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

  it('should open the settings dialog on init when requested through query params', async () => {
    await setupComponent({ queryParams: { openSettings: 'true' } });

    expect(dialogOpenSpy).toHaveBeenCalledTimes(1);
    expect(dialogOpenSpy).toHaveBeenCalledWith(WebhookSettingsDialogComponent, {
      width: '750px',
      data: API_ID,
    });

    expect(routerNavigateSpy).toHaveBeenCalledWith([], {
      relativeTo: activatedRouteMock,
      queryParams: { openSettings: null },
      queryParamsHandling: 'merge',
    });
  });
});
