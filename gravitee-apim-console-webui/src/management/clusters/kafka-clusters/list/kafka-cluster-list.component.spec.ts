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

import { KafkaClusterListComponent } from './kafka-cluster-list.component';
import { KafkaClusterListPageHarness } from './kafka-cluster-list.harness';

import { KafkaClustersAddDialogHarness } from '../add-dialog/kafka-clusters-add-dialog.harness';
import { GioTestingModule } from '../../../../shared/testing';
import {
  GioTestingPermissionProvider,
  GioTestingRoleScopePermission,
  GioTestingRolesScopePermissionProvider,
} from '../../../../shared/components/gio-permission/gio-permission.service';
import {
  expectCreateClusterRequest,
  expectDeleteClusterRequest,
  expectListClusterRequest,
} from '../../../../services-ngx/cluster.service.spec';
import { fakeCluster, fakePagedResult } from '../../../../entities/management-api-v2';

describe('KafkaClusterListComponent', () => {
  let fixture: ComponentFixture<KafkaClusterListComponent>;
  let componentHarness: KafkaClusterListPageHarness;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [KafkaClusterListComponent, NoopAnimationsModule, GioTestingModule],
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
              permissions: ['cluster-definition-r', 'cluster-definition-u', 'cluster-definition-d'],
              id: 'kafka-cluster-id',
            },
            {
              roleScope: 'CLUSTER',
              permissions: ['cluster-definition-r'],
              id: 'kafka-cluster-readonly-id',
            },
          ] satisfies GioTestingRoleScopePermission[],
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(KafkaClusterListComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.autoDetectChanges();
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, KafkaClusterListPageHarness);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);

    expectListClusterRequest(
      httpTestingController,
      fakePagedResult([
        fakeCluster({
          id: 'kafka-cluster-id',
          type: 'KAFKA_CLUSTER',
          name: 'Production Kafka Cluster',
          description: 'Main production cluster',
        }),
        fakeCluster({
          id: 'kafka-cluster-readonly-id',
          type: 'KAFKA_CLUSTER',
          name: 'Staging Kafka Cluster',
          description: 'Staging environment',
        }),
      ]),
      '?page=1&perPage=25&type=KAFKA_CLUSTER',
    );
  });

  afterEach(() => {
    httpTestingController.verify({
      ignoreCancelled: true,
    });
  });

  it('should display kafka clusters table', async () => {
    const table = await componentHarness.getTable();

    expect(await table.getCellTextByIndex()).toStrictEqual([
      ['Production Kafka Cluster', 'Main production cluster', 'Jan 1, 2023, 12:00:00 AM', ''],
      ['Staging Kafka Cluster', 'Staging environment', 'Jan 1, 2023, 12:00:00 AM', ''],
    ]);
  });

  it('should display actions column only for clusters where user has write permission', async () => {
    const editBtn = await componentHarness.getEditButton(0);
    const removeBtn = await componentHarness.getRemoveButton(0);
    expect(editBtn).not.toBeNull();
    expect(removeBtn).not.toBeNull();

    const readonlyEditBtn = await componentHarness.getEditButton(1);
    const readonlyRemoveBtn = await componentHarness.getRemoveButton(1);
    expect(readonlyEditBtn).toBeNull();
    expect(readonlyRemoveBtn).toBeNull();
  });

  it('should refresh the table when filters change', async () => {
    const table = await componentHarness.getTable();
    const getTableWrapper = await componentHarness.getTableWrapper();

    expect(await table.getCellTextByIndex()).toStrictEqual([
      ['Production Kafka Cluster', 'Main production cluster', 'Jan 1, 2023, 12:00:00 AM', ''],
      ['Staging Kafka Cluster', 'Staging environment', 'Jan 1, 2023, 12:00:00 AM', ''],
    ]);

    await getTableWrapper.setSearchValue('Production');
    await fixture.whenStable();

    expectListClusterRequest(
      httpTestingController,
      fakePagedResult([
        fakeCluster({
          id: 'kafka-cluster-id',
          type: 'KAFKA_CLUSTER',
          name: 'Production Kafka Cluster',
          description: 'Main production cluster',
        }),
      ]),
      '?page=1&perPage=25&q=Production&type=KAFKA_CLUSTER',
    );

    expect(await table.getCellTextByIndex()).toStrictEqual([
      ['Production Kafka Cluster', 'Main production cluster', 'Jan 1, 2023, 12:00:00 AM', ''],
    ]);
  });

  it('should create a new kafka cluster using the dialog', async () => {
    await componentHarness.clickAddButton();

    const dialogHarness = await rootLoader.getHarness(KafkaClustersAddDialogHarness);

    await dialogHarness.setName('New Kafka Cluster');
    await dialogHarness.setDescription('A new kafka cluster');

    await dialogHarness.create();

    expectCreateClusterRequest(httpTestingController, {
      type: 'KAFKA_CLUSTER',
      name: 'New Kafka Cluster',
      description: 'A new kafka cluster',
      crossId: undefined,
      configuration: {
        connections: [],
      },
    });
  });

  it('should create a new kafka cluster with explicit crossId', async () => {
    await componentHarness.clickAddButton();

    const dialogHarness = await rootLoader.getHarness(KafkaClustersAddDialogHarness);

    await dialogHarness.setName('New Kafka Cluster');
    await dialogHarness.setCrossId('my-custom-cross-id');
    await dialogHarness.setDescription('A new kafka cluster');

    await dialogHarness.create();

    expectCreateClusterRequest(httpTestingController, {
      type: 'KAFKA_CLUSTER',
      crossId: 'my-custom-cross-id',
      name: 'New Kafka Cluster',
      description: 'A new kafka cluster',
      configuration: {
        connections: [],
      },
    });
  });

  it('should delete the kafka cluster', async () => {
    const removeBtn = await componentHarness.getRemoveButton(0);
    await removeBtn.click();

    const confirmDialog = await rootLoader.getHarness(GioConfirmAndValidateDialogHarness);
    await confirmDialog.confirm();

    expectDeleteClusterRequest(httpTestingController, 'kafka-cluster-id');
    await fixture.whenStable();

    expectListClusterRequest(
      httpTestingController,
      fakePagedResult([fakeCluster({ id: 'kafka-cluster-id', type: 'KAFKA_CLUSTER', name: 'Production Kafka Cluster' })]),
      '?page=1&perPage=25&type=KAFKA_CLUSTER',
    );
  });
});
