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
import { HttpTestingController } from '@angular/common/http/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';

import { OrgSettingsPlatformPoliciesComponent } from './org-settings-platform-policies.component';

import { OrganizationSettingsModule } from '../organization-settings.module';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { fakePolicyListItem } from '../../../entities/policy';
import { fakeOrganization } from '../../../entities/organization/organization.fixture';
import { fakePlatformFlowSchema } from '../../../entities/flow/platformFlowSchema.fixture';
import { fakeFlow } from '../../../entities/flow/flow.fixture';
import { fakeFlowConfigurationSchema } from '../../../entities/flow/configurationSchema.fixture';

describe('OrgSettingsPlatformPoliciesComponent', () => {
  let fixture: ComponentFixture<OrgSettingsPlatformPoliciesComponent>;
  let component: OrgSettingsPlatformPoliciesComponent;
  let httpTestingController: HttpTestingController;
  let rootLoader: HarnessLoader;

  const platformFlowSchema = fakePlatformFlowSchema();
  const policies = [fakePolicyListItem()];
  const organization = fakeOrganization({
    flows: [
      fakeFlow({
        condition: '',
        enabled: true,
        methods: [],
        name: 'Flow',
        'path-operator': { operator: 'STARTS_WITH', path: '' },
        post: [],
        pre: [],
        consumers: [
          { consumerId: 'Consumer 1', consumerType: 'TAG' },
          { consumerId: 'Consumer 2', consumerType: 'TAG' },
        ],
      }),
    ],
    flowMode: 'BEST_MATCH',
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, OrganizationSettingsModule],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(OrgSettingsPlatformPoliciesComponent);
    component = fixture.componentInstance;
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/flows/flow-schema`).flush(platformFlowSchema);

    httpTestingController
      .expectOne(`${CONSTANTS_TESTING.env.baseURL}/policies?expand=schema&expand=icon&withResource=false`)
      .flush(policies);

    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}`).flush(organization);
  });

  describe('ngOnInit', () => {
    it('should setup properties', async () => {
      expect(component.policies).toStrictEqual(policies);
      expect(component.platformFlowSchema).toStrictEqual(platformFlowSchema);
      expect(component.organization).toStrictEqual(organization);
      expect(component.definition).toStrictEqual({
        flows: [
          {
            condition: '',
            consumers: ['Consumer 1', 'Consumer 2'],
            enabled: true,
            methods: [],
            name: 'Flow',
            'path-operator': { operator: 'STARTS_WITH', path: '' },
            post: [],
            pre: [],
          },
        ],
        flow_mode: 'BEST_MATCH',
      });
    });
  });

  describe('onSave', () => {
    it('should call the API with updated organization', async () => {
      component.onSave({
        definition: {
          flow_mode: 'DEFAULT',
          flows: [
            {
              condition: '',
              consumers: ['New Consumer', 'Consumer 2'],
              enabled: true,
              methods: [],
              name: 'Flow',
              'path-operator': { operator: 'STARTS_WITH', path: '' },
              post: [],
              pre: [],
            },
            {
              condition: '',
              consumers: ['Consumer 3', 'Consumer 4'],
              enabled: true,
              methods: [],
              name: 'Flow 2 ',
              'path-operator': { operator: 'STARTS_WITH', path: '' },
              post: [],
              pre: [],
            },
          ],
        },
      });

      const dialog = await rootLoader.getHarness(MatDialogHarness);
      await (await dialog.getHarness(MatButtonHarness.with({ text: /^Yes/ }))).click();

      const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.org.baseURL}` });

      expect(req.request.body).toStrictEqual({
        ...organization,
        flowMode: 'DEFAULT',
        flows: [
          {
            condition: '',
            consumers: [
              { consumerId: 'New Consumer', consumerType: 'TAG' },
              { consumerId: 'Consumer 2', consumerType: 'TAG' },
            ],
            enabled: true,
            methods: [],
            name: 'Flow',
            'path-operator': { operator: 'STARTS_WITH', path: '' },
            post: [],
            pre: [],
          },
          {
            condition: '',
            consumers: [
              { consumerId: 'Consumer 3', consumerType: 'TAG' },
              { consumerId: 'Consumer 4', consumerType: 'TAG' },
            ],
            enabled: true,
            methods: [],
            name: 'Flow 2 ',
            'path-operator': { operator: 'STARTS_WITH', path: '' },
            post: [],
            pre: [],
          },
        ],
      });
      req.flush(organization);

      // This one is send by the gio-policy-studio component
      httpTestingController
        .expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/flows/configuration-schema`)
        .flush(fakeFlowConfigurationSchema());
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
