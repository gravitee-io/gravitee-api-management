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
import * as _ from 'lodash';
import UserService from "../../../services/user.service";

export class Role {
  default: boolean;
  name: string;
}

function DialogAddGroupMemberController(
  $mdDialog: angular.material.IDialogService,
  UserService: UserService,
  group: any,
  apiRoles: Role[],
  applicationRoles: Role[]
  ) {
  'ngInject';

  this.group = group;
  this.apiRoles = apiRoles;
  this.applicationRoles = applicationRoles;
  this.usersSelected = [];
  this.searchText = "";
  this.defaultApiRole = _.find(apiRoles, {default: true}).name;
  this.defaultApplicationRole = _.find(applicationRoles, {default: true}).name;

  this.hide = () => {
    $mdDialog.cancel();
  };

  this.searchUser = (query) => {
    if (query) {
      return UserService.search(query).then((response) =>
        response.data
    );
    }
  };

  this.getUserAvatar = (id?: string) => {
    return (id) ? UserService.getUserAvatar(id) : 'assets/default_photo.png';
  };

  this.selectUser = (user) => {
    if (user && user.reference) {
      let selected = _.find(this.usersSelected, {reference: user.reference});
      if (!selected) {
        this.usersSelected.push(user);
      }
      this.searchText = '';
    }
  };

  this.addMembers = () => {
    let members = [];
    for (let i = 0; i < this.usersSelected.length; i++) {
      let member = this.usersSelected[i];
      let membership = {
        id: member.id,
        reference: member.reference,
        displayName: (!member.firstname || !member.lastname) ? member.username : (member.firstname + ' ' + member.lastname),
        roles: {
          API: this.defaultApiRole,
          APPLICATION: this.defaultApplicationRole
        }
      };

      members.push(membership);
    }
    $mdDialog.hide(members);
  };

  this.invalid = () => {
    return (!this.defaultApiRole && !this.defaultApplicationRole) || (this.usersSelected.length === 0);
  };
}

export default DialogAddGroupMemberController;
