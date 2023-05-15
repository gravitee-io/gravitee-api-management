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

import { OrgSettingsPlatformPoliciesConfigComponent } from './org-settings-platform-policies-config.component';

import { OrganizationSettingsModule } from '../../organization-settings.module';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { fakeOrganization } from '../../../../entities/organization/organization.fixture';
import { fakeFlow } from '../../../../entities/flow/flow.fixture';
import { fakeFlowConfigurationSchema } from '../../../../entities/flow/configurationSchema.fixture';

describe('OrgSettingsPlatformPoliciesConfigComponent', () => {
  let fixture: ComponentFixture<OrgSettingsPlatformPoliciesConfigComponent>;
  let component: OrgSettingsPlatformPoliciesConfigComponent;
  let httpTestingController: HttpTestingController;

  const flowConfigurationSchema = fakeFlowConfigurationSchema();
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

    fixture = TestBed.createComponent(OrgSettingsPlatformPoliciesConfigComponent);
    component = fixture.componentInstance;

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    httpTestingController
      .expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/flows/configuration-schema`)
      .flush(flowConfigurationSchema);

    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}`).flush(organization);
  });

  describe('ngOnInit', () => {
    it('should setup properties', async () => {
      expect(component.flowConfigurationSchema).toStrictEqual(flowConfigurationSchema);
      expect(component.fromValue).toStrictEqual({
        flow_mode: 'BEST_MATCH',
      });
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
