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

import { CustomUserFieldsMigratedComponent } from './custom-user-fields-migrated.component';
import { CustomUserFieldsMigratedHarness } from './custom-user-fields-migrated.harness';

import { GioTestingPermission, GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { SettingsModule } from '../../settings.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { CustomUserField } from '../../../../entities/customUserFields';
import { fakeCustomUserField } from '../../../../entities/custom-user-fields/custom-user-fields.fixture';

describe('CustomUserFieldsMigratedComponent', () => {
  let fixture: ComponentFixture<CustomUserFieldsMigratedComponent>;
  let componentHarness: CustomUserFieldsMigratedHarness;
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
      declarations: [CustomUserFieldsMigratedComponent],
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

    fixture = TestBed.createComponent(CustomUserFieldsMigratedComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, CustomUserFieldsMigratedHarness);
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
  });

  function expectUserFieldsGetRequest(fakeResponse: CustomUserField[] = [fakeCustomUserField()]) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/custom-user-fields`);

    req.flush(fakeResponse);
    expect(req.request.method).toEqual('GET');
  }
});
