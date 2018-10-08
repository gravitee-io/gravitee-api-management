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
import ApiHeaderService from "../../../services/apiHeader.service";
import {ApiPortalHeader} from "../../../entities/apiPortalHeader";

function UpdateApiPortalHeaderDialogController(
  ApiHeaderService: ApiHeaderService,
  $mdDialog: angular.material.IDialogService,
  header: ApiPortalHeader) {
  'ngInject';

  this.header = header;

  this.cancel = function() {
    $mdDialog.cancel();
  };

  this.save = function() {
    ApiHeaderService.update(this.header).then( (response) => {
      $mdDialog.hide(response.data);
    });
  };
}

export default UpdateApiPortalHeaderDialogController;
