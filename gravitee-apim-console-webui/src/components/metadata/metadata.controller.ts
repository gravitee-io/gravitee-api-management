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

import MetadataService from '../../services/metadata.service';
import NotificationService from '../../services/notification.service';
import UserService from '../../services/user.service';

class MetadataController {
  private metadata: any;
  private metadataFormats: [any];
  private referenceType: string;
  private canCreate: boolean;
  private canUpdate: boolean;
  private canDelete: boolean;

  constructor(
    private MetadataService: MetadataService,
    private NotificationService: NotificationService,
    private $mdDialog: angular.material.IDialogService,
    private UserService: UserService,
    private $rootScope: IScope,
    private $state: StateService,
  ) {
    'ngInject';
    this.$rootScope = $rootScope;

    if ($state.params.apiId) {
      this.referenceType = 'API';
    } else if ($state.params.applicationId) {
      this.referenceType = 'Application';
    }
    const permissionPrefix = this.referenceType ? this.referenceType.toLowerCase() : 'environment';
    this.canCreate = this.UserService.isUserHasPermissions([permissionPrefix + '-metadata-c']);
    this.canUpdate = this.UserService.isUserHasPermissions([permissionPrefix + '-metadata-u']);
    this.canDelete = this.UserService.isUserHasPermissions([permissionPrefix + '-metadata-d']);
  }

  newMetadata() {
    this.$mdDialog
      .show({
        controller: 'NewMetadataDialogController',
        controllerAs: '$ctrl',
        template: require('./dialog/save.metadata.dialog.html'),
        locals: {
          metadataFormats: this.metadataFormats,
        },
      })
      .then((savedMetadata) => {
        this.NotificationService.show(`Metadata '${savedMetadata.name}' created with success`);
        this.$state.reload();
      })
      .catch(() => {
        // don't display error in console
      });
  }

  updateMetadata(metadata) {
    this.$mdDialog
      .show({
        controller: 'UpdateMetadataDialogController',
        controllerAs: '$ctrl',
        template: require('./dialog/save.metadata.dialog.html'),
        locals: {
          metadata: _.clone(metadata),
          metadataFormats: this.metadataFormats,
        },
      })
      .then((savedMetadata) => {
        this.NotificationService.show(`Metadata '${savedMetadata.name}' updated with success`);
        this.$state.reload();
      })
      .catch(() => {
        // don't display error in console
      });
  }

  deleteMetadata(metadata) {
    this.$mdDialog
      .show({
        controller: 'DeleteMetadataDialogController',
        controllerAs: '$ctrl',
        template: require('./dialog/delete.metadata.dialog.html'),
        locals: {
          metadata: metadata,
        },
      })
      .then((deleteMetadata) => {
        if (deleteMetadata) {
          this.NotificationService.show("Metadata '" + metadata.name + "' deleted with success");
          this.$state.reload();
        }
      })
      .catch(() => {
        // don't display error in console
      });
  }

  metadataDeletable(metadata) {
    return !this.referenceType || (this.referenceType && metadata.value !== undefined);
  }
}

export default MetadataController;
