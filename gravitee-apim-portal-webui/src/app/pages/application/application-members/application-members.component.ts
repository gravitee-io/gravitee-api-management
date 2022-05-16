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
import { ChangeDetectorRef, Component, HostListener, OnInit } from '@angular/core';
import {
  Application,
  ApplicationService,
  GroupService,
  Member,
  PermissionsResponse,
  PermissionsService,
  PortalService,
  Subscription,
  User,
  UsersService,
} from '../../../../../projects/portal-webclient-sdk/src/lib';
import { ActivatedRoute, Router } from '@angular/router';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import '@gravitee/ui-components/wc/gv-autocomplete';
import '@gravitee/ui-components/wc/gv-button';
import '@gravitee/ui-components/wc/gv-confirm';
import '@gravitee/ui-components/wc/gv-icon';
import '@gravitee/ui-components/wc/gv-identity-picture';
import '@gravitee/ui-components/wc/gv-relative-time';
import '@gravitee/ui-components/wc/gv-input';
import '@gravitee/ui-components/wc/gv-list';
import '@gravitee/ui-components/wc/gv-select';
import '@gravitee/ui-components/wc/gv-table';
import { TranslateService } from '@ngx-translate/core';
import { FormControl, FormGroup } from '@angular/forms';
import { ItemResourceTypeEnum } from '../../../model/itemResourceType.enum';
import { NotificationService } from '../../../services/notification.service';
import { CurrentUserService } from 'src/app/services/current-user.service';
import { SearchQueryParam } from '../../../utils/search-query-param.enum';
import StatusEnum = Subscription.StatusEnum;

@Component({
  selector: 'app-application-members',
  templateUrl: './application-members.component.html',
  styleUrls: ['./application-members.component.css'],
})
export class ApplicationMembersComponent implements OnInit {
  constructor(
    private applicationService: ApplicationService,
    private groupService: GroupService,
    private portalService: PortalService,
    private translateService: TranslateService,
    private route: ActivatedRoute,
    private router: Router,
    private notificationService: NotificationService,
    private permissionService: PermissionsService,
    private usersService: UsersService,
    private currentUser: CurrentUserService,
    private ref: ChangeDetectorRef,
  ) {
    this.resetAddMember();
    this.resetTransferOwnership();
  }

  get hasNewMember() {
    return this.selectedUserToAdd !== null;
  }

  get selectedUserToAddName() {
    return this.hasNewMember ? this.selectedUserToAdd.display_name : '';
  }

  get hasUserForTransferOwnership() {
    return this.selectedUserForTransferOwnership !== null;
  }

  get selectedUserForTransferOwnershipName() {
    return this.hasUserForTransferOwnership ? this.selectedUserForTransferOwnership.display_name : '';
  }

  readonly = true;
  canSearchUser = false;
  application: Application;
  connectedApis: Promise<any[]>;
  members: Array<Member>;
  membersOptions: any;
  roles: Array<{ label: string; value: string }>;
  tableTranslations: any[];

  groups: Array<{ groupId: string; groupName: string; groupMembers: Array<Member>; nbGroupMembers: any }>;
  groupMembersOptions: any;

  addMemberForm: FormGroup;
  userListForAddMember: Array<User> = [];
  selectedUserToAdd: User;

  transferOwnershipForm: FormGroup;
  userListForTransferOwnership: Array<User> = [];
  selectedUserForTransferOwnership: User;

  async ngOnInit() {
    this.application = this.route.snapshot.data.application;
    if (this.application) {
      this.canSearchUser =
        this.currentUser.get().getValue() &&
        this.currentUser.get().getValue().permissions.USER &&
        this.currentUser.get().getValue().permissions.USER.includes('R');

      this.readonly = await this.isReadOnly(true);

      this.portalService
        .getApplicationRoles()
        .toPromise()
        .then((appRoles) => {
          this.roles = [];
          appRoles.data.forEach((appRole) => {
            if (appRole.name !== 'PRIMARY_OWNER') {
              this.roles.push({
                label: appRole.name,
                value: appRole.id,
              });
            }
          });
        });

      if (this.application.groups && this.application.groups.length > 0) {
        this.groups = [];
        this.application.groups.forEach((grp) => {
          this.groupService
            .getMembersByGroupId({ groupId: grp.id, size: -1 })
            .toPromise()
            .then((membersResponse) => {
              this.groups.push({
                groupId: grp.id,
                groupName: grp.name,
                groupMembers: membersResponse.data,
                nbGroupMembers: membersResponse.metadata.data.total,
              });
            });
        });
      }

      this.translateService
        .get([
          i18n('application.members.list.member'),
          i18n('application.members.list.role'),
          i18n('application.members.list.remove.message'),
          i18n('application.members.list.remove.title'),
        ])
        .toPromise()
        .then((translations) => {
          this.tableTranslations = Object.values(translations);
          this.membersOptions = this._buildMemberOptions();

          if (this.application.groups && this.application.groups.length > 0) {
            this.groupMembersOptions = {
              paging: 5,
              data: [
                { field: 'user._links.avatar', type: 'image', alt: 'user.display_name' },
                { field: 'user.display_name', label: this.tableTranslations[0] },
                { field: 'role', label: this.tableTranslations[1] },
              ],
            };
          }
          this.loadMembersTable();
        });

      this.connectedApis = this.applicationService
        .getSubscriberApisByApplicationId({
          applicationId: this.application.id,
          statuses: [StatusEnum.ACCEPTED],
        })
        .toPromise()
        .then((response) => response.data.map((api) => ({ item: api, type: ItemResourceTypeEnum.API })));
    }
  }

  _buildMemberOptions() {
    const data: any[] = [
      { field: 'user._links.avatar', type: 'image', alt: 'user.display_name' },
      { field: 'user.display_name', label: this.tableTranslations[0] },
    ];
    if (this.readonly) {
      data.push({ field: 'role', label: this.tableTranslations[1] });
    } else {
      data.push(this._renderRole(this.tableTranslations[1]));
      data.push(this._renderDelete(this.tableTranslations[2], this.tableTranslations[3]));
    }
    return {
      paging: 5,
      data,
    };
  }

  _renderRole(roleLabel: any) {
    return {
      field: 'role',
      label: roleLabel,
      type: 'gv-select',
      attributes: {
        options: (item) => (item.role === 'PRIMARY_OWNER' ? ['PRIMARY_OWNER'] : this.roles),
        disabled: (item) => item.role === 'PRIMARY_OWNER',
        'ongv-select:select': (item, e) => this.updateMember(item, e),
      },
    };
  }

  _renderDelete(confirmMessage: any, iconTitle: any) {
    return {
      type: 'gv-icon',
      width: '25px',
      confirm: { msg: confirmMessage, danger: true, position: 'left' },
      condition: (item) => item.role !== 'PRIMARY_OWNER',
      attributes: {
        onClick: (item) => this.removeMember(item),
        shape: 'general:trash',
        title: iconTitle,
      },
    };
  }

  async isReadOnly(init?: boolean) {
    let permissions: PermissionsResponse;
    if (init) {
      permissions = this.route.snapshot.data.permissions;
    } else {
      permissions = await this.permissionService.getCurrentUserPermissions({ applicationId: this.application.id }).toPromise();
    }

    if (permissions) {
      const memberPermissions = permissions.MEMBER;
      return !memberPermissions || memberPermissions.length === 0 || !memberPermissions.includes('U');
    } else {
      return true;
    }
  }

  loadMembersTable() {
    return this.applicationService
      .getMembersByApplicationId({ applicationId: this.application.id })
      .toPromise()
      .then((membersResponse) => {
        this.members = membersResponse.data;
      });
  }

  resetAddMember() {
    this.selectedUserToAdd = null;
    this.userListForAddMember = [];
    this.addMemberForm = new FormGroup({
      newMemberRole: new FormControl('USER'),
    });
  }

  resetTransferOwnership() {
    this.selectedUserForTransferOwnership = null;
    this.userListForTransferOwnership = [];
    this.transferOwnershipForm = new FormGroup({
      primaryOwnerNewRole: new FormControl('USER'),
    });
  }

  onSearchUserToAdd({ detail }) {
    this.searchUser(detail).then(
      (users) =>
        (this.userListForAddMember = users.filter((user) => this.members.findIndex((member) => member.user.id === user.id) === -1)),
    );
  }

  onSearchUserForTransferOwnership({ detail }) {
    this.searchUser(detail).then(
      (users) => (this.userListForTransferOwnership = users.filter((user) => this.application.owner.id !== user.id)),
    );
  }

  onSelectUserToAdd({ detail }) {
    this.selectedUserToAdd = detail.data;
  }

  onSelectUserForTransferOwnership({ detail }) {
    this.selectedUserForTransferOwnership = detail.data;
  }

  searchUser(query: string): Promise<User[]> {
    return this.usersService
      .getUsers({ q: query })
      .toPromise()
      .then((usersResponse) => {
        let result: User[] = [];
        if (usersResponse.data.length) {
          result = usersResponse.data.map((u) => {
            const row = document.createElement('gv-row');
            // @ts-ignore
            row.item = { name: u.display_name, picture: u._links ? u._links.avatar : '' };
            return { value: u.display_name, element: row, id: u.id, data: u };
          });
        }
        return result;
      });
  }

  removeMember(member: Member) {
    this.applicationService
      .deleteApplicationMember({
        applicationId: this.application.id,
        memberId: member.user.id,
      })
      .toPromise()
      .then(() => this.loadMembersTable())
      .then(() => this.notificationService.success(i18n('application.members.list.remove.success')));
  }

  addMember() {
    this.applicationService
      .createApplicationMember({
        applicationId: this.application.id,
        memberInput: {
          user: this.selectedUserToAdd.id,
          reference: this.selectedUserToAdd.reference,
          role: this.addMemberForm.controls.newMemberRole.value,
        },
      })
      .toPromise()
      .then(() => {
        this.notificationService.success(i18n('application.members.add.success'));
        this.loadMembersTable();
        this.resetAddMember();
      });
  }

  toggleGroupMembers($event: any) {
    $event.target.closest('.groupMembers').classList.toggle('show');
  }

  transferOwnership() {
    this.applicationService
      .transferMemberOwnership({
        applicationId: this.application.id,
        transferOwnershipInput: {
          new_primary_owner_id: this.selectedUserForTransferOwnership.id,
          new_primary_owner_reference: this.selectedUserForTransferOwnership.reference,
          primary_owner_newrole: this.transferOwnershipForm.controls.primaryOwnerNewRole.value,
        },
      })
      .toPromise()
      .then(() => this.router.navigate(['applications']))
      .then(() => this.notificationService.success(i18n('application.members.transferOwnership.success')));
  }

  updateMember(member: Member, { detail }) {
    this.applicationService
      .updateApplicationMemberByApplicationIdAndMemberId({
        applicationId: this.application.id,
        memberId: member.user.id,
        memberInput: {
          user: member.user.id,
          role: detail,
        },
      })
      .toPromise()
      .then(() => {
        this.notificationService.success(i18n('application.members.list.success'));
        if (this.currentUser.exist() && this.currentUser.getUser().id === member.user.id) {
          this.isReadOnly().then((isReadOnly) => {
            this.readonly = isReadOnly;
            this.membersOptions = this._buildMemberOptions();
            this.ref.detectChanges();
          });
        }
      });
  }

  toLocaleDateString(date: string) {
    return new Date(date).toLocaleDateString(this.translateService.currentLang);
  }

  @HostListener(':gv-list:click', ['$event.detail'])
  onGvListClick(detail: any) {
    const queryParams = {};
    queryParams[SearchQueryParam.APPLICATION] = this.application.id;
    this.router.navigate([`/catalog/api/${detail.item.id}`], { queryParams });
  }
}
