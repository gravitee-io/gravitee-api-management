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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { GioConfirmAndValidateDialogHarness } from '@gravitee/ui-particles-angular';

import { ClusterListComponent } from './cluster-list.component';
import { ClustersListPageHarness } from './cluster-list.harness';

import { ClustersAddDialogHarness } from '../add-dialog/clusters-add-dialog.harness';
import { GioTestingModule } from '../../../shared/testing';
import {
  GioTestingPermissionProvider,
  GioTestingRoleScopePermission,
  GioTestingRolesScopePermissionProvider,
} from '../../../shared/components/gio-permission/gio-permission.service';
import {
  expectCreateClusterRequest,
  expectDeleteClusterRequest,
  expectListClusterRequest,
} from '../../../services-ngx/cluster.service.spec';
import { fakeCluster, fakeCreateCluster, fakePagedResult } from '../../../entities/management-api-v2';

describe('ClustersListPageComponent', () => {
  let fixture: ComponentFixture<ClusterListComponent>;
  let componentHarness: ClustersListPageHarness;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ClusterListComponent, NoopAnimationsModule, GioTestingModule],
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: ['environment-cluster-c'],
        },
        {
          provide: GioTestingRolesScopePermissionProvider,
          useValue: [
            {
              roleScope: 'CLUSTER',
              permissions: ['cluster-definition-r', 'cluster-definition-u', 'cluster-definition-d', 'cluster-configuration-r'],
              id: 'cluster-id',
            },
            {
              roleScope: 'CLUSTER',
              permissions: ['cluster-definition-r'],
              id: 'cluster-readonly-id',
            },
          ] satisfies GioTestingRoleScopePermission[],
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ClusterListComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.autoDetectChanges();
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ClustersListPageHarness);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);

    expectListClusterRequest(
      httpTestingController,
      fakePagedResult([
        fakeCluster({
          name: 'Production Cluster',
          configuration: {
            bootstrapServers: 'kafka-prod.example.com:9092',
            security: {
              protocol: 'SSL',
            },
          },
        }),

        fakeCluster({
          name: 'Development Cluster',
          configuration: {
            bootstrapServers: 'kafka-dev.example.com:9092',
            security: {
              protocol: 'SASL_PLAINTEXT',
            },
          },
        }),
        fakeCluster({
          id: 'cluster-readonly-id',
          name: 'Testing Cluster (ReadOnly)',
          configuration: {
            bootstrapServers: 'kafka-test.example.com:9092',
            security: {
              protocol: 'PLAINTEXT',
            },
          },
        }),
      ]),
    );
  });

  afterEach(() => {
    httpTestingController.verify({
      ignoreCancelled: true,
    });
  });

  it('should display clusters table', async () => {
    const table = await componentHarness.getTable();

    expect(await table.getCellTextByIndex()).toStrictEqual([
      ['Production Cluster', 'kafka-prod.example.com:9092', 'SSL', 'Jan 1, 2023, 12:00:00 AM', ''],
      ['Development Cluster', 'kafka-dev.example.com:9092', 'SASL_PLAINTEXT', 'Jan 1, 2023, 12:00:00 AM', ''],
      ['Testing Cluster (ReadOnly)', '••••••••', '••••••••', 'Jan 1, 2023, 12:00:00 AM', ''],
    ]);
  });

  it('should display actions column only for clusters where user has write permission', async () => {
    const productionClusterEditBtn = await componentHarness.getEditButton(0);
    const productionClusterRemoveBtn = await componentHarness.getRemoveButton(0);
    expect(productionClusterEditBtn).not.toBeNull();
    expect(productionClusterRemoveBtn).not.toBeNull();

    const testingClusterEditBtn = await componentHarness.getEditButton(2);
    const testingClusterRemoveBtn = await componentHarness.getRemoveButton(2);
    expect(testingClusterEditBtn).toBeNull();
    expect(testingClusterRemoveBtn).toBeNull();
  });

  it('should refresh the table when filters change', async () => {
    const table = await componentHarness.getTable();
    const getTableWrapper = await componentHarness.getTableWrapper();

    expect(await table.getCellTextByIndex()).toStrictEqual([
      ['Production Cluster', 'kafka-prod.example.com:9092', 'SSL', 'Jan 1, 2023, 12:00:00 AM', ''],
      ['Development Cluster', 'kafka-dev.example.com:9092', 'SASL_PLAINTEXT', 'Jan 1, 2023, 12:00:00 AM', ''],
      ['Testing Cluster (ReadOnly)', '••••••••', '••••••••', 'Jan 1, 2023, 12:00:00 AM', ''],
    ]);

    await getTableWrapper.setSearchValue('Production');
    await fixture.whenStable();

    expectListClusterRequest(httpTestingController, fakePagedResult([fakeCluster()]), '?page=1&perPage=25&q=Production');

    expect(await table.getCellTextByIndex()).toStrictEqual([
      ['Cluster Name', 'kafka.example.com:9092', 'PLAINTEXT', 'Jan 1, 2023, 12:00:00 AM', ''],
    ]);
  });

  it('should create a new cluster using the dialog', async () => {
    await componentHarness.clickAddButton();

    const dialogHarness = await rootLoader.getHarness(ClustersAddDialogHarness);

    await dialogHarness.setName('Test Cluster');
    await dialogHarness.setDescription('A test cluster created by automated test');
    await dialogHarness.setBootstrapServers('kafka-test-new.example.com:9092');

    await dialogHarness.create();

    expectCreateClusterRequest(
      httpTestingController,
      fakeCreateCluster({
        name: 'Test Cluster',
        description: 'A test cluster created by automated test',
        configuration: {
          bootstrapServers: 'kafka-test-new.example.com:9092',
          security: {
            protocol: 'PLAINTEXT',
          },
        },
      }),
    );
  });

  it('should delete the cluster', async () => {
    const CLUSTER_ID = fakeCluster().id;
    const removeBtn = await componentHarness.getRemoveButton(0);
    await removeBtn.click();

    const confirmDialog = await rootLoader.getHarness(GioConfirmAndValidateDialogHarness);
    await confirmDialog.confirm();

    expectDeleteClusterRequest(httpTestingController, CLUSTER_ID);
    await fixture.whenStable();

    // After deletion, we expect the list to be refreshed
    expectListClusterRequest(httpTestingController);
  });
});
