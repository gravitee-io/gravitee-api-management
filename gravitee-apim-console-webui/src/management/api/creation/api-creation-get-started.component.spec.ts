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
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { ApiCreationGetStartedComponent } from './api-creation-get-started.component';
import { ApiCreationModule } from './api-creation.module';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { CurrentUserService, UIRouterState } from '../../../ajs-upgraded-providers';
import { fakeInstallation } from '../../../entities/installation/installation.fixture';
import { User } from '../../../entities/user';
import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';

describe('ApiCreationGetStartedComponent', () => {
  const fakeAjsState = {
    go: jest.fn(),
  };

  let fixture: ComponentFixture<ApiCreationGetStartedComponent>;
  let component: ApiCreationGetStartedComponent;
  let httpTestingController: HttpTestingController;

  const initConfigureTestingModule = (currentUser: User) => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioPermissionModule, GioHttpTestingModule, ApiCreationModule, MatIconTestingModule],
      providers: [
        { provide: UIRouterState, useValue: fakeAjsState },
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

    fixture = TestBed.createComponent(ApiCreationGetStartedComponent);
    component = fixture.componentInstance;

    httpTestingController = TestBed.inject(HttpTestingController);

    fixture.detectChanges();
  };

  describe('as Admin', function () {
    beforeEach(() => {
      const currentUser = new User();
      currentUser.userPermissions = ['organization-installation-r'];
      initConfigureTestingModule(currentUser);
    });

    it('should use the cockpit link', async () => {
      httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/installation`).flush(
        fakeInstallation({
          cockpitURL: 'https://cockpit.gravitee.io',
          additionalInformation: {
            COCKPIT_INSTALLATION_STATUS: 'ACCEPTED',
          },
        }),
      );
      expect(component.cockpitLink).toEqual('https://cockpit.gravitee.io');
    });

    it('should use the gravitee.io link', async () => {
      httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/installation`).flush(
        fakeInstallation({
          cockpitURL: 'https://cockpit.gravitee.io',
          additionalInformation: {},
        }),
      );
      expect(component.cockpitLink).toEqual('https://www.gravitee.io/platform/api-designer?utm_source=apim');
    });

    it('should go to api creation wizard', async () => {
      httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/installation`);
      component.goToApiCreationWizard();
      expect(fakeAjsState.go).toHaveBeenCalledWith('management.apis.create', { definitionVersion: '2.0.0' });
    });

    it('should go to api creation import', async () => {
      httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/installation`);
      component.goToApiImport();
      expect(fakeAjsState.go).toHaveBeenCalledWith('management.apis.new-import', { definitionVersion: '2.0.0' });
    });
  });

  describe('as ApiUser', function () {
    beforeEach(() => {
      const currentUser = new User();
      currentUser.userPermissions = [];
      initConfigureTestingModule(currentUser);
    });

    it('should use the gravitee.io link', async () => {
      httpTestingController.expectNone(`${CONSTANTS_TESTING.org.baseURL}/installation`);

      expect(component.cockpitLink).toEqual('https://www.gravitee.io/platform/api-designer?utm_source=apim');
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
