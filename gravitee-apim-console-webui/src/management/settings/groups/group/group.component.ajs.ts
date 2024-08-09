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
import { IPromise } from 'angular';

import { Router } from '@angular/router';
import { forEach, sortBy } from 'lodash';

import { ApiOwnershipTransferType, OwnershipTransferResult } from './transferOwnershipDialog.controller';
import { Member, MembershipState, MemberState, RoleName, RoleScope } from './membershipState';

import GroupService from '../../../../services/group.service';
import RoleService from '../../../../services/role.service';
import TagService from '../../../../services/tag.service';
import NotificationService from '../../../../services/notification.service';
import UserService from '../../../../services/user.service';
import ApiPrimaryOwnerModeService from '../../../../services/apiPrimaryOwnerMode.service';

interface IGroupDetailComponentScope extends ng.IScope {
  groupApis: any[];
  groupApplications: any[];
  selectedApiRole: string;
  selectedApplicationRole: string;
  selectedIntegrationRole: string;
  currentTab: string;
  formGroup: any;
}

interface MemberPageQuery {
  page: number;
  size: number;
}

const GroupComponentAjs: ng.IComponentOptions = {
  bindings: {
    activatedRoute: '<',
  },
  template: require('html-loader!./group.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
  controller: [
    'GroupService',
    'RoleService',
    'TagService',
    'NotificationService',
    'ApiPrimaryOwnerModeService',
    '$mdDialog',
    '$scope',
    'UserService',
    '$rootScope',
    'ngRouter',
    function (
      GroupService: GroupService,
      RoleService: RoleService,
      TagService: TagService,
      NotificationService: NotificationService,
      ApiPrimaryOwnerModeService: ApiPrimaryOwnerModeService,
      $mdDialog: angular.material.IDialogService,
      $scope: IGroupDetailComponentScope,
      UserService: UserService,
      $rootScope,
      ngRouter: Router,
    ) {
      this.$rootScope = $rootScope;
      this.group = {};
      this.group.lock_api_role = false;
      this.group.lock_application_role = false;
      this.group.disable_membership_notifications = false;

      this.$onInit = () => {
        this.membersLoaded = false;
        this.defaultPageSize = 10;
        this.updateMode = !!this.activatedRoute?.snapshot?.params?.groupId;
        if (this.activatedRoute?.snapshot?.params?.groupId) {
          Promise.all([
            GroupService.get(this.activatedRoute?.snapshot?.params?.groupId),
            RoleService.list('API'),
            RoleService.list('APPLICATION'),
            RoleService.list('INTEGRATION'),
            GroupService.getInvitations(this.activatedRoute?.snapshot?.params?.groupId),
          ])
            .then(([groupsResponse, apiRolesResponse, applicationRolesResponse, integrationResponse, invitationsResponse]) => {
              this.group = groupsResponse.data;
              this.apiRoles = [{ scope: 'API', name: '', system: false }].concat(apiRolesResponse);
              this.applicationRoles = [{ scope: 'APPLICATION', name: '', system: false }].concat(applicationRolesResponse);
              this.integrationRoles = [{ scope: 'INTEGRATION', name: '', system: false }].concat(integrationResponse);
              this.invitations = invitationsResponse.data;

              if (this.group.roles) {
                this.selectedApiRole = this.group.roles.API;
                this.selectedApplicationRole = this.group.roles.APPLICATION;
                this.selectedIntegrationRole = this.group.roles.INTEGRATION;
              }
              this.apiByDefault = this.group.event_rules && this.group.event_rules.findIndex((rule) => rule.event === 'API_CREATE') !== -1;
              this.applicationByDefault =
                this.group.event_rules && this.group.event_rules.findIndex((rule) => rule.event === 'APPLICATION_CREATE') !== -1;
            })
            .then(() => GroupService.getMembers(this.group?.id))
            .then((response) => {
              this.membershipState = new MembershipState(response.data);
              this.getMembersPage(true);
            });
        }
        TagService.list().then((response) => (this.tags = response.data));

        this.membersPageQuery = {
          page: this.activatedRoute.snapshot.queryParams.page ? parseInt(this.activatedRoute.snapshot.queryParams.page, 10) : 1,
          size: this.defaultPageSize,
        };

        $scope.groupApis = [];
        $scope.groupApplications = [];
        $scope.currentTab = 'users';

        this.currentUserId = UserService.currentUser.id;
        this.isSuperAdmin = UserService.isUserHasPermissions(['environment-group-u']);
        this.canChangeDefaultApiRole = this.isSuperAdmin || !this.group.lock_api_role;
        this.canChangeDefaultApplicationRole = this.isSuperAdmin || !this.group.lock_application_role;
        this.canChangeDefaultIntegrationRole = this.isSuperAdmin;

        /*
        It is written in the members list: "Enable email invitation and/or user search to allow the group administrator to add users."
        It means that to add members, the group must be manageable (i.e. the current user is a group admin) and the group must have email invitation or system invitation enabled.
       */
        /*
        It is possible to add members only when a group is first created, otherwise we can't associate members to the group (without id)
       */
        this.canAddMembers =
          this.updateMode &&
          (this.isSuperAdmin || (this.group.manageable && (this.group.system_invitation || this.group.email_invitation)));

        this.loadGroupApis();
      };

      this.updateRole = (member: any, scope: RoleScope) => {
        if (this.membershipState.isPrimaryOwnerDemotion(member, scope)) {
          this.demotePrimaryOwner(this.membershipState.stateOf(member), scope);
        } else if (this.membershipState.isPrimaryOwnerPromotion(member, scope)) {
          this.promoteNewPrimaryOwner(this.membershipState.stateOf(member), [scope]);
        } else {
          GroupService.addOrUpdateMember(this.group.id, [member]).then(() => {
            NotificationService.show('Member successfully updated');
            this.$onInit();
          });
        }
      };

      this.associateToApis = () => {
        GroupService.associate(this.group.id, 'api').then(() => {
          this.$onInit();
          NotificationService.show("Group '" + this.group.name + "' has been associated to all APIs");
        });
      };

      this.associateToApplications = () => {
        GroupService.associate(this.group.id, 'application').then(() => {
          this.$onInit();
          NotificationService.show("Group '" + this.group.name + "' has been associated to all applications");
        });
      };

      this.update = () => {
        GroupService.updateEventRules(this.group, this.apiByDefault, this.applicationByDefault);

        if (!this.updateMode) {
          GroupService.create(this.group).then((response) => {
            ngRouter.navigate(['../', response.data.id], { relativeTo: this.activatedRoute });
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

          if (this.selectedIntegrationRole) {
            roles.INTEGRATION = this.selectedIntegrationRole;
          } else {
            delete roles.INTEGRATION;
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
        const primaryOwnerScopes = memberState.getPrimaryOwnerScopes();
        if (primaryOwnerScopes.length > 0) {
          this.showTransferOwnershipModal(
            member,
            ApiOwnershipTransferType.DELETE_PRIMARY_OWNER,
            this.membershipState.primaryOwnerWithScopes(primaryOwnerScopes),
          ).then(({ newPrimaryOwnerRef, primaryOwner }) => {
            this.deleteMember(primaryOwner).then(() => {
              const newPrimaryOwner = this.membershipState.findByRef(newPrimaryOwnerRef);
              memberState.getPrimaryOwnerScopes().forEach((scope: RoleScope) => (newPrimaryOwner.roles[scope] = 'PRIMARY_OWNER'));
              this.updateRole(newPrimaryOwner);
            });
          });
        } else {
          $mdDialog
            .show({
              controller: 'DialogConfirmController',
              controllerAs: 'ctrl',
              template: require('html-loader!../../../../components/dialog/confirmWarning.dialog.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
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
            template: require('html-loader!./addMember.dialog.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
            clickOutsideToClose: true,
            locals: {
              defaultApiRole: this.selectedApiRole,
              defaultApplicationRole: this.selectedApplicationRole,
              defaultIntegrationRole: this.selectedIntegrationRole,
              group: this.group,
              apiRoles: this.apiRoles,
              applicationRoles: this.applicationRoles,
              integrationRoles: this.integrationRoles,
              canChangeDefaultApiRole: this.canChangeDefaultApiRole,
              canChangeDefaultApplicationRole: this.canChangeDefaultApplicationRole,
              canChangeDefaultIntegrationRole: this.canChangeDefaultIntegrationRole,
              isApiRoleDisabled: this.isApiRoleDisabled,
              isIntegrationRoleDisabled: this.isIntegrationRoleDisabled,
            },
          })
          .then(
            (members = []) => {
              if (members.length > 0) {
                const memberState = this.membershipState.stateOf(members[0]);
                const primaryOwnerScopes = memberState.getPrimaryOwnerScopes();
                if (this.membershipState.isPrimaryOwnerPromotion(members[0], primaryOwnerScopes[0])) {
                  this.promoteNewPrimaryOwner(memberState, primaryOwnerScopes);
                } else {
                  GroupService.addOrUpdateMember(this.group.id, members)
                    .then(() => {
                      NotificationService.show('Member(s) successfully added');
                      members
                        .filter((member) => this.membershipState.isPrimaryOwnerDemotion(member, primaryOwnerScopes[0]))
                        .forEach((member) => this.demotePrimaryOwner(this.membershipState.stateOf(member), primaryOwnerScopes[0]));
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

      this.showTransferOwnershipModal = (primaryOwner, transferType, primaryOwnerWithScopes): IPromise<OwnershipTransferResult> => {
        const members = this.membershipState.findAll();
        return $mdDialog.show({
          controller: 'DialogTransferOwnershipController',
          controllerAs: '$ctrl',
          template: require('html-loader!./transferOwnershipDialog.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
          clickOutsideToClose: true,
          locals: {
            primaryOwner,
            members,
            group: this.group,
            transferType,
            primaryOwnerWithScopes: primaryOwnerWithScopes,
          },
        });
      };

      this.demotePrimaryOwner = (memberState: MemberState, scope: RoleScope): void => {
        this.showTransferOwnershipModal(memberState.getLastState(), ApiOwnershipTransferType.DEMOTE_PRIMARY_OWNER, [
          { member: memberState.getLastState(), roleScope: scope },
        ]).then(
          ({ newPrimaryOwnerRef }) => {
            const newPrimaryOwner = this.membershipState.findByRef(newPrimaryOwnerRef);
            const previousPrimaryOwner = memberState.getCurrentState();

            newPrimaryOwner.roles[scope] = 'PRIMARY_OWNER';

            GroupService.addOrUpdateMember(this.group.id, [previousPrimaryOwner, newPrimaryOwner])
              .then(() => {
                NotificationService.show('Member successfully updated');
              })
              .finally(() => this.getMembersPage());
          },
          () => this.getMembersPage(),
        );
      };

      this.promoteNewPrimaryOwner = (memberState: MemberState, scopes: RoleScope[]): void => {
        const primaryOwnersWithScopes = this.membershipState.primaryOwnerWithScopes(scopes);
        this.showTransferOwnershipModal(
          this.membershipState.getPrimaryOwner(scopes[0]),
          ApiOwnershipTransferType.PROMOTE_NEW_PRIMARY_OWNER,
          primaryOwnersWithScopes,
        ).then(
          () => {
            primaryOwnersWithScopes.forEach((memberWithScope) => {
              const previousPrimaryOwner = this.membershipState.getPrimaryOwner(memberWithScope.roleScope);
              const newPrimaryOwner = memberState.getCurrentState();

              previousPrimaryOwner.roles[memberWithScope.roleScope] = RoleName.OWNER;
              newPrimaryOwner.roles[memberWithScope.roleScope] = RoleName.PRIMARY_OWNER;

              GroupService.addOrUpdateMember(this.group.id, [previousPrimaryOwner, newPrimaryOwner])
                .then(() => {
                  NotificationService.show('Member successfully updated');
                })
                .finally(() => this.getMembersPage());
            });
          },
          () => this.getMembersPage(),
        );
      };

      this.deleteMember = (member: Member): IPromise<void> => {
        return GroupService.deleteMember(this.group.id, member.id).then(() => {
          NotificationService.show('Member ' + member.displayName + ' has been successfully removed');
          if (this.members.length === 1 && this.membersPageQuery.page > 1) {
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
        if (this.group?.id) {
          GroupService.getMemberships(this.group.id, 'api').then((response) => {
            $scope.groupApis = sortBy(response.data, 'name');
          });
        }
      };

      this.loadGroupApplications = () => {
        if (this.group?.id) {
          GroupService.getMemberships(this.group.id, 'application').then((response) => {
            $scope.groupApplications = sortBy(response.data, 'name');
          });
        }
      };

      this.reset = () => {
        this.$onInit();
      };

      this.isMaxInvitationReached = () => {
        if (!this.group.max_invitation) {
          return false;
        }
        const numberOfMembers = this.membersPage ? this.membersPage.page.total_elements : 0;
        const numberOfInvitations = this.invitations ? this.invitations.length : 0;
        const numberOfSlots = numberOfMembers + numberOfInvitations;
        return numberOfSlots >= this.group.max_invitation;
      };

      this.canSave = () => {
        return !this.updateMode || this.group.manageable;
      };

      this.updateInvitation = (invitation: any) => {
        GroupService.updateInvitation(this.group.id, invitation).then(() => {
          NotificationService.show('Invitation successfully updated');
          this.$onInit();
        });
      };

      this.showInviteMemberModal = () => {
        $mdDialog
          .show({
            controller: [
              '$mdDialog',
              'group',
              'apiRoles',
              'applicationRoles',
              'defaultApiRole',
              'defaultApplicationRole',
              'canChangeDefaultApiRole',
              'canChangeDefaultApplicationRole',
              'isApiRoleDisabled',
              function (
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
            ],
            controllerAs: '$ctrl',
            template: require('html-loader!./inviteMember.dialog.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
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
                this.$onInit();
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
            template: require('html-loader!../../../../components/dialog/confirmWarning.dialog.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
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
                this.$onInit();
              });
            }
          });
      };

      this.hasGroupAdmin = () => {
        let hasGroupAdmin = false;
        forEach(this.members, (member) => {
          if (member.roles.GROUP && member.roles.GROUP === 'ADMIN') {
            hasGroupAdmin = true;
          }
        });
        return hasGroupAdmin;
      };

      this.isApiRoleDisabled = (role) => {
        if (ApiPrimaryOwnerModeService.isUserOnly()) {
          return role.name === 'PRIMARY_OWNER';
        }
        return role.system && role.name !== 'PRIMARY_OWNER';
      };

      this.isIntegrationRoleDisabled = (role) => {
        if (ApiPrimaryOwnerModeService.isUserOnly()) {
          return role.name === 'PRIMARY_OWNER';
        }
        return role.system && role.name !== 'PRIMARY_OWNER';
      };

      this.backToList = () => {
        ngRouter.navigate(['..'], { relativeTo: this.activatedRoute });
      };
    },
  ],
};

export default GroupComponentAjs;
