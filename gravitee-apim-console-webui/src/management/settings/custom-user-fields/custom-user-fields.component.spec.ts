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
import { HttpTestingController } from '@angular/common/http/testing';
import { BrowserAnimationsModule, NoopAnimationsModule } from '@angular/platform-browser/animations';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';

import { CustomUserFieldsComponent } from './custom-user-fields.component';
import { CustomUserFieldsHarness } from './custom-user-fields.harness';
import { CustomUserFieldsDialogHarness } from './dialog/custom-user-fields-dialog.harness';

import { SettingsModule } from '../settings.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { GioTestingPermission, GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { CustomUserField } from '../../../entities/customUserFields';
import { fakeCustomUserField } from '../../../entities/custom-user-fields/custom-user-fields.fixture';

describe('CustomUserFieldsComponent', () => {
  let fixture: ComponentFixture<CustomUserFieldsComponent>;
  let componentHarness: CustomUserFieldsHarness;
  let httpTestingController: HttpTestingController;

  const init = async (
    permissions: GioTestingPermission = [
      'organization-custom_user_fields-c',
      'organization-custom_user_fields-r',
      'organization-custom_user_fields-u',
      'organization-custom_user_fields-d',
    ],
  ) => {
    await TestBed.configureTestingModule({
      declarations: [CustomUserFieldsComponent],
      imports: [SettingsModule, GioTestingModule, BrowserAnimationsModule, NoopAnimationsModule],
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: [...permissions],
        },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
          isTabbable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(CustomUserFieldsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, CustomUserFieldsHarness);
    fixture.componentInstance.filters = {
      pagination: { index: 1, size: 25 },
      searchTerm: '',
    };
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('CustomUserFields', () => {
    beforeEach(() => {
      init();
    });

    it('should call for custom fields on init', () => {
      expectUserFieldsGetRequest();
    });

    it('table show correct number of rows', async () => {
      expectUserFieldsGetRequest([fakeCustomUserField(), fakeCustomUserField(), fakeCustomUserField()]);
      fixture.detectChanges();
      expect(await componentHarness.rowsNumber()).toEqual(3);
    });

    it('should create new field', async () => {
      const list = [fakeCustomUserField(), fakeCustomUserField(), fakeCustomUserField()];
      expectUserFieldsGetRequest(list);

      const createButton = await componentHarness.getAddButton();
      await createButton.click();

      const addDialog = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(CustomUserFieldsDialogHarness);
      expect(addDialog).toBeTruthy();

      await addDialog.setKey('new-field');
      await addDialog.setLabel('new-label');
      await addDialog.clickOnSave();
      fixture.detectChanges();

      expectPostRequest({
        key: 'new-field',
        label: 'new-label',
        required: false,
        values: [],
      });

      expectUserFieldsGetRequest();
    });

    it('should edit field', async () => {
      const list = [fakeCustomUserField(), fakeCustomUserField(), fakeCustomUserField()];
      expectUserFieldsGetRequest(list);
      fixture.detectChanges();

      await componentHarness.editField(0);
      const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(CustomUserFieldsDialogHarness);
      expect(dialogHarness).toBeTruthy();

      await dialogHarness.setKey('TEST');
      await dialogHarness.setLabel('TEST-label');
      await dialogHarness.clickOnSave();
      fixture.detectChanges();

      expectPutRequest(fakeCustomUserField());

      expectUserFieldsGetRequest();
    });

    it('should delete field', async () => {
      const list = [fakeCustomUserField(), fakeCustomUserField(), fakeCustomUserField()];
      expectUserFieldsGetRequest(list);
      fixture.detectChanges();

      await componentHarness.deleteField(0);
      const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
      await dialogHarness.confirm();

      expectDeleteRequest(list[0].key);
      expectUserFieldsGetRequest();
    });
  });

  function expectUserFieldsGetRequest(fakeResponse: CustomUserField[] = [fakeCustomUserField()]) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/custom-user-fields`);

    req.flush(fakeResponse);
    expect(req.request.method).toEqual('GET');
  }

  function expectPostRequest(payload: CustomUserField) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/custom-user-fields`);

    req.flush([]);
    expect(req.request.method).toEqual('POST');
    expect(req.request.body).toEqual(payload);
  }

  function expectPutRequest(payload: CustomUserField) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/custom-user-fields/${payload.key}`);

    req.flush([]);
    expect(req.request.method).toEqual('PUT');
  }

  function expectDeleteRequest(key: string) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/custom-user-fields/${key}`);

    req.flush([]);
    expect(req.request.method).toEqual('DELETE');
  }
});
