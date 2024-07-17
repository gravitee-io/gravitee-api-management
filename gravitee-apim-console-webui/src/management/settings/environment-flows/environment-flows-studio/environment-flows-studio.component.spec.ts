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

import { EnvironmentFlowsStudioComponent } from './environment-flows-studio.component';
import { EnvironmentFlowsStudioHarness } from './environment-flows-studio.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { fakeEnvironmentFlow, fakePagedResult, fakePoliciesPlugin, fakePolicyPlugin } from '../../../../entities/management-api-v2';
import { EnvironmentFlowsService } from '../../../../services-ngx/environment-flows.service';

describe('EnvironmentFlowsStudioComponent', () => {
  const ENVIRONMENT_FLOW_ID = 'environmentFlowId';

  let fixture: ComponentFixture<EnvironmentFlowsStudioComponent>;
  let componentHarness: EnvironmentFlowsStudioHarness;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EnvironmentFlowsStudioComponent, GioTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { params: { environmentFlowId: ENVIRONMENT_FLOW_ID } } },
        },
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

    // TODO: Remove when the API is available
    const environmentFlowsService = TestBed.inject(EnvironmentFlowsService);
    environmentFlowsService.environmentFlows$.next(
      fakePagedResult([
        fakeEnvironmentFlow({
          id: ENVIRONMENT_FLOW_ID,
          phase: 'REQUEST',
        }),
      ]),
    );

    fixture = TestBed.createComponent(EnvironmentFlowsStudioComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, EnvironmentFlowsStudioHarness);
    fixture.detectChanges();
    expectGetPolicies();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should display empty request phase', async () => {
    const studio = await componentHarness.getEnvironmentFlowsStudio();

    const phase = await studio.getFlowPhase('REQUEST');
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

  function expectGetPolicies() {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/policies`,
        method: 'GET',
      })
      .flush([fakePolicyPlugin(), ...fakePoliciesPlugin()]);
  }
});
