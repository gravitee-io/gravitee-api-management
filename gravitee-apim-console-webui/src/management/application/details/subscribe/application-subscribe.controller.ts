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

import { ApiService } from '../../../../services/api.service';
import ApplicationService from '../../../../services/application.service';
import NotificationService from '../../../../services/notification.service';

class ApplicationSubscribeController {
  private subscriptions: any;
  private subscribedAPIs: any[] = [];
  private subscribedPlans: any[] = [];
  private application: any;
  private selectedAPI: any;
  private canAccessSelectedApiPlans = false;
  private apis: any[] = [];
  private plans: any[] = [];
  private groups: any[] = [];

  constructor(
    private ApiService: ApiService,
    private ApplicationService: ApplicationService,
    private NotificationService: NotificationService,
    private $mdDialog,
    private $state,
    private $transitions,
  ) {
    'ngInject';
  }

  $onInit = () => {
    const subscriptionsByAPI = _.groupBy(this.subscriptions.data, 'api');
    _.forEach(subscriptionsByAPI, (subscriptions, api) => {
      this.subscribedAPIs.push(
        _.merge(_.find(this.apis, { id: api }), {
          plans: _.join(
            _.map(subscriptions, (sub) => this.subscriptions.metadata[sub.plan].name),
            ', ',
          ),
        }),
      );
    });

    this.subscribedPlans = _.map(this.subscriptions.data, 'plan');
  };

  onSelectAPI = (api) => {
    if (api) {
      const authorizedSecurity = this.getAuthorizedSecurity();
      this.selectedAPI = api;
      this.canAccessSelectedApiPlans = false;
      this.ApiService.getApiPlans(api.id, 'PUBLISHED')
        .then((response) => {
          this.canAccessSelectedApiPlans = true;
          this.plans = _.filter(response.data, (plan) => {
            plan.alreadySubscribed = _.includes(this.subscribedPlans, plan.id);
            const subscription = _.find(this.subscriptions.data, { plan: plan.id });
            plan.pending = subscription && 'PENDING' === subscription.status;
            return _.includes(authorizedSecurity, plan.security);
          });
          this.selectedAPI = api;
          this.refreshPlansExcludedGroupsNames();
        })
        .catch((error) => {
          if (error.status === 403 && error.interceptorFuture) {
            error.interceptorFuture.cancel();
          }
        });
    } else {
      delete this.plans;
      delete this.selectedAPI;
    }
  };

  getAuthorizedSecurity = (): string[] => {
    const authorizedSecurity = ['API_KEY'];
    if (this.application.settings) {
      if (this.application.settings.oauth || (this.application.settings.app && this.application.settings.app.client_id)) {
        authorizedSecurity.push('JWT', 'OAUTH2');
      }
    }
    return authorizedSecurity;
  };

  onSubscribe(api, plan) {
    if (plan.comment_required) {
      const confirm = this.$mdDialog
        .prompt()
        .title('Subscription message')
        .placeholder(plan.comment_message ? plan.comment_message : 'Fill a message to the API owner')
        .ariaLabel('Subscription message')
        .required(true)
        .ok('Confirm')
        .cancel('Cancel');

      this.$mdDialog.show(confirm).then((message) => {
        this.ApplicationService.subscribe(this.application.id, plan.id, message).then(() => {
          this.NotificationService.show('Subscription to application ' + this.application.name + ' has been successfully created');
          this.$state.reload();
        });
      });
    } else {
      this.ApplicationService.subscribe(this.application.id, plan.id).then(() => {
        this.NotificationService.show('Subscription to application ' + this.application.name + ' has been successfully created');
        this.$state.reload();
      });
    }
  }

  onUnsubscribe(api, plan) {
    const alert = this.$mdDialog.confirm({
      title: 'Close subscription?',
      content: 'Are you sure you want to close this subscription?',
      ok: 'CLOSE',
      cancel: 'CANCEL',
    });

    this.$mdDialog.show(alert).then(() => {
      this.ApplicationService.closeSubscription(this.application.id, _.find(this.subscriptions.data, { plan: plan.id }).id).then(() => {
        this.NotificationService.show('Subscription has been successfully closed');
        this.$state.reload();
      });
    });
  }

  refreshPlansExcludedGroupsNames() {
    this.plans.forEach(
      (plan) =>
        (plan.excluded_groups_names = plan.excluded_groups?.map(
          (excludedGroupId) => this.groups.find((apiGroup) => apiGroup.id === excludedGroupId)?.name,
        )),
    );
  }
}

export default ApplicationSubscribeController;
