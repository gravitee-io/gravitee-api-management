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
import { HttpTestingController } from '@angular/common/http/testing';
import { MatSelectHarness } from '@angular/material/select/testing';

import { OrgSettingAddTagDialogComponent, OrgSettingAddTagDialogData } from './org-settings-add-tag-dialog.component';

import { OrganizationSettingsModule } from '../organization-settings.module';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { fakeGroup } from '../../../entities/group/group.fixture';
import { Group } from '../../../entities/group/group';
import { fakeTag } from '../../../entities/tag/tag.fixture';

describe('OrgSettingAddTagDialogComponent', () => {
  let component: OrgSettingAddTagDialogComponent;
  let fixture: ComponentFixture<OrgSettingAddTagDialogComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const matDialogRefMock = {
    close: jest.fn(),
  };

  afterEach(() => {
    matDialogRefMock.close.mockClear();
  });

  describe('tag creation', () => {
    beforeEach(() => {
      const dialogData: OrgSettingAddTagDialogData = {};
      TestBed.configureTestingModule({
        imports: [OrganizationSettingsModule, GioHttpTestingModule],
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
    });

    it('should fill and submit form', async () => {
      fixture.detectChanges();
      expectGroupListByOrganizationRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);

      const submitButton = await loader.getHarness(MatButtonHarness.with({ selector: 'button[type=submit]' }));
      expect(await submitButton.isDisabled()).toBeTruthy();

      const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=name]' }));
      await nameInput.setValue('Tag name');

      const descriptionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=description' }));
      await descriptionInput.setValue('Tag description');

      const restrictedGroupSelect = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName=restrictedGroups' }));
      await restrictedGroupSelect.clickOptions({ text: 'Group A' });

      await submitButton.click();

      expect(matDialogRefMock.close).toHaveBeenCalledWith({
        name: 'Tag name',
        description: 'Tag description',
        restricted_groups: ['group-a'],
      });
    });
  });

  describe('tag edition', () => {
    beforeEach(() => {
      const dialogData: OrgSettingAddTagDialogData = {
        tag: fakeTag({
          id: 'tagId',
          name: 'Tag name',
          description: 'Tag description',
          restricted_groups: ['group-a'],
        }),
      };
      TestBed.configureTestingModule({
        imports: [OrganizationSettingsModule, GioHttpTestingModule],
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

      const descriptionInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=description' }));
      await descriptionInput.setValue('Internal tenant');

      const restrictedGroupSelect = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName=restrictedGroups' }));
      await restrictedGroupSelect.clickOptions({ text: 'Group A' });

      await submitButton.click();

      expect(matDialogRefMock.close).toHaveBeenCalledWith({
        id: 'tagId',
        name: 'Internal',
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
