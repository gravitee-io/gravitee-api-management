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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatRadioGroupHarness } from '@angular/material/radio/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { omit } from 'lodash';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatCardHarness } from '@angular/material/card/testing';
import { MatTableHarness } from '@angular/material/table/testing';
import {
  GioFormTagsInputHarness,
  GioSaveBarHarness,
  GioLicenseTestingModule,
  GioFormSelectionInlineHarness,
} from '@gravitee/ui-particles-angular';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';

import { OrgSettingsIdentityProviderComponent } from './org-settings-identity-provider.component';

import { OrganizationSettingsModule } from '../organization-settings.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { GioFormColorInputHarness } from '../../../shared/components/gio-form-color-input/gio-form-color-input.harness';
import { NewIdentityProvider } from '../../../entities/identity-provider/newIdentityProvider';
import { fakeIdentityProvider, IdentityProvider } from '../../../entities/identity-provider';
import { Group } from '../../../entities/group/group';
import { fakeGroup } from '../../../entities/group/group.fixture';
import { Role } from '../../../entities/role/role';
import { Environment } from '../../../entities/environment/environment';
import { fakeRole } from '../../../entities/role/role.fixture';
import { fakeEnvironment } from '../../../entities/environment/environment.fixture';

describe('OrgSettingsIdentityProviderComponent', () => {
  let fixture: ComponentFixture<OrgSettingsIdentityProviderComponent>;
  let loader: HarnessLoader;
  let component: OrgSettingsIdentityProviderComponent;
  let httpTestingController: HttpTestingController;
  let spyRouterNavigate: jest.SpyInstance;

  afterEach(() => {
    httpTestingController.verify();
    jest.resetAllMocks();
  });

  describe('new', () => {
    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [NoopAnimationsModule, GioTestingModule, OrganizationSettingsModule, MatIconTestingModule, GioLicenseTestingModule],
      });

      fixture = TestBed.createComponent(OrgSettingsIdentityProviderComponent);
      loader = TestbedHarnessEnvironment.loader(fixture);
      component = fixture.componentInstance;

      httpTestingController = TestBed.inject(HttpTestingController);

      const router = TestBed.inject(Router);
      spyRouterNavigate = jest.spyOn(router, 'navigate');

      fixture.detectChanges();
    });

    it('should be in new mode', async () => {
      expect(component.mode).toEqual('new');
    });

    it('should change provider type', async () => {
      const formCardGroup = await loader.getHarness(GioFormSelectionInlineHarness.with({ selector: '[formControlName=type]' }));

      expect(await formCardGroup.getSelectedValue()).toEqual('GRAVITEEIO_AM');

      await formCardGroup.select('GITHUB');

      expect(await formCardGroup.getSelectedValue()).toEqual('GITHUB');
      expect(Object.keys(component.identityProviderFormGroup.get('configuration').value)).toEqual(['clientId', 'clientSecret']);
    });

    it('should save identity provider general settings', async () => {
      const saveBar = await loader.getHarness(GioSaveBarHarness);

      // Set value for all General fields
      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=name]' }));
      await nameInput.setValue('Name');

      const descriptionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=description]' }));
      await descriptionInput.setValue('Description');

      const allowPortalToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName=enabled]' }));
      await allowPortalToggle.toggle();

      const emailRequiredToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName=emailRequired]' }));
      await emailRequiredToggle.toggle();

      const syncMappingsRadioGroupe = await loader.getHarness(MatRadioGroupHarness.with({ selector: '[formControlName=syncMappings]' }));
      await syncMappingsRadioGroupe.checkRadioButton({ label: /^Computed during each user/ });

      expect(await saveBar.isSubmitButtonInvalid()).toEqual(true);

      // Set value for required GRAVITEEIO_AM fields
      const clientIdInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=clientId]' }));
      await clientIdInput.setValue('Client Id');

      const clientSecretInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=clientSecret]' }));
      await clientSecretInput.setValue('Client Secret');

      const serverURLInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=serverURL]' }));
      await serverURLInput.setValue('ServerURL');

      const domainInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=domain]' }));
      await domainInput.setValue('Domain');

      const idInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=id]' }));
      await idInput.setValue('Id');

      expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);

      await saveBar.clickSubmit();

      expectIdentityProviderCreateRequest({
        description: 'Description',
        emailRequired: false,
        enabled: false,
        name: 'Name',
        syncMappings: true,
        type: 'GRAVITEEIO_AM',
        configuration: {
          clientId: 'Client Id',
          clientSecret: 'Client Secret',
          color: null,
          domain: 'Domain',
          scopes: null,
          serverURL: 'ServerURL',
        },
        userProfileMapping: {
          email: 'email',
          firstname: 'given_name',
          id: 'Id',
          lastname: 'family_name',
          picture: 'picture',
        },
      });

      expect(spyRouterNavigate).toHaveBeenCalledWith(['../', 'google-idp'], expect.anything());
    });

    describe('github', () => {
      it('should save identity provider github configuration ', async () => {
        const formCardGroup = await loader.getHarness(GioFormSelectionInlineHarness.with({ selector: '[formControlName=type]' }));

        await formCardGroup.select('GITHUB');

        const clientIdInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=clientId]' }));
        await clientIdInput.setValue('Client Id');

        const clientSecretInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=clientSecret]' }));
        await clientSecretInput.setValue('Client Secret');

        expect(fixture.componentInstance.identityProviderFormGroup.get('configuration').value).toEqual({
          clientId: 'Client Id',
          clientSecret: 'Client Secret',
        });

        // Set value for required general field
        const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=name]' }));
        await nameInput.setValue('Name');

        const saveBar = await loader.getHarness(GioSaveBarHarness);
        await saveBar.clickSubmit();

        expectIdentityProviderCreateRequest({
          description: null,
          emailRequired: true,
          enabled: true,
          name: 'Name',
          syncMappings: false,
          type: 'GITHUB',
          configuration: {
            clientId: 'Client Id',
            clientSecret: 'Client Secret',
          },
        });
      });
    });

    describe('google', () => {
      it('should save identity provider google configuration ', async () => {
        const formCardGroup = await loader.getHarness(GioFormSelectionInlineHarness.with({ selector: '[formControlName=type]' }));

        await formCardGroup.select('GOOGLE');

        const clientIdInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=clientId]' }));
        await clientIdInput.setValue('Client Id');

        const clientSecretInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=clientSecret]' }));
        await clientSecretInput.setValue('Client Secret');

        expect(fixture.componentInstance.identityProviderFormGroup.get('configuration').value).toEqual({
          clientId: 'Client Id',
          clientSecret: 'Client Secret',
        });

        // Set value for required general field
        const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=name]' }));
        await nameInput.setValue('Name');

        const saveBar = await loader.getHarness(GioSaveBarHarness);
        await saveBar.clickSubmit();

        expectIdentityProviderCreateRequest({
          description: null,
          emailRequired: true,
          enabled: true,
          name: 'Name',
          syncMappings: false,
          type: 'GOOGLE',
          configuration: {
            clientId: 'Client Id',
            clientSecret: 'Client Secret',
          },
        });
      });
    });

    describe('gravitee am', () => {
      it('should save identity provider gravitee am configuration ', async () => {
        const clientIdInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=clientId]' }));
        await clientIdInput.setValue('Client Id');

        const clientSecretInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=clientSecret]' }));
        await clientSecretInput.setValue('Client Secret');

        const serverURLInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=serverURL]' }));
        await serverURLInput.setValue('ServerURL');

        const domainInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=domain]' }));
        await domainInput.setValue('Domain');

        const scopesInput = await loader.getHarness(GioFormTagsInputHarness.with({ selector: '[formControlName=scopes]' }));
        await scopesInput.addTag('Scope A');
        await scopesInput.addTag('Scope B');

        const colorInput = await loader.getHarness(GioFormColorInputHarness.with({ selector: '[formControlName=color]' }));
        await colorInput.setValue('#ffffff');

        expect(fixture.componentInstance.identityProviderFormGroup.get('configuration').value).toEqual({
          clientId: 'Client Id',
          clientSecret: 'Client Secret',
          color: '#ffffff',
          domain: 'Domain',
          scopes: ['Scope A', 'Scope B'],
          serverURL: 'ServerURL',
        });

        const idInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=id]' }));
        await idInput.setValue('Id');

        const firstnameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=firstname]' }));
        await firstnameInput.setValue('Firstname');

        const lastnameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=lastname]' }));
        await lastnameInput.setValue('Lastname');

        const emailInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=email]' }));
        await emailInput.setValue('Email');

        const pictureInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=picture]' }));
        await pictureInput.setValue('Picture');

        expect(fixture.componentInstance.identityProviderFormGroup.get('userProfileMapping').value).toEqual({
          email: 'Email',
          firstname: 'Firstname',
          id: 'Id',
          lastname: 'Lastname',
          picture: 'Picture',
        });

        // Set value for required general field
        const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=name]' }));
        await nameInput.setValue('Name');

        const saveBar = await loader.getHarness(GioSaveBarHarness);
        await saveBar.clickSubmit();

        expectIdentityProviderCreateRequest({
          description: null,
          emailRequired: true,
          enabled: true,
          name: 'Name',
          syncMappings: false,
          type: 'GRAVITEEIO_AM',
          configuration: {
            clientId: 'Client Id',
            clientSecret: 'Client Secret',
            color: '#ffffff',
            domain: 'Domain',
            scopes: ['Scope A', 'Scope B'],
            serverURL: 'ServerURL',
          },
          userProfileMapping: {
            email: 'Email',
            firstname: 'Firstname',
            id: 'Id',
            lastname: 'Lastname',
            picture: 'Picture',
          },
        });
      });
    });

    describe('oidc', () => {
      it('should not load form without license', async () => {
        const formCardGroup = await loader.getHarness(GioFormSelectionInlineHarness.with({ selector: '[formControlName=type]' }));

        await formCardGroup.select('OIDC');

        const element = await loader.getAllChildLoaders('org-settings-identity-provider-oidc');
        expect(element.length).toEqual(0);
      });
    });
  });

  describe('new (with license)', () => {
    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [NoopAnimationsModule, GioTestingModule, OrganizationSettingsModule, GioLicenseTestingModule.with(true)],
      });

      fixture = TestBed.createComponent(OrgSettingsIdentityProviderComponent);
      loader = TestbedHarnessEnvironment.loader(fixture);
      component = fixture.componentInstance;

      httpTestingController = TestBed.inject(HttpTestingController);

      fixture.detectChanges();
    });

    describe('oidc', () => {
      it('should save identity provider oidc configuration ', async () => {
        const formCardGroup = await loader.getHarness(GioFormSelectionInlineHarness.with({ selector: '[formControlName=type]' }));

        await formCardGroup.select('OIDC');

        const oidc = await loader.getChildLoader('org-settings-identity-provider-oidc');

        expect(oidc).not.toBeNull();

        const clientIdInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=clientId]' }));
        await clientIdInput.setValue('Client Id');

        const clientSecretInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=clientSecret]' }));
        await clientSecretInput.setValue('Client Secret');

        const tokenEndpointInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=tokenEndpoint]' }));
        await tokenEndpointInput.setValue('Token Endpoint');

        const tokenIntrospectionEndpointInput = await loader.getHarness(
          MatInputHarness.with({ selector: '[formControlName=tokenIntrospectionEndpoint]' }),
        );
        await tokenIntrospectionEndpointInput.setValue('Token Introspection Endpoint');

        const authorizeEndpointInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=authorizeEndpoint]' }));
        await authorizeEndpointInput.setValue('Authorize Endpoint');

        const userInfoEndpointInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=userInfoEndpoint]' }));
        await userInfoEndpointInput.setValue('User Info Endpoint');

        const userLogoutEndpointInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=userLogoutEndpoint]' }));
        await userLogoutEndpointInput.setValue('User Logout Endpoint');

        const scopesInput = await loader.getHarness(GioFormTagsInputHarness.with({ selector: '[formControlName=scopes]' }));
        await scopesInput.addTag('Scope A');
        await scopesInput.addTag('Scope B');

        const colorInput = await loader.getHarness(GioFormColorInputHarness.with({ selector: '[formControlName=color]' }));
        await colorInput.setValue('#ffffff');

        expect(fixture.componentInstance.identityProviderFormGroup.get('configuration').value).toEqual({
          authorizeEndpoint: 'AuthorizeEndpoint',
          clientId: 'Client Id',
          clientSecret: 'Client Secret',
          color: '#ffffff',
          scopes: ['Scope A', 'Scope B'],
          tokenEndpoint: 'TokenEndpoint',
          tokenIntrospectionEndpoint: 'TokenIntrospectionEndpoint',
          userInfoEndpoint: 'UserInfoEndpoint',
          userLogoutEndpoint: 'UserLogoutEndpoint',
        });

        const idInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=id]' }));
        await idInput.setValue('Id');

        const firstnameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=firstname]' }));
        await firstnameInput.setValue('Firstname');

        const lastnameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=lastname]' }));
        await lastnameInput.setValue('Lastname');

        const emailInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=email]' }));
        await emailInput.setValue('Email');

        const pictureInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=picture]' }));
        await pictureInput.setValue('Picture');

        expect(fixture.componentInstance.identityProviderFormGroup.get('userProfileMapping').value).toEqual({
          email: 'Email',
          firstname: 'Firstname',
          id: 'Id',
          lastname: 'Lastname',
          picture: 'Picture',
        });

        // Set value for required general field
        const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=name]' }));
        await nameInput.setValue('Name');

        const saveBar = await loader.getHarness(GioSaveBarHarness);
        await saveBar.clickSubmit();

        expectIdentityProviderCreateRequest({
          description: null,
          emailRequired: true,
          enabled: true,
          name: 'Name',
          syncMappings: false,
          type: 'OIDC',
          configuration: {
            authorizeEndpoint: 'AuthorizeEndpoint',
            clientId: 'Client Id',
            clientSecret: 'Client Secret',
            color: '#ffffff',
            scopes: ['Scope A', 'Scope B'],
            tokenEndpoint: 'TokenEndpoint',
            tokenIntrospectionEndpoint: 'TokenIntrospectionEndpoint',
            userInfoEndpoint: 'UserInfoEndpoint',
            userLogoutEndpoint: 'UserLogoutEndpoint',
          },
          userProfileMapping: {
            email: 'Email',
            firstname: 'Firstname',
            id: 'Id',
            lastname: 'Lastname',
            picture: 'Picture',
          },
        });
      });
    });
  });

  describe('edit', () => {
    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [NoopAnimationsModule, GioTestingModule, OrganizationSettingsModule, GioLicenseTestingModule, MatIconTestingModule],
        providers: [
          {
            provide: ActivatedRoute,
            useValue: {
              snapshot: {
                params: {
                  id: 'providerId',
                },
              },
              fragment: of(''),
            },
          },
        ],
      });

      fixture = TestBed.createComponent(OrgSettingsIdentityProviderComponent);
      loader = TestbedHarnessEnvironment.loader(fixture);
      component = fixture.componentInstance;

      httpTestingController = TestBed.inject(HttpTestingController);

      fixture.detectChanges();
    });

    it('should be in edit mode', async () => {
      expectIdentityProviderGetRequest(fakeIdentityProvider({ id: 'providerId' }));
      expectEnvironmentListRequest([]);

      expect(component.mode).toEqual('edit');
    });

    it('should not allow to change the provider type', async () => {
      expectIdentityProviderGetRequest(fakeIdentityProvider({ id: 'providerId' }));
      expectEnvironmentListRequest([]);

      expect(component.mode).toEqual('edit');
      const providerType = await loader.getAllHarnesses(GioFormSelectionInlineHarness.with({ selector: '[formControlName=type]' }));
      expect(providerType.length).toBe(0);
    });

    it('should save identity provider general settings', async () => {
      expectIdentityProviderGetRequest(
        fakeIdentityProvider({
          id: 'providerId',
          type: 'GRAVITEEIO_AM',
          name: 'Name',
          description: 'Description',
          groupMappings: [{ condition: 'A', groups: ['groupAId'] }],
          configuration: {
            clientId: 'Client Id',
            clientSecret: 'Client Secret',
            color: null,
            domain: 'Domain',
            scopes: null,
            serverURL: 'ServerURL',
          },
          userProfileMapping: {
            email: null,
            firstname: null,
            id: 'Id',
            lastname: null,
            picture: null,
          },
        }),
      );
      expectEnvironmentListRequest([]);
      expectGroupListRequest([fakeGroup({ id: 'groupAId', name: 'Group A' })]);

      const saveBar = await loader.getHarness(GioSaveBarHarness);

      // Set value for all General fields

      const descriptionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=description]' }));
      await descriptionInput.setValue('Updated Description');

      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=name]' }));
      await nameInput.setValue('Updated Name');

      const allowPortalToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName=enabled]' }));
      await allowPortalToggle.toggle();

      const emailRequiredToggle = await loader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName=emailRequired]' }));
      await emailRequiredToggle.toggle();

      const syncMappingsRadioGroupe = await loader.getHarness(MatRadioGroupHarness.with({ selector: '[formControlName=syncMappings]' }));
      await syncMappingsRadioGroupe.checkRadioButton({ label: /^Computed during each user/ });

      // Update value for some GRAVITEEIO_AM fields
      const clientIdInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=clientId]' }));
      await clientIdInput.setValue('Updated Client Id');

      const clientSecretInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=clientSecret]' }));
      await clientSecretInput.setValue('Updated Client Secret');

      const serverURLInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=serverURL]' }));
      await serverURLInput.setValue('UpdatedServerURL');

      const domainInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=domain]' }));
      await domainInput.setValue('Updated Domain');

      const idInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=id]' }));
      await idInput.setValue('Updated Id');

      expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);

      await saveBar.clickSubmit();

      expectIdentityProviderUpdateRequest('providerId', {
        description: 'Updated Description',
        emailRequired: false,
        enabled: false,
        name: 'Updated Name',
        syncMappings: true,
        groupMappings: [{ condition: 'A', groups: ['groupAId'] }],
        roleMappings: [],
        configuration: {
          clientId: 'Updated Client Id',
          clientSecret: 'Updated Client Secret',
          color: null,
          domain: 'Updated Domain',
          scopes: null,
          serverURL: 'UpdatedServerURL',
        },
        userProfileMapping: {
          email: null,
          firstname: null,
          id: 'Updated Id',
          lastname: null,
          picture: null,
        },
      });
      // Expect the component is reset
      expect(component.isLoading).toBe(true);

      // no flush to end this test
      httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/identities/providerId`);
    });

    describe('group mappings', () => {
      let identityProviderToUpdate;
      let groupMappingsCard: MatCardHarness;

      beforeEach(async () => {
        identityProviderToUpdate = fakeIdentityProvider({
          id: 'providerId',
          type: 'GRAVITEEIO_AM',
          name: 'Name',
          description: 'Description',
          groupMappings: [{ condition: 'foo', groups: ['groupAId'] }],
          configuration: {
            clientId: 'Client Id',
            clientSecret: 'Client Secret',
            color: null,
            domain: 'Domain',
            scopes: null,
            serverURL: 'ServerURL',
          },
          userProfileMapping: {
            email: null,
            firstname: null,
            id: 'Id',
            lastname: null,
            picture: null,
          },
        });
        expectIdentityProviderGetRequest(identityProviderToUpdate);
        expectEnvironmentListRequest([]);

        expectGroupListRequest([fakeGroup({ id: 'groupAId', name: 'Group A' }), fakeGroup({ id: 'groupBId', name: 'Group B' })]);
        groupMappingsCard = await loader.getHarness(MatCardHarness.with({ selector: '[formArrayName=groupMappings]' }));
      });

      it('should edit group mapping', async () => {
        // 📝 [ng-reflect-name="0"] is the index of the first group mapping
        // Select the sub card to edit of groupMappingsCard
        const groupMappingCardToEdit = await groupMappingsCard.getHarness(MatCardHarness.with({ selector: '[ng-reflect-name="0"]' }));

        const conditionInput = await groupMappingCardToEdit.getHarness(MatInputHarness.with({ selector: '[formControlName=condition]' }));
        await conditionInput.setValue('new foo');

        const groupsSelect = await groupMappingCardToEdit.getHarness(MatSelectHarness.with({ selector: '[formControlName=groups]' }));
        await groupsSelect.clickOptions({ text: 'Group B' });

        const saveBar = await loader.getHarness(GioSaveBarHarness);
        expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
        await saveBar.clickSubmit();

        expectIdentityProviderUpdateRequest('providerId', {
          ...identityProviderToUpdate,
          groupMappings: [{ condition: 'new foo', groups: ['groupAId', 'groupBId'] }],
        });

        // Expect the component is reset
        expect(component.isLoading).toBe(true);

        // no flush to end this test
        httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/identities/providerId`);
      });

      it('should add group mapping', async () => {
        const saveBar = await loader.getHarness(GioSaveBarHarness);

        const addGroupMappingButton = await groupMappingsCard.getHarness(MatButtonHarness.with({ text: /Add group mapping/ }));
        await addGroupMappingButton.click();

        expect(await saveBar.isSubmitButtonInvalid()).toEqual(true);

        // 📝 [ng-reflect-name="0"] is the index of the first group mapping
        // Select the new card added of groupMappingsCard
        const groupMappingCardAdded = await groupMappingsCard.getHarness(MatCardHarness.with({ selector: '[ng-reflect-name="1"]' }));

        const conditionInput = await groupMappingCardAdded.getHarness(MatInputHarness.with({ selector: '[formControlName=condition]' }));
        await conditionInput.setValue('new bar');
        const groupsSelect = await groupMappingCardAdded.getHarness(MatSelectHarness.with({ selector: '[formControlName=groups]' }));
        await groupsSelect.clickOptions({ text: 'Group B' });

        expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
        await saveBar.clickSubmit();

        expectIdentityProviderUpdateRequest('providerId', {
          ...identityProviderToUpdate,
          groupMappings: [
            { condition: 'foo', groups: ['groupAId'] },
            { condition: 'new bar', groups: ['groupBId'] },
          ],
        });

        // no flush to end this test
        httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/identities/providerId`);
      });

      it('should delete group mapping', async () => {
        const saveBar = await loader.getHarness(GioSaveBarHarness);

        // 📝 [ng-reflect-name="0"] is the index of the group to delete
        const deleteGroupMappingButton = await groupMappingsCard.getHarness(
          MatButtonHarness.with({ text: /Delete/, ancestor: '[ng-reflect-name="0"]' }),
        );
        await deleteGroupMappingButton.click();

        expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
        await saveBar.clickSubmit();

        expectIdentityProviderUpdateRequest('providerId', {
          ...identityProviderToUpdate,
          groupMappings: [],
        });

        // no flush to end this test
        httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/identities/providerId`);
      });
    });

    describe('role mappings', () => {
      let identityProviderToUpdate;
      let roleMappingsCard: MatCardHarness;

      beforeEach(async () => {
        identityProviderToUpdate = fakeIdentityProvider({
          id: 'providerId',
          type: 'GRAVITEEIO_AM',
          name: 'Name',
          description: 'Description',
          roleMappings: [
            {
              condition: 'foo',
              organizations: ['ROLE_ORG_USER'],
              environments: { environmentAlphaId: ['ROLE_ENV_USER'], environmentBetaId: ['ROLE_ENV_USER'] },
            },
          ],
          configuration: {
            clientId: 'Client Id',
            clientSecret: 'Client Secret',
            color: null,
            domain: 'Domain',
            scopes: null,
            serverURL: 'ServerURL',
          },
          userProfileMapping: {
            email: null,
            firstname: null,
            id: 'Id',
            lastname: null,
            picture: null,
          },
        });
        expectIdentityProviderGetRequest(identityProviderToUpdate);

        expectEnvironmentListRequest([
          fakeEnvironment({ id: 'environmentAlphaId', name: 'Environment Alpha' }),
          fakeEnvironment({ id: 'environmentBetaId', name: 'Environment Beta' }),
        ]);
        expectRolesListRequest('ORGANIZATION', [
          fakeRole({ id: 'roleOrgUserId', name: 'ROLE_ORG_USER' }),
          fakeRole({ id: 'roleOrgAdminId', name: 'ROLE_ORG_ADMIN' }),
        ]);
        expectRolesListRequest('ENVIRONMENT', [
          fakeRole({ id: 'roleEnvApiId', name: 'ROLE_ENV_API' }),
          fakeRole({ id: 'roleEnvUserId', name: 'ROLE_ENV_USER' }),
        ]);

        roleMappingsCard = await loader.getHarness(MatCardHarness.with({ selector: '[formArrayName=roleMappings]' }));
      });

      it('should edit role mapping', async () => {
        // 📝 [ng-reflect-name="0"] is the index of the first role mapping
        // Select the sub card to edit of roleMappingsCard
        const roleMappingCardToEdit = await roleMappingsCard.getHarness(MatCardHarness.with({ selector: '[ng-reflect-name="0"]' }));

        const conditionInput = await roleMappingCardToEdit.getHarness(MatInputHarness.with({ selector: '[formControlName=condition]' }));
        await conditionInput.setValue('new foo');

        const organizationsSelect = await roleMappingCardToEdit.getHarness(
          MatSelectHarness.with({ selector: '[formControlName=organizations]' }),
        );
        expect(await organizationsSelect.getValueText()).toEqual('ROLE_ORG_USER');
        await organizationsSelect.clickOptions({ text: 'ROLE_ORG_ADMIN' });

        // Select ROLE_ENV_API option in environmentAlpha row of environment table
        const environmentsTable = await roleMappingCardToEdit.getHarness(MatTableHarness);
        const environmentAlphaRow = (await environmentsTable.getRows())[0]; // select first row
        const environmentAlphaActionsCell = (await environmentAlphaRow.getCells())[2]; // select third column = actions
        const environmentAlphaSelect = await environmentAlphaActionsCell.getHarness(MatSelectHarness);
        expect(await environmentAlphaSelect.getValueText()).toEqual('ROLE_ENV_USER');
        await environmentAlphaSelect.clickOptions({ text: 'ROLE_ENV_API' });

        const saveBar = await loader.getHarness(GioSaveBarHarness);
        expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
        await saveBar.clickSubmit();

        expectIdentityProviderUpdateRequest('providerId', {
          ...identityProviderToUpdate,
          roleMappings: [
            {
              condition: 'new foo',
              organizations: ['ROLE_ORG_USER', 'ROLE_ORG_ADMIN'],
              environments: {
                environmentAlphaId: ['ROLE_ENV_API', 'ROLE_ENV_USER'],
                environmentBetaId: ['ROLE_ENV_USER'],
              },
            },
          ],
        });

        // Expect the component is reset
        expect(component.isLoading).toBe(true);

        // no flush to end this test
        httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/identities/providerId`);
      });

      it('should add role mapping', async () => {
        const saveBar = await loader.getHarness(GioSaveBarHarness);

        const addRoleMappingButton = await roleMappingsCard.getHarness(MatButtonHarness.with({ text: /Add role mapping/ }));
        await addRoleMappingButton.click();

        expect(await saveBar.isSubmitButtonInvalid()).toEqual(true);

        // 📝 [ng-reflect-name="1"] is the index of the new role mapping added
        // Select the new card added of roleMappingsCard
        const roleMappingCardAdded = await roleMappingsCard.getHarness(MatCardHarness.with({ selector: '[ng-reflect-name="1"]' }));

        // Add condition and select role for new role mapping added
        const conditionInput = await roleMappingCardAdded.getHarness(MatInputHarness.with({ selector: '[formControlName=condition]' }));
        await conditionInput.setValue('new bar');
        const organizationsSelect = await roleMappingCardAdded.getHarness(
          MatSelectHarness.with({ selector: '[formControlName=organizations]' }),
        );
        await organizationsSelect.clickOptions({ text: 'ROLE_ORG_ADMIN' });

        const environmentsTable = await roleMappingCardAdded.getHarness(MatTableHarness);
        // Select ROLE_ENV_API option in environmentAlpha row of environment table
        const environmentAlphaRow = (await environmentsTable.getRows())[0]; // select first row
        const environmentAlphaActionsCell = (await environmentAlphaRow.getCells())[2]; // select third column = actions
        const environmentAlphaSelect = await environmentAlphaActionsCell.getHarness(MatSelectHarness);
        await environmentAlphaSelect.clickOptions({ text: 'ROLE_ENV_API' });

        // Same for Beta environment because role selection is required
        const environmentBetaRow = (await environmentsTable.getRows())[1]; // select second row
        const environmentBetaActionsCell = (await environmentBetaRow.getCells())[2]; // select third column = actions
        const environmentBetaSelect = await environmentBetaActionsCell.getHarness(MatSelectHarness);
        await environmentBetaSelect.clickOptions({ text: 'ROLE_ENV_USER' });

        expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
        await saveBar.clickSubmit();

        expectIdentityProviderUpdateRequest('providerId', {
          ...identityProviderToUpdate,
          roleMappings: [
            {
              condition: 'foo',
              organizations: ['ROLE_ORG_USER'],
              environments: {
                environmentAlphaId: ['ROLE_ENV_USER'],
                environmentBetaId: ['ROLE_ENV_USER'],
              },
            },
            {
              condition: 'new bar',
              organizations: ['ROLE_ORG_ADMIN'],
              environments: {
                environmentAlphaId: ['ROLE_ENV_API'],
                environmentBetaId: ['ROLE_ENV_USER'],
              },
            },
          ],
        });

        // no flush to end this test
        httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/identities/providerId`);
      });
    });
  });

  function expectIdentityProviderCreateRequest(newIdentityProvider: NewIdentityProvider) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/identities`);
    expect(req.request.method).toEqual('POST');
    expect(req.request.body).toStrictEqual(newIdentityProvider);
    req.flush(fakeIdentityProvider({ ...newIdentityProvider }));
  }

  function expectIdentityProviderUpdateRequest(id: string, identityProvider: IdentityProvider) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/identities/${id}`);
    expect(req.request.method).toEqual('PUT');
    expect(req.request.body).toStrictEqual(omit(identityProvider, ['id', 'type']));
    req.flush({ id, identityProvider });
  }

  function expectIdentityProviderGetRequest(identityProvider: IdentityProvider) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/identities/${identityProvider.id}`);
    expect(req.request.method).toEqual('GET');
    req.flush(identityProvider);
    fixture.detectChanges();
  }

  function expectGroupListRequest(groups: Group[]) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/groups`);
    expect(req.request.method).toEqual('GET');
    req.flush(groups);
    fixture.detectChanges();
  }

  function expectRolesListRequest(scope, roles: Role[]) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/${scope}/roles`);
    expect(req.request.method).toEqual('GET');
    req.flush(roles);
    fixture.detectChanges();
  }

  function expectEnvironmentListRequest(environments: Environment[]) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/environments`);
    expect(req.request.method).toEqual('GET');
    req.flush(environments);
    fixture.detectChanges();
  }
});
