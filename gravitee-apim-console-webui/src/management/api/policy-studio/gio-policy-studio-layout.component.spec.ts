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
import { GioSaveBarModule } from '@gravitee/ui-particles-angular';
import { MatTabsModule } from '@angular/material/tabs';

import { GioPolicyStudioLayoutComponent } from './gio-policy-studio-layout.component';
import { toApiDefinition } from './models/ApiDefinition';

import { User } from '../../../entities/user';
import { fakeApi } from '../../../entities/api/Api.fixture';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { AjsRootScope, CurrentUserService, UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { fakeUpdateApi } from '../../../entities/api/UpdateApi.fixture';
import { GioUiRouterTestingModule } from '../../../shared/testing/gio-uirouter-testing-module';

describe('GioPolicyStudioLayoutComponent', () => {
  let fixture: ComponentFixture<GioPolicyStudioLayoutComponent>;
  let component: GioPolicyStudioLayoutComponent;
  let httpTestingController: HttpTestingController;

  const currentUser = new User();
  currentUser.userApiPermissions = ['api-plan-r', 'api-plan-u'];

  const api = fakeApi();
  const $broadcast = jest.fn();

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [GioPolicyStudioLayoutComponent],
      imports: [NoopAnimationsModule, GioHttpTestingModule, MatSnackBarModule, MatTabsModule, GioSaveBarModule, GioUiRouterTestingModule],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId: api.id } },
        {
          provide: AjsRootScope,
          useValue: {
            $broadcast: $broadcast,
          },
        },
        {
          provide: CurrentUserService,
          useValue: { currentUser },
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

    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`).flush(api);

    fixture.detectChanges();
  });

  describe('onSubmit', () => {
    it('should call the API', async () => {
      component.onSubmit();

      httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`).flush(api);

      const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}` });

      expect(req.request.body).toStrictEqual(
        fakeUpdateApi({
          background: undefined,
          categories: undefined,
          paths: undefined,
          picture: undefined,
          plans: [],
          flows: api.flows,
        }),
      );
    });

    it('should broadcast `apiChangeSuccess` with api updated', async () => {
      const updateApi = fakeUpdateApi();
      const emitApiDefinitionSpy = jest.spyOn(component.policyStudioService, 'emitApiDefinition');

      component.onSubmit();

      httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`).flush(api);
      httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}` }).flush(updateApi);

      expect($broadcast).toHaveBeenCalledWith('apiChangeSuccess', { api: updateApi });
      expect(emitApiDefinitionSpy).toHaveBeenCalledTimes(1);
      expect(component.isDirty).toBeFalsy();
    });
  });

  describe('onReset', () => {
    it('should reload on reset', async () => {
      const resetSpy = jest.spyOn(component.policyStudioService, 'reset');
      const reloadSpy = jest.spyOn(component.ajsStateService, 'reload');

      component.onReset();

      expect(resetSpy).toHaveBeenCalledTimes(1);
      expect(reloadSpy).toHaveBeenCalledTimes(1);
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
