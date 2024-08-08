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
import { find } from 'lodash';

import { RoleName } from './membershipState';

export class Role {
  default: boolean;
  name: string;
  system: boolean;
}

function DialogAddGroupMemberController(
  $mdDialog: angular.material.IDialogService,
  group: any,
  defaultApiRole: string,
  defaultApplicationRole: string,
  defaultIntegrationRole: string,
  apiRoles: Role[],
  applicationRoles: Role[],
  integrationRoles: Role[],
  canChangeDefaultApiRole,
  canChangeDefaultApplicationRole,
  canChangeDefaultIntegrationRole,
  isApiRoleDisabled,
  isIntegrationRoleDisabled,
) {
  this.group = group;
  this.apiRoles = apiRoles;
  this.applicationRoles = applicationRoles;
  this.integrationRoles = integrationRoles;

  this.defaultApiRole = defaultApiRole;
  this.defaultApplicationRole = defaultApplicationRole;
  this.defaultIntegrationRole = defaultIntegrationRole;
  this.usersSelected = [];
  this.defaultApiRole = defaultApiRole ? defaultApiRole : find(apiRoles, { default: true })?.name;
  this.defaultApplicationRole = defaultApplicationRole ? defaultApplicationRole : find(applicationRoles, { default: true }).name;
  this.defaultIntegrationRole = defaultIntegrationRole ? defaultIntegrationRole : find(integrationRoles, { default: true }).name;

  this.canChangeDefaultApiRole = canChangeDefaultApiRole;
  this.canChangeDefaultApplicationRole = canChangeDefaultApplicationRole;
  this.canChangeDefaultIntegrationRole = canChangeDefaultIntegrationRole;
  this.isApiRoleDisabled = isApiRoleDisabled;
  this.isIntegrationRoleDisabled = isIntegrationRoleDisabled;

  this.hide = () => {
    $mdDialog.cancel();
  };

  this.addMembers = () => {
    const members = [];
    for (let i = 0; i < this.usersSelected.length; i++) {
      const member = this.usersSelected[i];
      const membership = {
        id: member.id,
        reference: member.reference,
        displayName: !member.firstname || !member.lastname ? member.username : member.firstname + ' ' + member.lastname,
        roles: {
          API: this.defaultApiRole,
          APPLICATION: this.defaultApplicationRole,
          INTEGRATION: this.defaultIntegrationRole,
        },
      };

      members.push(membership);
    }
    $mdDialog.hide(members);
  };

  this.invalid = (): boolean => {
    return (!this.defaultApiRole && !this.defaultApplicationRole && !this.defaultIntegrationRole) || this.usersSelected.length === 0;
  };

  this.hasPrimaryOwner = (): boolean => {
    return (
      (this.defaultApiRole === RoleName.PRIMARY_OWNER || this.defaultIntegrationRole === RoleName.PRIMARY_OWNER) &&
      this.usersSelected.length > 0
    );
  };
}
DialogAddGroupMemberController.$inject = [
  '$mdDialog',
  'group',
  'defaultApiRole',
  'defaultApplicationRole',
  'defaultIntegrationRole',
  'apiRoles',
  'applicationRoles',
  'integrationRoles',
  'canChangeDefaultApiRole',
  'canChangeDefaultApplicationRole',
  'canChangeDefaultIntegrationRole',
  'isApiRoleDisabled',
  'isIntegrationRoleDisabled',
];

export default DialogAddGroupMemberController;
