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
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { GioIconsModule, GioLicenseModule, GioSaveBarModule, LICENSE_CONFIGURATION_TESTING } from '@gravitee/ui-particles-angular';
import { MatTabsModule } from '@angular/material/tabs';
import { MatDialogModule } from '@angular/material/dialog';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { GioPolicyStudioLayoutComponent } from './gio-policy-studio-layout.component';
import { toApiDefinition, toApiPlansDefinition } from './models/ApiDefinition';
import { PolicyStudioService } from './policy-studio.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { fakeApiV2, fakePlanV2 } from '../../../entities/management-api-v2';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';

describe('GioPolicyStudioLayoutComponent', () => {
  let fixture: ComponentFixture<GioPolicyStudioLayoutComponent>;
  let component: GioPolicyStudioLayoutComponent;
  let httpTestingController: HttpTestingController;

  const api = fakeApiV2();
  const plans = [fakePlanV2()];

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [GioPolicyStudioLayoutComponent],
      imports: [
        RouterTestingModule,
        NoopAnimationsModule,
        GioTestingModule,
        MatSnackBarModule,
        MatTabsModule,
        GioSaveBarModule,
        GioLicenseModule,
        MatDialogModule,
        GioIconsModule,
      ],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: api.id } } } },
        {
          provide: GioTestingPermissionProvider,
          useValue: ['api-plan-r', 'api-plan-u'],
        },
        {
          provide: 'LicenseConfiguration',
          useValue: LICENSE_CONFIGURATION_TESTING,
        },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(GioPolicyStudioLayoutComponent);
    component = fixture.componentInstance;

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    httpTestingController.expectOne(LICENSE_CONFIGURATION_TESTING.resourceURL);

    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`).flush(api);
    httpTestingController.expectOne(
      `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}/plans?page=1&perPage=9999&statuses=PUBLISHED,DEPRECATED`,
    );

    fixture.detectChanges();
  });

  describe('onSubmit', () => {
    it('should call the API', async () => {
      const policyStudioService = TestBed.inject(PolicyStudioService);
      const apiDefinitionToSave = toApiDefinition(fakeApiV2());
      const apiPlansDefinitionToSave = toApiPlansDefinition(plans);
      apiPlansDefinitionToSave[0].flows[0].name = 'new name';

      policyStudioService.saveApiDefinition({
        ...apiDefinitionToSave,
        plans: apiPlansDefinitionToSave,
      });

      component.onSubmit();

      httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`).flush(api);
      httpTestingController
        .expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}/plans?page=1&perPage=9999&statuses=PUBLISHED,DEPRECATED`)
        .flush({
          data: plans,
        });

      const apiReq = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}` });
      expect(apiReq.request.body.flowMode).toEqual(apiDefinitionToSave.flow_mode);
      expect(apiReq.request.body.flows[0].id).toEqual(api.flows[0].id);

      const planReq = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}/plans/${plans[0].id}`,
      });
      expect(planReq.request.body.flows[0].name).toEqual('new name');
    });
  });

  describe('onDefinitionChange', () => {
    it('should mark as dirty and update api definition', async () => {
      const apiDefinition = toApiDefinition(api);

      component.onDefinitionChange(apiDefinition);

      expect(component.isDirty).toBeTruthy();
      expect(apiDefinition).toEqual(apiDefinition);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
