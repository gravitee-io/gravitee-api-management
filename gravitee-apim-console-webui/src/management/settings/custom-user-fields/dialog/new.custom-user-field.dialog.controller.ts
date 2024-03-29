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

import CustomUserFieldsService from '../../../../services/custom-user-fields.service';

function NewFieldDialogController(
  CustomUserFieldsService: CustomUserFieldsService,
  $mdDialog: angular.material.IDialogService,
  predefinedKeys,
) {
  this.updateAction = false;
  this.field = {};
  this.predefinedKeys = predefinedKeys;

  this.cancel = function () {
    $mdDialog.cancel();
  };

  this.save = () => {
    CustomUserFieldsService.create(this.field).then((response) => {
      $mdDialog.hide(response.data);
    });
  };
}
NewFieldDialogController.$inject = ['CustomUserFieldsService', '$mdDialog', 'predefinedKeys'];

export default NewFieldDialogController;
