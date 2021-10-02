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

import { OrgSettingsIdentityProviderComponent } from './org-settings-identity-provider.component';

import { OrganizationSettingsModule } from '../organization-settings.module';
import { GioFormCardGroupHarness } from '../../../shared/components/form-card-group/gio-form-card-group.harness';

describe('OrgSettingsIdentityProviderComponent', () => {
  let fixture: ComponentFixture<OrgSettingsIdentityProviderComponent>;
  let loader: HarnessLoader;
  let component: OrgSettingsIdentityProviderComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, OrganizationSettingsModule],
    });

    fixture = TestBed.createComponent(OrgSettingsIdentityProviderComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should change provider type', async () => {
    const formCardGroup = await loader.getHarness(GioFormCardGroupHarness.with({ selector: '[formControlName=type]' }));

    expect(await formCardGroup.getSelectedValue()).toEqual('GRAVITEEIO_AM');

    await formCardGroup.select('GITHUB');

    expect(await formCardGroup.getSelectedValue()).toEqual('GITHUB');
    expect(Object.keys(component.identityProviderSettings.get('configuration').value)).toEqual(['clientId', 'clientSecret']);
  });

  it('should save identity provider general settings', async () => {
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

    expect(fixture.componentInstance.identityProviderSettings.value).toEqual({
      description: 'Description',
      emailRequired: true,
      enabled: true,
      name: 'Name',
      syncMappings: true,
      tokenExchangeEndpoint: null,
      type: 'GRAVITEEIO_AM',
    });
  });

  describe('github', () => {
    it('should save identity provider github configuration ', async () => {
      const formCardGroup = await loader.getHarness(GioFormCardGroupHarness.with({ selector: '[formControlName=type]' }));

      await formCardGroup.select('GITHUB');

      const clientIdInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=clientId]' }));
      await clientIdInput.setValue('Client Id');

      const clientSecretInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=clientSecret]' }));
      await clientSecretInput.setValue('Client Secret');

      expect(fixture.componentInstance.identityProviderSettings.get('configuration').value).toEqual({
        clientId: 'Client Id',
        clientSecret: 'Client Secret',
      });
    });
  });

  describe('google', () => {
    it('should save identity provider google configuration ', async () => {
      const formCardGroup = await loader.getHarness(GioFormCardGroupHarness.with({ selector: '[formControlName=type]' }));

      await formCardGroup.select('GOOGLE');

      const clientIdInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=clientId]' }));
      await clientIdInput.setValue('Client Id');

      const clientSecretInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=clientSecret]' }));
      await clientSecretInput.setValue('Client Secret');

      expect(fixture.componentInstance.identityProviderSettings.get('configuration').value).toEqual({
        clientId: 'Client Id',
        clientSecret: 'Client Secret',
      });
    });
  });
});
