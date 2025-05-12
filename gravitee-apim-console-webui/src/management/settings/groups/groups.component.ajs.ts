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
import { IScope } from 'angular';

import { Router } from '@angular/router';

import GroupService from '../../../services/group.service';
import NotificationService from '../../../services/notification.service';
import UserService from '../../../services/user.service';

const GroupsComponentAjs: ng.IComponentOptions = {
  bindings: {
    activatedRoute: '<',
  },
  template: require('html-loader!./groups.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
  controller: [
    'GroupService',
    'UserService',
    'NotificationService',
    '$mdDialog',
    '$rootScope',
    'ngRouter',
    function (
      GroupService: GroupService,
      UserService: UserService,
      NotificationService: NotificationService,
      $mdDialog: angular.material.IDialogService,
      $rootScope: IScope,
      ngRouter: Router,
    ) {
      this.$rootScope = $rootScope;
      this.ngRouter = ngRouter;

      this.$onInit = () => {
        GroupService.listPaginated().then((response) => {
          this.groups = response.data.data;
          this.page = response.data.page;
        });
        this.canRemoveGroup = UserService.isUserHasPermissions(['environment-group-d']);
      };

      this.loadGroups = () => {
        GroupService.listPaginated(this.page.current, this.page.per_page, this.searchTerm ?? '').then((response) => {
          this.groups = response.data.data;
          this.page = response.data.page;
        });
      };

      this.create = () => {
        this.ngRouter.navigate(['./new'], { relativeTo: this.activatedRoute });
      };

      this.removeGroup = (ev, groupId, groupName) => {
        ev.stopPropagation();
        $mdDialog
          .show({
            controller: 'DialogConfirmController',
            controllerAs: 'ctrl',
            template: require('html-loader!../../../components/dialog/confirmWarning.dialog.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
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
                this.loadGroups();
                this.initEventRules();
              });
            }
          });
      };

      this.saveEventRules = (group: any) => {
        GroupService.updateEventRules(group, this.apiByDefault[group.id], this.applicationByDefault[group.id]);
        GroupService.update(group).then(() => {
          NotificationService.show('Group ' + group.name + ' has been updated.');
        });
      };

      this.selectGroupUrl = (group: any) => {
        return this.ngRouter.navigate([group.id], { relativeTo: this.activatedRoute });
      };

      this.hasEvent = (group: any, event: string) => {
        return group.event_rules && group.event_rules.findIndex((rule) => rule.event === event) !== -1;
      };
    },
  ],
};

export default GroupsComponentAjs;
