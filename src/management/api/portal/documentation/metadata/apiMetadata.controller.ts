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
import NotificationService from '../../../../../services/notification.service';
import ApiService from '../../../../../services/api.service';
import UserService from "../../../../../services/user.service";

class ApiMetadataController {
  private api: any;

  constructor(
    private NotificationService: NotificationService,
    private $mdDialog: angular.material.IDialogService,
    private resolvedApi: any,
    private ApiService: ApiService,
    private UserService: UserService,
    private metadataFormats: any,
    private metadata: any,
    private $state) {
    'ngInject';
    this.api = resolvedApi.data;
  }

  newMetadata() {
    this.$mdDialog.show({
      controller: 'NewApiMetadataDialogController',
      controllerAs: '$ctrl',
      template: require('./dialog/save.api.metadata.dialog.html'),
      locals: {
        api: this.api,
        metadataFormats: this.metadataFormats
      }
    }).then((savedMetadata) => {
      this.NotificationService.show(`Metadata '${savedMetadata.name}' created with success`);
      this.$state.reload();
    }).catch(function () {});
  }

  updateMetadata(metadata) {
    if (this.UserService.isUserHasPermissions(["api-metadata-u"])) {
      this.$mdDialog.show({
        controller: 'UpdateApiMetadataDialogController',
        controllerAs: '$ctrl',
        template: require('./dialog/save.api.metadata.dialog.html'),
        locals: {
          api: this.api,
          apiMetadata: _.clone(metadata),
          metadataFormats: this.metadataFormats
        }
      }).then((metadata) => {
        this.NotificationService.show(`API's Metadata '${metadata.name}' updated with success`);
        this.$state.reload();
      }).catch(() => {});
    }
  }

  deleteMetadata(metadata) {
    this.$mdDialog.show({
      controller: 'DeleteApiMetadataDialogController',
      controllerAs: '$ctrl',
      template: require('./dialog/delete.api.metadata.dialog.html'),
      locals: {
        metadata: metadata
      }
    }).then((deleteMetadata) => {
      if (deleteMetadata) {
        this.ApiService.deleteMetadata(this.api.id, metadata.key).then(() => {
          this.NotificationService.show("Metadata '" + metadata.name + "' deleted with success");
          this.$state.reload();
        });
      }
    });
  }
}

export default ApiMetadataController;
