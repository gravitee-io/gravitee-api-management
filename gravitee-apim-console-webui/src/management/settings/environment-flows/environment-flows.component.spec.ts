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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HarnessLoader } from '@angular/cdk/testing';
import { HttpTestingController } from '@angular/common/http/testing';

import { EnvironmentFlowsComponent } from './environment-flows.component';
import { EnvironmentFlowsHarness } from './environment-flows.harness';
import { EnvironmentFlowsAddEditDialogHarness } from './environment-flows-add-edit-dialog/environment-flows-add-edit-dialog.harness';

import { GioTestingModule } from '../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';

describe('EnvironmentFlowsComponent', () => {
  let fixture: ComponentFixture<EnvironmentFlowsComponent>;
  let componentHarness: EnvironmentFlowsHarness;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EnvironmentFlowsComponent, NoopAnimationsModule, GioTestingModule],
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: [
            'environment-environment_flows-c',
            'environment-environment_flows-r',
            'environment-environment_flows-u',
            'environment-environment_flows-d',
          ],
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(EnvironmentFlowsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.autoDetectChanges();
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, EnvironmentFlowsHarness);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should display resources table', async () => {
    const table = await componentHarness.getTable();

    // TODO: When the API is available
    // expect(await table.getCellTextByIndex()).toStrictEqual([['Loading...']]);
    // expectListEnvironmentFlowsGetRequest();

    expect(await table.getCellTextByIndex()).toStrictEqual([
      ['Search env flowSearch query: , sortBy: undefined, page: 1, perPage: 25', 'REQUEST', expect.any(String), expect.any(String), ''],
    ]);
  });

  it('should refresh the table when filters change', async () => {
    const table = await componentHarness.getTable();
    const getTableWrapper = await componentHarness.getTableWrapper();

    // TODO: When the API is available
    // expect(await table.getCellTextByIndex()).toStrictEqual([['Loading...']]);
    // expectListEnvironmentFlowsGetRequest();

    await getTableWrapper.setSearchValue('test');

    // TODO: When the API is available
    // expect(await table.getCellTextByIndex()).toStrictEqual([['Loading...']]);
    // expectListEnvironmentFlowsGetRequest();

    expect(await table.getCellTextByIndex()).toStrictEqual([
      ['Search env flowSearch query: test, sortBy: undefined, page: 1, perPage: 25', 'REQUEST', expect.any(String), expect.any(String), ''],
    ]);
  });

  it('should add a new environment flow', async () => {
    await componentHarness.clickAddButton('MESSAGE');

    fixture.detectChanges();
    const addDialog = await rootLoader.getHarness(EnvironmentFlowsAddEditDialogHarness);

    await addDialog.setName('test');
    await addDialog.setDescription('test');
    await addDialog.setPhase('test');
    await addDialog.save();

    // TODO: When the API is available
    // expectCreateEnvironmentFlowsPostRequest();
  });
});
