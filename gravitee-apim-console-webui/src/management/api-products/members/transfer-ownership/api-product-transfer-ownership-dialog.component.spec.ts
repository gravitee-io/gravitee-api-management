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
import { HttpTestingController } from '@angular/common/http/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { HarnessLoader } from '@angular/cdk/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { set } from 'lodash';

import {
  ApiProductTransferOwnershipDialogComponent,
  ApiProductOwnershipDialogData,
} from './api-product-transfer-ownership-dialog.component';
import { ApiProductTransferOwnershipDialogHarness } from './api-product-transfer-ownership-dialog.component.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
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
  let harness: ApiProductTransferOwnershipDialogHarness;
  let httpTestingController: HttpTestingController;

  async function createComponent(dialogData: ApiProductOwnershipDialogData, primaryOwnerMode: string = 'HYBRID') {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, ApiProductTransferOwnershipDialogComponent],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: dialogData },
        { provide: MatDialogRef, useValue: { close: closeSpy } },
      ],
    })
      .overrideProvider(Constants, {
        useFactory: () => {
          const constants = { ...CONSTANTS_TESTING };
          set(constants, 'env.settings.apiProduct.primaryOwnerMode', primaryOwnerMode);
          return constants;
        },
      })
      .overrideProvider(InteractivityChecker, {
        useValue: { isFocusable: () => true, isTabbable: () => true },
      });

    fixture = TestBed.createComponent(ApiProductTransferOwnershipDialogComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiProductTransferOwnershipDialogHarness);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController?.verify();
  });

  function flushNextOrgUserSearchRequest(searchTerm: string, searchableUsers: SearchableUser[]): void {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.org.baseURL}/search/users?q=${searchTerm}`,
      })
      .flush(searchableUsers);
  }

  async function searchAutocompleteThenSelectUser(
    userAutocomplete: GioFormUserAutocompleteHarness,
    searchTerm: string,
    user: SearchableUser,
    optionLabel: string = user.displayName ?? '',
  ): Promise<void> {
    await userAutocomplete.setSearchText(searchTerm);
    flushNextOrgUserSearchRequest(searchTerm, [user]);
    await userAutocomplete.selectOption({ text: optionLabel });
    flushNextOrgUserSearchRequest(searchTerm, [user]);
  }

  describe('Hybrid mode', () => {
    it('should_show_three_toggle_buttons', async () => {
      await createComponent({
        apiProduct: fakeApiProduct(),
        groups: [],
        roles: defaultRoles,
        members: [fakeMember({ id: '1', displayName: 'Joe', roles: [defaultRole] })],
      });

      const toggleGroup = await harness.getUserOrGroupToggleGroup();
      const toggles = await toggleGroup.getToggles();
      expect(toggles.length).toBe(3);
      expect(await toggles[0].getText()).toContain('API Product member');
      expect(await toggles[1].getText()).toContain('Other user');
      expect(await toggles[2].getText()).toContain('Primary owner group');
    });

    it('should_transfer_ownership_to_api_product_member', async () => {
      await createComponent({
        apiProduct: fakeApiProduct(),
        groups: [],
        roles: defaultRoles,
        members: [fakeMember({ id: 'member-1', displayName: 'Joe', roles: [defaultRole] })],
      });

      const toggleGroup = await harness.getUserOrGroupToggleGroup();
      const memberToggle = await toggleGroup.getToggles({ text: /Product member/ });
      await memberToggle[0].check();

      const userSelect = await harness.getUserSelect();
      await userSelect.clickOptions({ text: 'Joe' });

      const roleSelect = await harness.getRoleSelect();
      expect(await roleSelect.getValueText()).toBe('DEFAULT_ROLE');
      await roleSelect.open();
      const roleOptions = await roleSelect.getOptions();
      expect(roleOptions.length).toBe(2);
      await roleSelect.clickOptions({ text: 'ROLE_1' });

      const transferBtn = await harness.getTransferButton();
      expect(await transferBtn.isDisabled()).toBe(false);
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
      await createComponent({
        apiProduct: fakeApiProduct(),
        groups: [],
        roles: defaultRoles,
        members: [],
      });

      const toggleGroup = await harness.getUserOrGroupToggleGroup();
      const otherUserToggle = await toggleGroup.getToggles({ text: 'Other user' });
      await otherUserToggle[0].check();

      const userAutocomplete = await loader.getHarness(GioFormUserAutocompleteHarness);
      await searchAutocompleteThenSelectUser(userAutocomplete, 'Joe', fakeSearchableUser({ displayName: 'Joe' }), 'Joe');

      const roleSelect = await harness.getRoleSelect();
      expect(await roleSelect.getValueText()).toBe('DEFAULT_ROLE');

      const transferBtn = await harness.getTransferButton();
      expect(await transferBtn.isDisabled()).toBe(false);
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
      await createComponent({
        apiProduct: fakeApiProduct(),
        groups: [
          fakeGroup({ id: 'group1', name: 'Group 1', apiProductPrimaryOwner: 'apo-uuid' }),
          fakeGroup({ id: 'group2', name: 'Group Null', apiProductPrimaryOwner: null }),
        ],
        roles: defaultRoles,
        members: [fakeMember({ id: '1', displayName: 'Joe', roles: [defaultRole] })],
      });

      const toggleGroup = await harness.getUserOrGroupToggleGroup();
      const groupToggle = await toggleGroup.getToggles({ text: 'Primary owner group' });
      await groupToggle[0].check();

      const groupSelect = await harness.getGroupSelect();
      await groupSelect.open();
      const options = await groupSelect.getOptions();
      expect(options.length).toBe(1);
      await groupSelect.clickOptions({ text: 'Group 1' });

      const transferBtn = await harness.getTransferButton();
      expect(await transferBtn.isDisabled()).toBe(false);
      await transferBtn.click();

      expect(closeSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          isUserMode: false,
          transferOwnershipToGroup: expect.objectContaining({
            newPrimaryOwnerId: 'group1',
            currentPrimaryOwnerNewRole: 'DEFAULT_ROLE',
            userType: 'GROUP',
          }),
        }),
      );
    });

    it('should_exclude_primary_owner_from_member_list', async () => {
      await createComponent({
        apiProduct: fakeApiProduct(),
        groups: [],
        roles: defaultRoles,
        members: [
          fakeMember({ id: 'po-1', displayName: 'Admin', roles: [poRole] }),
          fakeMember({ id: 'user-1', displayName: 'Joe', roles: [defaultRole] }),
        ],
      });

      const userSelect = await harness.getUserSelect();
      await userSelect.open();
      const options = await userSelect.getOptions();
      expect(options.length).toBe(1);
      expect(await options[0].getText()).toBe('Joe');
    });

    it('should_exclude_primary_owner_from_role_options', async () => {
      await createComponent({
        apiProduct: fakeApiProduct(),
        groups: [],
        roles: defaultRoles,
        members: [fakeMember({ id: '1', displayName: 'Joe', roles: [defaultRole] })],
      });

      const userSelect = await harness.getUserSelect();
      await userSelect.clickOptions({ text: 'Joe' });

      const roleSelect = await harness.getRoleSelect();
      await roleSelect.open();
      const roleOptions = await roleSelect.getOptions();
      const roleTexts = await Promise.all(roleOptions.map(o => o.getText()));
      expect(roleTexts).not.toContain('PRIMARY_OWNER');
      expect(roleTexts).toEqual(expect.arrayContaining(['ROLE_1', 'DEFAULT_ROLE']));
    });

    it('should_show_warning_when_no_eligible_groups', async () => {
      await createComponent({
        apiProduct: fakeApiProduct(),
        groups: [fakeGroup({ id: 'g1', name: 'No PO Group', apiProductPrimaryOwner: null })],
        roles: defaultRoles,
        members: [],
      });

      const toggleGroup = await harness.getUserOrGroupToggleGroup();
      const groupToggle = await toggleGroup.getToggles({ text: 'Primary owner group' });
      await groupToggle[0].check();

      const warningBanner = await harness.getOptionalGroupsEligibilityBanner();
      expect(warningBanner).not.toBeNull();
      expect(await warningBanner!.text()).toContain("can't set a group as primary owner");
    });

    it('should_show_irreversible_banner_when_form_is_valid', async () => {
      await createComponent({
        apiProduct: fakeApiProduct(),
        groups: [],
        roles: defaultRoles,
        members: [fakeMember({ id: 'member-1', displayName: 'Joe', roles: [defaultRole] })],
      });

      expect(await harness.getOptionalIrreversibleBanner()).toBeNull();

      const userSelect = await harness.getUserSelect();
      await userSelect.clickOptions({ text: 'Joe' });

      const irreversibleBanner = await harness.getOptionalIrreversibleBanner();
      expect(irreversibleBanner).not.toBeNull();
      expect(await irreversibleBanner!.text()).toContain('cannot be undone');
    });

    it('should_reset_selection_when_switching_tabs', async () => {
      await createComponent({
        apiProduct: fakeApiProduct(),
        groups: [fakeGroup({ id: 'group1', name: 'Group 1', apiProductPrimaryOwner: 'apo-uuid' })],
        roles: defaultRoles,
        members: [fakeMember({ id: '1', displayName: 'Joe', roles: [defaultRole] })],
      });

      const userSelect = await harness.getUserSelect();
      await userSelect.clickOptions({ text: 'Joe' });

      const toggleGroup = await harness.getUserOrGroupToggleGroup();
      const otherUserToggle = await toggleGroup.getToggles({ text: 'Other user' });
      await otherUserToggle[0].check();

      const transferBtn = await harness.getTransferButton();
      expect(await transferBtn.isDisabled()).toBe(true);
    });
  });

  describe('Group mode', () => {
    it('should_only_show_group_select_without_toggles', async () => {
      await createComponent(
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

      const toggleGroups = await harness.getOptionalUserOrGroupToggleGroups();
      expect(toggleGroups.length).toBe(0);

      const groupSelect = await harness.getGroupSelect();
      await groupSelect.open();
      const options = await groupSelect.getOptions();
      expect(await Promise.all(options.map(o => o.getText()))).toEqual(['group1', 'group2']);
    });
  });

  describe('User mode', () => {
    it('should_not_show_group_toggle', async () => {
      await createComponent(
        {
          apiProduct: fakeApiProduct(),
          groups: [],
          roles: defaultRoles,
          members: [fakeMember({ id: '1', displayName: 'Joe', roles: [defaultRole] })],
        },
        'USER',
      );

      const toggleGroup = await harness.getUserOrGroupToggleGroup();
      const toggles = await toggleGroup.getToggles();
      expect(toggles.length).toBe(2);
      expect(await toggles[0].getText()).toContain('Product member');
      expect(await toggles[1].getText()).toContain('Other user');
    });
  });
});
