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
import { ActivatedRoute } from '@angular/router';
import { HttpTestingController } from '@angular/common/http/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { importProvidersFrom } from '@angular/core';
import { GioFormJsonSchemaModule } from '@gravitee/ui-particles-angular';

import { SharedPolicyGroupsStudioComponent } from './shared-policy-groups-studio.component';
import { SharedPolicyGroupsStudioHarness } from './shared-policy-groups-studio.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import {
  fakeSharedPolicyGroup,
  fakePoliciesPlugin,
  fakePolicyPlugin,
  fakeUpdateSharedPolicyGroup,
} from '../../../../entities/management-api-v2';
import { SharedPolicyGroupsAddEditDialogHarness } from '../shared-policy-groups-add-edit-dialog/shared-policy-groups-add-edit-dialog.harness';
import {
  expectGetSharedPolicyGroupRequest,
  expectUpdateSharedPolicyGroupRequest,
} from '../../../../services-ngx/shared-policy-groups.service.spec';

describe('SharedPolicyGroupsStudioComponent', () => {
  const SHARED_POLICY_GROUP_ID = 'sharedPolicyGroupId';

  let fixture: ComponentFixture<SharedPolicyGroupsStudioComponent>;
  let componentHarness: SharedPolicyGroupsStudioHarness;
  let httpTestingController: HttpTestingController;
  let rootLoader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SharedPolicyGroupsStudioComponent, GioTestingModule, NoopAnimationsModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { params: { sharedPolicyGroupId: SHARED_POLICY_GROUP_ID } } },
        },
        {
          provide: GioTestingPermissionProvider,
          useValue: [
            'environment-shared_policy_group-c',
            'environment-shared_policy_group-r',
            'environment-shared_policy_group-u',
            'environment-shared_policy_group-d',
          ],
        },
        importProvidersFrom(GioFormJsonSchemaModule),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SharedPolicyGroupsStudioComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);

    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, SharedPolicyGroupsStudioHarness);
    fixture.detectChanges();
    expectGetSharedPolicyGroupRequest(
      httpTestingController,
      fakeSharedPolicyGroup({
        id: SHARED_POLICY_GROUP_ID,
        name: 'Shared Policy Group',
        description: '',
        steps: [],
      }),
    );
    fixture.detectChanges();
    expectGetPolicies();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should display empty request phase', async () => {
    const studio = await componentHarness.getPolicyGroupStudio();

    const phase = await studio.getPolicyGroupPhase();
    expect(await phase.getSteps()).toEqual([
      {
        name: 'Incoming request',
        type: 'connector',
      },
      {
        name: 'Outgoing request',
        type: 'connector',
      },
    ]);
  });

  it('should edit shared policy group', async () => {
    await componentHarness.clickEditButton();

    fixture.detectChanges();
    const addDialog = await rootLoader.getHarness(SharedPolicyGroupsAddEditDialogHarness);

    expect(await addDialog.getName()).toEqual('Shared Policy Group');
    expect(await addDialog.getDescription()).toEqual('');
    expect(await addDialog.getPhase()).toEqual('REQUEST');

    await addDialog.setName('New name');
    await addDialog.setDescription('New description');
    await addDialog.save();

    expectUpdateSharedPolicyGroupRequest(
      httpTestingController,
      SHARED_POLICY_GROUP_ID,
      fakeUpdateSharedPolicyGroup({
        name: 'New name',
        description: 'New description',
      }),
    );

    expectGetSharedPolicyGroupRequest(
      httpTestingController,
      fakeSharedPolicyGroup({
        id: SHARED_POLICY_GROUP_ID,
        name: 'Shared Policy Group',
        description: '',
        steps: [],
      }),
    );
  });

  it('should add policy to phase', async () => {
    const studio = await componentHarness.getPolicyGroupStudio();

    const phase = await studio.getPolicyGroupPhase();

    await phase.addStep(0, {
      policyName: fakePolicyPlugin().name,
      description: 'What does the 🦊 say?',
      waitForInitHttpRequestCompletionCb: async () => {
        httpTestingController.expectOne(`${CONSTANTS_TESTING.org.v2BaseURL}/plugins/policies/${fakePolicyPlugin().id}/schema`).flush({});
        httpTestingController
          .expectOne(`${CONSTANTS_TESTING.org.v2BaseURL}/plugins/policies/${fakePolicyPlugin().id}/documentation`)
          .flush('');
      },
    });

    await componentHarness.save();

    expectUpdateSharedPolicyGroupRequest(
      httpTestingController,
      SHARED_POLICY_GROUP_ID,
      fakeUpdateSharedPolicyGroup({
        name: 'Shared Policy Group',
        description: '',
        steps: [
          {
            policy: 'test-policy',
            name: 'Test policy',
            description: 'What does the 🦊 say?',
            condition: undefined,
            configuration: undefined,
            enabled: true,
          },
        ],
      }),
    );

    expectGetSharedPolicyGroupRequest(
      httpTestingController,
      fakeSharedPolicyGroup({
        id: SHARED_POLICY_GROUP_ID,
        name: 'Shared Policy Group',
        description: '',
        steps: [],
      }),
    );
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
