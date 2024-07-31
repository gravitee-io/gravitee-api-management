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
import { InteractivityChecker } from '@angular/cdk/a11y';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { ActivatedRoute } from '@angular/router';
import { MatButtonToggleGroupHarness } from '@angular/material/button-toggle/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatDialogHarness } from '@angular/material/dialog/testing';

import { ApplicationGeneralTransferOwnershipComponent } from './application-general-transfer-ownership.component';

import { ApplicationGeneralUserGroupModule } from '../application-general-user-group.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import { fakeMembers } from '../../../../../entities/members/Members.fixture';
import { Role } from '../../../../../entities/role/role';
import { fakeRole } from '../../../../../entities/role/role.fixture';
import { GioTestingPermissionProvider } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { Member } from '../../../../../entities/management-api-v2';
import { GioFormUserAutocompleteHarness } from '../../../../../shared/components/gio-user-autocomplete/gio-form-user-autocomplete.harness';
import { SearchableUser } from '../../../../../entities/user/searchableUser';
import { fakeSearchableUser } from '../../../../../entities/user/searchableUser.fixture';
import { Application } from '../../../../../entities/application/Application';
import { fakeApplication } from '../../../../../entities/application/Application.fixture';

describe('ApplicationGeneralTransferOwnershipComponent', () => {
  let fixture: ComponentFixture<ApplicationGeneralTransferOwnershipComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  const APPLICATION_ID = 'id_test';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, ApplicationGeneralUserGroupModule, GioTestingModule, MatIconTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { applicationId: APPLICATION_ID } },
        {
          provide: GioTestingPermissionProvider,
          useValue: ['application-member-u', 'application-member-d', 'application-member-c', 'application-member-r'],
        },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This checks focus trap, set it to true to  avoid the warning
          isTabbable: () => true,
        },
      })
      .overrideProvider(ActivatedRoute, { useValue: { snapshot: { params: { applicationId: APPLICATION_ID } } } });

    fixture = TestBed.createComponent(ApplicationGeneralTransferOwnershipComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);

    fixture.detectChanges();
  });

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  it('should disable transfer with kubernetes origin', async () => {
    expectGetApplication(fakeApplication({ origin: 'KUBERNETES' }));
    expectGetMembers([fakeMembers()]);
    expectApplicationRoleGetRequest([
      fakeRole({ name: 'TEST_ROLE1', default: false }),
      fakeRole({ name: 'PRIMARY_OWNER' }),
      fakeRole({ name: 'DEFAULT_ROLE', default: true }),
    ]);
    const methodRadio = await loader.getHarness(MatButtonToggleGroupHarness.with({ selector: '[formControlName="method"' }));
    const disabled = await methodRadio.isDisabled();
    expect(disabled).toBe(true);
  });

  it('should no disable transfer with non kubernetes origin', async () => {
    expectGetApplication(fakeApplication({ origin: '' }));
    expectGetMembers([fakeMembers()]);
    expectApplicationRoleGetRequest([
      fakeRole({ name: 'TEST_ROLE1', default: false }),
      fakeRole({ name: 'PRIMARY_OWNER' }),
      fakeRole({ name: 'DEFAULT_ROLE', default: true }),
    ]);
    const methodRadio = await loader.getHarness(MatButtonToggleGroupHarness.with({ selector: '[formControlName="method"' }));
    const disabled = await methodRadio.isDisabled();
    expect(disabled).toBe(false);
  });

  it('should transfer ownership to user', async () => {
    const membersList = [fakeMembers()];
    expectGetApplication(fakeApplication());
    expectGetMembers(membersList);
    expectApplicationRoleGetRequest([
      fakeRole({ name: 'TEST_ROLE1', default: false }),
      fakeRole({ name: 'PRIMARY_OWNER' }),
      fakeRole({ name: 'DEFAULT_ROLE', default: true }),
    ]);

    const methodRadio = await loader.getHarness(MatButtonToggleGroupHarness.with({ selector: '[formControlName="method"' }));
    expect(methodRadio).toBeTruthy();

    const otherUserButton = await methodRadio.getToggles({ text: 'Other user' });
    await otherUserButton[0].check();

    const userSelect = await loader.getHarness(GioFormUserAutocompleteHarness);
    await userSelect.setSearchText('Test');
    respondToUserSearchRequest('Test', [fakeSearchableUser({ displayName: 'Test' })]);
    await userSelect.selectOption({ text: 'Test' });
    respondToUserSearchRequest('Test', [fakeSearchableUser({ displayName: 'Test' })]);

    const roleSelect = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="roleId"]' }));
    await roleSelect.open();

    const roleOptions = await roleSelect.getOptions();
    expect(roleOptions.length).toBe(2);
    await roleSelect.clickOptions({ text: 'TEST_ROLE1' });

    const transferButton = await loader.getHarness(MatButtonHarness.with({ text: 'Transfer' }));
    await transferButton.click();

    const dialog = await rootLoader.getHarness(MatDialogHarness);
    await (await dialog.getHarness(MatButtonHarness.with({ text: 'Transfer' }))).click();

    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}/members/transfer_ownership`,
      method: 'POST',
    });
    expect(req.request.body).toEqual({
      id: '1d4fae8c-3705-43ab-8fae-8c370543abf3',
      reference:
        'ZXlKamRIa2lPaUpLVjFRaUxDSmxibU1pT2lKQk1qVTJSME5OSWl3aVlXeG5Jam9pWkdseUluMC4uU1hpaXBCSUhaZFpYTTdubC5uQ241WWR1MEhDS3FPOF9uaWpzVHJad2RCaEppVWxVRE9XMnB1dVoya2c0QW9SQi1Vb0o1azdKSndwNXMwSE5kcjU0ZVd0cUN5bUxFWUdOTGJOdlNRZjNOLVFGa3Q4UHhyYmFha05BbEd2NzlUeE5ySWJCYmxrcHhpQnNDRjMwcVY1emJxdjhnZVZ3RjF1RzM0cTZ5R25kRWhjeFAyQ2h0S1lqd3UwcUxES0dqNUkwLlB2NENWZHBPcUM3cnlWcTBFalRwa1E=',
      role: 'TEST_ROLE1',
    });
  });

  it('should transfer ownership to application member', async () => {
    const membersList = [{ id: '1', displayName: 'TestName', role: 'USER' }];
    expectGetApplication(fakeApplication());
    expectGetMembers(membersList);
    expectApplicationRoleGetRequest([
      fakeRole({ name: 'TEST_ROLE1', default: false }),
      fakeRole({ name: 'PRIMARY_OWNER' }),
      fakeRole({ name: 'DEFAULT_ROLE', default: true }),
    ]);

    const methodRadio = await loader.getHarness(MatButtonToggleGroupHarness.with({ selector: '[formControlName="method"' }));
    expect(methodRadio).toBeTruthy();

    const otherUserButton = await methodRadio.getToggles({ text: 'Application member' });
    await otherUserButton[0].check();

    const userSelect = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="user"' }));
    await userSelect.clickOptions({ text: 'TestName' });

    const roleSelect = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="roleId"]' }));
    await roleSelect.open();

    const roleOptions = await roleSelect.getOptions();
    expect(roleOptions.length).toBe(2);
    await roleSelect.clickOptions({ text: 'TEST_ROLE1' });

    const transferButton = await loader.getHarness(MatButtonHarness.with({ text: 'Transfer' }));
    await transferButton.click();

    const dialog = await rootLoader.getHarness(MatDialogHarness);
    await (await dialog.getHarness(MatButtonHarness.with({ text: 'Transfer' }))).click();

    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}/members/transfer_ownership`,
      method: 'POST',
    });
    expect(req.request.body).toEqual({
      id: '1',
      role: 'TEST_ROLE1',
    });
  });

  function expectGetMembers(members: Member[]) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}/members`, method: 'GET' })
      .flush(members);
  }

  function respondToUserSearchRequest(searchTerm: string, searchableUsers: SearchableUser[]) {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.org.baseURL}/search/users?q=${searchTerm}`,
      })
      .flush(searchableUsers);
  }

  function expectApplicationRoleGetRequest(roles: Role[] = []) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/APPLICATION/roles`, method: 'GET' })
      .flush(roles);
  }

  function expectGetApplication(application: Application) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}`,
        method: 'GET',
      })
      .flush(application);
  }
});
