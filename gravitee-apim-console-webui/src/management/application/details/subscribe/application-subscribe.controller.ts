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
import * as _ from 'lodash';

import { ApiService } from '../../../../services/api.service';
import ApplicationService from '../../../../services/application.service';
import NotificationService from '../../../../services/notification.service';
import PortalConfigService from '../../../../services/portalConfig.service';
import { ApiKeyMode } from '../../../../entities/applications/application';

class ApplicationSubscribeController {
  private subscriptions: any;
  private subscribedAPIs: any[] = [];
  private subscribedPlans: any[] = [];
  private application: any;
  private selectedAPI: any;
  private apis: any[] = [];
  private plans: any[] = [];
  private groups: any[] = [];
  private portalSettings: any;

  constructor(
    private ApiService: ApiService,
    private ApplicationService: ApplicationService,
    private NotificationService: NotificationService,
    private PortalConfigService: PortalConfigService,
    private $mdDialog,
    private $state,
    private $transitions,
  ) {
    'ngInject';
  }

  async $onInit() {
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

    const { data: portalSettings } = await this.PortalConfigService.get();
    this.portalSettings = portalSettings;

    this.subscribedPlans = _.map(this.subscriptions.data, 'plan');
  }

  onSelectAPI(api) {
    if (api) {
      const authorizedSecurity = this.getAuthorizedSecurity();
      this.ApiService.getApiPlans(api.id, 'PUBLISHED').then((response) => {
        this.plans = _.filter(response.data, (plan) => {
          plan.alreadySubscribed = _.includes(this.subscribedPlans, plan.id);
          const subscription = _.find(this.subscriptions.data, { plan: plan.id });
          plan.pending = subscription && 'PENDING' === subscription.status;
          return _.includes(authorizedSecurity, plan.security);
        });
        this.selectedAPI = api;
        this.refreshPlansExcludedGroupsNames();
      });
    } else {
      delete this.plans;
      delete this.selectedAPI;
    }
  }

  getAuthorizedSecurity(): string[] {
    const authorizedSecurity = ['API_KEY'];
    if (this.application.settings) {
      if (this.application.settings.oauth || (this.application.settings.app && this.application.settings.app.client_id)) {
        authorizedSecurity.push('JWT', 'OAUTH2');
      }
    }
    return authorizedSecurity;
  }

  async onSubscribe(api, plan) {
    if (this.shouldPromptForKeyMode) {
      this.selectKeyMode()
        .then((mode) => this.ApplicationService.update({ ...this.application, api_key_mode: mode }))
        .then(() => this.doSubscribe(plan), _.noop);
    } else {
      await this.doSubscribe(plan);
    }
  }

  async doSubscribe(plan) {
    const message = await this.getMessage(plan);

    this.ApplicationService.subscribe(this.application.id, plan.id, message).then(() => {
      this.NotificationService.show('Subscription to application ' + this.application.name + ' has been successfully created');
      this.$state.reload();
    });
  }

  async getMessage(plan: any) {
    if (plan.comment_required) {
      const confirm = this.$mdDialog
        .prompt()
        .title('Subscription message')
        .placeholder(plan.comment_message ? plan.comment_message : 'Fill a message to the API owner')
        .ariaLabel('Subscription message')
        .required(true)
        .ok('Confirm')
        .cancel('Cancel');

      return this.$mdDialog.show(confirm, _.noop);
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

  selectKeyMode() {
    const dialog = {
      controller: 'ApplicationKeyDialogController',
      controllerAs: '$ctrl',
      template: require('./application-key.dialog.html'),
      clickOutsideToClose: true,
    };

    return this.$mdDialog.show(dialog);
  }

  get shouldPromptForKeyMode(): boolean {
    return this.allowsSharedApiKeys && this.subscribedAPIs.length >= 1 && this.application.api_key_mode === ApiKeyMode.UNSPECIFIED;
  }

  get allowsSharedApiKeys(): boolean {
    return this.portalSettings?.plan?.security?.sharedApiKey?.enabled;
  }
}

export default ApplicationSubscribeController;
