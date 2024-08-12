/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpClientModule } from '@angular/common/http';
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ApplicationTabSettingsEditHarness } from './application-tab-settings-edit/application-tab-settings-edit.harness';
import { ApplicationTabSettingsComponent } from './application-tab-settings.component';
import { DeleteConfirmDialogComponent } from './delete-confirm-dialog/delete-confirm-dialog.component';
import {
  fakeApplication,
  fakeBackendToBackendApplicationType,
  fakeBrowserApplicationType,
  fakeNativeApplicationType,
  fakeSimpleApplicationType,
  fakeWebApplicationType,
} from '../../../../entities/application/application.fixture';
import { fakeUserApplicationPermissions } from '../../../../entities/permission/permission.fixtures';
import { ConfigService } from '../../../../services/config.service';
import { AppTestingModule, TESTING_BASE_URL } from '../../../../testing/app-testing.module';

describe('ApplicationTabSettingsComponent', () => {
  let component: ApplicationTabSettingsComponent;
  let fixture: ComponentFixture<ApplicationTabSettingsComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;
  let updateHarness: ApplicationTabSettingsEditHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApplicationTabSettingsComponent, DeleteConfirmDialogComponent, HttpClientModule, NoopAnimationsModule, AppTestingModule],
      providers: [
        {
          provide: ConfigService,
          useValue: {
            baseURL: TESTING_BASE_URL,
          },
        },
      ],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ApplicationTabSettingsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    loader = TestbedHarnessEnvironment.loader(fixture);

    component = fixture.componentInstance;
    component.userApplicationPermissions = fakeUserApplicationPermissions({
      DEFINITION: ['U'],
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('Display a simple application', () => {
    const simpleApplication = fakeApplication({
      name: 'Simple application',
      description: 'Simple description',
      picture: 'data:image/png;base64,xxxxxxxx',
      applicationType: 'SIMPLE',
      settings: {
        app: {
          type: 'Custom Application Type',
          client_id: 'Custom Client ID',
        },
      },
    });

    beforeEach(async () => {
      component.application = simpleApplication;
      component.applicationTypeConfiguration = fakeSimpleApplicationType();
      fixture.detectChanges();
      updateHarness = await loader.getHarness(ApplicationTabSettingsEditHarness);
    });

    it('Should display name, description, picture, client ID & application type fields', async () => {
      expect(await updateHarness.getName()).toEqual('Simple application');
      expect(await updateHarness.getDescription()).toEqual('Simple description');
      expect(await updateHarness.getDisplayedPictureSource()).toEqual('data:image/png;base64,xxxxxxxx');
      expect(await updateHarness.getIntegrationTitle()).toEqual('OAuth2 Integration');
      expect(await updateHarness.getSimpleClientId()).toEqual('Custom Client ID');
      expect(await updateHarness.getSimpleType()).toEqual('Custom Application Type');
      expect(await updateHarness.getType()).toBeUndefined();
      expect(await updateHarness.getTypeDescription()).toBeUndefined();
      expect(await updateHarness.getClientId()).toBeUndefined();
      expect(await updateHarness.getHiddenClientSecret()).toBeUndefined();
      expect(await updateHarness.getClearClientSecret()).toBeUndefined();
      expect(await updateHarness.getRedirectUris()).toBeUndefined();
      expect(await updateHarness.getGrantTypes()).toBeUndefined();
    });

    it('Should be able to update name, description, client ID & application type fields', async () => {
      await updateHarness.changeName('New simple application');
      await updateHarness.changeDescription('New simple description');
      await updateHarness.changeSimpleClientId('New custom client ID');
      await updateHarness.changeSimpleType('');

      expect(await updateHarness.isSaveButtonDisabled()).toBeFalsy();
      await updateHarness.saveApplication();

      const updatedApplication = {
        ...simpleApplication,
        name: 'New simple application',
        description: 'New simple description',
        settings: { app: { client_id: 'New custom client ID', type: '' } },
      };

      const req = httpTestingController.expectOne({ url: `${TESTING_BASE_URL}/applications/${component.application.id}`, method: 'PUT' });
      expect(req.request.body).toEqual(updatedApplication);
      req.flush(updatedApplication);
    });
  });

  describe('Display Backend to backend application', () => {
    const b2bApplication = fakeApplication({
      name: 'B2b application',
      description: 'B2b description',
      picture: 'data:image/png;base64,xxxxxxxx',
      applicationType: 'BACKEND_TO_BACKEND',
      settings: {
        oauth: {
          client_id: 'my client id',
          client_secret: 'my client secret',
          redirect_uris: [],
          response_types: [],
          grant_types: ['client_credentials'],
          renew_client_secret_supported: false,
        },
        app: undefined,
      },
    });

    beforeEach(async () => {
      component.application = b2bApplication;
      component.applicationTypeConfiguration = fakeBackendToBackendApplicationType();
      fixture.detectChanges();
      updateHarness = await loader.getHarness(ApplicationTabSettingsEditHarness);
    });

    it('Should display name, description, picture, client ID, client secret & grant types fields', async () => {
      expect(await updateHarness.getName()).toEqual('B2b application');
      expect(await updateHarness.getDescription()).toEqual('B2b description');
      expect(await updateHarness.getDisplayedPictureSource()).toEqual('data:image/png;base64,xxxxxxxx');
      expect(await updateHarness.getSimpleClientId()).toBeUndefined();
      expect(await updateHarness.getSimpleType()).toBeUndefined();
      expect(await updateHarness.getIntegrationTitle()).toEqual('OpenID Connect Integration');
      expect(await updateHarness.getType()).toEqual('Backend to backend');
      expect(await updateHarness.getTypeDescription()).toEqual('Machine to machine');
      expect(await updateHarness.getClientId()).toEqual('my client id');
      expect(await updateHarness.getHiddenClientSecret()).toEqual('****************');
      expect(await updateHarness.getClearClientSecret()).toEqual('my client secret');
      expect(await updateHarness.getRedirectUris()).toBeUndefined();
      expect(await updateHarness.getGrantTypes()).toEqual(['Client Credentials - (Mandatory)']);
    });

    it('Should be able to update name & description fields', async () => {
      await updateHarness.changeName('New b2b application');
      await updateHarness.changeDescription('New b2b description');

      expect(await updateHarness.isSaveButtonDisabled()).toBeFalsy();
      await updateHarness.saveApplication();

      const updatedApplication = {
        ...b2bApplication,
        name: 'New b2b application',
        description: 'New b2b description',
      };

      const req = httpTestingController.expectOne({ url: `${TESTING_BASE_URL}/applications/${component.application.id}`, method: 'PUT' });
      expect(req.request.body).toEqual(updatedApplication);
      req.flush(updatedApplication);
    });
  });

  describe('Display Native Application', () => {
    const nativeApplication = fakeApplication({
      name: 'Native application',
      description: 'Native description',
      picture: 'data:image/png;base64,xxxxxxxx',
      applicationType: 'NATIVE',
      settings: {
        oauth: {
          client_id: 'my client id',
          client_secret: 'my client secret',
          redirect_uris: ['http://localhost/native'],
          response_types: ['code'],
          grant_types: ['authorization_code'],
          renew_client_secret_supported: false,
        },
        app: undefined,
      },
    });

    beforeEach(async () => {
      component.application = nativeApplication;
      component.applicationTypeConfiguration = fakeNativeApplicationType();
      fixture.detectChanges();
      updateHarness = await loader.getHarness(ApplicationTabSettingsEditHarness);
    });

    it('Should display name, description, picture, client ID, client secret, redirect URIs & grant types fields', async () => {
      expect(await updateHarness.getName()).toEqual('Native application');
      expect(await updateHarness.getDescription()).toEqual('Native description');
      expect(await updateHarness.getDisplayedPictureSource()).toEqual('data:image/png;base64,xxxxxxxx');
      expect(await updateHarness.getSimpleClientId()).toBeUndefined();
      expect(await updateHarness.getSimpleType()).toBeUndefined();
      expect(await updateHarness.getIntegrationTitle()).toEqual('OpenID Connect Integration');
      expect(await updateHarness.getType()).toEqual('Native');
      expect(await updateHarness.getTypeDescription()).toEqual('iOS, Android, ...');
      expect(await updateHarness.getClientId()).toEqual('my client id');
      expect(await updateHarness.getHiddenClientSecret()).toEqual('****************');
      expect(await updateHarness.getClearClientSecret()).toEqual('my client secret');
      expect(await updateHarness.getRedirectUris()).toEqual(['http://localhost/native']);
      expect(await updateHarness.getGrantTypes()).toEqual(['Authorization Code - (Mandatory)']);
    });

    it('Should update name, description, redirect URIs & grant types fields', async () => {
      await updateHarness.changeName('New native application');
      await updateHarness.changeDescription('New native description');

      await updateHarness.checkGrantType(/Implicit/);
      await updateHarness.addRedirectUri('http://localhost/newNative');

      expect(await updateHarness.isSaveButtonDisabled()).toBeFalsy();
      await updateHarness.saveApplication();

      const updatedApplication = {
        ...nativeApplication,
        name: 'New native application',
        description: 'New native description',
        settings: {
          ...nativeApplication.settings,
          oauth: {
            ...nativeApplication.settings.oauth,
            grant_types: ['authorization_code', 'implicit'],
            response_types: ['code', 'token', 'id_token'],
            redirect_uris: ['http://localhost/native', 'http://localhost/newNative'],
          },
        },
      };

      const req = httpTestingController.expectOne({ url: `${TESTING_BASE_URL}/applications/${component.application.id}`, method: 'PUT' });
      expect(req.request.body).toEqual(updatedApplication);
      req.flush(updatedApplication);
    });
  });

  describe('Display Browser Application', () => {
    const browserApplication = fakeApplication({
      name: 'Browser application',
      description: 'Browser description',
      picture: 'data:image/png;base64,xxxxxxxx',
      applicationType: 'BROWSER',
      settings: {
        oauth: {
          client_id: 'my client id',
          client_secret: 'my client secret',
          redirect_uris: ['http://localhost/browser'],
          response_types: ['code', 'token', 'id_token'],
          grant_types: ['authorization_code', 'implicit'],
          renew_client_secret_supported: false,
        },
        app: undefined,
      },
    });

    beforeEach(async () => {
      component.application = browserApplication;
      component.applicationTypeConfiguration = fakeBrowserApplicationType();
      fixture.detectChanges();
      updateHarness = await loader.getHarness(ApplicationTabSettingsEditHarness);
    });

    it('Should display name, description, picture, client ID, client secret, redirect URIs & grant types fields', async () => {
      expect(await updateHarness.getName()).toEqual('Browser application');
      expect(await updateHarness.getDescription()).toEqual('Browser description');
      expect(await updateHarness.getDisplayedPictureSource()).toEqual('data:image/png;base64,xxxxxxxx');
      expect(await updateHarness.getSimpleClientId()).toBeUndefined();
      expect(await updateHarness.getSimpleType()).toBeUndefined();
      expect(await updateHarness.getIntegrationTitle()).toEqual('OpenID Connect Integration');
      expect(await updateHarness.getType()).toEqual('SPA');
      expect(await updateHarness.getTypeDescription()).toEqual('Angular, React, Ember, ...');
      expect(await updateHarness.getClientId()).toEqual('my client id');
      expect(await updateHarness.getHiddenClientSecret()).toEqual('****************');
      expect(await updateHarness.getClearClientSecret()).toEqual('my client secret');
      expect(await updateHarness.getRedirectUris()).toEqual(['http://localhost/browser']);
      expect(await updateHarness.getGrantTypes()).toEqual(['Authorization Code', 'Implicit']);
    });

    it('Should update name, description, redirect URIs & grant types fields', async () => {
      await updateHarness.changeName('New browser application');
      await updateHarness.changeDescription('New browser description');

      await updateHarness.checkGrantType(/Implicit/);
      await updateHarness.addRedirectUri('http://localhost/newBrowser');

      expect(await updateHarness.isSaveButtonDisabled()).toBeFalsy();
      await updateHarness.saveApplication();

      const updatedApplication = {
        ...browserApplication,
        name: 'New browser application',
        description: 'New browser description',
        settings: {
          ...browserApplication.settings,
          oauth: {
            ...browserApplication.settings.oauth,
            grant_types: ['authorization_code'],
            response_types: ['code'],
            redirect_uris: ['http://localhost/browser', 'http://localhost/newBrowser'],
          },
        },
      };

      const req = httpTestingController.expectOne({ url: `${TESTING_BASE_URL}/applications/${component.application.id}`, method: 'PUT' });
      expect(req.request.body).toEqual(updatedApplication);
      req.flush(updatedApplication);
    });
  });

  describe('Display Web Application', () => {
    const webApplication = fakeApplication({
      name: 'Web application',
      description: 'Web description',
      picture: 'data:image/png;base64,xxxxxxxx',
      applicationType: 'WEB',
      settings: {
        oauth: {
          client_id: 'my client id',
          client_secret: 'my client secret',
          redirect_uris: ['http://localhost/web'],
          response_types: ['code'],
          grant_types: ['authorization_code'],
          renew_client_secret_supported: false,
        },
        app: undefined,
      },
    });

    beforeEach(async () => {
      component.application = webApplication;
      component.applicationTypeConfiguration = fakeWebApplicationType();
      fixture.detectChanges();
      updateHarness = await loader.getHarness(ApplicationTabSettingsEditHarness);
    });

    it('Should display name, description, picture, client ID, client secret, redirect URIs & grant types fields', async () => {
      expect(await updateHarness.getName()).toEqual('Web application');
      expect(await updateHarness.getDescription()).toEqual('Web description');
      expect(await updateHarness.getDisplayedPictureSource()).toEqual('data:image/png;base64,xxxxxxxx');
      expect(await updateHarness.getSimpleClientId()).toBeUndefined();
      expect(await updateHarness.getSimpleType()).toBeUndefined();
      expect(await updateHarness.getIntegrationTitle()).toEqual('OpenID Connect Integration');
      expect(await updateHarness.getType()).toEqual('Web');
      expect(await updateHarness.getTypeDescription()).toEqual('Java, .Net, ...');
      expect(await updateHarness.getClientId()).toEqual('my client id');
      expect(await updateHarness.getHiddenClientSecret()).toEqual('****************');
      expect(await updateHarness.getClearClientSecret()).toEqual('my client secret');
      expect(await updateHarness.getRedirectUris()).toEqual(['http://localhost/web']);
      expect(await updateHarness.getGrantTypes()).toEqual(['Authorization Code - (Mandatory)']);
    });

    it('Should update name, description, picture, redirect URIs & grant types fields', async () => {
      await updateHarness.changeName('New web application');
      await updateHarness.changeDescription('New web description');

      await updateHarness.checkGrantType(/Refresh/);
      await updateHarness.removeRedirectUri('http://localhost/web');
      await updateHarness.addRedirectUri('http://localhost/newWeb');

      expect(await updateHarness.isSaveButtonDisabled()).toBeFalsy();
      await updateHarness.saveApplication();

      const updatedApplication = {
        ...webApplication,
        name: 'New web application',
        description: 'New web description',
        settings: {
          ...webApplication.settings,
          oauth: {
            ...webApplication.settings.oauth,
            grant_types: ['authorization_code', 'refresh_token'],
            response_types: ['code'],
            redirect_uris: ['http://localhost/newWeb'],
          },
        },
      };

      const req = httpTestingController.expectOne({ url: `${TESTING_BASE_URL}/applications/${component.application.id}`, method: 'PUT' });
      expect(req.request.body).toEqual(updatedApplication);
      req.flush(updatedApplication);
    });
  });

  describe('Other commands', () => {
    beforeEach(async () => {
      component.application = fakeApplication({
        name: 'Native application',
        description: 'Native description',
        picture: 'data:image/png;base64,xxxxxxxx',
        applicationType: 'NATIVE',
        settings: {
          oauth: {
            client_id: 'my client id',
            client_secret: 'my client secret',
            redirect_uris: ['http://localhost/native'],
            response_types: ['code'],
            grant_types: ['authorization_code'],
            renew_client_secret_supported: false,
          },
          app: undefined,
        },
      });
      component.applicationTypeConfiguration = fakeNativeApplicationType();
      fixture.detectChanges();
      updateHarness = await loader.getHarness(ApplicationTabSettingsEditHarness);
    });

    it('Should be able to discard changes', async () => {
      expect(await updateHarness.isDiscardButtonDisabled()).toBeTruthy();

      await updateHarness.changeName('New Value');
      expect(await updateHarness.isDiscardButtonDisabled()).toBeFalsy();

      await updateHarness.discardChanges();
      expect(await updateHarness.getName()).toEqual('Native application');
      expect(await updateHarness.isDiscardButtonDisabled()).toBeTruthy();
    });

    it('Should update a picture', async () => {
      expect(await updateHarness.getDisplayedPictureSource()).toEqual('data:image/png;base64,xxxxxxxx');
      expect(await updateHarness.isSaveButtonDisabled()).toBeTruthy();
      await updateHarness.changePicture('newImage.png');

      expect(await updateHarness.isSaveButtonDisabled()).toBeFalsy();
      expect(await updateHarness.getDisplayedPictureSource()).toEqual('data:image/png;base64,bmV3SW1hZ2UucG5n');
    });

    it('Should delete a picture', async () => {
      expect(await updateHarness.isDeletePictureButtonDisabled()).toBeFalsy();
      expect(await updateHarness.getDisplayedPictureSource()).toEqual('data:image/png;base64,xxxxxxxx');

      await updateHarness.deletePicture();
      expect(await updateHarness.isDeletePictureButtonDisabled()).toBeTruthy();
      expect(await updateHarness.getDisplayedPictureSource()).toEqual('');
    });
  });
});
