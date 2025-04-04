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
import { SearchableUser } from 'src/entities/user/searchableUser';

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { of } from 'rxjs';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatInputHarness } from '@angular/material/input/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { MatMenuHarness, MatMenuItemHarness } from '@angular/material/menu/testing';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatTabGroupHarness } from '@angular/material/tabs/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatAutocompleteHarness } from '@angular/material/autocomplete/testing';

import { GroupComponent } from './group.component';

import { Group } from '../../../../entities/group/group';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { Member } from '../../../../entities/management-api-v2';
import { Invitation } from '../../../../entities/invitation/invitation';
import { Role } from '../../../../entities/role/role';
import { EnvironmentSettingsService } from '../../../../services-ngx/environment-settings.service';
import { UsersService } from '../../../../services-ngx/users.service';
import { GroupMembership } from '../../../../entities/group/groupMember';

describe('GroupComponent', () => {
  let fixture: ComponentFixture<GroupComponent>;
  let component: GroupComponent;
  let httpTestingController: HttpTestingController;
  let rootLoader: HarnessLoader;
  let harnessLoader: HarnessLoader;
  let router: Router;

  const SETTINGS_SNAPSHOT = {
    api: {
      primaryOwnerMode: 'USER',
    },
  };

  const GROUP: Group = {
    disable_membership_notifications: false,
    email_invitation: false,
    event_rules: [],
    lock_api_role: false,
    lock_application_role: false,
    max_invitation: 2,
    name: 'Group 1',
    roles: {},
    system_invitation: false,
    id: '1',
  };

  const GROUP_MEMBERS: Member[] = [
    {
      id: '1',
      displayName: 'Test Member 1',
      roles: [
        { name: 'OWNER', scope: 'API' },
        { name: 'OWNER', scope: 'APPLICATION' },
        {
          name: 'OWNER',
          scope: 'INTEGRATION',
        },
      ],
    },
  ];

  const USERS: SearchableUser[] = [
    {
      id: '1',
      reference: 'testmember1',
      email: 'testmember1@xxx.com',
      displayName: 'Test Member 1',
    },
    {
      id: '2',
      reference: 'testmember2',
      email: 'testmember2@xxx.com',
      displayName: 'Test Member 2',
    },
  ];

  const ROLES: Role[] = [
    { id: '1', name: 'OWNER', scope: 'API' },
    { id: '2', name: 'PRIMARY_OWNER', scope: 'API' },
    {
      id: '3',
      name: 'REVIEWER',
      scope: 'API',
    },
    { id: '4', name: 'USER', scope: 'API' },
  ];

  const GROUP_APIS = [{ name: 'Test API 1', visibility: 'PRIVATE' }];

  const GROUP_APPLICATIONS = [{ name: 'Test Application 1' }];

  const INVITATIONS: Invitation[] = [
    {
      id: '1',
      email: 'temp@temp.com',
      api_role: 'OWNER',
      application_role: 'OWNER',
      reference_id: '1',
      reference_type: 'GROUP',
    },
  ];

  const init = async (groupId: string, snapshot = SETTINGS_SNAPSHOT, users = USERS) => {
    await TestBed.configureTestingModule({
      declarations: [],
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: ['environment-group-u', 'environment-group-d', 'environment-group-c'],
        },
        {
          provide: ActivatedRoute,
          useValue: { params: of({ groupId: groupId }) },
        },
        { provide: EnvironmentSettingsService, useValue: { getSnapshot: () => snapshot } },
        { provide: UsersService, useValue: { search: () => of(users) } },
      ],
      imports: [GroupComponent, GioTestingModule, NoopAnimationsModule],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true,
          isTabbable: () => true,
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(GroupComponent);
    component = fixture.componentInstance;
    httpTestingController = TestBed.inject(HttpTestingController);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('Create group', () => {
    beforeEach(async () => {
      await init('new');
      fixture.detectChanges();
    });

    it('should initialize form with default values', async () => {
      expectGetDefaultRoles();
      expect(component.groupForm.getRawValue()).toEqual({
        name: null,
        defaultAPIRole: null,
        defaultApplicationRole: null,
        maxNumberOfMembers: null,
        shouldAllowInvitationViaSearch: false,
        shouldAllowInvitationViaEmail: false,
        canAdminChangeAPIRole: true,
        canAdminChangeApplicationRole: true,
        shouldNotifyWhenMemberAdded: true,
        shouldAddToNewAPIs: false,
        shouldAddToNewApplications: false,
      });
    });

    it('should submit form to create group', async () => {
      expectGetDefaultRoles();
      const spy = jest.spyOn(router, 'navigate');
      await getNameInput().then((input) => input.setValue('Group 1'));
      const saveBar = await getSaveBar();
      expect(await saveBar.isVisible()).toEqual(true);
      expect(await saveBar.isSubmitButtonVisible()).toEqual(true);
      await saveBar.clickSubmit();
      expectPostGroup(
        {
          disable_membership_notifications: false,
          email_invitation: false,
          event_rules: [],
          lock_api_role: false,
          lock_application_role: false,
          max_invitation: null,
          name: 'Group 1',
          roles: {},
          system_invitation: false,
        },
        GROUP,
      );
      expect(spy).toHaveBeenCalledTimes(1);
      expect(spy).toHaveBeenCalledWith(['..', GROUP.id], expect.anything());
    });
  });

  describe('Update group', () => {
    beforeEach(async () => {
      await init(GROUP.id);
      expectGetGroup();
      expect(component.mode).toEqual('edit');
      fixture.detectChanges();
    });

    it('should initialize form with group properties', async () => {
      expectGetDefaultRoles();
      expectGetGroupMembers();
      expectGetGroupAPIs();
      expectGetGroupApplications();
      expect(component.groupForm.getRawValue()).toEqual({
        canAdminChangeAPIRole: !GROUP.lock_api_role,
        canAdminChangeApplicationRole: !GROUP.lock_application_role,
        defaultAPIRole: null,
        defaultApplicationRole: null,
        maxNumberOfMembers: GROUP.max_invitation,
        name: GROUP.name,
        shouldAddToNewAPIs: false,
        shouldAddToNewApplications: false,
        shouldAllowInvitationViaEmail: GROUP.email_invitation,
        shouldAllowInvitationViaSearch: GROUP.system_invitation,
        shouldNotifyWhenMemberAdded: !GROUP.disable_membership_notifications,
      });
    });

    it('should submit form to update group', async () => {
      expectGetDefaultRoles();
      expectGetGroupMembers();
      expectGetGroupAPIs();
      expectGetGroupApplications();
      await getNameInput().then((input) => input.setValue('Test Group 1'));
      const saveBar = await getSaveBar();
      expect(await saveBar.isVisible()).toEqual(true);
      expect(await saveBar.isSubmitButtonVisible()).toEqual(true);
      await saveBar.clickSubmit();
      expectPutGroup({ ...GROUP, name: 'Test Group 1' });
    });
  });

  describe('Update existing APIs and Applications', () => {
    beforeEach(async () => {
      await init(GROUP.id);
      expectGetGroup();
      expect(component.mode).toEqual('edit');
      fixture.detectChanges();
    });

    it('should add group to existing APIs', async () => {
      expectGetDefaultRoles();
      expectGetGroupMembers();
      expectGetGroupAPIs();
      expectGetGroupApplications();
      const buttonHarness = await getButtonByTooltipText('Click to add this group to all existing APIs');
      await buttonHarness.click();
      const dialogHarness = await rootLoader.getHarness(MatDialogHarness);
      const confirmButtonHarness = await dialogHarness.getHarness(MatButtonHarness.with({ text: 'Add' }));
      await confirmButtonHarness.click();
      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/configuration/groups/${GROUP.id}/memberships?type=api`,
        method: 'POST',
      });
      expect(req.request.body).toEqual({});
      req.flush(GROUP);
    });

    it('should add group to existing applications', async () => {
      expectGetDefaultRoles();
      expectGetGroupMembers();
      expectGetGroupAPIs();
      expectGetGroupApplications();
      const buttonHarness = await getButtonByTooltipText('Click to add this group to all existing applications');
      await buttonHarness.click();
      const dialogHarness = await rootLoader.getHarness(MatDialogHarness);
      const confirmButtonHarness = await dialogHarness.getHarness(MatButtonHarness.with({ text: 'Add' }));
      await confirmButtonHarness.click();
      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/configuration/groups/${GROUP.id}/memberships?type=application`,
        method: 'POST',
      });
      expect(req.request.body).toEqual({});
      req.flush(GROUP);
    });
  });

  describe('Group Members', () => {
    beforeEach(async () => {
      await init(GROUP.id);
      expectGetGroup();
      expect(component.mode).toEqual('edit');
      fixture.detectChanges();
    });

    it('should display list of group members', async () => {
      expectGetDefaultRoles();
      expectGetGroupMembers();
      expectGetGroupAPIs();
      expectGetGroupApplications();
      const tableHarness = await harnessLoader.getHarness(MatTableHarness.with({ selector: '#membersDataTable' }));
      const rows = await tableHarness.getRows();
      expect(rows.length).toEqual(1);
      const cell = await rows[0].getCells({ columnName: 'name' }).then((cells) => cells[0]);
      const text = await cell.getText();
      expect(text).toEqual('Test Member 1');
    });

    it('should delete member', async () => {
      expectGetDefaultRoles();
      expectGetGroupMembers();
      expectGetGroupAPIs();
      expectGetGroupApplications();
      const tableHarness = await harnessLoader.getHarness(MatTableHarness.with({ selector: '#membersDataTable' }));
      const rows = await tableHarness.getRows();
      const cell = await rows[0].getCells({ columnName: 'actions' }).then((cells) => cells[0]);
      const deleteButton = await cell.getHarness(MatButtonHarness.with({ selector: '[mattooltip="Remove member from group"]' }));
      await deleteButton.click();
      const dialogHarness = await rootLoader.getHarness(MatDialogHarness);
      const confirmButtonHarness = await dialogHarness.getHarness(MatButtonHarness.with({ text: 'Delete' }));
      await confirmButtonHarness.click();
      expectDeleteMember('1');
    });

    it('should update member', async () => {
      expectGetDefaultRoles();
      expectGetGroupMembers();
      expectGetGroupAPIs();
      expectGetGroupApplications();
      const tableHarness = await harnessLoader.getHarness(MatTableHarness.with({ selector: '#membersDataTable' }));
      const rows = await tableHarness.getRows();
      const cell = await rows[0].getCells({ columnName: 'actions' }).then((cells) => cells[0]);
      const editButton = await cell.getHarness(MatButtonHarness.with({ selector: '[mattooltip="Modify member settings"]' }));
      await editButton.click();
      const dialogHarness = await rootLoader.getHarness(MatDialogHarness);
      const matSelectHarnesses = await dialogHarness.getAllHarnesses(MatSelectHarness);
      expect(matSelectHarnesses.length).toEqual(3);
      await matSelectHarnesses[0].open();
      const apiRoleOptions = await matSelectHarnesses[0].getOptions();
      await apiRoleOptions[2].click();
      await matSelectHarnesses[1].open();
      const applicationRoleOptions = await matSelectHarnesses[1].getOptions();
      await applicationRoleOptions[0].click();
      await matSelectHarnesses[2].open();
      const integrationRoleOptions = await matSelectHarnesses[2].getOptions();
      await integrationRoleOptions[0].click();
      const confirmButtonHarness = await dialogHarness.getHarness(MatButtonHarness.with({ text: 'Save' }));
      await confirmButtonHarness.click();
      expectAddOrUpdateMembership('1', 'testmember1');
    });
  });

  describe('Invitations', () => {
    beforeEach(async () => {
      await init(GROUP.id);
      expectGetGroup();
      expect(component.mode).toEqual('edit');
      fixture.detectChanges();
    });

    it('should display list of invitations', async () => {
      expectGetDefaultRoles();
      expectGetGroupMembers();
      expectGetGroupAPIs();
      expectGetGroupApplications();
      const tabGroupHarness = await harnessLoader.getHarness(MatTabGroupHarness);
      const tabs = await tabGroupHarness.getTabs();
      await tabs[1].select();
      expectGetInvitations();
      const tableHarness = await harnessLoader.getHarness(MatTableHarness.with({ selector: '#invitationsDataTable' }));
      const rows = await tableHarness.getRows();
      expect(rows.length).toEqual(1);
      const cell = await rows[0].getCells({ columnName: 'guestEmail' }).then((cells) => cells[0]);
      const text = await cell.getText();
      expect(text).toEqual('temp@temp.com');
    });

    it('should delete invitation', async () => {
      expectGetDefaultRoles();
      expectGetGroupMembers();
      expectGetGroupAPIs();
      expectGetGroupApplications();
      const tabGroupHarness = await harnessLoader.getHarness(MatTabGroupHarness);
      const tabs = await tabGroupHarness.getTabs();
      await tabs[1].select();
      expectGetInvitations();
      const tableHarness = await harnessLoader.getHarness(MatTableHarness.with({ selector: '#invitationsDataTable' }));
      const rows = await tableHarness.getRows();
      const cell = await rows[0].getCells({ columnName: 'guestActions' }).then((cells) => cells[0]);
      const deleteButton = await cell.getHarness(MatButtonHarness.with({ selector: '[mattooltip="Delete invitation"]' }));
      await deleteButton.click();
      const dialogHarness = await rootLoader.getHarness(MatDialogHarness);
      const confirmButtonHarness = await dialogHarness.getHarness(MatButtonHarness.with({ text: 'Delete' }));
      await confirmButtonHarness.click();
      expectDeleteInvitation('1');
    });
  });

  describe('Group Applications', () => {
    beforeEach(async () => {
      await init(GROUP.id);
      expectGetGroup();
      expect(component.mode).toEqual('edit');
      fixture.detectChanges();
    });

    it('should display list of associated applications', async () => {
      expectGetDefaultRoles();
      expectGetGroupMembers();
      expectGetGroupAPIs();
      expectGetGroupApplications();
      const tableHarness = await harnessLoader.getHarness(MatTableHarness.with({ selector: '#groupApplicationsDataTable' }));
      const rows = await tableHarness.getRows();
      expect(rows.length).toEqual(1);
      const cell = await rows[0].getCells({ columnName: 'applicationName' }).then((cells) => cells[0]);
      const text = await cell.getText();
      expect(text).toEqual('Test Application 1');
    });
  });

  describe('Group APIs', () => {
    beforeEach(async () => {
      await init(GROUP.id);
      expectGetGroup();
      expect(component.mode).toEqual('edit');
      fixture.detectChanges();
    });

    it('should display list of associated APIs', async () => {
      expectGetDefaultRoles();
      expectGetGroupMembers();
      expectGetGroupAPIs();
      expectGetGroupApplications();
      const tableHarness = await harnessLoader.getHarness(MatTableHarness.with({ selector: '#groupApisDataTable' }));
      const rows = await tableHarness.getRows();
      expect(rows.length).toEqual(1);
      const cell = await rows[0].getCells({ columnName: 'apiName' }).then((cells) => cells[0]);
      const text = await cell.getText();
      expect(text).toEqual('Test API 1 Private');
    });
  });

  describe('Search and invite users', () => {
    it('should initialize api and application default roles', async () => {
      await init(GROUP.id);
      expectGetGroup({
        disable_membership_notifications: false,
        email_invitation: false,
        event_rules: [],
        lock_api_role: false,
        lock_application_role: false,
        max_invitation: 10,
        manageable: true,
        name: 'Group 1',
        roles: { API: 'USER', APPLICATION: 'OWNER' },
        system_invitation: true,
        id: '1',
      });
      expect(component.mode).toEqual('edit');
      fixture.detectChanges();
      expectGetDefaultRoles();
      expectGetGroupMembers();
      expectGetGroupAPIs();
      expectGetGroupApplications();
      const buttonHarness = await getButtonByTooltipText('Search and invite users to the group');
      await buttonHarness.click();
      const menuHarness = await harnessLoader.getHarness(MatMenuHarness);
      const userSearchMenuItem = await menuHarness.getHarness(
        MatMenuItemHarness.with({ selector: '[aria-label="Click to invite user via search"]' }),
      );
      await userSearchMenuItem.click();
      const dialogHarness = await rootLoader.getHarness(MatDialogHarness);
      const matSelectHarnesses = await dialogHarness.getAllHarnesses(MatSelectHarness);
      expect(matSelectHarnesses.length).toEqual(3);
      await matSelectHarnesses[0].open();
      const apiRoleOptions = await matSelectHarnesses[0].getOptions();
      expect(await apiRoleOptions[3].isSelected()).toEqual(true);
      await matSelectHarnesses[1].open();
      const applicationRoleOptions = await matSelectHarnesses[1].getOptions();
      expect(await applicationRoleOptions[0].isSelected()).toEqual(true);
    });

    it('should disable add users button when maximum allowed members have been added', async () => {
      await init(GROUP.id);
      expectGetGroup();
      expect(component.mode).toEqual('edit');
      fixture.detectChanges();
      expectGetDefaultRoles();
      expectGetGroupMembers([
        {
          id: '1',
          displayName: 'Test Member 1',
          roles: [
            { name: 'OWNER', scope: 'API' },
            { name: 'OWNER', scope: 'APPLICATION' },
            {
              name: 'OWNER',
              scope: 'INTEGRATION',
            },
          ],
        },
        {
          id: '2',
          displayName: 'Test Member 2',
          roles: [
            { name: 'OWNER', scope: 'API' },
            { name: 'OWNER', scope: 'APPLICATION' },
            {
              name: 'OWNER',
              scope: 'INTEGRATION',
            },
          ],
        },
      ]);
      expectGetGroupAPIs();
      expectGetGroupApplications();
      const buttonHarness = await getButtonByTooltipText('Search and invite users to the group');
      const disabled = await buttonHarness.isDisabled();
      expect(disabled).toEqual(true);
    });

    it('should display menu to search and invite users', async () => {
      await init(GROUP.id);
      expectGetGroup();
      expect(component.mode).toEqual('edit');
      fixture.detectChanges();
      expectGetDefaultRoles();
      expectGetGroupMembers();
      expectGetGroupAPIs();
      expectGetGroupApplications();
      const buttonHarness = await getButtonByTooltipText('Search and invite users to the group');
      await buttonHarness.click();
      const menuHarness = await harnessLoader.getHarness(MatMenuHarness);
      const menuItems = await menuHarness.getItems();
      expect(menuItems.length).toEqual(2);
    });

    it('should disable user search when system invitation is disabled', async () => {
      await init(GROUP.id);
      expectGetGroup({
        disable_membership_notifications: false,
        email_invitation: true,
        event_rules: [],
        lock_api_role: false,
        lock_application_role: false,
        max_invitation: 10,
        name: 'Group 1',
        roles: {},
        system_invitation: false,
        manageable: true,
        id: '1',
      });
      expect(component.mode).toEqual('edit');
      fixture.detectChanges();
      expectGetDefaultRoles();
      expectGetGroupMembers();
      expectGetGroupAPIs();
      expectGetGroupApplications();
      const buttonHarness = await getButtonByTooltipText('Search and invite users to the group');
      await buttonHarness.click();
      const menuHarness = await harnessLoader.getHarness(MatMenuHarness);
      const userSearchMenuItem = await menuHarness.getHarness(
        MatMenuItemHarness.with({ selector: '[aria-label="Click to invite user via search"]' }),
      );
      const disabled = await userSearchMenuItem.isDisabled();
      expect(disabled).toEqual(true);
    });

    it('should disable email invite when email invitation is disabled', async () => {
      await init(GROUP.id);
      expectGetGroup({
        disable_membership_notifications: false,
        email_invitation: false,
        event_rules: [],
        lock_api_role: false,
        lock_application_role: false,
        max_invitation: 10,
        name: 'Group 1',
        roles: {},
        system_invitation: true,
        manageable: true,
        id: '1',
      });
      expect(component.mode).toEqual('edit');
      fixture.detectChanges();
      expectGetDefaultRoles();
      expectGetGroupMembers();
      expectGetGroupAPIs();
      expectGetGroupApplications();
      const buttonHarness = await getButtonByTooltipText('Search and invite users to the group');
      await buttonHarness.click();
      const menuHarness = await harnessLoader.getHarness(MatMenuHarness);
      const userSearchMenuItem = await menuHarness.getHarness(
        MatMenuItemHarness.with({ selector: '[aria-label="Click to invite user via email"]' }),
      );
      const disabled = await userSearchMenuItem.isDisabled();
      expect(disabled).toEqual(true);
    });

    it('should disable user search when maximum allowed invitations limit reached', async () => {
      await init(GROUP.id);
      expectGetGroup({
        disable_membership_notifications: false,
        email_invitation: false,
        event_rules: [],
        lock_api_role: false,
        lock_application_role: false,
        max_invitation: 2,
        manageable: true,
        name: 'Group 1',
        roles: {},
        system_invitation: true,
        id: '1',
      });
      expect(component.mode).toEqual('edit');
      fixture.detectChanges();
      expectGetDefaultRoles();
      expectGetGroupMembers();
      expectGetGroupAPIs();
      expectGetGroupApplications();
      const buttonHarness = await getButtonByTooltipText('Search and invite users to the group');
      await buttonHarness.click();
      const menuHarness = await harnessLoader.getHarness(MatMenuHarness);
      const userSearchMenuItem = await menuHarness.getHarness(
        MatMenuItemHarness.with({ selector: '[aria-label="Click to invite user via search"]' }),
      );
      await userSearchMenuItem.click();
      await rootLoader.getHarness(MatDialogHarness);
      const autoCompleteHarness = await rootLoader.getHarness(MatAutocompleteHarness);
      expect(await autoCompleteHarness.isDisabled()).toEqual(false);
      await autoCompleteHarness.enterText('test');
      const searchResults = await autoCompleteHarness.getOptions();
      await searchResults[0].click();
      expect(await autoCompleteHarness.isDisabled()).toEqual(true);
    });

    it('should search and add users', async () => {
      await init(GROUP.id);
      expectGetGroup({
        disable_membership_notifications: false,
        email_invitation: false,
        event_rules: [],
        lock_api_role: false,
        lock_application_role: false,
        max_invitation: 10,
        manageable: true,
        name: 'Group 1',
        roles: {},
        system_invitation: true,
        id: '1',
      });
      expect(component.mode).toEqual('edit');
      fixture.detectChanges();
      expectGetDefaultRoles();
      expectGetGroupMembers();
      expectGetGroupAPIs();
      expectGetGroupApplications();
      const buttonHarness = await getButtonByTooltipText('Search and invite users to the group');
      await buttonHarness.click();
      const menuHarness = await harnessLoader.getHarness(MatMenuHarness);
      const userSearchMenuItem = await menuHarness.getHarness(
        MatMenuItemHarness.with({ selector: '[aria-label="Click to invite user via search"]' }),
      );
      await userSearchMenuItem.click();
      const dialogHarness = await rootLoader.getHarness(MatDialogHarness);
      const matSelectHarnesses = await dialogHarness.getAllHarnesses(MatSelectHarness);
      expect(matSelectHarnesses.length).toEqual(3);
      await matSelectHarnesses[0].open();
      const apiRoleOptions = await matSelectHarnesses[0].getOptions();
      await apiRoleOptions[2].click();
      await matSelectHarnesses[1].open();
      const applicationRoleOptions = await matSelectHarnesses[1].getOptions();
      await applicationRoleOptions[0].click();
      await matSelectHarnesses[2].open();
      const integrationRoleOptions = await matSelectHarnesses[2].getOptions();
      await integrationRoleOptions[0].click();
      const autoCompleteHarness = await rootLoader.getHarness(MatAutocompleteHarness);
      await autoCompleteHarness.enterText('test');
      const searchResults = await autoCompleteHarness.getOptions();
      await searchResults[0].click();
      const confirmButtonHarness = await dialogHarness.getHarness(MatButtonHarness.with({ text: 'Add Users' }));
      await confirmButtonHarness.click();
      expectAddOrUpdateMembership('2', 'testmember2');
    });

    it('should invite by email', async () => {
      await init(GROUP.id);
      expectGetGroup({
        disable_membership_notifications: false,
        email_invitation: true,
        event_rules: [],
        lock_api_role: false,
        lock_application_role: false,
        max_invitation: 10,
        manageable: true,
        name: 'Group 1',
        roles: {},
        system_invitation: true,
        id: '1',
      });
      expect(component.mode).toEqual('edit');
      fixture.detectChanges();
      expectGetDefaultRoles();
      expectGetGroupMembers();
      expectGetGroupAPIs();
      expectGetGroupApplications();
      const buttonHarness = await getButtonByTooltipText('Search and invite users to the group');
      await buttonHarness.click();
      const menuHarness = await harnessLoader.getHarness(MatMenuHarness);
      const emailInvitationMenuItem = await menuHarness.getHarness(
        MatMenuItemHarness.with({ selector: '[aria-label="Click to invite user via email"]' }),
      );
      await emailInvitationMenuItem.click();
      const dialogHarness = await rootLoader.getHarness(MatDialogHarness);
      const matSelectHarnesses = await dialogHarness.getAllHarnesses(MatSelectHarness);
      expect(matSelectHarnesses.length).toEqual(2);
      await matSelectHarnesses[0].open();
      const apiRoleOptions = await matSelectHarnesses[0].getOptions();
      await apiRoleOptions[2].click();
      await matSelectHarnesses[1].open();
      const applicationRoleOptions = await matSelectHarnesses[1].getOptions();
      await applicationRoleOptions[0].click();
      const inputHarness = await rootLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="email"]' }));
      await inputHarness.setValue('temp@temp.com');
      const confirmButtonHarness = await dialogHarness.getHarness(MatButtonHarness.with({ text: 'Send Invitation' }));
      await confirmButtonHarness.click();
      expectPostInvitation();
    });
  });

  async function getNameInput(): Promise<MatInputHarness> {
    return await harnessLoader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
  }

  async function getSaveBar(): Promise<GioSaveBarHarness> {
    return await rootLoader.getHarness(GioSaveBarHarness);
  }

  async function getButtonByTooltipText(tooltipText: string): Promise<MatButtonHarness> {
    return await harnessLoader.getHarness(MatButtonHarness.with({ selector: `[mattooltip="${tooltipText}"]` }));
  }

  function expectGetDefaultRoles() {
    expectGetRolesList('API');
    expectGetRolesList('APPLICATION', [
      { id: '5', name: 'OWNER', scope: 'APPLICATION' },
      {
        id: '7',
        name: 'USER',
        scope: 'APPLICATION',
      },
    ]);
    expectGetRolesList('INTEGRATION', [{ id: '6', name: 'OWNER', scope: 'INTEGRATION' }]);
  }

  function expectGetRolesList(type: string, roles: Role[] = ROLES) {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/rolescopes/${type}/roles`).flush(roles);
  }

  function expectGetGroupMembers(members: Member[] = GROUP_MEMBERS) {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/configuration/groups/${GROUP.id}/members`).flush(members);
  }

  function expectGetGroupAPIs() {
    httpTestingController
      .expectOne(`${CONSTANTS_TESTING.env.baseURL}/configuration/groups/${GROUP.id}/memberships?type=api`)
      .flush(GROUP_APIS);
  }

  function expectGetGroupApplications() {
    httpTestingController
      .expectOne(`${CONSTANTS_TESTING.env.baseURL}/configuration/groups/${GROUP.id}/memberships?type=application`)
      .flush(GROUP_APPLICATIONS);
  }

  function expectGetInvitations() {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/configuration/groups/${GROUP.id}/invitations`).flush(INVITATIONS);
  }

  function expectGetGroup(group: Group = GROUP) {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/configuration/groups/${group.id}`).flush(group);
  }

  function expectPostGroup(newGroup: any, group: Group) {
    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.baseURL}/configuration/groups`,
      method: 'POST',
    });
    expect(req.request.body).toEqual(newGroup);
    req.flush(group);
  }

  function expectPutGroup(group: Group) {
    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.baseURL}/configuration/groups/${group.id}`,
      method: 'PUT',
    });
    expect(req.request.body).toEqual(group);
    req.flush(group);
  }

  function expectDeleteInvitation(invitationId: string) {
    httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.baseURL}/configuration/groups/${GROUP.id}/invitations/${invitationId}`,
      method: 'DELETE',
    });
  }

  function expectAddOrUpdateMembership(id: string, reference: string) {
    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.baseURL}/configuration/groups/${GROUP.id}/members`,
      method: 'POST',
    });
    const memberships: GroupMembership[] = [
      {
        id: id,
        reference: reference,
        roles: [
          { name: 'REVIEWER', scope: 'API' },
          { name: 'OWNER', scope: 'APPLICATION' },
          { name: 'OWNER', scope: 'INTEGRATION' },
        ],
      },
    ];
    expect(req.request.body).toEqual(memberships);
  }

  function expectDeleteMember(memberId: string) {
    httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.baseURL}/configuration/groups/${GROUP.id}/members/${memberId}`,
      method: 'DELETE',
    });
  }

  function expectPostInvitation() {
    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.baseURL}/configuration/groups/${GROUP.id}/invitations`,
      method: 'POST',
    });
    const invitation: Invitation = {
      reference_type: 'GROUP',
      reference_id: GROUP.id,
      email: 'temp@temp.com',
      api_role: 'REVIEWER',
      application_role: 'OWNER',
    };
    expect(req.request.body).toEqual(invitation);
  }
});
