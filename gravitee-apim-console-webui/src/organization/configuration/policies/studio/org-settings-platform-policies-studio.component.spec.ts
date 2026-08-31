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
import { InteractivityChecker } from '@angular/cdk/a11y';
import { GioLicenseTestingModule } from '@gravitee/ui-particles-angular';

import { OrgSettingsPlatformPoliciesStudioComponent } from './org-settings-platform-policies-studio.component';

import { OrganizationSettingsModule } from '../../organization-settings.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { fakePolicyListItem } from '../../../../entities/policy';
import { fakeOrganization } from '../../../../entities/organization/organization.fixture';
import { fakePlatformFlowSchema } from '../../../../entities/flow/platformFlowSchema.fixture';
import { fakeFlow } from '../../../../entities/flow/flow.fixture';

describe('OrgSettingsPlatformPoliciesStudioComponent', () => {
  let fixture: ComponentFixture<OrgSettingsPlatformPoliciesStudioComponent>;
  let component: OrgSettingsPlatformPoliciesStudioComponent;
  let httpTestingController: HttpTestingController;

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

  const createComponent = (isReadonly = false) => {
    fixture = TestBed.createComponent(OrgSettingsPlatformPoliciesStudioComponent);
    component = fixture.componentInstance;
    component.isReadonly = isReadonly;

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/flows/flow-schema`).flush(platformFlowSchema);

    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/policies?expand=schema&expand=icon`).flush(policies);

    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/resources?expand=schema&expand=icon`).flush(organization);

    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}`).flush(organization);

    fixture.detectChanges();
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, OrganizationSettingsModule, GioLicenseTestingModule],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();
  });

  describe('ngOnInit', () => {
    it('should setup properties', async () => {
      createComponent();

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
      });
    });
  });

  describe('readonly mode', () => {
    it('should let the design be edited when not readonly', async () => {
      createComponent();

      expect(fixture.nativeElement.querySelector('gv-design').hasAttribute('readonly')).toEqual(false);
    });

    it('should make the design readonly when readonly', async () => {
      createComponent(true);

      expect(fixture.nativeElement.querySelector('gv-design').hasAttribute('readonly')).toEqual(true);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
