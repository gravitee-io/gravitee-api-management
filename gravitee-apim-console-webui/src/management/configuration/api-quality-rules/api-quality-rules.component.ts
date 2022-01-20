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
import * as _ from 'lodash';

import NotificationService from '../../../services/notification.service';
import PortalSettingsService from '../../../services/portalSettings.service';

const ApiQualityRulesComponent: ng.IComponentOptions = {
  bindings: {
    qualityRules: '<',
  },
  template: require('./api-quality-rules.html'),
  controller: function (
    Constants,
    $rootScope: IScope,
    PortalSettingsService: PortalSettingsService,
    NotificationService: NotificationService,
    $mdDialog: angular.material.IDialogService,
  ) {
    'ngInject';
    this.$rootScope = $rootScope;
    this.settings = _.cloneDeep(Constants.env.settings);
    this.providedConfigurationMessage = 'Configuration provided by the system';

    this.save = () => {
      PortalSettingsService.save(this.settings).then((response) => {
        _.merge(Constants.env.settings, response.data);
        NotificationService.show('API Quality settings saved!');
        this.formQuality.$setPristine();
      });
    };

    this.reset = () => {
      this.settings = _.cloneDeep(Constants.env.settings);
      this.formQuality.$setPristine();
    };

    this.delete = (qualityRule) => {
      $mdDialog
        .show({
          controller: 'DeleteApiQualityRuleDialogController',
          controllerAs: '$ctrl',
          template: require('./api-quality-rule/delete-api-quality-rule.dialog.html'),
          locals: {
            qualityRule: qualityRule,
          },
        })
        .then((deletedQualityRule) => {
          if (deletedQualityRule) {
            NotificationService.show("Quality rule '" + qualityRule.name + "' deleted with success");
            _.remove(this.qualityRules, qualityRule);
          }
        });
    };

    this.isReadonlySetting = (property: string): boolean => {
      return PortalSettingsService.isReadonly(this.settings, property);
    };
  },
};

export default ApiQualityRulesComponent;
