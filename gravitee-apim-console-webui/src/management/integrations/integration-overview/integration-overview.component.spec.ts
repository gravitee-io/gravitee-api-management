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
import { HttpTestingController, TestRequest } from '@angular/common/http/testing';
import { BrowserAnimationsModule, NoopAnimationsModule } from '@angular/platform-browser/animations';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { TestElement } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatPaginatorHarness } from '@angular/material/paginator/testing';

import { IntegrationOverviewComponent } from './integration-overview.component';
import { IntegrationOverviewHarness } from './integration-overview.harness';

import { IntegrationsModule } from '../integrations.module';
import { AgentStatus, FederatedAPI, FederatedAPIsResponse, Integration } from '../integrations.model';
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

  describe('permissions', () => {
    beforeEach(() => {
      init(['environment-integration-r']);
    });

    it('button should be hidden without permissions', async () => {
      expectIntegrationGetRequest(fakeIntegration({ id: integrationId }));
      expectFederatedAPIsGetRequest();

      const discoverBtn: MatButtonHarness = await componentHarness.getDiscoverButton();
      expect(discoverBtn).toBeNull();
    });
  });

  describe('discover', () => {
    beforeEach(() => {
      init();
    });

    it('button should be disabled when agent "DISCONNECTED"', async () => {
      expectIntegrationGetRequest(fakeIntegration({ id: integrationId, agentStatus: AgentStatus.DISCONNECTED }));
      expectFederatedAPIsGetRequest();

      const discoverBtn: MatButtonHarness = await componentHarness.getDiscoverButton();
      expect(discoverBtn).toBeTruthy();
      expect(discoverBtn.isDisabled()).toBeTruthy();
    });

    describe('when confirming', () => {
      it('should handle a pending Ingestion Job', async () => {
        expectIntegrationGetRequest(fakeIntegration({ id: integrationId }));
        expectFederatedAPIsGetRequest();

        const discoverBtn: MatButtonHarness = await componentHarness.getDiscoverButton();
        await discoverBtn.click();
        expectPreviewGetRequest();

        const dialogHarness: GioConfirmDialogHarness =
          await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
        await dialogHarness.confirm();

        expectIngestPostRequest({
          status: 'PENDING',
        });
        expect(fakeSnackBarService.success).toHaveBeenCalledWith('Federated APIs ingestion started');

        const banner = await componentHarness.getPendingJobBanner().then((e) => e.text());
        expect(banner).toContain('A discovery is in progress. The process should only take a few minutes to complete.');
      });

      it('should handle a completed Ingestion Job', async () => {
        expectIntegrationGetRequest(fakeIntegration({ id: integrationId }));
        expectFederatedAPIsGetRequest();

        const discoverBtn: MatButtonHarness = await componentHarness.getDiscoverButton();
        await discoverBtn.click();
        expectPreviewGetRequest();

        const dialogHarness: GioConfirmDialogHarness =
          await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
        await dialogHarness.confirm();

        expectIngestPostRequest({
          status: 'SUCCESS',
        });
        expect(fakeSnackBarService.success).toHaveBeenCalledWith('Federated APIs ingestion completed');
        expectFederatedAPIsGetRequest();
      });

      it('should handle a failed Ingestion Job', async () => {
        expectIntegrationGetRequest(fakeIntegration({ id: integrationId }));
        expectFederatedAPIsGetRequest();

        const discoverBtn: MatButtonHarness = await componentHarness.getDiscoverButton();
        await discoverBtn.click();
        expectPreviewGetRequest();

        const dialogHarness: GioConfirmDialogHarness =
          await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
        await dialogHarness.confirm();

        expectIngestPostRequest({
          status: 'ERROR',
          message: 'error message',
        });
        expect(fakeSnackBarService.error).toHaveBeenCalledWith('Federated APIs ingestion failed: error message');
      });
    });

    it('should not call _ingest endpoint on cancel', async () => {
      expectIntegrationGetRequest(fakeIntegration({ id: integrationId }));
      expectFederatedAPIsGetRequest();

      const discoverBtn: MatButtonHarness = await componentHarness.getDiscoverButton();
      await discoverBtn.click();
      expectPreviewGetRequest();

      const dialogHarness: GioConfirmDialogHarness =
        await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
      await dialogHarness.cancel();

      httpTestingController.expectNone(`${CONSTANTS_TESTING.env.v2BaseURL}/integrations/${integrationId}/_ingest`);
    });

    it('should handle error with message', async () => {
      expectIntegrationGetRequest(fakeIntegration({ id: integrationId }));
      expectFederatedAPIsGetRequest();

      const discoverBtn: MatButtonHarness = await componentHarness.getDiscoverButton();
      await discoverBtn.click();
      expectPreviewGetRequest();

      const dialogHarness: GioConfirmDialogHarness =
        await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
      await dialogHarness.confirm();

      const req: TestRequest = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/integrations/${integrationId}/_ingest`);
      req.flush({}, { status: 400, statusText: 'Bad Request' });

      fixture.detectChanges();

      expect(fakeSnackBarService.error).toHaveBeenCalledWith('Discovery error');
    });
  });

  describe('details', () => {
    beforeEach(() => {
      init();
    });

    it('should call backend with proper integration id', () => {
      const integrationMock: Integration = fakeIntegration({ id: integrationId });
      expectIntegrationGetRequest(integrationMock);
      expectFederatedAPIsGetRequest();
    });

    it('should display error badge', async (): Promise<void> => {
      expectIntegrationGetRequest(fakeIntegration({ id: integrationId, agentStatus: AgentStatus.DISCONNECTED }));
      expectFederatedAPIsGetRequest();

      const errorBadge: TestElement = await componentHarness.getErrorBadge();
      expect(errorBadge).toBeTruthy();

      const errorBanner = await componentHarness.getErrorBanner().then((e) => e.text());
      expect(errorBanner).toEqual(
        'Check your agent status and ensure connectivity with the provider to start importing your APIs in Gravitee.',
      );
    });

    it('should display success badge', async (): Promise<void> => {
      expectIntegrationGetRequest(fakeIntegration({ id: integrationId, agentStatus: AgentStatus.CONNECTED }));
      expectFederatedAPIsGetRequest();

      const successBadge: TestElement = await componentHarness.getSuccessBadge();
      expect(successBadge).toBeTruthy();

      expect(await componentHarness.getErrorBanner()).toBeNull();
    });

    it('should display pending job banner when a job is pending', async (): Promise<void> => {
      expectIntegrationGetRequest(fakeIntegration({ id: integrationId, agentStatus: AgentStatus.CONNECTED }));
      expectFederatedAPIsGetRequest();

      const successBadge: TestElement = await componentHarness.getSuccessBadge();
      expect(successBadge).toBeTruthy();

      expect(await componentHarness.getErrorBanner()).toBeNull();
    });
  });

  describe('federated APIs table', () => {
    beforeEach(() => {
      init();
    });

    it('should disable Discover button when data is loading', async (): Promise<void> => {
      expectIntegrationGetRequest();
      expectFederatedAPIsGetRequest();

      fixture.componentInstance.isLoadingFederatedAPI = true;
      fixture.detectChanges();

      const discoverButton = await componentHarness.getDiscoverButton();
      expect(discoverButton.isDisabled()).toBeTruthy();
    });

    it('should not be in UI without data', async (): Promise<void> => {
      expectIntegrationGetRequest();
      expectFederatedAPIsGetRequest([]);

      const table: MatTableHarness = await componentHarness.getTable();
      expect(table).toBeNull();
    });

    it('should display correct number of rows', async (): Promise<void> => {
      expectIntegrationGetRequest();
      expectFederatedAPIsGetRequest([fakeFederatedAPI(), fakeFederatedAPI(), fakeFederatedAPI(), fakeFederatedAPI()]);

      const rows: number = await componentHarness.rowsNumber();
      expect(rows).toEqual(4);
    });

    it('pagination should request proper url', async () => {
      expectIntegrationGetRequest();

      expectFederatedAPIsGetRequest([fakeFederatedAPI()], 1, 10);

      const pagination: MatPaginatorHarness = await componentHarness.getPagination();

      await pagination.setPageSize(5);
      expectFederatedAPIsGetRequest([fakeFederatedAPI()], 1, 5);

      pagination.getPageSize().then((value) => {
        expect(value).toEqual(5);
      });

      await pagination.setPageSize(25);
      expectFederatedAPIsGetRequest([fakeFederatedAPI()], 1, 25);

      pagination.getPageSize().then((value) => {
        expect(value).toEqual(25);
      });
    });
  });

  function expectPreviewGetRequest(totalCount = 10): void {
    const req: TestRequest = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/integrations/${integrationId}/_preview`);
    req.flush({ totalCount });
    expect(req.request.method).toEqual('GET');
  }

  function expectIntegrationGetRequest(integrationMock: Integration = fakeIntegration()): void {
    const req: TestRequest = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/integrations/${integrationId}`);
    req.flush(integrationMock);
    expect(req.request.method).toEqual('GET');
  }

  function expectIngestPostRequest(res): void {
    const req: TestRequest = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/integrations/${integrationId}/_ingest`);
    req.flush(res);
    expect(req.request.method).toEqual('POST');
  }

  function expectFederatedAPIsGetRequest(
    federatedAPIs: FederatedAPI[] = [fakeFederatedAPI(), fakeFederatedAPI()],
    page = 1,
    perPage = 10,
  ): void {
    const req: TestRequest = httpTestingController.expectOne(
      `${CONSTANTS_TESTING.env.v2BaseURL}/integrations/${integrationId}/apis?page=${page}&perPage=${perPage}`,
    );
    const res: FederatedAPIsResponse = {
      data: federatedAPIs,
      pagination: {
        page,
        perPage,
      },
    };

    req.flush(res);
    expect(req.request.method).toEqual('GET');
  }
});
