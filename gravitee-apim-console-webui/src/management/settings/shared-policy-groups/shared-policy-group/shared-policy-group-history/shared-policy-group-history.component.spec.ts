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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';

import { SharedPolicyGroupHistoryComponent } from './shared-policy-group-history.component';
import { SharedPolicyGroupHistoryHarness } from './shared-policy-group-history.harness';
import { HistoryJsonDialogHarness } from './history-json-dialog/history-json-dialog.harness';
import { HistoryStudioDialogHarness } from './history-studio-dialog/history-studio-dialog.harness';
import { HistoryCompareDialogHarness } from './history-compare-dialog/history-compare-dialog.harness';

import {
  expectGetSharedPolicyGroupRequest,
  expectListSharedPolicyGroupHistoriesRequest,
} from '../../../../../services-ngx/shared-policy-groups.service.spec';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import { fakePagedResult, fakePoliciesPlugin, fakePolicyPlugin, fakeSharedPolicyGroup } from '../../../../../entities/management-api-v2';

describe('SharedPolicyGroupHistoryComponent', () => {
  const SHARED_POLICY_GROUP_ID = 'sharedPolicyGroupId';
  let fixture: ComponentFixture<SharedPolicyGroupHistoryComponent>;
  let rootLoader: HarnessLoader;
  let componentHarness: SharedPolicyGroupHistoryHarness;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, SharedPolicyGroupHistoryComponent, GioTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { params: { sharedPolicyGroupId: SHARED_POLICY_GROUP_ID } } },
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

    fixture = TestBed.createComponent(SharedPolicyGroupHistoryComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, SharedPolicyGroupHistoryHarness);
    expectGetSharedPolicyGroupRequest(
      httpTestingController,
      fakeSharedPolicyGroup({
        id: SHARED_POLICY_GROUP_ID,
      }),
    );
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should display resources table', async () => {
    const table = await componentHarness.getTable();

    expect(await table.getCellTextByIndex()).toStrictEqual([['Loading...']]);
    expectListSharedPolicyGroupHistoriesRequest(httpTestingController, undefined, SHARED_POLICY_GROUP_ID);

    expect(await table.getCellTextByIndex()).toStrictEqual([['', '1', 'Shared policy group', expect.any(String), 'codeeye']]);
  });

  it('should refresh the table when filters change', async () => {
    const table = await componentHarness.getTable();
    const getTableWrapper = await componentHarness.getTableWrapper();

    expect(await table.getCellTextByIndex()).toStrictEqual([['Loading...']]);
    expectListSharedPolicyGroupHistoriesRequest(httpTestingController, undefined, SHARED_POLICY_GROUP_ID);

    await (await getTableWrapper.getPaginator()).setPageSize(50);
    expect(await table.getCellTextByIndex()).toStrictEqual([['Loading...']]);

    expectListSharedPolicyGroupHistoriesRequest(httpTestingController, undefined, SHARED_POLICY_GROUP_ID, '?page=1&perPage=50');

    expect(await table.getCellTextByIndex()).toStrictEqual([['', '1', 'Shared policy group', expect.any(String), 'codeeye']]);
  });

  it('should display the JSON dialog', async () => {
    expectListSharedPolicyGroupHistoriesRequest(httpTestingController, undefined, SHARED_POLICY_GROUP_ID);

    const table = await componentHarness.getTable();
    await table
      .getRows()
      .then((rows) =>
        rows[0]
          .getCells({ columnName: 'actions' })
          .then((cells) => cells[0].getHarness(MatButtonHarness.with({ text: 'code' })).then((button) => button.click())),
      );
    const dialog = await rootLoader.getHarness(HistoryJsonDialogHarness);

    expect(dialog).toBeTruthy();
  });

  it('should display the details dialog', async () => {
    expectListSharedPolicyGroupHistoriesRequest(httpTestingController, undefined, SHARED_POLICY_GROUP_ID);

    const table = await componentHarness.getTable();
    await table
      .getRows()
      .then((rows) =>
        rows[0]
          .getCells({ columnName: 'actions' })
          .then((cells) => cells[0].getHarness(MatButtonHarness.with({ text: 'eye' })).then((button) => button.click())),
      );
    expectGetPolicies();
    const dialog = await rootLoader.getHarness(HistoryStudioDialogHarness);

    expect(await dialog.getTitleText()).toEqual('Version 1 details');
  });

  it('should display compare dialog', async () => {
    expectListSharedPolicyGroupHistoriesRequest(
      httpTestingController,
      fakePagedResult([
        fakeSharedPolicyGroup({
          version: 2,
        }),
        fakeSharedPolicyGroup({ version: 1 }),
      ]),
      SHARED_POLICY_GROUP_ID,
    );

    const table = await componentHarness.getTable();
    const rows = await table.getRows();
    await rows[0]
      .getCells({ columnName: 'checkbox' })
      .then((cells) => cells[0].getHarness(MatCheckboxHarness).then((checkbox) => checkbox.check()));
    await rows[1]
      .getCells({ columnName: 'checkbox' })
      .then((cells) => cells[0].getHarness(MatCheckboxHarness).then((checkbox) => checkbox.check()));

    await componentHarness.compareTwoSPGButton().then((button) => button.click());

    const dialog = await rootLoader.getHarness(HistoryCompareDialogHarness);
    expect(await dialog.getTitleText()).toEqual('Comparing version 1 with version 2');
  });

  it('should always compare older to newer', async () => {
    expectListSharedPolicyGroupHistoriesRequest(
      httpTestingController,
      fakePagedResult([
        fakeSharedPolicyGroup({
          version: 2,
        }),
        fakeSharedPolicyGroup({ version: 1 }),
      ]),
      SHARED_POLICY_GROUP_ID,
    );

    const table = await componentHarness.getTable();
    const rows = await table.getRows();
    await rows[0]
      .getCells({ columnName: 'checkbox' })
      .then((cells) => cells[0].getHarness(MatCheckboxHarness).then((checkbox) => checkbox.check()));
    await rows[1]
      .getCells({ columnName: 'checkbox' })
      .then((cells) => cells[0].getHarness(MatCheckboxHarness).then((checkbox) => checkbox.check()));

    await componentHarness.compareTwoSPGButton().then((button) => button.click());

    const dialog = await rootLoader.getHarness(HistoryCompareDialogHarness);
    expect(await dialog.getTitleText()).toEqual('Comparing version 1 with version 2');
  });

  it('should restore version', async () => {
    expectListSharedPolicyGroupHistoriesRequest(
      httpTestingController,
      fakePagedResult([fakeSharedPolicyGroup({ id: SHARED_POLICY_GROUP_ID, name: 'Old name' })]),
      SHARED_POLICY_GROUP_ID,
    );
    const table = await componentHarness.getTable();
    await table
      .getRows()
      .then((rows) =>
        rows[0]
          .getCells({ columnName: 'actions' })
          .then((cells) => cells[0].getHarness(MatButtonHarness.with({ text: 'eye' })).then((button) => button.click())),
      );
    expectGetPolicies();
    const dialog = await rootLoader.getHarness(HistoryStudioDialogHarness);
    expect(await dialog.getTitleText()).toEqual('Version 1 details');

    await dialog.clickRestoreVersion();

    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/shared-policy-groups/${SHARED_POLICY_GROUP_ID}`,
      method: 'PUT',
    });

    expect(req.request.body).toEqual(fakeSharedPolicyGroup({ id: SHARED_POLICY_GROUP_ID, name: 'Old name' }));
    req.flush(fakeSharedPolicyGroup({ id: SHARED_POLICY_GROUP_ID, name: 'Old name' }));
  });

  function expectGetPolicies() {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/policies`,
        method: 'GET',
      })
      .flush([fakePolicyPlugin(), ...fakePoliciesPlugin()]);
  }
});
