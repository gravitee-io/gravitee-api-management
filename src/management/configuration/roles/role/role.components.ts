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
import RoleService from '../../../../services/role.service';
import NotificationService from '../../../../services/notification.service';
import _ = require('lodash');
import { StateService } from '@uirouter/core';

const RoleComponent: ng.IComponentOptions = {
  bindings: {},
  template: require('./role.html'),
  controller: function ( RoleService: RoleService,
                         NotificationService: NotificationService,
                         $state: StateService) {
    'ngInject';
    this.$onInit = () => {
      let that = this;
      this.editMode = !!$state.params.role;
      if ($state.params.role) {
        RoleService.get($state.params.roleScope, $state.params.role).then( (role) => {
          that.permissions = RoleService.listPermissionsByScope($state.params.roleScope);
          that.role = role;
          that._modelToView();
        });
      } else {
        this.permissions = RoleService.listPermissionsByScope($state.params.roleScope);

        if (this.permissions) {
          this.role = {
            scope: $state.params.roleScope,
            permissions: _.zipObject(this.permissions, _.map(this.permissions, () => {
              return {};
            }))
          };
        } else {
          $state.go('management.settings.roles');
        }
      }
    };

    this.save = () => {
      let savePromise;
      if (this.editMode) {
        savePromise = RoleService.update(this._viewToModel());
      } else {
        savePromise = RoleService.create(this._viewToModel());
      }

      let that = this;
      savePromise.then(function (savedRole) {
        that.role = savedRole;
        that._modelToView();
        that.formRole.$setPristine();
        NotificationService.show(`Role ${that.editMode ? 'updated' : 'created'} with success`);
      });
    };

    this._viewToModel = () => {
      let roleCopy = _.clone(this.role);
      roleCopy.permissions = _(roleCopy.permissions)
        .mapValues(value => {
          return _(value).pickBy((value) => value).keys()
        })
        .value();
      return roleCopy;
    };

    this._modelToView = () => {
      this.role.permissions = _(this.role.permissions)
        .mapValues(value => {
          let values = _.values(value);
          return _.zipObject(values, _.map(values, () => true))
        })
        .value();
    };
  }
};

export default RoleComponent;
