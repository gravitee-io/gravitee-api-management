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

import { OrgSettingAddTenantComponent, OrgSettingAddTenantDialogData } from './org-settings-add-tenant.component';

import { OrganizationSettingsModule } from '../organization-settings.module';
import { fakeTenant } from '../../../entities/tenant/tenant.fixture';

describe('GioConfirmDialogComponent', () => {
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
        imports: [OrganizationSettingsModule],
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

      const descriptionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=description' }));
      await descriptionInput.setValue('External tenant');

      await submitButton.click();

      expect(matDialogRefMock.close).toHaveBeenCalledWith({
        name: 'External',
        description: 'External tenant',
      });
    });
  });

  describe('tenant edition', () => {
    beforeEach(() => {
      const dialogData: OrgSettingAddTenantDialogData = {
        tenant: fakeTenant({
          id: 'external',
          name: 'External',
          description: 'External tenant',
        }),
      };
      TestBed.configureTestingModule({
        imports: [OrganizationSettingsModule],
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

      const idInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=id]' }));
      expect(await idInput.isDisabled()).toBeTruthy();
      expect(await idInput.getValue()).toEqual('external');

      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=name]' }));
      await nameInput.setValue('');
      expect(await submitButton.isDisabled()).toBeTruthy();
      await nameInput.setValue('Internal');

      const descriptionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=description' }));
      await descriptionInput.setValue('Internal tenant');

      await submitButton.click();

      expect(matDialogRefMock.close).toHaveBeenCalledWith({
        id: 'external',
        name: 'Internal',
        description: 'Internal tenant',
      });
    });
  });
});
