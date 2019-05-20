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
import ApiService from "../../../services/api.service";
import NotificationService from "../../../services/notification.service";
import * as _ from "lodash";
import ApplicationService from "../../../services/application.service";

const ApiSubscribeComponent: ng.IComponentOptions = {
  bindings: {
    api: '<',
    plans: '<',
    applications: '<',
    subscriptions: '<',
    entrypoints: '<'
  },
  template: require('./api-subscribe.html'),
  controller: class {

    private api: any;
    private plans: any;

    private selectedPlan: any;
    private selectedApp: any;

    private planInformation: any;
    private apiKey: any;
    private subscription: any;
    private requestMessage: string;

    constructor(
      private $stateParams,
      private NotificationService: NotificationService,
      private ApplicationService: ApplicationService,
      private ApiService: ApiService,
      private Constants) {
      'ngInject';
    }

    $onInit() {
      this.selectedPlan = _.find(this.plans, (plan: any) => {
        return plan.id === this.$stateParams.planId;
      });
    }

    checkSubscriptions() {
      if (this.selectedApp) {
        this.ApplicationService.listSubscriptions(this.selectedApp.id, '?status=accepted,pending&plan=' + this.selectedPlan.id)
          .then((subscriptions: any) => {
            this.subscription = subscriptions.data.data[0];
            if (this.subscription !== undefined) {
              this.fetchApiKey(this.subscription.id);
            }
          });
      }
    }

    fetchApiKey(subscriptionId) {
      this.ApplicationService.listApiKeys(this.selectedApp.id, subscriptionId).then((apiKeys) => {
        let apiKey = _.find(apiKeys.data, function (apiKey: any) {
          return !apiKey.revoked;
        });
        if (apiKey) {
          this.apiKey = apiKey.key;
        }
      });
    }

    subscribe(application: any) {
      this.ApplicationService.subscribe(application.id, this.selectedPlan.id, this.requestMessage).then((subscription) => {
        this.subscription = subscription.data;
        this.NotificationService.show('api.subscription.step3.successful', null, {planName: this.selectedPlan.name});
        this.fetchApiKey(this.subscription.id);
      });
    }

    isPlanSubscribable() {
      return this.selectedPlan && 'key_less' !== this.selectedPlan.security;
    }

    canApplicationSubscribe() {
      return this.isPlanSubscribable()
        && (!this.selectedPlan['comment_required'] || (this.selectedPlan['comment_required'] === true && this.requestMessage))
        && this.selectedApp
        && !this.subscription
        && (
          (
            ('oauth2' === this.selectedPlan.security || 'jwt' === this.selectedPlan.security)
            && (this.selectedApp.settings && this.selectedApp.settings.client_id))
          || (this.selectedPlan.security === 'api_key')
        );
    }

    onApiKeyClipboardSuccess(e) {
      this.NotificationService.show('api.subscription.step3.apikey.clipboard');
      e.clearSelection();
    }

    onApplicationSearchChange() {
      delete this.apiKey;
      delete this.selectedApp;
      delete this.subscription;
      this.checkSubscriptions();
    }

    onApplicationSelect() {
      this.checkSubscriptions();
    }

    onPlanSelect() {
      delete this.selectedApp;
      delete this.subscription;
    }

    getCurlSample() {
      let entrypoints = this.ApiService.getTagEntrypoints(this.api, this.entrypoints);
      let planEntrypoints = _.filter(entrypoints, (entrypoint) => _.intersection(entrypoint.tags, this.selectedPlan.tags).length > 0);

      return _.map(planEntrypoints, (entrypoint) => {
        return 'curl -X GET "' + entrypoint.value + this.api.context_path + '" -H "' + (this.apiKey ?
          this.Constants.portal.apikeyHeader + ': ' + (this.apiKey ? this.apiKey : 'given_api_key') + '"' :
          '"Authorization: Bearer xxxx-xxxx-xxxx-xxxx"');
      });
    }
  }
};

export default ApiSubscribeComponent;
