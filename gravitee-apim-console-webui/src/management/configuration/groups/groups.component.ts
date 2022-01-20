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
import { StateService } from '@uirouter/core';
import { IScope } from 'angular';
import * as _ from 'lodash';

import GroupService from '../../../services/group.service';
import NotificationService from '../../../services/notification.service';
import UserService from '../../../services/user.service';

const GroupsComponent: ng.IComponentOptions = {
  bindings: {
    groups: '<',
  },
  template: require('./groups.html'),
  controller: function (
    GroupService: GroupService,
    UserService: UserService,
    NotificationService: NotificationService,
    $mdDialog: angular.material.IDialogService,
    $state: StateService,
    $rootScope: IScope,
  ) {
    'ngInject';
    this.$rootScope = $rootScope;

    this.create = () => {
      $state.go('management.settings.groups.create');
    };

    this.removeGroup = (ev, groupId, groupName) => {
      ev.stopPropagation();
      $mdDialog
        .show({
          controller: 'DialogConfirmController',
          controllerAs: 'ctrl',
          template: require('../../../components/dialog/confirmWarning.dialog.html'),
          clickOutsideToClose: true,
          locals: {
            title: 'Would you like to remove the group "' + groupName + '"?',
            confirmButton: 'Remove',
          },
        })
        .then((response) => {
          if (response) {
            GroupService.remove(groupId).then(() => {
              NotificationService.show('Group ' + groupName + ' has been deleted.');
              GroupService.list().then((response) => {
                this.groups = _.filter(response.data, 'manageable');
                this.initEventRules();
              });
            });
          }
        });
    };

    this.saveEventRules = (group: any) => {
      if (group.manageable) {
        GroupService.updateEventRules(group, this.apiByDefault[group.id], this.applicationByDefault[group.id]);
        GroupService.update(group).then(() => {
          NotificationService.show('Group ' + group.name + ' has been updated.');
        });
      }
    };

    this.selectGroupUrl = (group: any) => {
      if (group.manageable) {
        return $state.go('management.settings.groups.group', { groupId: group.id });
      }
      return null;
    };

    this.hasEvent = (group: any, event: string) => {
      return group.event_rules && group.event_rules.findIndex((rule) => rule.event === event) !== -1;
    };
  },
};

export default GroupsComponent;
