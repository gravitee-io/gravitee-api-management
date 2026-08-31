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
import { HttpTestingController, TestRequest } from '@angular/common/http/testing';
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

    fixture = TestBed.createComponent(OrgSettingsPlatformPoliciesStudioComponent);
    component = fixture.componentInstance;

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  const expectLoadRequests = (respondToOrganization: (request: TestRequest) => void) => {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/flows/flow-schema`).flush(platformFlowSchema);

    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/policies?expand=schema&expand=icon`).flush(policies);

    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/resources?expand=schema&expand=icon`).flush(organization);

    respondToOrganization(httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}`));
  };

  describe('ngOnInit', () => {
    beforeEach(() => {
      expectLoadRequests(request => request.flush(organization));
      fixture.detectChanges();
    });

    it('should render the studio', () => {
      expect(fixture.nativeElement.querySelector('gv-design')).not.toBeNull();
    });

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
      });
    });
  });

  describe('when the load fails', () => {
    it('should report the error instead of loading forever', async () => {
      expectLoadRequests(request =>
        request.flush({ message: 'You do not have sufficient rights to access this resource' }, { status: 403, statusText: 'Forbidden' }),
      );
      fixture.detectChanges();

      expect(fixture.nativeElement.textContent).toContain('You do not have sufficient rights to access this resource');
      expect(fixture.nativeElement.textContent).not.toContain('Loading...');
      expect(fixture.nativeElement.querySelector('gv-design')).toBeNull();
    });

    it('should report a fallback message when the failure carries none', async () => {
      expectLoadRequests(request => request.error(new ProgressEvent('error')));
      fixture.detectChanges();

      expect(fixture.nativeElement.textContent).toContain('Platform policies could not be loaded');
      expect(fixture.nativeElement.textContent).not.toContain('Loading...');
      expect(fixture.nativeElement.querySelector('gv-design')).toBeNull();
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
