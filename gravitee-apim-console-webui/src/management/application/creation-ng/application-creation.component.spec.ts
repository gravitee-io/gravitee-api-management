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
import { HarnessLoader } from '@angular/cdk/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';

import { ApplicationCreationComponent } from './application-creation.component';
import { ApplicationCreationFormHarness } from './components/application-creation-form.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { fakeApplicationTypes } from '../../../entities/application-type/ApplicationType.fixture';
import { ApplicationType } from '../../../entities/application-type/ApplicationType';

describe('ApplicationCreationComponent', () => {
  let fixture: ComponentFixture<ApplicationCreationComponent>;
  let loader: HarnessLoader;
  let applicationCreationForm: ApplicationCreationFormHarness;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, ApplicationCreationComponent, GioTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(ApplicationCreationComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.autoDetectChanges();
  });

  describe('when multiple types available', () => {
    beforeEach(async () => {
      expectGetEnabledApplicationTypes(fakeApplicationTypes());
      applicationCreationForm = await loader.getHarness(ApplicationCreationFormHarness);
    });

    it('should create application type=simple', async () => {
      await applicationCreationForm.setGeneralInformation('name', 'description', 'domain');
      await applicationCreationForm.setApplicationType('SIMPLE');

      await applicationCreationForm.setSimpleApplicationType('appType', 'appClientId');
      await applicationCreationForm.setApplicationClientCertificate('PEM certificate');

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      await saveBar.clickSubmit();

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.baseURL}/applications`,
      });
      expect(req.request.body).toEqual({
        name: 'name',
        description: 'description',
        domain: 'domain',
        settings: {
          app: {
            client_id: 'appClientId',
            type: 'appType',
          },
          tls: {
            client_certificate: 'PEM certificate',
          },
        },
      });
    });

    it('should create application type=web', async () => {
      await applicationCreationForm.setGeneralInformation('name', 'description', 'domain');
      await applicationCreationForm.setApplicationType('WEB');

      await applicationCreationForm.setOAuthApplicationType(['Refresh Token'], ['redirectUri']);

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      await saveBar.clickSubmit();

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.baseURL}/applications`,
      });
      expect(req.request.body).toEqual({
        name: 'name',
        description: 'description',
        domain: 'domain',
        settings: {
          oauth: {
            application_type: 'WEB',
            grant_types: ['authorization_code', 'refresh_token'],
            redirect_uris: ['redirectUri'],
          },
          tls: {
            client_certificate: null,
          },
        },
      });
    });

    it('should create application type=BACKEND_TO_BACKEND', async () => {
      await applicationCreationForm.setGeneralInformation('name', 'description', 'domain');
      await applicationCreationForm.setApplicationType('BACKEND_TO_BACKEND');

      expect(await applicationCreationForm.getOauthRedirectUrisInput()).toEqual(null);

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      await saveBar.clickSubmit();

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.baseURL}/applications`,
      });
      expect(req.request.body).toEqual({
        name: 'name',
        description: 'description',
        domain: 'domain',
        settings: {
          oauth: {
            application_type: 'BACKEND_TO_BACKEND',
            grant_types: ['client_credentials'],
            redirect_uris: [],
          },
          tls: {
            client_certificate: null,
          },
        },
      });
    });
  });

  describe('when only one type available', () => {
    beforeEach(async () => {
      const SIMPLE = fakeApplicationTypes().find((type) => type.id === 'simple');
      expectGetEnabledApplicationTypes([SIMPLE]);
      applicationCreationForm = await loader.getHarness(ApplicationCreationFormHarness);
    });

    it('should preselect the only available type', async () => {
      await applicationCreationForm.setGeneralInformation('name', 'description', 'domain');

      await applicationCreationForm.setSimpleApplicationType('appType', 'appClientId');

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      await saveBar.clickSubmit();

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.baseURL}/applications`,
      });
      expect(req.request.body).toEqual({
        name: 'name',
        description: 'description',
        domain: 'domain',
        settings: {
          app: {
            client_id: 'appClientId',
            type: 'appType',
          },
          tls: {
            client_certificate: null,
          },
        },
      });
    });
  });

  function expectGetEnabledApplicationTypes(fakeApplicationTypes: ApplicationType[]) {
    const req = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.baseURL}/configuration/applications/types`,
    });
    req.flush(fakeApplicationTypes);
  }
});
