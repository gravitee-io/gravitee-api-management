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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { UIRouterModule } from '@uirouter/angular';
import { MatInputHarness } from '@angular/material/input/testing';

import { ApplicationGeneralComponent } from './application-general.component';
import { ApplicationGeneralModule } from './application-general.module';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../shared/testing';
import { UIRouterStateParams, CurrentUserService } from '../../../../../ajs-upgraded-providers';
import { User } from '../../../../../entities/user';
import { fakeApplication } from '../../../../../entities/application/Application.fixture';
import { Application } from '../../../../../entities/application/application';

describe('ApplicationGeneralInfoComponent', () => {
  const APPLICATION_ID = 'id_test';
  const currentUser = new User();
  currentUser.userPermissions = [
    'application-definition-u',
    'application-definition-d',
    'application-definition-c',
    'application-definition-r',
  ];

  let fixture: ComponentFixture<ApplicationGeneralComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        NoopAnimationsModule,
        GioHttpTestingModule,
        ApplicationGeneralModule,
        MatIconTestingModule,
        UIRouterModule.forRoot({
          useHash: true,
        }),
      ],
      providers: [
        { provide: UIRouterStateParams, useValue: { applicationId: APPLICATION_ID } },
        { provide: CurrentUserService, useValue: { currentUser } },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        isTabbable: () => true, // This traps focus checks and so avoid warnings when dealing with
      },
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ApplicationGeneralComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('Application General details', () => {
    it('should edit application details', async () => {
      const applicationDetails = fakeApplication();
      expectListApplicationRequest(applicationDetails);
      fixture.detectChanges();
      await waitImageCheck();

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
      expect(await nameInput.getValue()).toEqual('Default application');
      await nameInput.setValue('new test name');

      const typeInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="type"]' }));
      expect(await typeInput.getValue()).toEqual('SIMPLE');
      await typeInput.setValue('new TYPE');

      expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
      await saveBar.clickSubmit();

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/applications/${applicationDetails.id}`);
      expect(req.request.method).toEqual('PUT');
      expect(req.request.body).toEqual({
        api_key_mode: 'UNSPECIFIED',
        background: null,
        description: 'My default application',
        domain: null,
        name: 'new test name',
        picture: null,
        settings: {
          app: {
            client_id: null,
          },
        },
      });
    });
  });

  describe('Application General details when 0Auth2 integration enabled', () => {
    it('should edit 0Auth2 form details', async () => {
      const applicationDetails = fakeApplication();
      expectListApplicationRequest(applicationDetails);
      fixture.detectChanges();
      await waitImageCheck();

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="client_id"]' }));
      expect(await nameInput.getValue()).toEqual('');
      await nameInput.setValue('123');

      expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
      await saveBar.clickSubmit();

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/applications/${applicationDetails.id}`);
      expect(req.request.method).toEqual('PUT');
      expect(req.request.body).toEqual({
        api_key_mode: 'UNSPECIFIED',
        background: null,
        description: 'My default application',
        domain: null,
        name: 'Default application',
        picture: null,
        settings: {
          app: {
            client_id: '123',
          },
        },
      });
    });
  });

  function expectListApplicationRequest(applicationDetails: Application) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}`,
        method: 'GET',
      })
      .flush(applicationDetails);
  }
});

const waitImageCheck = () => new Promise((resolve) => setTimeout(resolve, 1));
