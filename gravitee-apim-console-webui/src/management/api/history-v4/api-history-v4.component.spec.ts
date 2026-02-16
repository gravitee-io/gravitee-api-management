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
import { HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { HarnessLoader, parallel } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ActivatedRoute, Router } from '@angular/router';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';

import { ApiHistoryV4Component } from './api-history-v4.component';
import { ApiHistoryV4DeploymentsTableComponent } from './deployments-table/api-history-v4-deployments-table.component';
import { ApiHistoryV4Module } from './api-history-v4.module';

import { Api, fakeApiV4, fakeEvent, fakeEventsResponse, Pagination, SearchApiEventParam } from '../../../entities/management-api-v2';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { GioTableWrapperHarness } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.harness';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { GioDiffHarness } from '../../../shared/components/gio-diff/gio-diff.harness';

describe('ApiHistoryV4Component', () => {
  const API_ID = 'an-api-id';

  let fixture: ComponentFixture<ApiHistoryV4Component>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let routerNavigateSpy: jest.SpyInstance;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [ApiHistoryV4Module, NoopAnimationsModule, MatIconTestingModule, GioTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID } } } },
        {
          provide: GioTestingPermissionProvider,
          useValue: ['api-definition-u'],
        },
      ],
      declarations: [ApiHistoryV4Component, ApiHistoryV4DeploymentsTableComponent],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        isTabbable: () => true, // This traps focus checks and so avoid warnings when dealing with
      },
    });

    fixture = TestBed.createComponent(ApiHistoryV4Component);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    const router = TestBed.inject(Router);
    routerNavigateSpy = jest.spyOn(router, 'navigate');
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('Events table', () => {
    beforeEach(() => {
      expectApiGetRequest(fakeApiV4({ id: API_ID, deploymentState: 'DEPLOYED' }));
    });

    it('should display events', async () => {
      expectApiEventsListRequest(
        undefined,
        undefined,
        fakeEventsResponse({
          data: [fakeEvent({ type: 'PUBLISH_API' })],
        }),
      );

      const table = await loader.getHarness(MatTableHarness.with({ selector: '#deploymentsTable' }));
      expect(await table.getRows().then(value => value[0].getCellTextByColumnName())).toEqual({
        version: '1  In use',
        createdAt: 'Jan 1, 2021, 12:00:00 AM',
        user: 'John Doe',
        label: 'sample-label',
        action: '',
        checkbox: '',
      });
    });

    it('should display an empty table', async () => {
      expectApiEventsListRequest(
        undefined,
        undefined,
        fakeEventsResponse({
          data: [],
        }),
      );

      const table = await loader.getHarness(MatTableHarness.with({ selector: '#deploymentsTable' }));
      const rows = await table.getRows();
      const rowCells = await parallel(() => rows.map(row => row.getCellTextByIndex()));
      expect(rowCells).toHaveLength(0);

      const tableElement = await table.host();
      expect(await tableElement.text()).toContain('There is no published API (yet).');
    });

    describe('pagination', () => {
      it('should disable next & previous page links when only one page', async () => {
        expectApiEventsListRequest(
          { types: 'PUBLISH_API' },
          undefined,
          fakeEventsResponse({ pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 5, totalCount: 5 } }),
        );
        const table = await loader.getHarness(GioTableWrapperHarness);
        const paginator = await table.getPaginator();

        expect(await paginator.isNextPageDisabled()).toEqual(true);
        expect(await paginator.isPreviousPageDisabled()).toEqual(true);
      });

      it('should enable next & previous page links', async () => {
        expectApiEventsListRequest(
          undefined,
          { page: 1, perPage: 10 },
          fakeEventsResponse({ pagination: { page: 1, perPage: 10, pageCount: 3, pageItemsCount: 10, totalCount: 29 } }),
        );
        const table = await loader.getHarness(GioTableWrapperHarness);
        const paginator = await table.getPaginator();

        // 1st page
        expect(await paginator.isNextPageDisabled()).toEqual(false);
        expect(await paginator.isPreviousPageDisabled()).toEqual(true);

        // navigate to 2nd page
        await paginator.goToNextPage();
        expectApiEventsListRequest(
          { types: 'PUBLISH_API' },
          { page: 2, perPage: 10 },
          fakeEventsResponse({ pagination: { page: 2, perPage: 10, pageCount: 3, pageItemsCount: 10, totalCount: 29 } }),
        );
        expect(await paginator.isNextPageDisabled()).toEqual(false);
        expect(await paginator.isPreviousPageDisabled()).toEqual(false);

        // navigate to 3rd and last page
        await paginator.goToNextPage();
        expectApiEventsListRequest(
          undefined,
          { page: 3, perPage: 10 },
          fakeEventsResponse({ pagination: { page: 3, perPage: 10, pageCount: 3, pageItemsCount: 10, totalCount: 29 } }),
        );
        expect(await paginator.isNextPageDisabled()).toEqual(true);
        expect(await paginator.isPreviousPageDisabled()).toEqual(false);
      });
    });
  });

  describe('Rollback', () => {
    describe('Message API', () => {
      it('should rollback an API', async () => {
        expectApiGetRequest(fakeApiV4({ id: API_ID, deploymentState: 'NEED_REDEPLOY', type: 'MESSAGE' }));
        expectApiEventsListRequest(
          undefined,
          undefined,
          fakeEventsResponse({
            data: [fakeEvent({ type: 'PUBLISH_API', properties: { DEPLOYMENT_NUMBER: '1' } })],
          }),
        );
        fixture.detectChanges();

        const compareCurrentButton = await loader.getHarness(
          MatButtonHarness.with({ selector: '[aria-label="Button to compare with current version to be deployed"]' }),
        );
        await compareCurrentButton.click();
        expectDeploymentCurrentGetRequest();

        const rollbackButton = await rootLoader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Button to rollback to 1"]' }));
        await rollbackButton.click();

        const confirmDialog = await rootLoader.getHarness(GioConfirmDialogHarness);
        await confirmDialog.confirm();

        const req = httpTestingController.expectOne({
          method: 'POST',
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/_rollback`,
        });
        req.flush(null);

        expectApiGetRequest(fakeApiV4({ id: API_ID, deploymentState: 'NEED_REDEPLOY', type: 'MESSAGE' }));
        expectApiEventsListRequest(
          undefined,
          undefined,
          fakeEventsResponse({
            data: [fakeEvent({ type: 'PUBLISH_API', properties: { DEPLOYMENT_NUMBER: '1' } })],
          }),
        );
        fixture.detectChanges();

        expect(routerNavigateSpy).toHaveBeenCalledWith(['../..'], { relativeTo: expect.anything() });
      });
    });
    describe('Native API', () => {
      beforeEach(() => {
        expectApiGetRequest(fakeApiV4({ id: API_ID, deploymentState: 'NEED_REDEPLOY', type: 'NATIVE' }));
        expectApiEventsListRequest(
          undefined,
          undefined,
          fakeEventsResponse({
            data: [
              fakeEvent({
                id: '2',
                type: 'PUBLISH_API',
                properties: { DEPLOYMENT_NUMBER: '2' },
                payload: JSON.stringify({ definition: JSON.stringify({ name: 'Foo' }) }),
              }),
              fakeEvent({
                id: '1',
                type: 'PUBLISH_API',
                properties: { DEPLOYMENT_NUMBER: '1' },
                payload: JSON.stringify({ definition: JSON.stringify({ name: 'Bar' }) }),
              }),
            ],
          }),
        );
      });

      it('should not allow rollback when comparing with deployed version', async () => {
        const compareCurrentButton = await loader.getHarness(
          MatButtonHarness.with({ selector: '[aria-label="Button to compare with current version to be deployed"]' }),
        );
        await compareCurrentButton.click();
        expectDeploymentCurrentGetRequest();

        fixture.detectChanges();
        expect(await rootLoader.hasHarness(MatButtonHarness.with({ selector: '[aria-label="Button to rollback to 1"]' }))).toEqual(false);
      });

      it('should not allow rollback when comparing two versions', async () => {
        const table = await loader.getHarness(MatTableHarness.with({ selector: '#deploymentsTable' }));
        const rows = await table.getRows();
        await (await (await rows[0].getCells({ columnName: 'checkbox' }))[0].getHarness(MatCheckboxHarness)).check();
        await (await (await rows[1].getCells({ columnName: 'checkbox' }))[0].getHarness(MatCheckboxHarness)).check();

        const dialog = await rootLoader.getHarness(MatDialogHarness);
        expect(await dialog.getActionsText()).toEqual('Close');
      });

      it('should not allow rollback when viewing version to be deployed', async () => {
        const viewToDeployButton = await loader.getHarness(MatButtonHarness.with({ text: /View version to be deployed/ }));
        await viewToDeployButton.click();
        expectDeploymentCurrentGetRequest();

        const dialog = await rootLoader.getHarness(MatDialogHarness);
        expect(await dialog.getActionsText()).toEqual('Close');
      });

      it('should not allow rollback when viewing version details', async () => {
        const table = await loader.getHarness(MatTableHarness.with({ selector: '#deploymentsTable' }));
        const rows = await table.getRows();
        await (
          await (
            await rows[0].getCells({ columnName: 'action' })
          )[0].getHarness(MatButtonHarness.with({ selector: '[aria-label="Button to show version"]' }))
        ).click();

        const dialog = await rootLoader.getHarness(MatDialogHarness);
        expect(await dialog.getActionsText()).toEqual('Close');
      });
    });
  });

  describe('Compare with version to be deployed', () => {
    beforeEach(() => {
      expectApiGetRequest(fakeApiV4({ id: API_ID, deploymentState: 'NEED_REDEPLOY' }));
      expectApiEventsListRequest(
        undefined,
        undefined,
        fakeEventsResponse({
          data: [fakeEvent({ type: 'PUBLISH_API', payload: JSON.stringify({ definition: JSON.stringify({ name: 'Diff' }) }) })],
        }),
      );
    });

    it('should open a dialog to compare version "to be deployed" with the selected event', async () => {
      const compareButton = await loader.getHarness(
        MatButtonHarness.with({ selector: '[aria-label="Button to compare with current version to be deployed"]' }),
      );
      await compareButton.click();
      expectDeploymentCurrentGetRequest();

      const dialog = await rootLoader.getHarness(MatDialogHarness);
      expect(dialog).toBeTruthy();
      expect(await dialog.getTitleText()).toEqual('Comparing version 1 with version to be deployed');
      expect(await dialog.getActionsText()).toContain('Rollback');

      const diffHarness = await dialog.getHarness(GioDiffHarness);
      expect(await diffHarness.hasNoDiffToDisplay()).toEqual(false);
    });
  });

  describe('Compare two versions', () => {
    beforeEach(() => {
      expectApiGetRequest(fakeApiV4({ id: API_ID, deploymentState: 'NEED_REDEPLOY' }));
      expectApiEventsListRequest(
        undefined,
        undefined,
        fakeEventsResponse({
          data: [
            fakeEvent({
              id: '2',
              type: 'PUBLISH_API',
              properties: { DEPLOYMENT_NUMBER: '2' },
              payload: JSON.stringify({ definition: JSON.stringify({ name: 'Foo' }) }),
            }),
            fakeEvent({
              id: '1',
              type: 'PUBLISH_API',
              properties: { DEPLOYMENT_NUMBER: '1' },
              payload: JSON.stringify({ definition: JSON.stringify({ name: 'Bar' }) }),
            }),
          ],
        }),
      );
    });

    it('should open a dialog to compare two versions', async () => {
      const table = await loader.getHarness(MatTableHarness.with({ selector: '#deploymentsTable' }));
      const rows = await table.getRows();
      await (await (await rows[0].getCells({ columnName: 'checkbox' }))[0].getHarness(MatCheckboxHarness)).check();
      await (await (await rows[1].getCells({ columnName: 'checkbox' }))[0].getHarness(MatCheckboxHarness)).check();

      const dialog = await rootLoader.getHarness(MatDialogHarness);
      expect(dialog).toBeTruthy();
      expect(await dialog.getTitleText()).toEqual('Comparing version 2 with version 1');
      expect(await dialog.getActionsText()).toContain('Rollback');

      const diffHarness = await dialog.getHarness(GioDiffHarness);
      expect(await diffHarness.hasNoDiffToDisplay()).toEqual(false);
    });
  });

  describe('View version to be deployed', () => {
    beforeEach(() => {
      expectApiGetRequest(fakeApiV4({ id: API_ID, deploymentState: 'NEED_REDEPLOY' }));
      expectApiEventsListRequest(
        undefined,
        undefined,
        fakeEventsResponse({
          data: [fakeEvent({ type: 'PUBLISH_API', payload: JSON.stringify({ definition: JSON.stringify({ name: 'Diff' }) }) })],
        }),
      );
    });

    it('should open a dialog to view the version to be deployed', async () => {
      const viewToDeployButton = await loader.getHarness(MatButtonHarness.with({ text: /View version to be deployed/ }));
      await viewToDeployButton.click();
      expectDeploymentCurrentGetRequest();

      const dialog = await rootLoader.getHarness(MatDialogHarness);
      expect(dialog).toBeTruthy();
      expect(await dialog.getTitleText()).toEqual('Version to be deployed');
      expect(await dialog.getActionsText()).toContain('Rollback');
    });
  });

  describe('View version details', () => {
    beforeEach(() => {
      expectApiGetRequest(fakeApiV4({ id: API_ID, deploymentState: 'DEPLOYED' }));
      expectApiEventsListRequest(
        undefined,
        undefined,
        fakeEventsResponse({
          data: [fakeEvent({ type: 'PUBLISH_API', properties: { DEPLOYMENT_NUMBER: '1', DEPLOYMENT_LABEL: 'sample-label' } })],
        }),
      );
    });

    it('should open a dialog to view the version info', async () => {
      const table = await loader.getHarness(MatTableHarness.with({ selector: '#deploymentsTable' }));
      const rows = await table.getRows();
      await (
        await (
          await rows[0].getCells({ columnName: 'action' })
        )[0].getHarness(MatButtonHarness.with({ selector: '[aria-label="Button to show version"]' }))
      ).click();

      const dialog = await rootLoader.getHarness(MatDialogHarness);
      expect(dialog).toBeTruthy();
      expect(await dialog.getTitleText()).toEqual('Version 1');
      expect(await dialog.getActionsText()).toContain('Rollback');

      const contentText = await dialog.getContentText();
      expect(contentText).toContain('sample-label');
      expect(contentText).toContain('Jan 1, 2021, 12:00:00 AM');
      expect(contentText).toContain('John Doe');
    });
  });

  function expectApiGetRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }

  function expectApiEventsListRequest(
    filters: SearchApiEventParam = { types: 'PUBLISH_API' },
    pagination: Pagination = { page: 1, perPage: 10 },
    response = fakeEventsResponse(),
  ) {
    const req = httpTestingController.expectOne(
      `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/events?page=${pagination.page}&perPage=${pagination.perPage}${
        filters.from ? '&from=' + filters.from : ''
      }${filters.to ? '&to=' + filters.to : ''}${filters.types ? '&types=' + filters.types : ''}`,
    );
    req.flush(response);
    fixture.detectChanges();
  }

  function expectDeploymentCurrentGetRequest() {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/deployments/current`, method: 'GET' })
      .flush({});
  }
});
