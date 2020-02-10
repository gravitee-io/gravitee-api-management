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
import MetadataService from '../../../services/metadata.service';
import NotificationService from '../../../services/notification.service';
import UserService from '../../../services/user.service';
import {IScope} from 'angular';

class MetadataController {
  private metadata: any;
  private metadataFormats: [any];

  constructor(
    private MetadataService: MetadataService,
    private NotificationService: NotificationService,
    private $mdDialog: angular.material.IDialogService,
    private UserService: UserService,
    private $rootScope: IScope) {
    'ngInject';
    this.$rootScope = $rootScope;
  }

  newMetadata() {
    const that = this;
    this.$mdDialog.show({
      controller: 'NewMetadataDialogController',
      controllerAs: '$ctrl',
      template: require('./dialog/save.metadata.dialog.html'),
      locals: {
        metadataFormats: this.metadataFormats
      }
    }).then(function (savedMetadata) {
      that.metadata.push(savedMetadata);
      that.NotificationService.show(`Metadata '${savedMetadata.name}' created with success`);
    }).catch(function () {});
  }

  updateMetadata(metadata) {
    if (this.UserService.isUserHasPermissions(['environment-metadata-u'])) {
      const that = this;
      this.$mdDialog.show({
        controller: 'UpdateMetadataDialogController',
        controllerAs: '$ctrl',
        template: require('./dialog/save.metadata.dialog.html'),
        locals: {
          metadata: _.clone(metadata),
          metadataFormats: this.metadataFormats
        }
      }).then(function (savedMetadata) {
        _.remove(that.metadata, metadata);
        that.metadata.push(savedMetadata);
        that.NotificationService.show(`Metadata '${savedMetadata.name}' updated with success`);
      }).catch(function () {
      });
    }
  }

  deleteMetadata(metadata) {
    const that = this;
    this.$mdDialog.show({
      controller: 'DeleteMetadataDialogController',
      controllerAs: '$ctrl',
      template: require('./dialog/delete.metadata.dialog.html'),
      locals: {
        metadata: metadata
      }
    }).then(function (deleteMetadata) {
      if (deleteMetadata) {
        that.MetadataService.delete(metadata).then(function () {
          that.NotificationService.show('Metadata \'' + metadata.name + '\' deleted with success');
          _.remove(that.metadata, metadata);
        });
      }
    });
  }
}

export default MetadataController;
