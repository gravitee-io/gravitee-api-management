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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { HarnessLoader } from '@angular/cdk/testing';

import { ApiCreationGetStartedComponent } from './api-creation-get-started.component';
import { ApiCreationGetStartedModule } from './api-creation-get-started.module';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { fakeInstallation } from '../../../entities/installation/installation.fixture';
import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { GioTestingPermission, GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';

describe('ApiCreationGetStartedComponent', () => {
  let fixture: ComponentFixture<ApiCreationGetStartedComponent>;
  let rootLoader: HarnessLoader;
  let component: ApiCreationGetStartedComponent;
  let httpTestingController: HttpTestingController;

  const initConfigureTestingModule = (permissions: GioTestingPermission) => {
    TestBed.configureTestingModule({
      imports: [GioPermissionModule, GioHttpTestingModule, ApiCreationGetStartedModule, MatIconTestingModule, NoopAnimationsModule],
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: permissions,
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
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    component = fixture.componentInstance;

    httpTestingController = TestBed.inject(HttpTestingController);

    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('as Admin', function () {
    beforeEach(() => {
      initConfigureTestingModule(['organization-installation-r']);
    });

    it('should use the cockpit link if installation registered', async () => {
      httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/installation`).flush(
        fakeInstallation({
          cockpitURL: 'https://cockpit.gravitee.io',
          additionalInformation: {
            COCKPIT_INSTALLATION_STATUS: 'ACCEPTED',
          },
        }),
      );
      expect(component.cockpitLink).toEqual(
        'https://cockpit.gravitee.io?utm_source=apim&utm_medium=InApp&utm_campaign=api_designer&utm_term=registered',
      );
    });

    it('should always use the cockpit.gravitee.io link even if installation not registered', async () => {
      httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/installation`).flush(
        fakeInstallation({
          cockpitURL: 'https://cockpit.gravitee.io',
          additionalInformation: {},
        }),
      );
      expect(component.cockpitLink).toEqual(
        'https://cockpit.gravitee.io?utm_source=apim&utm_medium=InApp&utm_campaign=api_designer&utm_term=not_registered',
      );
    });

    it('should open api import dialog', async () => {
      httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/installation`);
      component.goToApiImport();

      expectPoliciesSwaggerGetRequest();

      const confirmDialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#importApiDialog' }));
      await confirmDialog.close();
    });
  });

  describe('as ApiUser', function () {
    beforeEach(() => {
      initConfigureTestingModule([]);
    });

    it('should always use the cockpit.gravitee.io link even if no right to access the installation', async () => {
      httpTestingController.expectNone(`${CONSTANTS_TESTING.org.baseURL}/installation`);

      expect(component.cockpitLink).toEqual(
        'https://cockpit.gravitee.io?utm_source=apim&utm_medium=InApp&utm_campaign=api_designer&utm_term=not_registered',
      );
    });
  });

  function expectPoliciesSwaggerGetRequest() {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/policies/swagger`, method: 'GET' }).flush([]);
  }
});
