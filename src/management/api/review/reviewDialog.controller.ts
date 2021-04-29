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
import _ = require('lodash');
import QualityRuleService from '../../../services/qualityRule.service';
import { QualityRule } from '../../../entities/qualityRule';

function DialogReviewController($scope, $mdDialog, api, QualityRuleService: QualityRuleService, $q, apiQualityRules, qualityRules) {
  'ngInject';
  this.apiQualityRules = apiQualityRules;
  this.qualityRules = qualityRules;

  this.cancel = function () {
    $mdDialog.hide();
  };

  this.confirm = function (accept: boolean) {
    let promises = [];
    _.forEach(this.qualityRules, (qualityRule) => {
      let apiQualityRule: any = _.find(this.apiQualityRules, { quality_rule: qualityRule.id });
      let checked: boolean = apiQualityRule && apiQualityRule.checked;
      if (!apiQualityRule || apiQualityRule.new) {
        promises.push(QualityRuleService.createApiRule(api.id, qualityRule.id, checked));
      } else {
        promises.push(QualityRuleService.updateApiRule(api.id, qualityRule.id, checked));
      }
    });
    $q.all(promises).then(() => {
      $mdDialog.hide({
        accept: accept,
        message: this.message,
      });
    });
  };

  this.toggleQualityRule = (qualityRule: QualityRule) => {
    let apiQualityRule: any = _.find(this.apiQualityRules, { quality_rule: qualityRule.id });
    if (apiQualityRule) {
      apiQualityRule.checked = !apiQualityRule.checked;
    } else {
      this.apiQualityRules.push({
        api: api.id,
        quality_rule: qualityRule.id,
        checked: true,
        new: true,
      });
    }
  };

  this.isChecked = (qualityRule: QualityRule) => {
    let apiQualityRule: any = _.find(this.apiQualityRules, { quality_rule: qualityRule.id });
    return apiQualityRule && apiQualityRule.checked;
  };
}

export default DialogReviewController;
