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
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { OrgSettingAddTenantComponent, OrgSettingAddTenantDialogData } from './org-settings-add-tenant.component';

import { OrganizationSettingsModule } from '../organization-settings.module';
import { fakeTenant } from '../../../entities/tenant/tenant.fixture';

describe('OrgSettingAddTenantComponent', () => {
  let component: OrgSettingAddTenantComponent;
  let fixture: ComponentFixture<OrgSettingAddTenantComponent>;
  let loader: HarnessLoader;

  const matDialogRefMock = {
    close: jest.fn(),
  };

  afterEach(() => {
    matDialogRefMock.close.mockClear();
  });

  describe('tenant creation', () => {
    beforeEach(() => {
      const dialogData: OrgSettingAddTenantDialogData = {};
      TestBed.configureTestingModule({
        imports: [NoopAnimationsModule, OrganizationSettingsModule],
        providers: [
          {
            provide: MAT_DIALOG_DATA,
            useFactory: () => dialogData,
          },
          { provide: MatDialogRef, useValue: matDialogRefMock },
        ],
      });
      fixture = TestBed.createComponent(OrgSettingAddTenantComponent);
      component = fixture.componentInstance;
      loader = TestbedHarnessEnvironment.loader(fixture);
    });

    it('should fill and submit form', async () => {
      fixture.detectChanges();
      expect(component.isUpdate).toBeFalsy();

      const submitButton = await loader.getHarness(MatButtonHarness.with({ selector: 'button[type=submit]' }));
      expect(await submitButton.isDisabled()).toBeTruthy();

      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=name]' }));
      await nameInput.setValue('External');

      const keyInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=key]' }));
      await keyInput.setValue('external');

      const descriptionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=description' }));
      await descriptionInput.setValue('External tenant');

      await submitButton.click();

      expect(matDialogRefMock.close).toHaveBeenCalledWith({
        name: 'External',
        key: 'external',
        description: 'External tenant',
      });
    });
  });

  describe('tenant edition', () => {
    beforeEach(() => {
      const dialogData: OrgSettingAddTenantDialogData = {
        tenant: fakeTenant({
          id: '875fb0a0-1ea2-3a1d-bfd6-f59f9a18bd5b',
          key: 'external',
          name: 'External',
          description: 'External tenant',
        }),
      };
      TestBed.configureTestingModule({
        imports: [NoopAnimationsModule, OrganizationSettingsModule],
        providers: [
          {
            provide: MAT_DIALOG_DATA,
            useFactory: () => dialogData,
          },
          { provide: MatDialogRef, useValue: matDialogRefMock },
        ],
      });
      fixture = TestBed.createComponent(OrgSettingAddTenantComponent);
      component = fixture.componentInstance;
      loader = TestbedHarnessEnvironment.loader(fixture);
    });

    it('should fill and submit form', async () => {
      fixture.detectChanges();
      expect(component.isUpdate).toBeTruthy();

      const submitButton = await loader.getHarness(MatButtonHarness.with({ selector: 'button[type=submit]' }));

      const keyInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=key]' }));
      expect(await keyInput.isDisabled()).toBeTruthy();
      expect(await keyInput.getValue()).toEqual('external');

      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=name]' }));
      await nameInput.setValue('');
      expect(await submitButton.isDisabled()).toBeTruthy();
      await nameInput.setValue('Internal');

      const descriptionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=description' }));
      await descriptionInput.setValue('Internal tenant');

      await submitButton.click();

      expect(matDialogRefMock.close).toHaveBeenCalledWith({
        key: 'external',
        name: 'Internal',
        description: 'Internal tenant',
      });
    });
    it('should disable submit if name exceeds 40 characters', async () => {
      fixture.detectChanges();
      const submitButton = await loader.getHarness(MatButtonHarness.with({ selector: 'button[type=submit]' }));
      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=name]' }));
      // 41-character string
      const longName = 'A'.repeat(41);
      await nameInput.setValue(longName);
      expect(await submitButton.isDisabled()).toBeTruthy();
      expect(component.tenantForm.controls['name'].valid).toBeFalsy();
      expect(component.tenantForm.controls['name'].errors?.['maxlength']).toBeTruthy();
    });

    it.each`
      key                                 | sanitized
      ${'My Tenant Key'}                  | ${'my-tenant-key'}
      ${'Tênant Spécîal @#$ Nàme!'}       | ${'tenant-special-name'}
      ${'Tenant   With    Multiple---Sp'} | ${'tenant-with-multiple-sp'}
      ${'Tenant Key---'}                  | ${'tenant-key'}
      ${'UPPERCASE KEY'}                  | ${'uppercase-key'}
      ${'Key 123 Value 456'}              | ${'key-123-value-456'}
      ${'eu east 1! @#$%'}                | ${'eu-east-1'}
    `('should sanitize key "$key" to "$sanitized"', async ({ key, sanitized }) => {
      fixture.detectChanges();

      component.tenantForm.controls.key.setValue(key);
      fixture.detectChanges();

      const keyInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=key]' }));
      await keyInput.blur();
      fixture.detectChanges();

      expect(await keyInput.getValue()).toBe(sanitized);
    });
  });
});
