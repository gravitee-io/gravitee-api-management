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
import GroupService from '../../../../services/group.service';
import NotificationService from '../../../../services/notification.service';
import { StateService } from '@uirouter/core';
import UserService from '../../../../services/user.service';
import { ApiOwnershipTransferType, OwnershipTransferResult } from './transferOwnershipDialog.controller';
import { Member, MembershipState, MemberState, RoleName, RoleScope } from './membershipState';
import { IPromise } from 'angular';
import _ = require('lodash');

interface IGroupDetailComponentScope extends ng.IScope {
  groupApis: any[];
  groupApplications: any[];
  selectedApiRole: string;
  selectedApplicationRole: string;
  currentTab: string;
  formGroup: any;
}

interface MemberPageQuery {
  page: number;
  size: number;
}

const GroupComponent: ng.IComponentOptions = {
  bindings: {
    group: '<',
    apiRoles: '<',
    applicationRoles: '<',
    invitations: '<',
    tags: '<',
  },
  template: require('./group.html'),
  controller: function (
    GroupService: GroupService,
    NotificationService: NotificationService,
    $mdDialog: angular.material.IDialogService,
    $state: StateService,
    $scope: IGroupDetailComponentScope,
    UserService: UserService,
    $rootScope,
  ) {
    'ngInject';
    this.$rootScope = $rootScope;

    this.$onInit = () => {
      this.updateMode = this.group !== undefined && this.group.id !== undefined;
      this.membersLoaded = false;
      this.defaultPageSize = 10;

      this.membersPageQuery = {
        page: $state.params.page ? parseInt($state.params.page, 0) : 1,
        size: this.defaultPageSize,
      };

      if (!this.updateMode) {
        this.group = {};
        this.group.lock_api_role = false;
        this.group.lock_application_role = false;
        this.group.disable_membership_notifications = false;
      } else {
        GroupService.getMembers(this.group?.id).then((response) => {
          this.membershipState = new MembershipState(response.data);
          this.getMembersPage(true);
        });
      }

      $scope.groupApis = [];
      $scope.groupApplications = [];
      $scope.currentTab = 'users';

      if (this.group.roles) {
        this.selectedApiRole = this.group.roles.API;
        this.selectedApplicationRole = this.group.roles.APPLICATION;
      }

      this.apiByDefault = this.group.event_rules && this.group.event_rules.findIndex((rule) => rule.event === 'API_CREATE') !== -1;
      this.applicationByDefault =
        this.group.event_rules && this.group.event_rules.findIndex((rule) => rule.event === 'APPLICATION_CREATE') !== -1;

      this.loadGroupApis();
    };

    this.updateRole = (member: any) => {
      if (this.membershipState.isPrimaryOwnerDemotion(member)) {
        this.demotePrimaryOwner(this.membershipState.stateOf(member));
      } else if (this.membershipState.isPrimaryOwnerPromotion(member)) {
        this.promoteNewPrimaryOwner(this.membershipState.stateOf(member));
      } else {
        GroupService.addOrUpdateMember(this.group.id, [member]).then(() => {
          NotificationService.show('Member successfully updated');
          $state.reload();
        });
      }
    };

    this.associateToApis = () => {
      GroupService.associate(this.group.id, 'api').then(() => {
        $state.reload();
        NotificationService.show("Group '" + this.group.name + "' has been associated to all APIs");
      });
    };

    this.associateToApplications = () => {
      GroupService.associate(this.group.id, 'application').then(() => {
        $state.reload();
        NotificationService.show("Group '" + this.group.name + "' has been associated to all applications");
      });
    };

    this.update = () => {
      GroupService.updateEventRules(this.group, this.apiByDefault, this.applicationByDefault);

      if (!this.updateMode) {
        GroupService.create(this.group).then((response) => {
          $state.go('management.settings.groups.group', { groupId: response.data.id }, { reload: true });
          NotificationService.show("Group '" + this.group.name + "' has been created");
        });
      } else {
        const roles: any = {};

        if (this.selectedApiRole) {
          roles.API = this.selectedApiRole;
        } else {
          delete roles.API;
        }

        if (this.selectedApplicationRole) {
          roles.APPLICATION = this.selectedApplicationRole;
        } else {
          delete roles.APPLICATION;
        }

        this.group.roles = roles;

        GroupService.update(this.group).then((response) => {
          this.group = response.data;
          this.$onInit();
          $scope.formGroup.$setPristine();
          NotificationService.show("Group '" + this.group.name + "' has been updated");
        });
      }
    };

    this.onPaginate = (page: number) => {
      this.getMembersPage(true, { page, size: this.defaultPageSize });
    };

    this.removeUser = (ev, member: any) => {
      ev.stopPropagation();

      const memberState = this.membershipState.stateOf(member);
      if (memberState.wasPrimaryOwner()) {
        this.showTransferOwnershipModal(member, ApiOwnershipTransferType.DELETE_PRIMARY_OWNER).then(
          ({ newPrimaryOwnerRef, primaryOwner }) => {
            this.deleteMember(primaryOwner).then(() => {
              const newPrimaryOwner = this.membershipState.findByRef(newPrimaryOwnerRef);
              newPrimaryOwner.roles['API'] = 'PRIMARY_OWNER';
              this.updateRole(newPrimaryOwner);
            });
          },
        );
      } else {
        $mdDialog
          .show({
            controller: 'DialogConfirmController',
            controllerAs: 'ctrl',
            template: require('../../../../components/dialog/confirmWarning.dialog.html'),
            clickOutsideToClose: true,
            locals: {
              msg: '',
              title: 'Are you sure you want to remove the user "' + member.displayName + '"?',
              confirmButton: 'Remove',
            },
          })
          .then((response) => {
            if (response) {
              this.deleteMember(member);
            }
          });
      }
    };

    this.showAddMemberModal = () => {
      $mdDialog
        .show({
          controller: 'DialogAddGroupMemberController',
          controllerAs: '$ctrl',
          template: require('./addMember.dialog.html'),
          clickOutsideToClose: true,
          locals: {
            defaultApiRole: this.selectedApiRole,
            defaultApplicationRole: this.selectedApplicationRole,
            group: this.group,
            apiRoles: this.apiRoles,
            applicationRoles: this.applicationRoles,
            canChangeDefaultApiRole: this.canChangeDefaultApiRole,
            canChangeDefaultApplicationRole: this.canChangeDefaultApplicationRole,
            isApiRoleDisabled: this.isApiRoleDisabled,
          },
        })
        .then(
          (members = []) => {
            if (members.length > 0) {
              if (this.membershipState.isPrimaryOwnerPromotion(members[0])) {
                this.promoteNewPrimaryOwner(this.membershipState.stateOf(members[0]));
              } else {
                GroupService.addOrUpdateMember(this.group.id, members)
                  .then(() => {
                    NotificationService.show('Member(s) successfully added');
                    members
                      .filter((member) => this.membershipState.isPrimaryOwnerDemotion(member))
                      .forEach((member) => this.demotePrimaryOwner(this.membershipState.stateOf(member)));
                  })
                  .finally(() => this.getMembersPage());
              }
            }
          },
          () => {
            // you cancelled the dialog
          },
        );
    };

    this.showTransferOwnershipModal = (primaryOwner, transferType): IPromise<OwnershipTransferResult> => {
      const members = this.membershipState.findAll();

      return $mdDialog.show({
        controller: 'DialogTransferOwnershipController',
        controllerAs: '$ctrl',
        template: require('./transferOwnershipDialog.html'),
        clickOutsideToClose: true,
        locals: {
          transferType,
          members,
          primaryOwner,
          group: this.group,
        },
      });
    };

    this.demotePrimaryOwner = (memberState: MemberState): void => {
      this.showTransferOwnershipModal(memberState.getLastState(), ApiOwnershipTransferType.DEMOTE_PRIMARY_OWNER).then(
        ({ newPrimaryOwnerRef }) => {
          const newPrimaryOwner = this.membershipState.findByRef(newPrimaryOwnerRef);
          const previousPrimaryOwner = memberState.getCurrentState();

          newPrimaryOwner.roles['API'] = 'PRIMARY_OWNER';

          GroupService.addOrUpdateMember(this.group.id, [previousPrimaryOwner, newPrimaryOwner])
            .then(() => {
              NotificationService.show('Member successfully updated');
            })
            .finally(() => this.getMembersPage());
        },
        () => this.getMembersPage(),
      );
    };

    this.promoteNewPrimaryOwner = (memberState: MemberState): void => {
      this.showTransferOwnershipModal(this.membershipState.getPrimaryOwner(), ApiOwnershipTransferType.PROMOTE_NEW_PRIMARY_OWNER).then(
        () => {
          const previousPrimaryOwner = this.membershipState.getPrimaryOwner();
          const newPrimaryOwner = memberState.getCurrentState();

          previousPrimaryOwner.roles[RoleScope.API] = RoleName.OWNER;
          newPrimaryOwner.roles[RoleScope.API] = RoleName.PRIMARY_OWNER;

          GroupService.addOrUpdateMember(this.group.id, [previousPrimaryOwner, newPrimaryOwner])
            .then(() => {
              NotificationService.show('Member successfully updated');
            })
            .finally(() => this.getMembersPage());
        },
        () => this.getMembersPage(),
      );
    };

    this.deleteMember = (member: Member): IPromise<void> => {
      return GroupService.deleteMember(this.group.id, member.id).then(() => {
        NotificationService.show('Member ' + member.displayName + ' has been successfully removed');
        if (this.members.length === 1 && this.membersPageQuery.page > 0) {
          --this.membersPageQuery.page;
        }
        return this.getMembersPage().then(() => {
          if (this.group.apiPrimaryOwner === member.id) {
            delete this.group.apiPrimaryOwner;
          }
        });
      });
    };

    this.getMembersPage = (useLoader = false, membersPageQuery: Partial<MemberPageQuery> = {}): IPromise<void> => {
      this.membersLoaded = !useLoader;

      return GroupService.getMembers(this.group.id, Object.assign(this.membersPageQuery, membersPageQuery))
        .then((response) => {
          this.membersPage = response.data;
          this.membersPageQuery.page = this.membersPage.page.current;
          this.members = this.membersPage.data;
          this.membershipState.sync(this.members);
        })
        .finally(() => {
          this.membersLoaded = true;
        });
    };

    this.loadGroupApis = () => {
      GroupService.getMemberships(this.group.id, 'api').then((response) => {
        $scope.groupApis = _.sortBy(response.data, 'name');
      });
    };

    this.loadGroupApplications = () => {
      GroupService.getMemberships(this.group.id, 'application').then((response) => {
        $scope.groupApplications = _.sortBy(response.data, 'name');
      });
    };

    this.reset = () => {
      $state.reload();
    };

    this.canChangeDefaultApiRole = () => {
      return this.isSuperAdmin() || !this.group.lock_api_role;
    };

    this.canChangeDefaultApplicationRole = () => {
      return this.isSuperAdmin() || !this.group.lock_application_role;
    };

    this.canAddMembers = () => {
      if (this.isSuperAdmin()) {
        return true;
      } else if (this.group.manageable) {
        const numberOfMembers = this.members ? this.members.length : 0;
        const numberOfInvitations = this.invitations ? this.invitations.length : 0;
        const numberOfSlots = numberOfMembers + numberOfInvitations;
        return !this.group.max_invitation || numberOfSlots < this.group.max_invitation;
      } else {
        return false;
      }
    };

    this.isSuperAdmin = () => {
      return UserService.isUserHasPermissions(['environment-group-u']);
    };

    this.canSave = () => {
      return !this.updateMode || this.group.manageable;
    };

    this.updateInvitation = (invitation: any) => {
      GroupService.updateInvitation(this.group.id, invitation).then(() => {
        NotificationService.show('Invitation successfully updated');
        $state.reload();
      });
    };

    this.showInviteMemberModal = () => {
      $mdDialog
        .show({
          controller: function (
            $mdDialog,
            group,
            apiRoles,
            applicationRoles,
            defaultApiRole,
            defaultApplicationRole,
            canChangeDefaultApiRole,
            canChangeDefaultApplicationRole,
            isApiRoleDisabled,
          ) {
            'ngInject';
            this.group = group;
            this.group.api_role = group.api_role || defaultApiRole;
            this.group.application_role = group.application_role || defaultApplicationRole;
            this.apiRoles = apiRoles;
            this.applicationRoles = applicationRoles;
            this.canChangeDefaultApiRole = canChangeDefaultApiRole;
            this.canChangeDefaultApplicationRole = canChangeDefaultApplicationRole;
            this.isApiRoleDisabled = isApiRoleDisabled;
            this.hide = function () {
              $mdDialog.hide();
            };
            this.save = function () {
              $mdDialog.hide(this.email);
            };
          },
          controllerAs: '$ctrl',
          template: require('./inviteMember.dialog.html'),
          clickOutsideToClose: true,
          locals: {
            defaultApiRole: this.selectedApiRole,
            defaultApplicationRole: this.selectedApplicationRole,
            group: this.group,
            apiRoles: this.apiRoles,
            applicationRoles: this.applicationRoles,
            canChangeDefaultApiRole: this.canChangeDefaultApiRole,
            canChangeDefaultApplicationRole: this.canChangeDefaultApplicationRole,
            isApiRoleDisabled: this.isApiRoleDisabled,
          },
        })
        .then((email) => {
          if (email) {
            GroupService.inviteMember(this.group, email).then((response) => {
              if (response.data.id) {
                NotificationService.show('Invitation successfully sent');
              } else {
                NotificationService.show('Member successfully added');
              }
              $state.reload();
            });
          }
        });
    };

    this.removeInvitation = (ev, invitation: any) => {
      ev.stopPropagation();
      $mdDialog
        .show({
          controller: 'DialogConfirmController',
          controllerAs: 'ctrl',
          template: require('../../../../components/dialog/confirmWarning.dialog.html'),
          clickOutsideToClose: true,
          locals: {
            msg: '',
            title: 'Are you sure you want to remove the invitation for "' + invitation.email + '"?',
            confirmButton: 'Remove',
          },
        })
        .then((response) => {
          if (response) {
            GroupService.deleteInvitation(this.group.id, invitation.id).then(() => {
              NotificationService.show('Invitation for ' + invitation.email + ' has been successfully removed');
              $state.reload();
            });
          }
        });
    };

    this.hasGroupAdmin = () => {
      let hasGroupAdmin = false;
      _.forEach(this.members, (member) => {
        if (member.roles.GROUP && member.roles.GROUP === 'ADMIN') {
          hasGroupAdmin = true;
        }
      });
      return hasGroupAdmin;
    };

    this.isApiRoleDisabled = (role) => role.system && role.name !== 'PRIMARY_OWNER';
  },
};

export default GroupComponent;
