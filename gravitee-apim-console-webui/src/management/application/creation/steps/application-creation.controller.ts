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
import { ActivatedRoute, Router } from '@angular/router';
import { find, findIndex, groupBy, join, keys, map, remove, uniqBy } from 'lodash';

import { ApplicationType } from '../../../../entities/application';
import { ApiService } from '../../../../services/api.service';
import ApplicationService from '../../../../services/application.service';
import NotificationService from '../../../../services/notification.service';
import '@gravitee/ui-components/wc/gv-icon';
import { PlanSecurityType } from '../../../../entities/plan';

class ApplicationCreationController {
  application: any;
  enabledApplicationTypes: ApplicationType[];
  isApplicationCreationInProgress: boolean = false;
  private activatedRoute: ActivatedRoute;

  private steps: any[];
  private selectedStep = 0;
  private selectedAPIs: any[] = [];
  private selectedPlans: any[] = [];
  private messageByPlan: any = {};
  private applicationType: string;
  private apis: any[] = [];
  private groups: any[];

  constructor(
    private Constants,
    private $mdDialog,
    private ApplicationService: ApplicationService,
    private NotificationService: NotificationService,
    private $q,
    private ApiService: ApiService,
    private ngRouter: Router,
  ) {}

  $onInit() {
    // Initialize the application so that underlying onInit can init some part of the application object as well
    this.application = {};
    this.ApiService.list().then((response) => (this.apis = response.data));
  }

  next() {
    if (this.selectedStep > 0) {
      this.applicationType = this.ApplicationService.getType(this.application);
    }
    this.goToStep(this.selectedStep + 1);
  }

  previous() {
    if (this.selectedStep === 0) {
      this.ngRouter.navigate(['../'], { relativeTo: this.activatedRoute });
    } else {
      this.goToStep(this.selectedStep - 1);
    }
  }

  goToStep(step) {
    this.selectedStep = step;
  }

  clickOnCreate() {
    if (this.shouldPromptForKeyMode()) {
      this.selectKeyMode()
        .then((mode) => (this.application.api_key_mode = mode))
        .then(() => this.createApplication());
    } else {
      this.createApplication();
    }
  }

  async createApplication() {
    if (!this.isApplicationCreationInProgress) {
      this.isApplicationCreationInProgress = true;
      const { data: application } = await this.ApplicationService.create(this.application).catch((err) => {
        this.isApplicationCreationInProgress = false;
        throw err;
      });
      for (const plan of this.selectedPlans) {
        await this.ApplicationService.subscribe(application.id, plan.id, this.messageByPlan[plan.id]).catch((err) => {
          this.isApplicationCreationInProgress = false;
          throw err;
        });
      }
      this.NotificationService.show('Application ' + this.application.name + ' has been created');
      this.ngRouter.navigate(['../', application.id], { relativeTo: this.activatedRoute });
    }
  }

  clientRegistrationEnabled() {
    return (
      this.Constants.env.settings.application &&
      this.Constants.env.settings.application.registration &&
      this.Constants.env.settings.application.registration.enabled
    );
  }

  isOAuthClient() {
    return this.application && this.application.settings && this.application.settings.oauth;
  }

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
        this.messageByPlan[plan.id] = message;
        this.confirmSubscription(api, plan);
      });
    } else {
      this.confirmSubscription(api, plan);
    }
  }

  confirmSubscription(api, plan) {
    this.selectedAPIs.push(api);
    plan.alreadySubscribed = true;
    this.selectedPlans.push(plan);
  }

  onUnsubscribe(api, plan) {
    plan.alreadySubscribed = false;
    remove(this.selectedPlans, { id: plan.id });
    const index = findIndex(this.selectedAPIs, { id: api.id });
    this.selectedAPIs.splice(index, 1);
  }

  getReadableApiSubscriptions(): string {
    const plansByApi = groupBy(this.selectedPlans, 'api');
    const multipleApis = keys(plansByApi).length > 1;
    return (
      `Subscribed to API${multipleApis ? 's:' : ''} ` +
      map(plansByApi, (plans, api) => {
        return (
          `${multipleApis ? '</br>- <code>' : '<code>'} ` +
          find(this.selectedAPIs, (a) => a.id === api).name +
          '</code> with plan <code>' +
          join(map(plans, 'name'), '</code>, ') +
          '</code>'
        );
      }) +
      '.'
    );
  }

  shouldPromptForKeyMode(): boolean {
    const apiKeyPlans = this.selectedPlans.filter((plan) => plan.security === PlanSecurityType.API_KEY);
    const uniqueApiKeyPlans = uniqBy(apiKeyPlans, 'api');
    return this.allowsSharedApiKeys && uniqueApiKeyPlans.length === apiKeyPlans.length && apiKeyPlans.length > 1;
  }

  selectKeyMode() {
    const dialog = {
      controller: 'ApiKeyModeChoiceDialogController',
      controllerAs: '$ctrl',
      template: require('html-loader!/src/components/dialog/apiKeyMode/api-key-mode-choice.dialog.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
      clickOutsideToClose: true,
    };
    return this.$mdDialog.show(dialog);
  }

  get allowsSharedApiKeys(): boolean {
    return this.Constants.env?.settings?.plan?.security?.sharedApiKey?.enabled;
  }
}
ApplicationCreationController.$inject = [
  'Constants',
  '$mdDialog',
  'ApplicationService',
  'NotificationService',
  '$q',
  'ApiService',
  'ngRouter',
];

export default ApplicationCreationController;
