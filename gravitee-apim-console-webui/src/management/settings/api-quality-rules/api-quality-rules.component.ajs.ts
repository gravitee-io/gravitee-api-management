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

import { Router } from '@angular/router';
import { cloneDeep, merge, remove } from 'lodash';

import NotificationService from '../../../services/notification.service';
import PortalSettingsService from '../../../services/portalSettings.service';
import QualityRuleService from '../../../services/qualityRule.service';
import UserService from '../../../services/user.service';

const ApiQualityRulesComponentAjs: ng.IComponentOptions = {
  bindings: {
    activatedRoute: '<',
  },
  template: require('html-loader!./api-quality-rules.html'),
  controller: [
    'Constants',
    '$rootScope',
    'PortalSettingsService',
    'NotificationService',
    'QualityRuleService',
    '$mdDialog',
    'UserService',
    'ngRouter',
    function (
      Constants,
      $rootScope: IScope,
      PortalSettingsService: PortalSettingsService,
      NotificationService: NotificationService,
      QualityRuleService: QualityRuleService,
      $mdDialog: angular.material.IDialogService,
      UserService: UserService,
      ngRouter: Router,
    ) {
      this.$rootScope = $rootScope;
      this.ngRouter = ngRouter;
      this.settings = cloneDeep(Constants.env.settings);
      this.providedConfigurationMessage = 'Configuration provided by the system';
      this.canUpdateSettings = UserService.isUserHasPermissions([
        'environment-settings-c',
        'environment-settings-u',
        'environment-settings-d',
      ]);

      this.$onInit = () => {
        QualityRuleService.list().then((response) => (this.qualityRules = response?.data));
      };

      this.save = () => {
        PortalSettingsService.save(this.settings).then((response) => {
          merge(Constants.env.settings, response.data);
          NotificationService.show('API Quality settings saved!');
          this.formQuality.$setPristine();
        });
      };

      this.reset = () => {
        this.settings = cloneDeep(Constants.env.settings);
        this.formQuality.$setPristine();
      };

      this.delete = (qualityRule) => {
        $mdDialog
          .show({
            controller: 'DeleteApiQualityRuleDialogController',
            controllerAs: '$ctrl',
            template: require('html-loader!./api-quality-rule/delete-api-quality-rule.dialog.html'),
            locals: {
              qualityRule: qualityRule,
            },
          })
          .then((deletedQualityRule) => {
            if (deletedQualityRule) {
              NotificationService.show("Quality rule '" + qualityRule.name + "' deleted with success");
              remove(this.qualityRules, qualityRule);
            }
          });
      };

      this.isReadonlySetting = (property: string): boolean => {
        return PortalSettingsService.isReadonly(this.settings, property);
      };

      this.createNewRule = () => {
        return this.ngRouter.navigate(['new'], { relativeTo: this.activatedRoute });
      };

      this.editRule = (ruleId: string) => {
        return this.ngRouter.navigate([ruleId], { relativeTo: this.activatedRoute });
      };
    },
  ],
};

export default ApiQualityRulesComponentAjs;
