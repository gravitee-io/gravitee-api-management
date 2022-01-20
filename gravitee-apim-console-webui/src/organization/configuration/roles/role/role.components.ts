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
import * as _ from 'lodash';

import NotificationService from '../../../../services/notification.service';
import RoleService from '../../../../services/role.service';

const RoleComponent: ng.IComponentOptions = {
  bindings: {
    roleScopes: '<',
  },
  template: require('./role.html'),
  controller: function (RoleService: RoleService, NotificationService: NotificationService, $state: StateService) {
    'ngInject';
    this.$onInit = () => {
      this.editMode = !!$state.params.role;
      this.permissions = this.roleScopes[$state.params.roleScope];
      if ($state.params.role) {
        RoleService.get($state.params.roleScope, $state.params.role).then((role) => {
          this.role = role;
          this._modelToView();
          this.checkSelectAll();
        });
      } else {
        if (this.permissions) {
          this.role = {
            scope: $state.params.roleScope,
            permissions: _.zipObject(
              this.permissions,
              _.map(this.permissions, () => {
                return {};
              }),
            ),
          };
        } else {
          $state.go('organization.settings.ajs-roles');
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

      savePromise.then((savedRole) => {
        this.role = savedRole;
        this._modelToView();
        this.formRole.$setPristine();
        NotificationService.show(`Role ${this.editMode ? 'updated' : 'created'} with success`);
        if (!this.editMode) {
          $state.go('organization.settings.ajs-roleedit', { roleScope: this.role.scope, role: this.role.name });
        }
      });
    };

    this._viewToModel = () => {
      const roleCopy = _.clone(this.role);
      roleCopy.permissions = _(roleCopy.permissions)
        .mapValues((value) => {
          return _(value)
            .pickBy((value) => value)
            .keys();
        })
        .value();
      return roleCopy;
    };

    this._modelToView = () => {
      this.role.permissions = _(this.role.permissions)
        .mapValues((value) => {
          const values = _.values(value);
          return _.zipObject(
            values,
            _.map(values, () => true),
          );
        })
        .value();
    };

    this.selectAll = (action, checked) => {
      _.forEach(this.permissions, (permission) => {
        if (!this.role.permissions[permission]) {
          this.role.permissions[permission] = {};
        }
        this.role.permissions[permission][action] = checked;
      });
    };

    this.checkSelectAll = function () {
      this.createCheckedAll = _.every(this.permissions, (permission) => this.role.permissions[permission].C);
      this.readCheckedAll = _.every(this.permissions, (permission) => this.role.permissions[permission].R);
      this.updateCheckedAll = _.every(this.permissions, (permission) => this.role.permissions[permission].U);
      this.deleteCheckedAll = _.every(this.permissions, (permission) => this.role.permissions[permission].D);
    };

    this.isEnvironmentTagOrTenant = (permission: string): boolean => {
      return this.role?.scope === 'ENVIRONMENT' && (permission === 'TAG' || permission === 'TENANT' || permission === 'ENTRYPOINT');
    };
  },
};

export default RoleComponent;
