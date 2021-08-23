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

import { QualityRule } from '../../../../entities/qualityRule';
import NotificationService from '../../../../services/notification.service';
import QualityRuleService from '../../../../services/qualityRule.service';

class ApiQualityRuleController {
  private createMode = false;
  private qualityRule: QualityRule;

  constructor(
    private QualityRuleService: QualityRuleService,
    private NotificationService: NotificationService,
    private $state: StateService,
    private $location: ng.ILocationService,
  ) {
    'ngInject';
    this.createMode = $location.path().endsWith('new');
  }

  $onInit() {
    if (!this.qualityRule) {
      this.qualityRule = {
        description: '',
        name: '',
        weight: 0,
      };
    }
  }

  save() {
    const save = this.createMode ? this.QualityRuleService.create(this.qualityRule) : this.QualityRuleService.update(this.qualityRule);
    save.then((response) => {
      const qualityRule = response.data;
      this.NotificationService.show('Quality rule ' + qualityRule.name + ' has been saved.');
      this.$state.go('management.settings.qualityRule', { qualityRuleId: qualityRule.id }, { reload: true });
    });
  }
}

export default ApiQualityRuleController;
