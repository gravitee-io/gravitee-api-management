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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { set } from 'lodash';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatButtonToggleGroupHarness } from '@angular/material/button-toggle/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideHttpClient } from '@angular/common/http';

import {
  ApiProductTransferOwnershipDialogComponent,
  ApiProductOwnershipDialogData,
} from './api-product-transfer-ownership-dialog.component';

import { CONSTANTS_TESTING } from '../../../../shared/testing';
import { Role } from '../../../../entities/role/role';
import { fakeRole } from '../../../../entities/role/role.fixture';
import { GioFormUserAutocompleteHarness } from '../../../../shared/components/gio-user-autocomplete/gio-form-user-autocomplete.harness';
import { fakeSearchableUser } from '../../../../entities/user/searchableUser.fixture';
import { SearchableUser } from '../../../../entities/user/searchableUser';
import { fakeMember } from '../../../../entities/management-api-v2/member/member.fixture';
import { fakeGroup } from '../../../../entities/management-api-v2/group/group.fixture';
import { Constants } from '../../../../entities/Constants';
import { ApiProduct } from '../../../../entities/management-api-v2/api-product';

function fakeApiProduct(overrides?: Partial<ApiProduct>): ApiProduct {
  return {
    id: 'api-product-1',
    name: 'Test API Product',
    version: '1.0',
    primaryOwner: { displayName: 'Admin', id: 'admin-1', email: 'admin@test.com', type: 'USER' },
    ...overrides,
  };
}

describe('ApiProductTransferOwnershipDialogComponent', () => {
  const defaultRole = fakeRole({ name: 'DEFAULT_ROLE', default: true });
  const poRole = fakeRole({ name: 'PRIMARY_OWNER', default: false });
  const role1 = fakeRole({ name: 'ROLE_1', default: false });
  const defaultRoles: Role[] = [poRole, role1, defaultRole];
  const closeSpy = jest.fn();

  let fixture: ComponentFixture<ApiProductTransferOwnershipDialogComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  function createComponent(dialogData: ApiProductOwnershipDialogData, primaryOwnerMode: string = 'HYBRID') {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, MatIconTestingModule, ApiProductTransferOwnershipDialogComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MAT_DIALOG_DATA, useValue: dialogData },
        { provide: MatDialogRef, useValue: { close: closeSpy } },
        {
          provide: Constants,
          useFactory: () => {
            const constants = { ...CONSTANTS_TESTING };
            set(constants, 'env.settings.apiProduct.primaryOwnerMode', primaryOwnerMode);
            return constants;
          },
        },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: { isFocusable: () => true, isTabbable: () => true },
    });

    fixture = TestBed.createComponent(ApiProductTransferOwnershipDialogComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('Hybrid mode', () => {
    it('should_show_three_toggle_buttons', async () => {
      createComponent({
        apiProduct: fakeApiProduct(),
        groups: [],
        roles: defaultRoles,
        members: [fakeMember({ id: '1', displayName: 'Joe', roles: [defaultRole] })],
      });

      const toggleGroup = await loader.getHarness(MatButtonToggleGroupHarness.with({ selector: '[formControlName="userOrGroup"]' }));
      const toggles = await toggleGroup.getToggles();
      expect(toggles.length).toBe(3);
      expect(await toggles[0].getText()).toContain('API Product member');
      expect(await toggles[1].getText()).toContain('Other user');
      expect(await toggles[2].getText()).toContain('Primary owner group');
    });

    it('should_transfer_ownership_to_api_product_member', async () => {
      createComponent({
        apiProduct: fakeApiProduct(),
        groups: [],
        roles: defaultRoles,
        members: [fakeMember({ id: 'member-1', displayName: 'Joe', roles: [defaultRole] })],
      });

      const toggleGroup = await loader.getHarness(MatButtonToggleGroupHarness.with({ selector: '[formControlName="userOrGroup"]' }));
      const memberToggle = await toggleGroup.getToggles({ text: /API Product member/ });
      await memberToggle[0].check();

      const userSelect = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="user"]' }));
      await userSelect.clickOptions({ text: 'Joe' });

      const roleSelect = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="roleId"]' }));
      expect(await roleSelect.getValueText()).toBe('DEFAULT_ROLE');
      await roleSelect.open();
      const roleOptions = await roleSelect.getOptions();
      expect(roleOptions.length).toBe(2);
      await roleSelect.clickOptions({ text: 'ROLE_1' });

      const transferBtn = await loader.getHarness(MatButtonHarness.with({ text: 'Transfer' }));
      expect(await transferBtn.isDisabled()).toBeFalsy();
      await transferBtn.click();

      expect(closeSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          isUserMode: true,
          transferOwnershipToUser: expect.objectContaining({
            newPrimaryOwnerId: 'member-1',
            currentPrimaryOwnerNewRole: 'ROLE_1',
            userType: 'USER',
          }),
        }),
      );
    });

    it('should_transfer_ownership_to_other_user', async () => {
      createComponent({
        apiProduct: fakeApiProduct(),
        groups: [],
        roles: defaultRoles,
        members: [],
      });

      const toggleGroup = await loader.getHarness(MatButtonToggleGroupHarness.with({ selector: '[formControlName="userOrGroup"]' }));
      const otherUserToggle = await toggleGroup.getToggles({ text: 'Other user' });
      await otherUserToggle[0].check();

      const userAutocomplete = await loader.getHarness(GioFormUserAutocompleteHarness);
      await userAutocomplete.setSearchText('Joe');
      respondToUserSearchRequest('Joe', [fakeSearchableUser({ displayName: 'Joe' })]);
      await userAutocomplete.selectOption({ text: 'Joe' });
      respondToUserSearchRequest('Joe', [fakeSearchableUser({ displayName: 'Joe' })]);

      const roleSelect = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="roleId"]' }));
      expect(await roleSelect.getValueText()).toBe('DEFAULT_ROLE');

      const transferBtn = await loader.getHarness(MatButtonHarness.with({ text: 'Transfer' }));
      expect(await transferBtn.isDisabled()).toBeFalsy();
      await transferBtn.click();

      expect(closeSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          isUserMode: true,
          transferOwnershipToUser: expect.objectContaining({
            newPrimaryOwnerId: expect.any(String),
            userReference: expect.any(String),
            currentPrimaryOwnerNewRole: 'DEFAULT_ROLE',
            userType: 'USER',
          }),
        }),
      );
    });

    it('should_transfer_ownership_to_group', async () => {
      createComponent({
        apiProduct: fakeApiProduct(),
        groups: [
          fakeGroup({ id: 'group1', name: 'Group 1', apiProductPrimaryOwner: 'apo-uuid' }),
          fakeGroup({ id: 'group2', name: 'Group Null', apiProductPrimaryOwner: null }),
        ],
        roles: defaultRoles,
        members: [fakeMember({ id: '1', displayName: 'Joe', roles: [defaultRole] })],
      });

      const toggleGroup = await loader.getHarness(MatButtonToggleGroupHarness.with({ selector: '[formControlName="userOrGroup"]' }));
      const groupToggle = await toggleGroup.getToggles({ text: 'Primary owner group' });
      await groupToggle[0].check();

      const groupSelect = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="groupId"]' }));
      await groupSelect.open();
      const options = await groupSelect.getOptions();
      expect(options.length).toBe(1);
      await groupSelect.clickOptions({ text: 'Group 1' });

      const transferBtn = await loader.getHarness(MatButtonHarness.with({ text: 'Transfer' }));
      expect(await transferBtn.isDisabled()).toBeFalsy();
      await transferBtn.click();

      expect(closeSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          isUserMode: false,
          transferOwnershipToGroup: expect.objectContaining({
            newPrimaryOwnerId: 'group1',
            userReference: null,
            currentPrimaryOwnerNewRole: 'DEFAULT_ROLE',
            userType: 'GROUP',
          }),
        }),
      );
    });

    it('should_exclude_primary_owner_from_member_list', async () => {
      createComponent({
        apiProduct: fakeApiProduct(),
        groups: [],
        roles: defaultRoles,
        members: [
          fakeMember({ id: 'po-1', displayName: 'Admin', roles: [poRole] }),
          fakeMember({ id: 'user-1', displayName: 'Joe', roles: [defaultRole] }),
        ],
      });

      const userSelect = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="user"]' }));
      await userSelect.open();
      const options = await userSelect.getOptions();
      expect(options.length).toBe(1);
      expect(await options[0].getText()).toBe('Joe');
    });

    it('should_exclude_primary_owner_from_role_options', async () => {
      createComponent({
        apiProduct: fakeApiProduct(),
        groups: [],
        roles: defaultRoles,
        members: [fakeMember({ id: '1', displayName: 'Joe', roles: [defaultRole] })],
      });

      const userSelect = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="user"]' }));
      await userSelect.clickOptions({ text: 'Joe' });

      const roleSelect = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="roleId"]' }));
      await roleSelect.open();
      const roleOptions = await roleSelect.getOptions();
      const roleTexts = await Promise.all(roleOptions.map(o => o.getText()));
      expect(roleTexts).not.toContain('PRIMARY_OWNER');
      expect(roleTexts).toEqual(expect.arrayContaining(['ROLE_1', 'DEFAULT_ROLE']));
    });

    it('should_show_warning_when_no_eligible_groups', async () => {
      createComponent({
        apiProduct: fakeApiProduct(),
        groups: [fakeGroup({ id: 'g1', name: 'No PO Group', apiProductPrimaryOwner: null })],
        roles: defaultRoles,
        members: [],
      });

      const toggleGroup = await loader.getHarness(MatButtonToggleGroupHarness.with({ selector: '[formControlName="userOrGroup"]' }));
      const groupToggle = await toggleGroup.getToggles({ text: 'Primary owner group' });
      await groupToggle[0].check();

      const warningBanner = fixture.nativeElement.querySelector('gio-banner-warning');
      expect(warningBanner).not.toBeNull();
      expect(warningBanner.textContent).toContain("can't set a group as primary owner");
    });

    it('should_reset_selection_when_switching_tabs', async () => {
      createComponent({
        apiProduct: fakeApiProduct(),
        groups: [fakeGroup({ id: 'group1', name: 'Group 1', apiProductPrimaryOwner: 'apo-uuid' })],
        roles: defaultRoles,
        members: [fakeMember({ id: '1', displayName: 'Joe', roles: [defaultRole] })],
      });

      const userSelect = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="user"]' }));
      await userSelect.clickOptions({ text: 'Joe' });

      const toggleGroup = await loader.getHarness(MatButtonToggleGroupHarness.with({ selector: '[formControlName="userOrGroup"]' }));
      const otherUserToggle = await toggleGroup.getToggles({ text: 'Other user' });
      await otherUserToggle[0].check();

      const transferBtn = await loader.getHarness(MatButtonHarness.with({ text: 'Transfer' }));
      expect(await transferBtn.isDisabled()).toBeTruthy();
    });
  });

  describe('Group mode', () => {
    it('should_only_show_group_select_without_toggles', async () => {
      createComponent(
        {
          apiProduct: fakeApiProduct(),
          groups: [
            fakeGroup({ id: 'group1', name: 'group1', apiProductPrimaryOwner: 'apo-uuid' }),
            fakeGroup({ id: 'group2', name: 'group2', apiProductPrimaryOwner: 'apo-uuid' }),
          ],
          roles: defaultRoles,
          members: [],
        },
        'GROUP',
      );

      const toggleGroups = await loader.getAllHarnesses(MatButtonToggleGroupHarness.with({ selector: '[formControlName="userOrGroup"]' }));
      expect(toggleGroups.length).toBe(0);

      const groupSelect = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="groupId"]' }));
      await groupSelect.open();
      const options = await groupSelect.getOptions();
      expect(await Promise.all(options.map(o => o.getText()))).toEqual(['group1', 'group2']);
    });
  });

  describe('User mode', () => {
    it('should_not_show_group_toggle', async () => {
      createComponent(
        {
          apiProduct: fakeApiProduct(),
          groups: [],
          roles: defaultRoles,
          members: [fakeMember({ id: '1', displayName: 'Joe', roles: [defaultRole] })],
        },
        'USER',
      );

      const toggleGroup = await loader.getHarness(MatButtonToggleGroupHarness.with({ selector: '[formControlName="userOrGroup"]' }));
      const toggles = await toggleGroup.getToggles();
      expect(toggles.length).toBe(2);
      expect(await toggles[0].getText()).toContain('API Product member');
      expect(await toggles[1].getText()).toContain('Other user');
    });
  });

  function respondToUserSearchRequest(searchTerm: string, searchableUsers: SearchableUser[]) {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.org.baseURL}/search/users?q=${searchTerm}`,
      })
      .flush(searchableUsers);
  }
});
