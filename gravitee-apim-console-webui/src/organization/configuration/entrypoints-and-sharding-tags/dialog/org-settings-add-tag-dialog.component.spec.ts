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
import { ComponentFixture, fakeAsync, TestBed } from '@angular/core/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatSelectHarness } from '@angular/material/select/testing';

import { OrgSettingAddTagDialogComponent, OrgSettingAddTagDialogData } from './org-settings-add-tag-dialog.component';

import { OrganizationSettingsModule } from '../../organization-settings.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { fakeGroup } from '../../../../entities/group/group.fixture';
import { Group } from '../../../../entities/group/group';
import { fakeTag } from '../../../../entities/tag/tag.fixture';

describe('OrgSettingAddTagDialogComponent', () => {
  let component: OrgSettingAddTagDialogComponent;
  let fixture: ComponentFixture<OrgSettingAddTagDialogComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const matDialogRefMock = {
    close: jest.fn(),
  };

  const initComponent = (dialogData: OrgSettingAddTagDialogData) => {
    TestBed.configureTestingModule({
      imports: [OrganizationSettingsModule, GioTestingModule],
      providers: [
        {
          provide: MAT_DIALOG_DATA,
          useFactory: () => dialogData,
        },
        { provide: MatDialogRef, useValue: matDialogRefMock },
      ],
    });
    fixture = TestBed.createComponent(OrgSettingAddTagDialogComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
  };

  afterEach(() => {
    matDialogRefMock.close.mockClear();
  });

  describe('tag creation', () => {
    beforeEach(() => {
      initComponent({});
    });

    it('should fill and submit form', async () => {
      fixture.detectChanges();
      expectGroupListByOrganizationRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);

      const submitButton = await loader.getHarness(MatButtonHarness.with({ selector: 'button[type=submit]' }));
      expect(await submitButton.isDisabled()).toBeTruthy();

      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=name]' }));
      await nameInput.setValue('Tag name');

      const keyInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=key]' }));
      await keyInput.setValue('tag-key');

      const descriptionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=description' }));
      await descriptionInput.setValue('Tag description');

      const restrictedGroupSelect = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName=restrictedGroups' }));
      await restrictedGroupSelect.clickOptions({ text: 'Group A' });

      await submitButton.click();

      expect(matDialogRefMock.close).toHaveBeenCalledWith({
        name: 'Tag name',
        key: 'tag-key',
        description: 'Tag description',
        restricted_groups: ['group-a'],
      });
    });

    it.each`
      key                                  | sanitized
      ${'My Tag Key'}                      | ${'my-tag-key'}
      ${'Tâg Spécîal @#$ Nàme!'}           | ${'tag-special-name'}
      ${'Tag   With    Multiple---Spaces'} | ${'tag-with-multiple-spaces'}
      ${'Tag Key---'}                      | ${'tag-key'}
      ${'UPPERCASE KEY'}                   | ${'uppercase-key'}
      ${'Key 123 Value 456'}               | ${'key-123-value-456'}
      ${'eu east 1! @#$%'}                 | ${'eu-east-1'}
    `(
      'should sanitize key "$key" to "$sanitized"',
      fakeAsync(async ({ key, sanitized }) => {
        fixture.detectChanges();
        expectGroupListByOrganizationRequest([]);

        component.tagForm.controls.key.setValue(key);
        fixture.detectChanges();

        const keyInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=key]' }));
        await keyInput.blur();
        fixture.detectChanges();

        expect(await keyInput.getValue()).toBe(sanitized);
      }),
    );
  });

  describe('tag edition', () => {
    beforeEach(() => {
      initComponent({
        tag: fakeTag({
          id: '875fb0a0-1ea2-3a1d-bfd6-f59f9a18bd5b',
          name: 'Tag name',
          key: 'tag-key',
          description: 'Tag description',
          restricted_groups: ['group-a'],
        }),
      });
    });

    it('should fill and submit form', async () => {
      fixture.detectChanges();
      expectGroupListByOrganizationRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
      expect(component.isUpdate).toBeTruthy();

      const submitButton = await loader.getHarness(MatButtonHarness.with({ selector: 'button[type=submit]' }));

      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=name]' }));
      await nameInput.setValue('');
      expect(await submitButton.isDisabled()).toBeTruthy();
      await nameInput.setValue('Internal');

      const keyInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=key]' }));
      expect(await keyInput.isDisabled()).toBeTruthy();

      const descriptionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=description' }));
      await descriptionInput.setValue('Internal tenant');

      const restrictedGroupSelect = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName=restrictedGroups' }));
      await restrictedGroupSelect.clickOptions({ text: 'Group A' });

      await submitButton.click();

      expect(matDialogRefMock.close).toHaveBeenCalledWith({
        id: '875fb0a0-1ea2-3a1d-bfd6-f59f9a18bd5b',
        name: 'Internal',
        key: 'tag-key',
        description: 'Internal tenant',
        restricted_groups: [],
      });
    });
  });

  function expectGroupListByOrganizationRequest(groups: Group[] = []) {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.org.baseURL}/groups`,
      })
      .flush(groups);
  }
});
