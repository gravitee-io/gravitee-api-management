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
import { ApiService } from '../../../services/api.service';
import ApplicationService from '../../../services/application.service';
import MetadataService from '../../../services/metadata.service';
function UpdateMetadataDialogController(
  MetadataService: MetadataService,
  ApiService: ApiService,
  ApplicationService: ApplicationService,
  $mdDialog: angular.material.IDialogService,
  metadata,
  metadataFormats,
  $stateParams,
) {
  'ngInject';

  if ($stateParams.apiId) {
    this.referenceType = 'API';
    this.referenceId = $stateParams.apiId;
  } else if ($stateParams.applicationId) {
    this.referenceType = 'APPLICATION';
    this.referenceId = $stateParams.applicationId;
  }

  this.metadata = metadata;
  if ('DATE' === metadata.format) {
    metadata.value = metadata.value ? new Date(metadata.value) : null;
    if (metadata.defaultValue) {
      metadata.defaultValue = new Date(metadata.defaultValue);
    }
  }

  this.metadataFormats = metadataFormats;

  this.cancel = function () {
    $mdDialog.cancel();
  };

  this.save = () => {
    if ($stateParams.apiId) {
      ApiService.updateMetadata($stateParams.apiId, this.metadata).then((response) => {
        $mdDialog.hide(response.data);
      });
    } else if ($stateParams.applicationId) {
      ApplicationService.updateMetadata($stateParams.applicationId, this.metadata).then((response) => {
        $mdDialog.hide(response.data);
      });
    } else {
      MetadataService.update(this.metadata).then((response) => {
        $mdDialog.hide(response.data);
      });
    }
  };
}

export default UpdateMetadataDialogController;
