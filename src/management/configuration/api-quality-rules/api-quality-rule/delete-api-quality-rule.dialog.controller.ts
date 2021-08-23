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
import { IScope } from 'angular';

import { QualityRule } from '../../../../entities/qualityRule';
import QualityRuleService from '../../../../services/qualityRule.service';

function DeleteApiQualityRuleDialogController(
  $scope: IScope,
  $mdDialog: angular.material.IDialogService,
  qualityRule: QualityRule,
  QualityRuleService: QualityRuleService,
) {
  'ngInject';
  this.qualityRule = qualityRule;

  this.cancel = function () {
    $mdDialog.cancel();
  };

  this.ok = function () {
    QualityRuleService.delete(qualityRule).then(() => {
      $mdDialog.hide(true);
    });
  };
}

export default DeleteApiQualityRuleDialogController;
