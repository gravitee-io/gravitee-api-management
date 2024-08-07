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
import { InteractivityChecker } from '@angular/cdk/a11y';
import { HarnessLoader, TestKey } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpClientModule } from '@angular/common/http';
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatChipGridHarness } from '@angular/material/chips/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';

import { ApplicationTabSettingsComponent } from './application-tab-settings.component';
import { DeleteConfirmDialogComponent } from './delete-confirm-dialog/delete-confirm-dialog.component';
import { DeleteConfirmDialogHarness } from './delete-confirm-dialog/delete-confirm-dialog.harness';
import { CopyCodeHarness } from '../../../../components/copy-code/copy-code.harness';
import { PictureHarness } from '../../../../components/picture/picture.harness';
import {
  fakeApplication,
  fakeBackendToBackendApplicationType,
  fakeBrowserApplicationType,
  fakeNativeApplicationType,
  fakeSimpleApplicationType,
  fakeWebApplicationType,
} from '../../../../entities/application/application.fixture';
import { ConfigService } from '../../../../services/config.service';
import { AppTestingModule, TESTING_BASE_URL } from '../../../../testing/app-testing.module';

describe('ApplicationTabSettingsComponent', () => {
  let component: ApplicationTabSettingsComponent;
  let fixture: ComponentFixture<ApplicationTabSettingsComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;

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
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApplicationTabSettingsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);

    component = fixture.componentInstance;
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
        oauth: undefined,
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
    });

    it('Should display name, description, picture, client ID & application type fields', async () => {
      const name = await getNameInput();
      const description = await getDescriptionInput();
      const pictureComponent = await getPictureComponent();
      const clientId = await getAppClientIdInput();
      const type = await getAppTypeInput();
      const oauthClientId = await getOAuthClientIdCopyCode();
      const oauthClientSecret = await getOAuthClientSecretCopyCode();
      const oauthGrantTypes = await getOAuthGrantTypesSelect();
      const oauthRedirectUris = await getOAuthRedirectURIsChips();

      expect(await name.getValue()).toEqual('Simple application');
      expect(await description.getValue()).toEqual('Simple description');
      expect(await pictureComponent.getSource()).toEqual('data:image/png;base64,xxxxxxxx');
      expect(await clientId!.getValue()).toEqual('Custom Client ID');
      expect(await type!.getValue()).toEqual('Custom Application Type');
      expect(oauthClientId).toBeNull();
      expect(oauthClientSecret).toBeNull();
      expect(oauthGrantTypes).toBeNull();
      expect(oauthRedirectUris).toBeNull();
    });

    it('Should ne able to update name, description, picture, client ID & application type fields', async () => {
      const name = await getNameInput();
      const description = await getDescriptionInput();
      const pictureComponent = await getPictureComponent();
      const clientId = await getAppClientIdInput();
      const type = await getAppTypeInput();

      await name.setValue('New simple application');
      await description.setValue('New simple description');
      await clientId!.setValue('New custom client ID');
      await type!.setValue('');

      changePicture('New image');
      expect(await pictureComponent.getSource()).toEqual('New image');

      const saveButton = await getSaveButton();
      expect(await saveButton.isDisabled()).toBeFalsy();
      await saveButton.click();

      const updatedApplication = {
        ...simpleApplication,
        name: 'New simple application',
        description: 'New simple description',
        picture: 'New image',
        settings: { app: { client_id: 'New custom client ID', type: '' } },
      };

      const req = httpTestingController.expectOne({ url: `${TESTING_BASE_URL}/applications/${component.application.id}`, method: 'PUT' });
      expect(req.request.body).toEqual(updatedApplication);
      req.flush(updatedApplication);
    });
  });

  describe('Display Backend to backend Application', () => {
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
    });

    it('Should display name, description, picture, client ID, client secret & grant types fields', async () => {
      const name = await getNameInput();
      const description = await getDescriptionInput();
      const pictureComponent = await getPictureComponent();
      const clientId = await getAppClientIdInput();
      const type = await getAppTypeInput();
      const oauthClientId = await getOAuthClientIdCopyCode();
      const oauthClientSecret = await getOAuthClientSecretCopyCode();
      const oauthGrantTypes = await getOAuthGrantTypesSelect();
      const oauthRedirectUris = await getOAuthRedirectURIsChips();

      expect(await name.getValue()).toEqual('B2b application');
      expect(await description.getValue()).toEqual('B2b description');
      expect(await pictureComponent.getSource()).toEqual('data:image/png;base64,xxxxxxxx');
      expect(clientId).toBeNull();
      expect(type).toBeNull();
      expect(await oauthClientId!.getText()).toEqual('my client id');
      expect(await oauthClientSecret!.getText()).toEqual('****************');
      expect(await oauthGrantTypes!.getValueText()).toEqual('Client Credentials - (Mandatory)');
      expect(oauthRedirectUris).toBeNull();
    });

    it('Should update name, description & picture fields', async () => {
      const name = await getNameInput();
      const description = await getDescriptionInput();
      const pictureComponent = await getPictureComponent();

      await name.setValue('New b2b application');
      await description.setValue('New b2b description');
      changePicture('New image');
      expect(await pictureComponent.getSource()).toEqual('New image');

      const saveButton = await getSaveButton();
      expect(await saveButton.isDisabled()).toBeFalsy();
      await saveButton.click();

      const updatedApplication = {
        ...b2bApplication,
        name: 'New b2b application',
        description: 'New b2b description',
        picture: 'New image',
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
    });

    it('Should display name, description, picture, client ID, client secret, redirect URIs & grant types fields', async () => {
      const name = await getNameInput();
      const description = await getDescriptionInput();
      const pictureComponent = await getPictureComponent();
      const clientId = await getAppClientIdInput();
      const type = await getAppTypeInput();
      const oauthClientId = await getOAuthClientIdCopyCode();
      const oauthClientSecret = await getOAuthClientSecretCopyCode();
      const oauthGrantTypes = await getOAuthGrantTypesSelect();
      const oauthRedirectUris = await getOAuthRedirectURIsChips();

      expect(await name.getValue()).toEqual('Native application');
      expect(await description.getValue()).toEqual('Native description');
      expect(await pictureComponent.getSource()).toEqual('data:image/png;base64,xxxxxxxx');
      expect(clientId).toBeNull();
      expect(type).toBeNull();
      expect(await oauthClientId!.getText()).toEqual('my client id');
      expect(await oauthClientSecret!.getText()).toEqual('****************');
      expect(await oauthGrantTypes!.getValueText()).toEqual('Authorization Code - (Mandatory)');
      const redirectUrisRows = await oauthRedirectUris!.getRows();
      expect(await redirectUrisRows![0].getText()).toEqual('http://localhost/native');
    });

    it('Should update name, description, picture, redirect URIs & grant types fields', async () => {
      const name = await getNameInput();
      const description = await getDescriptionInput();
      const pictureComponent = await getPictureComponent();
      const oauthGrantTypes = await getOAuthGrantTypesSelect();
      const oauthRedirectUris = await getOAuthRedirectURIsChips();

      await name.setValue('New native application');
      await description.setValue('New native description');

      changePicture('New image');
      expect(await pictureComponent.getSource()).toEqual('New image');

      await oauthGrantTypes!.clickOptions({ text: /Implicit/ });
      const redirectUriInput = await oauthRedirectUris!.getInput();
      await redirectUriInput!.setValue('http://localhost/newNative');
      await redirectUriInput!.sendSeparatorKey(TestKey.ENTER);

      const saveButton = await getSaveButton();
      expect(await saveButton.isDisabled()).toBeFalsy();
      await saveButton.click();

      const updatedApplication = {
        ...nativeApplication,
        name: 'New native application',
        description: 'New native description',
        picture: 'New image',
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
    });

    it('Should display name, description, picture, client ID, client secret, redirect URIs & grant types fields', async () => {
      const name = await getNameInput();
      const description = await getDescriptionInput();
      const pictureComponent = await getPictureComponent();
      const clientId = await getAppClientIdInput();
      const type = await getAppTypeInput();
      const oauthClientId = await getOAuthClientIdCopyCode();
      const oauthClientSecret = await getOAuthClientSecretCopyCode();
      const oauthGrantTypes = await getOAuthGrantTypesSelect();
      const oauthRedirectUris = await getOAuthRedirectURIsChips();

      expect(await name.getValue()).toEqual('Browser application');
      expect(await description.getValue()).toEqual('Browser description');
      expect(await pictureComponent.getSource()).toEqual('data:image/png;base64,xxxxxxxx');
      expect(clientId).toBeNull();
      expect(type).toBeNull();
      expect(await oauthClientId?.getText()).toEqual('my client id');
      expect(await oauthClientSecret?.getText()).toEqual('****************');
      expect(await oauthGrantTypes?.getValueText()).toEqual('Authorization Code, Implicit');
      const redirectUrisRows = await oauthRedirectUris?.getRows();
      expect(redirectUrisRows).not.toBeNull();
      expect(await redirectUrisRows![0].getText()).toEqual('http://localhost/browser');
    });

    it('Should update name, description, picture, redirect URIs & grant types fields', async () => {
      const name = await getNameInput();
      const description = await getDescriptionInput();
      const pictureComponent = await getPictureComponent();
      const oauthGrantTypes = await getOAuthGrantTypesSelect();
      const oauthRedirectUris = await getOAuthRedirectURIsChips();

      await name.setValue('New browser application');
      await description.setValue('New browser description');

      changePicture('New image');
      expect(await pictureComponent.getSource()).toEqual('New image');

      await oauthGrantTypes!.clickOptions({ text: /Implicit/ });
      const redirectUriInput = await oauthRedirectUris!.getInput();
      await redirectUriInput!.setValue('http://localhost/newBrowser');
      await redirectUriInput!.sendSeparatorKey(TestKey.ENTER);

      const saveButton = await getSaveButton();
      expect(await saveButton.isDisabled()).toBeFalsy();
      await saveButton.click();

      const updatedApplication = {
        ...browserApplication,
        name: 'New browser application',
        description: 'New browser description',
        picture: 'New image',
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
    });

    it('Should display name, description, picture, client ID, client secret, redirect URIs & grant types fields', async () => {
      const name = await getNameInput();
      const description = await getDescriptionInput();
      const pictureComponent = await getPictureComponent();
      const clientId = await getAppClientIdInput();
      const type = await getAppTypeInput();
      const oauthClientId = await getOAuthClientIdCopyCode();
      const oauthClientSecret = await getOAuthClientSecretCopyCode();
      const oauthGrantTypes = await getOAuthGrantTypesSelect();
      const oauthRedirectUris = await getOAuthRedirectURIsChips();

      expect(await name.getValue()).toEqual('Web application');
      expect(await description.getValue()).toEqual('Web description');
      expect(await pictureComponent.getSource()).toEqual('data:image/png;base64,xxxxxxxx');
      expect(clientId).toBeNull();
      expect(type).toBeNull();
      expect(await oauthClientId?.getText()).toEqual('my client id');
      expect(await oauthClientSecret?.getText()).toEqual('****************');
      expect(await oauthGrantTypes?.getValueText()).toEqual('Authorization Code - (Mandatory)');
      const redirectUrisRows = await oauthRedirectUris?.getRows();
      expect(redirectUrisRows).not.toBeNull();
      expect(await redirectUrisRows![0].getText()).toEqual('http://localhost/web');
    });

    it('Should update name, description, picture, redirect URIs & grant types fields', async () => {
      const name = await getNameInput();
      const description = await getDescriptionInput();
      const pictureComponent = await getPictureComponent();
      const oauthGrantTypes = await getOAuthGrantTypesSelect();
      const oauthRedirectUris = await getOAuthRedirectURIsChips();

      await name.setValue('New web application');
      await description.setValue('New web description');

      changePicture('New image');
      expect(await pictureComponent.getSource()).toEqual('New image');

      await oauthGrantTypes!.clickOptions({ text: /Refresh/ });
      const redirectUrisRows = await oauthRedirectUris!.getRows();
      await redirectUrisRows[0].remove();
      const redirectUriInput = await oauthRedirectUris!.getInput();
      await redirectUriInput!.setValue('http://localhost/newWeb');
      await redirectUriInput!.sendSeparatorKey(TestKey.ENTER);

      const saveButton = await getSaveButton();
      expect(await saveButton.isDisabled()).toBeFalsy();
      await saveButton.click();

      const updatedApplication = {
        ...webApplication,
        name: 'New web application',
        description: 'New web description',
        picture: 'New image',
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
    });

    it('Should display client secret', async () => {
      const oauthClientSecret = await getOAuthClientSecretCopyCode();
      expect(await oauthClientSecret?.getText()).toEqual('****************');
      await oauthClientSecret?.changePasswordVisibility();
      expect(await oauthClientSecret?.getText()).toEqual('my client secret');
    });

    it('Should be able to discard changes', async () => {
      const cancelButton = await getCancelButton();
      expect(await cancelButton.isDisabled()).toBeTruthy();

      const nameInput = await getNameInput();
      await nameInput.setValue('New Value');
      expect(await cancelButton.isDisabled()).toBeFalsy();

      await cancelButton.click();
      expect(await nameInput.getValue()).toEqual('Native application');
      expect(await cancelButton.isDisabled()).toBeTruthy();
    });

    it('Should delete a picture', async () => {
      const deletePictureButton = await getDeletePictureButton();
      expect(await deletePictureButton.isDisabled()).toBeFalsy();

      const pictureComponent = await getPictureComponent();
      expect(await pictureComponent.getSource()).toEqual('data:image/png;base64,xxxxxxxx');

      await deletePictureButton.click();
      expect(await pictureComponent.getSource()).toEqual('');
      expect(await deletePictureButton.isDisabled()).toBeTruthy();
    });

    it('Should delete the application', async () => {
      const router: Router = TestBed.inject(Router);

      jest.spyOn(router, 'navigate');

      const deleteButton = await getDeleteButton();
      expect(await deleteButton.isDisabled()).toBeFalsy();
      await deleteButton.click();

      let confirmDialog = await getDeleteConfirmDialog();
      expect(confirmDialog).not.toBeNull();
      await confirmDialog!.cancel();

      confirmDialog = await getDeleteConfirmDialog();
      expect(confirmDialog).toBeNull();
      httpTestingController.expectNone({ url: `${TESTING_BASE_URL}/applications/${component.application.id}`, method: 'DELETE' });
      expect(router.navigate).not.toHaveBeenCalled();

      await deleteButton.click();
      confirmDialog = await getDeleteConfirmDialog();
      expect(confirmDialog).not.toBeNull();
      await confirmDialog!.confirm();

      confirmDialog = await getDeleteConfirmDialog();
      expect(confirmDialog).toBeNull();
      httpTestingController
        .expectOne({ url: `${TESTING_BASE_URL}/applications/${component.application.id}`, method: 'DELETE' })
        .flush(null);
      expect(router.navigate).toHaveBeenCalledWith(['/applications']);
    });
  });

  async function getNameInput(): Promise<MatInputHarness> {
    return await loader.getHarness(MatInputHarness.with({ selector: '[aria-label="Application name"]' }));
  }

  async function getDescriptionInput(): Promise<MatInputHarness> {
    return await loader.getHarness(MatInputHarness.with({ selector: '[aria-label="Application description"]' }));
  }

  function changePicture(fileName: string) {
    component.applicationSettingsForm.controls.picture.setValue(fileName);
  }

  async function getPictureComponent(): Promise<PictureHarness> {
    return await loader.getHarness(PictureHarness);
  }

  async function getDeletePictureButton(): Promise<MatButtonHarness> {
    return await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Delete picture"]' }));
  }

  async function getOAuthClientIdCopyCode() {
    return getCopyCodeHarnessOrNull('Client ID');
  }

  async function getOAuthClientSecretCopyCode() {
    return getCopyCodeHarnessOrNull('Client Secret');
  }

  async function getOAuthRedirectURIsChips(): Promise<MatChipGridHarness | null> {
    return await loader.getHarnessOrNull(MatChipGridHarness.with({ selector: '[aria-label="Redirect URIs"]' }));
  }

  async function getOAuthGrantTypesSelect(): Promise<MatSelectHarness | null> {
    return await loader.getHarnessOrNull(MatSelectHarness.with({ selector: '[aria-label="Grant types"]' }));
  }

  async function getCopyCodeHarnessOrNull(title: string): Promise<CopyCodeHarness | null> {
    return await loader.getHarnessOrNull(CopyCodeHarness.with({ selector: `[title="${title}"]` }));
  }

  async function getAppTypeInput(): Promise<MatInputHarness | null> {
    return await loader.getHarnessOrNull(MatInputHarness.with({ selector: '[aria-label="Application type"]' }));
  }

  async function getAppClientIdInput(): Promise<MatInputHarness | null> {
    return await loader.getHarnessOrNull(MatInputHarness.with({ selector: '[aria-label="Client ID"]' }));
  }

  async function getSaveButton(): Promise<MatButtonHarness> {
    return await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Save application"]' }));
  }

  async function getCancelButton(): Promise<MatButtonHarness> {
    return await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Discard changes"]' }));
  }

  async function getDeleteButton(): Promise<MatButtonHarness> {
    return await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Delete application"]' }));
  }

  async function getDeleteConfirmDialog(): Promise<DeleteConfirmDialogHarness | null> {
    return await rootLoader.getHarnessOrNull(DeleteConfirmDialogHarness);
  }
});
