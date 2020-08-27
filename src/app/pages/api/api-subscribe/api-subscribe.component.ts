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
import { Component, HostListener, OnInit } from '@angular/core';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import '@gravitee/ui-components/wc/gv-stepper';
import '@gravitee/ui-components/wc/gv-plans';
import '@gravitee/ui-components/wc/gv-code';
import '@gravitee/ui-components/wc/gv-list';
import {
  Api,
  ApiService,
  Application,
  ApplicationService,
  Subscription,
  SubscriptionService
} from '@gravitee/ng-portal-webclient';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { Plan } from '@gravitee/ng-portal-webclient';
import StatusEnum = Subscription.StatusEnum;
import { distinctUntilChanged } from 'rxjs/operators';
import { ConfigurationService } from '../../../services/configuration.service';
import { ItemResourceTypeEnum } from 'src/app/model/itemResourceType.enum';
import { FeatureEnum } from 'src/app/model/feature.enum';
import { getPictureDisplayName, getPicture } from '@gravitee/ui-components/src/lib/item';

@Component({
  selector: 'app-api-subscribe',
  templateUrl: './api-subscribe.component.html',
  styleUrls: ['./api-subscribe.component.css']
})
export class ApiSubscribeComponent implements OnInit {

  private _applications: Array<Application>;
  private _allSteps: any;
  private _currentPlan: Plan;
  private _commentLabel: any;
  private _planLabel: any;
  private _missingClientIdLabel: any;
  private _allSubscriptions: Array<Subscription>;
  private _subscription: Subscription;

  steps: any;
  currentStep: number;
  api: Api;
  plans: any;
  application: any;
  subscribeForm: FormGroup;
  availableApplications: { label: string; value: string }[];
  connectedApps: any[];
  apiId: any;
  skeleton: boolean;
  code: any;
  apiSample: any;
  apiName: string;
  showValidateLoader: boolean;
  hasSubscriptionError: boolean;
  subscriptionError: string;
  private _canSubscribe: boolean;

  constructor(private apiService: ApiService,
              private router: Router,
              private route: ActivatedRoute,
              private translateService: TranslateService,
              private apiServices: ApiService,
              private applicationService: ApplicationService,
              private subscriptionService: SubscriptionService,
              private formBuilder: FormBuilder,
              private configurationService: ConfigurationService,
  ) {
    this.currentStep = 1;
    this.skeleton = true;
    this._applications = [];
    this._canSubscribe = true;
  }

  async ngOnInit() {
    this.connectedApps = [];
    this.subscribeForm = this.formBuilder.group({
      application: new FormControl(null, [Validators.required]),
      plan: new FormControl(null, [Validators.required]),
      request: new FormControl(''),
    });
    this.translateService.get([
      i18n('apiSubscribe.apps.comment'),
      i18n('apiSubscribe.plan'),
      i18n('apiSubscribe.apps.missingClientId')
    ])
      .toPromise()
      .then((_translations) => {
        const translations = Object.values(_translations);
        this._commentLabel = translations[0];
        this._planLabel = translations[1];
        this._missingClientIdLabel = translations[2];
      });

    this.apiId = this.route.snapshot.params.apiId;
    this.api = this.route.snapshot.data.api;
    this.apiSample = `curl "${this.api.entrypoints[0]}"`;
    this.apiName = this.api.name;

    Promise.all([
      this.applicationService.getApplications({ size: -1, forSubscription: true }).toPromise(),
      this.apiService.getApiPlansByApiId({ apiId: this.apiId }).toPromise(),
      this.getSubscriptions(),
    ]).then(([allAppsResponse, apiPlansResponse]) => {
      this.plans = apiPlansResponse.data;
      this._applications = allAppsResponse.data;

      this.subscribeForm.valueChanges
        .pipe(distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr)))
        .subscribe(() => this.updateSteps());

      this.subscribeForm.valueChanges
        .pipe(distinctUntilChanged((prev, curr) => prev.plan === curr.plan))
        .subscribe(() => {
          this.subscribeForm.controls.application.setValue(null);
          this.updateApplications();
        });

      const plan = this.hasPlans() ? this.plans[0].id : null;
      this.subscribeForm.controls.plan.setValue(plan);
      this.skeleton = false;
    }).catch(() => (this.connectedApps = []));

    this._allSteps = await Promise.all([
      i18n('apiSubscribe.choosePlan.title'),
      i18n('apiSubscribe.chooseApp.title'),
      i18n('apiSubscribe.validate.title')
    ].map((title) => {
      return this.translateService.get(title).toPromise().then((_title) => {
        return { title: _title };
      });
    }));
    this.steps = this._allSteps;
  }

  private updateSteps() {
    const isValid = this.subscribeForm.get('application').errors == null && this.subscribeForm.get('request').errors == null;
    this.steps = [
      { description: this.getPlanName() },
      { description: this.getApplicationName(), valid: isValid },
      { description: this.getCreatedAt(), valid: this._subscription != null }
    ].map(({ description, valid }, index) => {
      const step = this._allSteps[index];
      step.description = description;
      step.valid = !!description && (typeof valid === 'boolean' ? valid : true);
      return step;
    }).slice(0, this.isKeyLess() ? 1 : this._allSteps.length);
  }

  onChangeStep({ detail: { current } }) {
    if (!this.isKeyLess()) {
      this.currentStep = current;
    }
  }

  getPlanName() {
    const currentPlan = this.getCurrentPlan();
    if (currentPlan) {
      return currentPlan.name;
    }
    return '';
  }

  private getCurrentPlan() {
    const planId = this.subscribeForm.value.plan;
    if (this.hasPlans()) {
      if (this._currentPlan == null || this._currentPlan.id !== planId) {
        this._currentPlan = this.findPlanById(planId);
      }
      return this._currentPlan;
    }
    return null;
  }

  private getPlanValidation() {
    const plan = this.getCurrentPlan();
    if (plan) {
      return plan.validation.toUpperCase();
    }
    return null;
  }

  public hasAutoValidation() {
    return this.getPlanValidation() === Plan.ValidationEnum.AUTO;
  }

  getApplicationName() {
    const applicationId = this.subscribeForm.value.application;
    if (applicationId) {
      const app = this._applications.find((application) => application.id === applicationId);
      return app ? app.name : '';
    }
    return '';
  }

  onNext() {
    if (this.canNext()) {
      this.currentStep += 1;
    }
  }

  canNext() {
    return this._allSteps && this.currentStep < this._allSteps.length;
  }

  canPrevious() {
    return this.currentStep > 1;
  }

  onPrevious() {
    this.hasSubscriptionError = false;
    this.currentStep -= 1;
  }

  hasPlans() {
    return this.plans && this.plans.length > 0;
  }

  hasStepper() {
    return this.hasPlans() && this.plans.filter((plan) => plan.security.toUpperCase() !== Plan.SecurityEnum.KEYLESS).length > 0;
  }

  isKeyLess() {
    const currentPlan = this.getCurrentPlan();
    if (currentPlan && currentPlan.security.toUpperCase() === Plan.SecurityEnum.KEYLESS) {
      return true;
    }
    return false;
  }

  isOAuth2() {
    const currentPlan = this.getCurrentPlan();
    if (currentPlan && currentPlan.security.toUpperCase() === Plan.SecurityEnum.OAUTH2) {
      return true;
    }
    return false;
  }

  isJWT() {
    const currentPlan = this.getCurrentPlan();
    if (currentPlan && currentPlan.security.toUpperCase() === Plan.SecurityEnum.JWT) {
      return true;
    }
    return false;
  }

  getCommentLabel() {
    const currentPlan = this.getCurrentPlan();
    let label = this._commentLabel;
    if (currentPlan && currentPlan.comment_question) {
      label = currentPlan.comment_question;
    }
    return label;
  }

  hasRequiredComment() {
    const currentPlan = this.getCurrentPlan();
    return currentPlan && currentPlan.comment_required === true;
  }

  hasConnectedApps() {
    return this.connectedApps && this.connectedApps.length > 0;
  }

  getConnectedAppsCount() {
    return this.connectedApps ? this.connectedApps.length : 0;
  }

  async onValidate() {
    if (this.subscribeForm.valid) {
      try {
        this.showValidateLoader = true;
        let subscription = await this.subscriptionService.createSubscription({
          SubscriptionInput: this.subscribeForm.value
        }).toPromise();

        if (this.hasAutoValidation()) {
          subscription = await this.subscriptionService.getSubscriptionById({
            subscriptionId: subscription.id,
            include: ['keys']
          }).toPromise();
          const currentPlan = this.getCurrentPlan();
          if (currentPlan.security.toUpperCase() === Plan.SecurityEnum.APIKEY) {
            const apikeyHeader = this.configurationService.get('portal.apikeyHeader');
            this.apiSample += ` -H "${apikeyHeader}:${subscription.keys[0].id}"`;
          } else if (currentPlan.security.toUpperCase() === Plan.SecurityEnum.OAUTH2 ||
            currentPlan.security.toUpperCase() === Plan.SecurityEnum.JWT) {
            this.apiSample += ` -H "Authorization: Bearer xxxx-xxxx-xxxx-xxxx"`;
          } else {
            this.apiSample = null;
          }
        } else {
          this.apiSample = null;
        }
        this._subscription = subscription;

      } catch (exception) {
        if (exception.error && exception.error.errors) {
          const error = exception.error.errors[0];
          const translation = await this.translateService.get(error.code).toPromise();
          this.subscriptionError = translation;
        }
        this.hasSubscriptionError = true;
      } finally {
        this.updateApplications();
        this.updateSteps();
        this.showValidateLoader = false;
      }
    }
  }

  onExit() {
    this.router.navigate(['../'], { relativeTo: this.route });
  }

  private findPlanById(planId: string): Plan {
    return this.plans.find((plan) => plan.id === planId);
  }

  hasSubscription() {
    return this._subscription != null && this.subscribeForm.valid;
  }

  hasSubscriptionAccepted() {
    return this.hasSubscription() && this._subscription.status.toUpperCase() === StatusEnum.ACCEPTED;
  }

  hasSubscriptionPending() {
    return this.hasSubscription() && this._subscription.status.toUpperCase() === StatusEnum.PENDING;
  }

  hasSubscriptionRejected() {
    return this.hasSubscription() && this._subscription.status.toUpperCase() === StatusEnum.REJECTED;
  }

  hasPreviousAction() {
    return !this.isKeyLess() && !this.hasSubscription();
  }

  hasNextAction() {
    return this.canNext() && !this.isKeyLess() && !this.hasSubscription() && !this.hasSubscriptionError;
  }

  hasValidateAction() {
    return this._canSubscribe === true && !this.canNext() && !this.isKeyLess() && !this.hasSubscription() && !this.hasSubscriptionError;
  }

  getSubscriptionKey() {
    return this._subscription && this._subscription.keys && this._subscription.keys[0] ? this._subscription.keys[0].id : null;
  }

  private updateApplications() {
    if (this._allSubscriptions) {
      const subscribedApps = [];
      if (this._subscription) {
        this._allSubscriptions = [this._subscription].concat(this._allSubscriptions);
      }
      const plan = this.getCurrentPlan();
      if (plan) {
        let disabled = false;
        let title;
        this.availableApplications = this._applications.map(application => {
          disabled = false;
          title = undefined;
          const label = `${application.name} (${application.owner.display_name})`;
          const appSubscriptions = this._allSubscriptions.filter((sub) => sub.application === application.id);
          if (appSubscriptions.length > 0) {
            const appPlansSubscriptions = appSubscriptions.filter((subscription) => subscription.plan === plan.id);
            if (appPlansSubscriptions.length > 0) {
              subscribedApps.push({
                item: application,
                subscriptions: appPlansSubscriptions,
                type: ItemResourceTypeEnum.APPLICATION,
              });
              if (!this.canSubscribe(appPlansSubscriptions, plan)) {
                return null;
              }
            }
          }
          if (!disabled) {
            disabled = !this.isSecure(application, plan);
            if (disabled) {
              title = this._missingClientIdLabel;
            }
          }

          return { label, value: application.id, disabled, title };
        }).filter((app) => app !== null);
        this.connectedApps = subscribedApps;
      }
    }
  }

  @HostListener(':gv-plans:redirect')
  public on() {
    this.router.navigate(['/catalog/api', this.apiId, 'contact']);
  }

  private getCreatedAt() {
    return this._subscription ? new Date(this._subscription.created_at).toLocaleString(this.translateService.currentLang) : '';
  }

  private canSubscribe(appSubscriptions: Subscription[], plan: Plan) {
    if (appSubscriptions && appSubscriptions.length > 0) {
      return appSubscriptions.find((subscription) => subscription.plan === plan.id) == null;
    }
    return true;
  }

  private isSecure(application: Application, plan: Plan) {
    if (plan && (plan.security.toUpperCase() === Plan.SecurityEnum.OAUTH2 || plan.security.toUpperCase() === Plan.SecurityEnum.JWT)) {
      return application.hasClientId;
    }
    return true;
  }

  hasAvailableApplications() {
    return this.availableApplications && this.availableApplications.length > 0;
  }

  private getSubscriptions() {
    return this.subscriptionService.getSubscriptions({
      apiId: this.apiId,
      size: -1,
      statuses: [StatusEnum.ACCEPTED, StatusEnum.PENDING, StatusEnum.PAUSED]
    })
      .toPromise()
      .then(response => {
        this._allSubscriptions = response.data;
      })
      .catch((err) => {
        if (err.status === 403) {
          this._canSubscribe = false;
        }
      });
  }

  canCreateApp() {
    return this.configurationService.hasFeature(FeatureEnum.applicationCreation);
  }

  get displayName() {
    return getPictureDisplayName(this.api);
  }

  get picture() {
    return getPicture(this.api);
  }

  goToCategory(category: string) {
    this.router.navigate(['/catalog/categories', category]);
  }

  goToSearch(tag: string) {
    this.router.navigate(['catalog/search'], { queryParams: { q: tag } });
  }

  goToExtern(url: string) {
    window.open(url, '_blank');
  }
}
