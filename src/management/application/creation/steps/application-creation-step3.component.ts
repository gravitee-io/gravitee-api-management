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
import ApiService from "../../../../services/api.service";
import _ = require('lodash');

const ApplicationCreationStep3Component: ng.IComponentOptions = {
  require: {
    parent: '^createApplication'
  },
  template: require("./application-creation-step3.html"),
  controller: function(ApiService: ApiService, $scope) {
    'ngInject';

    this.onSelectAPI = (api) => {
      if (api) {
        let authorizedSecurity = this.getAuthorizedSecurity();
        ApiService.getApiPlans(api.id, 'published').then((response) => {
          let filteredPlans = _.filter(response.data, (plan) => {
            return _.includes(authorizedSecurity, plan.security);
          });
          this.plans = _.map(filteredPlans, (plan) => {
            let selectedPlan = _.find(this.parent.selectedPlans, {id: plan.id});
            if (selectedPlan) {
              return selectedPlan;
            }
            return plan;
          });
          this.selectedAPI = api;
        });
      } else {
        delete this.plans;
        delete this.selectedAPI;
      }
    };

    this.getAuthorizedSecurity = (): string[] => {
      let authorizedSecurity = ['api_key'];
      if (this.parent.application.settings) {
        if (this.parent.application.settings.oauth ||
          (this.parent.application.settings.app && this.parent.application.settings.app.client_id)) {
          authorizedSecurity.push('jwt', 'oauth2');
        }
      }
      return authorizedSecurity;
    };

    this.getSelectedAPIs = (): any[] => {
      let selectedAPIs = _.uniqBy(this.parent.selectedAPIs, 'id');
      _.map(selectedAPIs, (api) => {
        let selectedPlans = _.filter(this.parent.selectedPlans, (plan) => {
          return plan.apis.indexOf(api.id) !== -1;
        });
        api.plans = _.join(_.map(selectedPlans, 'name'), ', ');
      });
      return selectedAPIs;
    };

    $scope.$watch('$ctrl.parent.application.settings', () => {
      this.parent.selectedAPIs = [];
      this.parent.selectedPlans = [];
      delete this.plans;
      delete this.selectedAPI;
      delete this.filterAPI;
    }, true);
  }
};

export default ApplicationCreationStep3Component;
