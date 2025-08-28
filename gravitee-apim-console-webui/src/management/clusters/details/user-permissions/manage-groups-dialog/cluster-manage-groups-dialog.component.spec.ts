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
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';

import { ClusterManageGroupsDialogComponent } from './cluster-manage-groups-dialog.component';
import { ClusterManageGroupsDialogHarness } from './cluster-manage-groups-dialog.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import { GioTestingPermission, GioTestingPermissionProvider } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { expectGetClusterRequest, expectUpdateGroupsRequest } from '../../../../../services-ngx/clusters.service.spec';
import { fakeBaseGroup, fakeCluster, fakeGroupsResponse } from '../../../../../entities/management-api-v2';

describe('ClusterManageGroupsDialogComponent (harness)', () => {
  let httpTestingController: HttpTestingController;
  let fixture: ComponentFixture<ClusterManageGroupsDialogComponent>;
  let componentHarness: ClusterManageGroupsDialogHarness;

  async function init(permissions: GioTestingPermission) {
    await TestBed.configureTestingModule({
      imports: [ClusterManageGroupsDialogComponent, NoopAnimationsModule, GioTestingModule, MatDialogModule],
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: permissions,
        },
        { provide: MatDialogRef, useValue: { close: jest.fn() } },
        { provide: MAT_DIALOG_DATA, useValue: { clusterId: 'cluster-id' } },
      ],
    }).compileComponents();

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ClusterManageGroupsDialogComponent);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ClusterManageGroupsDialogHarness);
    fixture.detectChanges();
  }

  afterEach(() => {
    httpTestingController.verify({ ignoreCancelled: true });
  });

  describe('with cluster write permission', () => {
    beforeEach(async () => {
      await init(['cluster-definition-u']);
    });

    it('should allow selecting groups and saving', async () => {
      // Expect the component to load the cluster and groups
      expectGetClusterRequest(httpTestingController, fakeCluster({ id: 'cluster-id', groups: [] }));
      fixture.detectChanges();

      httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/groups?page=1&perPage=9999`).flush(
        fakeGroupsResponse({
          data: [
            fakeBaseGroup({ id: 'g1', name: 'Group 1' }),
            fakeBaseGroup({ id: 'g2', name: 'Group 2' }),
            fakeBaseGroup({ id: 'g3', name: 'Group 3' }),
          ],
        }),
      );

      expect(await componentHarness.getTitleText()).toBe('Manage groups');
      expect(await componentHarness.getAvailableGroups()).toEqual(['Group 1', 'Group 2', 'Group 3']);

      // Select two groups and save
      await componentHarness.selectGroups(['Group 1', 'Group 2']);
      await componentHarness.save();

      // Expect updateGroups call with selected group ids in the same order
      expectUpdateGroupsRequest(httpTestingController, 'cluster-id', ['g1', 'g2']);
    });
  });

  describe('without cluster write permission', () => {
    beforeEach(async () => {
      await init([]);
    });

    it('should show read-only associated groups', async () => {
      // Cluster has one associated group (g1)
      expectGetClusterRequest(httpTestingController, fakeCluster({ id: 'cluster-id', groups: ['g1', 'g2'] }));
      fixture.detectChanges();

      httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/groups?page=1&perPage=9999`).flush(
        fakeGroupsResponse({
          data: [fakeBaseGroup({ id: 'g1', name: 'Group 1' }), fakeBaseGroup({ id: 'g2', name: 'Group 2' })],
        }),
      );

      // In read-only mode, the select is hidden and read-only text is displayed
      expect(await componentHarness.getReadOnlyGroupsText()).toBe('Group 1, Group 2');
    });
  });
});
