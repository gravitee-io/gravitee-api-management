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
import RoleService from '../../../../../services/role.service';
import NotificationService from '../../../../../services/notification.service';
import _ = require('lodash');

class RoleSaveController {
  private formRole: any;
  private role: any;
  private permissions: [string];
  private editMode: any;

  constructor(private RoleService: RoleService,
              private NotificationService: NotificationService,
              private $state: ng.ui.IStateService) {
    'ngInject';

    this.editMode = !!$state.params.role;
    if ($state.params.role) {
      const that = this;
      RoleService.get($state.params.roleScope, $state.params.role).then(function (role) {
        that.permissions = RoleService.listPermissionsByScope(that.$state.params.roleScope);
        that.role = role;
        that.modelToView();
      });
    } else {
      this.permissions = RoleService.listPermissionsByScope($state.params.roleScope);

      if (this.permissions) {
        this.role = {
          scope: $state.params.roleScope,
          permissions: _.zipObject(this.permissions, _.map(this.permissions, function () {
            return {};
          }))
        };
      } else {
        $state.go('management.settings.roles');
      }
    }
  }

  save() {
    let savePromise;
    if (this.editMode) {
      savePromise = this.RoleService.update(this.viewToModel());
    } else {
      savePromise = this.RoleService.create(this.viewToModel());
    }

    const that = this;
    savePromise.then(function (savedRole) {
      that.role = savedRole;
      that.modelToView();
      that.formRole.$setPristine();
      that.NotificationService.show(`Role ${that.editMode ? 'updated' : 'created'} with success`);
    });
  }

  private viewToModel() {
    let roleCopy = _.clone(this.role);
    roleCopy.permissions = _(roleCopy.permissions)
      .mapValues(value => {
        return _(value).pickBy((value) => value).keys()
      })
      .value();
    return roleCopy;
  }

  private modelToView() {
    this.role.permissions = _(this.role.permissions)
      .mapValues(value => {
        let values = _.values(value);
        return _.zipObject(values, _.map(values, () => true))
      })
      .value();
  }
}

export default RoleSaveController;
