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

import { StateService } from '@uirouter/angularjs';

import NotificationService from '../../../services/notification.service';
import UserService from '../../../services/user.service';

class CustomUserFieldsController {
  private canCreate: boolean;
  private canUpdate: boolean;
  private canDelete: boolean;

  private fieldFormats: [string];
  private predefinedKeys: [string];

  private fields: any;

  constructor(
    private $mdDialog: angular.material.IDialogService,
    private NotificationService: NotificationService,
    private UserService: UserService,
    private $state: StateService,
  ) {
    'ngInject';

    const permissionPrefix = 'organization-custom_user_fields';
    this.canCreate = this.UserService.isUserHasPermissions([permissionPrefix + '-c']);
    this.canUpdate = this.UserService.isUserHasPermissions([permissionPrefix + '-u']);
    this.canDelete = this.UserService.isUserHasPermissions([permissionPrefix + '-d']);
  }

  // eslint-disable-next-line @typescript-eslint/no-empty-function
  $onInit = () => {};

  newField() {
    this.$mdDialog
      .show({
        controller: 'NewFieldDialogController',
        controllerAs: '$ctrl',
        template: require('./dialog/save.custom-user-field.dialog.html'),
        locals: {
          fieldFormats: this.fieldFormats,
          predefinedKeys: this.predefinedKeys,
        },
      })
      .then((savedMetadata) => {
        this.NotificationService.show(`Field '${savedMetadata.key}' created with success`);
        this.$state.reload();
      })
      .catch(() => {
        // don't display error in console
      });
  }

  updateField(field) {
    this.$mdDialog
      .show({
        controller: 'UpdateFieldDialogController',
        controllerAs: '$ctrl',
        template: require('./dialog/save.custom-user-field.dialog.html'),
        locals: {
          field: field,
          fieldFormats: this.fieldFormats,
          predefinedKeys: this.predefinedKeys,
        },
      })
      .then((savedMetadata) => {
        this.NotificationService.show(`Field '${savedMetadata.key}' updated with success`);
        this.$state.reload();
      })
      .catch(() => {
        // don't display error in console
      });
  }

  deleteField(field) {
    this.$mdDialog
      .show({
        controller: 'DeleteFieldDialogController',
        controllerAs: '$ctrl',
        template: require('./dialog/delete.custom-user-field.dialog.html'),
        locals: {
          field: field,
        },
      })
      .then((savedMetadata) => {
        this.NotificationService.show(`Field '${savedMetadata.key}' deleted with success`);
        this.$state.reload();
      })
      .catch(() => {
        // don't display error in console
      });
  }

  fieldDeletable() {
    return this.canDelete;
  }
}

export default CustomUserFieldsController;
