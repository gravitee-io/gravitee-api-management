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
import ApplicationService from "../../../../services/application.service";
import NotificationService from "../../../../services/notification.service";
class ApplicationSubscribeController {
  private subscriptions: any[];
  private subscribedAPIs: any[] = [];
  private subscribedPlans: any[] = [];
  private application: any;
  private selectedAPI: any;
  private apis: any[] = [];
  private plans: any[] = [];

  constructor(private ApiService: ApiService, private ApplicationService: ApplicationService,
              private NotificationService: NotificationService, private $mdDialog, private $state, private $transitions) {
    'ngInject';
  }

  $onInit = () => {
    let subscriptionsByAPI = _.groupBy(this.subscriptions.data, 'api');
    _.forEach(subscriptionsByAPI, (subscriptions, api) => {
      this.subscribedAPIs.push(_.merge(_.find(this.apis, {id: api}),
        {plans: _.join(_.map(subscriptions, (sub) => this.subscriptions.metadata[sub.plan].name), ', ')}));
    });

    this.subscribedPlans = _.map(this.subscriptions.data, 'plan');
  };

  onSelectAPI = (api) => {
    if (api) {
      let authorizedSecurity = this.getAuthorizedSecurity();
      this.ApiService.getApiPlans(api.id, 'published').then((response) => {
        this.plans = _.filter(response.data, (plan) => {
          plan.alreadySubscribed = _.includes(this.subscribedPlans, plan.id);
          let subscription = _.find(this.subscriptions.data, {plan: plan.id});
          plan.pending = subscription && 'pending' === subscription.status;
          return _.includes(authorizedSecurity, plan.security);
        });
        this.selectedAPI = api;
      });
    } else {
      delete this.plans;
      delete this.selectedAPI;
    }
  };

  getAuthorizedSecurity = (): string[] => {
    let authorizedSecurity = ['api_key'];
    if (this.application.settings) {
      if (this.application.settings.oauth ||
        (this.application.settings.app && this.application.settings.app.client_id)) {
        authorizedSecurity.push('jwt', 'oauth2');
      }
    }
    return authorizedSecurity;
  };

  onSubscribe(api, plan) {
    if (plan.comment_required) {
      let confirm = this.$mdDialog.prompt()
        .title('Subscription message')
        .placeholder(plan.comment_message?plan.comment_message:'Fill a message to the API owner')
        .ariaLabel('Subscription message')
        .required(true)
        .ok('Confirm')
        .cancel('Cancel');

      this.$mdDialog.show(confirm).then((message) => {
        this.ApplicationService.subscribe(this.application.id, plan.id, message).then(() => {
          this.NotificationService.show('Subscription to application ' + this.application.name + ' has been successfully created');
          this.$state.reload();
        });
      }, () => {});
    } else {
      this.ApplicationService.subscribe(this.application.id, plan.id).then(() => {
        this.NotificationService.show('Subscription to application ' + this.application.name + ' has been successfully created');
        this.$state.reload();
      });
    }
  };

  onUnsubscribe(api, plan) {
    let alert = this.$mdDialog.confirm({
      title: 'Close subscription?',
      content: 'Are you sure you want to close this subscription?',
      ok: 'CLOSE',
      cancel: 'CANCEL'
    });

    this.$mdDialog.show(alert).then(() => {
      this.ApplicationService.closeSubscription(this.application.id, _.find(this.subscriptions.data, {plan: plan.id}).id).then(() => {
        this.NotificationService.show('Subscription has been successfully closed');
        this.$state.reload();
      });
    }, () => {});
  };
}

export default ApplicationSubscribeController;
