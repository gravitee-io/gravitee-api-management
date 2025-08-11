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
import { ActivatedRoute, Router } from '@angular/router';
import { GioConfirmAndValidateDialogHarness } from '@gravitee/ui-particles-angular';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { HarnessLoader } from '@angular/cdk/testing';
import { HttpTestingController } from '@angular/common/http/testing';

import { ClusterGeneralComponent } from './cluster-general.component';
import { ClusterGeneralHarness } from './cluster-general.harness';

import { GioTestingModule } from '../../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { expectDeleteClusterRequest, expectGetClusterRequest } from '../../../../services-ngx/clusters.service.spec';
import { fakeCluster } from '../../../../entities/management-api-v2';

describe('ClusterGeneralComponent', () => {
  const CLUSTER_ID = 'clusterId';

  let fixture: ComponentFixture<ClusterGeneralComponent>;
  let clusterGeneralHarness: ClusterGeneralHarness;
  let router: Router;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  let permissions: string[];

  beforeEach(async () => {
    permissions = ['cluster-definition-u', 'cluster-definition-d'];

    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, ClusterGeneralComponent],

      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { clusterId: CLUSTER_ID } } } },
        { provide: GioTestingPermissionProvider, useFactory: () => permissions },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
          isTabbable: () => true, // Allows to choose a day in the calendar
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ClusterGeneralComponent);
    httpTestingController = TestBed.inject(HttpTestingController);

    router = TestBed.inject(Router);
    jest.spyOn(router, 'navigate');

    fixture.detectChanges();
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    clusterGeneralHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ClusterGeneralHarness);
    expectGetClusterRequest(
      httpTestingController,
      fakeCluster({
        id: CLUSTER_ID,
      }),
    );
    fixture.detectChanges();
  });

  it('should initialize the form with cluster data', async () => {
    expect(await clusterGeneralHarness.getNameValue()).toBe('Cluster Name');
    expect(await clusterGeneralHarness.getDescriptionValue()).toBe('A test cluster');
  });

  it('should validate the form correctly', async () => {
    // Clear the name field (which is required)
    await clusterGeneralHarness.setNameValue('');

    expect(await clusterGeneralHarness.isFormValid()).toBe(false);

    await clusterGeneralHarness.setNameValue('New Cluster Name');

    expect(await clusterGeneralHarness.isFormValid()).toBe(true);
  });

  it('should submit the form and update the cluster', async () => {
    await clusterGeneralHarness.setNameValue('Updated Cluster Name');
    await clusterGeneralHarness.setDescriptionValue('Updated Description');

    await clusterGeneralHarness.submitForm();
    fixture.detectChanges();

    // TODO: Verify the service was called with the correct data
  });

  it('should delete the cluster', async () => {
    await clusterGeneralHarness.clickDeleteButton();

    const confirmDialog = await rootLoader.getHarness(GioConfirmAndValidateDialogHarness);
    await confirmDialog.confirm();

    expectDeleteClusterRequest(httpTestingController, CLUSTER_ID);

    expect(router.navigate).toHaveBeenCalledWith(['../../'], expect.anything());
  });
});
