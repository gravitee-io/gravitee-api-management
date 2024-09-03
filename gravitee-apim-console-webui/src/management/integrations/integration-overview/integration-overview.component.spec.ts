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

import { ComponentFixture, discardPeriodicTasks, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { HttpTestingController, TestRequest } from '@angular/common/http/testing';
import { BrowserAnimationsModule, NoopAnimationsModule } from '@angular/platform-browser/animations';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatPaginatorHarness } from '@angular/material/paginator/testing';

import { IntegrationOverviewComponent } from './integration-overview.component';
import { IntegrationOverviewHarness } from './integration-overview.harness';

import { IntegrationsModule } from '../integrations.module';
import { AgentStatus, FederatedAPI, FederatedAPIsResponse, AsyncJobStatus, Integration } from '../integrations.model';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { fakeIntegration } from '../../../entities/integrations/integration.fixture';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioTestingPermission, GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { fakeFederatedAPI } from '../../../entities/integrations/federatedAPI.fixture';

describe('IntegrationOverviewComponent', () => {
  let fixture: ComponentFixture<IntegrationOverviewComponent>;
  let componentHarness: IntegrationOverviewHarness;
  let httpTestingController: HttpTestingController;
  const integrationId: string = 'asd123';

  const fakeSnackBarService = {
    error: jest.fn(),
    success: jest.fn(),
  };

  const init = async (
    permissions: GioTestingPermission = [
      'environment-integration-u',
      'environment-integration-d',
      'environment-integration-c',
      'environment-integration-r',
    ],
  ) => {
    await TestBed.configureTestingModule({
      declarations: [IntegrationOverviewComponent],
      imports: [GioTestingModule, IntegrationsModule, BrowserAnimationsModule, NoopAnimationsModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ integrationId }) } },
        },
        {
          provide: SnackBarService,
          useValue: fakeSnackBarService,
        },
        {
          provide: GioTestingPermissionProvider,
          useValue: permissions,
        },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
          isTabbable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(IntegrationOverviewComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, IntegrationOverviewHarness);
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should display loading panel while loading', fakeAsync(async () => {
    await init();
    expect(await componentHarness.getLoaderPanel()).not.toBeNull();

    discardPeriodicTasks();
  }));

  describe('permissions', () => {
    beforeEach(() => {});

    it('button should be hidden without permissions', fakeAsync(async () => {
      await init(['environment-integration-r']);
      tick(1);
      expectIntegrationGetRequest(fakeIntegration({ id: integrationId }));
      expectFederatedAPIsGetRequest();

      const discoverBtn: MatButtonHarness = await componentHarness.getDiscoverButton();
      expect(discoverBtn).toBeNull();
    }));
  });

  describe('details', () => {
    it('should integration info once loaded', fakeAsync(async () => {
      await init();

      tick(1);
      expectIntegrationGetRequest(fakeIntegration({ id: integrationId }));
      expectFederatedAPIsGetRequest();

      expect(await componentHarness.getIntegrationProvider()).toEqual('test_provider');
      expect(await componentHarness.getAgentStatus()).toEqual('connected');
    }));

    it('should display error badge', fakeAsync(async (): Promise<void> => {
      await init();

      tick(1);
      expectIntegrationGetRequest(fakeIntegration({ id: integrationId, agentStatus: AgentStatus.DISCONNECTED }));
      expectFederatedAPIsGetRequest();

      expect(await componentHarness.getAgentStatus()).toEqual('disconnected');

      const errorBanner = await componentHarness.getErrorBanner().then((e) => e.text());
      expect(errorBanner).toEqual(
        'Check your agent status and ensure connectivity with the provider to start importing your APIs in Gravitee.',
      );
    }));
  });

  describe('federated APIs table', () => {
    it('should display a panel without data', fakeAsync(async (): Promise<void> => {
      await init();
      tick(1);
      expectIntegrationGetRequest();
      expectFederatedAPIsGetRequest([]);

      expect(await componentHarness.getTable()).toBeNull();
      expect(await componentHarness.getNoIntegrationMessage()).toContain('No APIs created');
    }));

    it('should display a panel when ingestion is pending and not API ingested yet ', fakeAsync(async (): Promise<void> => {
      await init();
      tick(1);
      expectIntegrationGetRequest(
        fakeIntegration({
          pendingJob: { id: 'job-id', status: AsyncJobStatus.PENDING, startedAt: '2023-08-27T18:04:37Z' },
        }),
      );
      expectFederatedAPIsGetRequest([]);

      expect(await componentHarness.getTable()).toBeNull();
      expect(await componentHarness.getNoIntegrationMessage()).toContain('APIs are being ingested');

      discardPeriodicTasks();
    }));

    it('should display the current APIs list with a banner when ingestion is pending', fakeAsync(async (): Promise<void> => {
      await init();
      tick(1);
      expectIntegrationGetRequest(
        fakeIntegration({
          pendingJob: { id: 'job-id', status: AsyncJobStatus.PENDING, startedAt: '2023-08-27T18:04:37Z' },
        }),
      );
      expectFederatedAPIsGetRequest([fakeFederatedAPI()]);

      expect(await componentHarness.getTable()).not.toBeNull();
      expect(await componentHarness.rowsNumber()).toEqual(1);
      expect(await componentHarness.getPendingJobBanner()).not.toBeNull();

      discardPeriodicTasks();
    }));

    it('should display correct number of rows', fakeAsync(async (): Promise<void> => {
      await init();
      tick(1);
      expectIntegrationGetRequest();
      expectFederatedAPIsGetRequest([fakeFederatedAPI(), fakeFederatedAPI(), fakeFederatedAPI(), fakeFederatedAPI()]);

      const rows: number = await componentHarness.rowsNumber();
      expect(rows).toEqual(4);
    }));

    it('pagination should request proper url', fakeAsync(async () => {
      await init();
      tick(1);
      expectIntegrationGetRequest();
      expectFederatedAPIsGetRequest([fakeFederatedAPI()], 1, 10);

      const pagination: MatPaginatorHarness = await componentHarness.getPagination();

      await pagination.setPageSize(5);
      tick(300);
      expectFederatedAPIsGetRequest([fakeFederatedAPI()], 1, 5);
      expect(await pagination.getPageSize()).toEqual(5);

      await pagination.setPageSize(25);
      tick(300);
      expectFederatedAPIsGetRequest([fakeFederatedAPI()], 1, 25);
      expect(await pagination.getPageSize()).toEqual(25);

      discardPeriodicTasks();
    }));
  });

  describe('discover', () => {
    it('button should be disabled when agent "DISCONNECTED"', fakeAsync(async () => {
      await init();
      tick(1);
      expectIntegrationGetRequest(fakeIntegration({ id: integrationId, agentStatus: AgentStatus.DISCONNECTED }));
      expectFederatedAPIsGetRequest();

      const discoverBtn: MatButtonHarness = await componentHarness.getDiscoverButton();
      expect(await discoverBtn.isDisabled()).toBe(true);
    }));

    it('button should be active when agent "CONNECTED"', fakeAsync(async () => {
      await init();
      tick(1);
      expectIntegrationGetRequest(fakeIntegration({ id: integrationId, agentStatus: AgentStatus.CONNECTED }));
      expectFederatedAPIsGetRequest();

      const discoverBtn: MatButtonHarness = await componentHarness.getDiscoverButton();
      expect(await discoverBtn.isDisabled()).toBe(false);
    }));
  });

  function expectIntegrationGetRequest(integrationMock: Integration = fakeIntegration()): void {
    const req: TestRequest = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/integrations/${integrationId}`);
    req.flush(integrationMock);
  }

  function expectFederatedAPIsGetRequest(
    federatedAPIs: FederatedAPI[] = [fakeFederatedAPI(), fakeFederatedAPI()],
    page = 1,
    perPage = 10,
    totalCount = federatedAPIs.length,
  ): void {
    const req: TestRequest = httpTestingController.expectOne(
      `${CONSTANTS_TESTING.env.v2BaseURL}/integrations/${integrationId}/apis?page=${page}&perPage=${perPage}`,
    );
    const res: FederatedAPIsResponse = {
      data: federatedAPIs,
      pagination: {
        page,
        perPage,
        totalCount,
      },
    };

    req.flush(res);
  }
});
