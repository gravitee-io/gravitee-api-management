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
import '@gravitee/ui-components/wc/gv-option';
import '@gravitee/ui-components/wc/gv-code';
import '@gravitee/ui-components/wc/gv-list';
import {
  Api,
  ApiKeyModeEnum,
  ApiService,
  Application,
  ApplicationService,
  Plan,
  Page,
  Subscription,
  SubscriptionService,
  SubscriptionsResponse,
} from '../../../../../projects/portal-webclient-sdk/src/lib';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { distinctUntilChanged } from 'rxjs/operators';
import { ConfigurationService } from '../../../services/configuration.service';
import { ItemResourceTypeEnum } from '../../../model/itemResourceType.enum';
import { FeatureEnum } from '../../../model/feature.enum';
import { getPicture, getPictureDisplayName } from '@gravitee/ui-components/src/lib/item';
import StatusEnum = Subscription.StatusEnum;
import { formatCurlCommandLine } from '../../../utils/utils';
import SecurityEnum = Plan.SecurityEnum;

@Component({
  selector: 'app-api-subscribe',
  templateUrl: './api-subscribe.component.html',
  styleUrls: ['./api-subscribe.component.css'],
})
export class ApiSubscribeComponent implements OnInit {
  private _applications: Array<Application>;
  private _allSteps: any;
  private _currentPlan: Plan;
  private _commentLabel: any;
  private _planLabel: any;
  private _missingClientIdLabel: any;
  private _allApiSubscriptions: Array<Subscription>;
  private _allApplicationSubscriptionsWithMetadata: Array<{ applicationId: string; subscriptionsResponse: SubscriptionsResponse }> = [];
  private _selectedApplicationSubscriptionsWithMetadata: SubscriptionsResponse;
  private _subscription: Subscription;
  private _currentGeneralConditions: Page;
  private _generalConditions: Map<string, Page>;

  steps: any;
  currentStep: number;
  stepperContentClass: any;
  api: Api;
  plans: any;
  application: any;
  subscribeForm: FormGroup;
  availableApplications: { label: string; value: string }[];
  apiKeyModeOptions: { id: string; title: string; description: string }[];
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

  constructor(
    private apiService: ApiService,
    private router: Router,
    private route: ActivatedRoute,
    private translateService: TranslateService,
    private applicationService: ApplicationService,
    private subscriptionService: SubscriptionService,
    private formBuilder: FormBuilder,
    private configurationService: ConfigurationService,
  ) {}

  async ngOnInit() {
    this.currentStep = 1;
    this._currentPlan = null;
    this.skeleton = true;
    this._applications = [];
    this._canSubscribe = true;
    this._generalConditions = new Map<string, Page>();
    this._currentGeneralConditions = null;
    this.connectedApps = [];

    this.subscribeForm = this.formBuilder.group({
      application: new FormControl(null, [Validators.required]),
      apiKeyMode: new FormControl(null),
      plan: new FormControl(null, [Validators.required]),
      request: new FormControl(''),
      general_conditions_accepted: new FormControl(null),
      general_conditions_content_revision: new FormControl(null),
    });
    this.translateService
      .get([i18n('apiSubscribe.apps.comment'), i18n('apiSubscribe.plan'), i18n('apiSubscribe.apps.missingClientId')])
      .toPromise()
      .then(_translations => {
        const translations = Object.values(_translations);
        this._commentLabel = translations[0];
        this._planLabel = translations[1];
        this._missingClientIdLabel = translations[2];
      });

    this.apiId = this.route.snapshot.params.apiId;
    this.api = this.route.snapshot.data.api;
    this.apiSample = formatCurlCommandLine(this.api.entrypoints[0]);
    this.apiName = this.api.name;

    if (this.canConfigureSharedApiKey()) {
      this.translateService
        .get([
          i18n('apiKeyMode.exclusive.title'),
          i18n('apiKeyMode.exclusive.description'),
          i18n('apiKeyMode.shared.title'),
          i18n('apiKeyMode.shared.description'),
        ])
        .toPromise()
        .then(_translations => {
          const translations: string[] = Object.values(_translations);
          this.apiKeyModeOptions = [
            {
              id: ApiKeyModeEnum.EXCLUSIVE,
              title: translations[0],
              description: translations[1],
            },
            {
              id: ApiKeyModeEnum.SHARED,
              title: translations[2],
              description: translations[3],
            },
          ];
        });
    }

    Promise.all([
      this.applicationService.getApplications({ size: -1, forSubscription: true }).toPromise(),
      this.apiService.getApiPlansByApiId({ apiId: this.apiId, size: -1 }).toPromise(),
      this.getSubscriptions(),
    ])
      .then(([allAppsResponse, apiPlansResponse]) => {
        this.plans = apiPlansResponse.data;
        this._applications = allAppsResponse.data;

        this.subscribeForm.valueChanges
          .pipe(distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr)))
          .subscribe(() => this.updateSteps());

        this.subscribeForm.valueChanges.pipe(distinctUntilChanged((prev, curr) => prev.application === curr.application)).subscribe(() => {
          if (this.canConfigureSharedApiKey()) {
            this.updateSelectedAppSubscriptions().then(() => {
              this.updateSteps();
              if (this.canDisplayApiKeyModeStep()) {
                this.subscribeForm.get('apiKeyMode').setValidators(Validators.required);
              } else {
                this.subscribeForm.get('apiKeyMode').clearValidators();
              }
              this.subscribeForm.get('apiKeyMode').setValue(null);
              this.subscribeForm.get('apiKeyMode').updateValueAndValidity();
            });
          } else {
            this.updateSteps();
          }
        });

        this.subscribeForm.valueChanges.pipe(distinctUntilChanged((prev, curr) => prev.plan === curr.plan)).subscribe(() => {
          this.subscribeForm.controls.application.setValue(null);
          this.updateApplications();
          if (this.planHasGeneralConditions()) {
            this.subscribeForm.get('general_conditions_accepted').setValidators(Validators.requiredTrue);
          } else {
            this.subscribeForm.get('general_conditions_accepted').clearValidators();
          }

          this.subscribeForm.get('general_conditions_accepted').setValue(false);
          this.subscribeForm.get('general_conditions_content_revision').setValue(null);
        });

        this.subscribeForm.valueChanges
          .pipe(distinctUntilChanged((prev, curr) => prev.general_conditions_accepted === curr.general_conditions_accepted))
          .subscribe(() => {
            if (this.subscribeForm.get('general_conditions_accepted').value === true) {
              this.subscribeForm.get('general_conditions_content_revision').setValue(this._currentGeneralConditions.contentRevisionId);
            } else {
              this.subscribeForm.get('general_conditions_content_revision').setValue(null);
            }
          });

        const plan = this.hasPlans() ? this.plans[0].id : null;
        this.subscribeForm.controls.plan.setValue(plan);
        this.skeleton = false;
      })
      .catch(() => (this.connectedApps = []));

    const stepsTitle: any[] = [
      i18n('apiSubscribe.choosePlan.title'),
      i18n('apiSubscribe.chooseApp.title'),
      i18n('apiSubscribe.validate.title'),
    ];
    if (this.canConfigureSharedApiKey()) {
      stepsTitle.splice(2, 0, i18n('apiSubscribe.chooseKeyMode.title'));
    }

    this._allSteps = await Promise.all(
      stepsTitle.map(title => {
        return this.translateService
          .get(title)
          .toPromise()
          .then(_title => {
            return { title: _title };
          });
      }),
    );
    this.steps = this._allSteps;
  }

  private updateSteps() {
    const isAppValid = this.subscribeForm.get('application').errors == null && this.subscribeForm.get('request').errors == null;
    const isKeyModeValid = this.subscribeForm.get('apiKeyMode').errors == null;

    const stepsItems: any[] = [
      { description: this.getPlanName() },
      { description: this.getApplicationName(), valid: isAppValid },
      { description: this.getCreatedAt(), valid: this._subscription != null },
    ];

    if (this.canConfigureSharedApiKey()) {
      stepsItems.splice(2, 0, { description: this.getApplicationKeyMode(), valid: isKeyModeValid });
    }
    this.steps = stepsItems
      .map(({ description, valid }, index) => {
        const step = this._allSteps[index];
        step.description = description;
        step.valid = !!description && (typeof valid === 'boolean' ? valid : true);
        return step;
      })
      .slice(0, this.isKeyLess() ? 1 : this._allSteps.length);

    if (!this.canDisplayApiKeyModeStep()) {
      this.steps.splice(2, 1);
    }
  }

  onChangeStep({ detail: { current } }) {
    if (this.isKeyLess()) {
      return;
    } else if (!this.canDisplayApiKeyModeStep() && current === 3) {
      this.currentStep = 4;
    } else {
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
        if (this.planHasGeneralConditions()) {
          if (this._generalConditions.get(this._currentPlan.general_conditions) == null) {
            this.apiService
              .getPageByApiIdAndPageId({
                apiId: this.apiId,
                pageId: this._currentPlan.general_conditions,
                include: ['content'],
              })
              .toPromise()
              .then(p => {
                this._generalConditions.set(p.id, p);
                this._currentGeneralConditions = p;
              });
          } else {
            this._currentGeneralConditions = this._generalConditions.get(this._currentPlan.general_conditions);
          }
        } else {
          this._currentGeneralConditions = null;
        }
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

  getSelectedApplication() {
    const applicationId = this.subscribeForm.value.application;
    if (applicationId) {
      return this._applications.find(application => application.id === applicationId);
    }
    return undefined;
  }

  getApplicationName() {
    const app = this.getSelectedApplication();
    return app ? app.name : '';
  }

  getApplicationKeyMode() {
    const apiKeyMode = this.subscribeForm.value.apiKeyMode;
    if (apiKeyMode) {
      const keyMode = this.apiKeyModeOptions.find(option => option.id === apiKeyMode);
      return keyMode ? keyMode.title : '';
    }
    return '';
  }

  onNext() {
    if (this.canNext()) {
      this.currentStep += 1;
    }
    if (!this.canDisplayApiKeyModeStep() && this.currentStep === 3) {
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
    if (!this.canDisplayApiKeyModeStep() && this.currentStep === 3) {
      this.currentStep -= 1;
    }
  }

  hasPlans() {
    return this.plans && this.plans.length > 0;
  }

  getCurrentGeneralConditions() {
    return this._currentGeneralConditions;
  }

  planHasGeneralConditions() {
    return this._currentPlan && this._currentPlan.general_conditions && this._currentPlan.general_conditions !== '';
  }

  hasStepper() {
    return this.hasPlans() && this.plans.filter(plan => plan.security.toUpperCase() !== Plan.SecurityEnum.KEYLESS).length > 0;
  }

  isApiKey() {
    const currentPlan = this.getCurrentPlan();
    if (currentPlan && currentPlan.security.toUpperCase() === Plan.SecurityEnum.APIKEY) {
      return true;
    }
    return false;
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

        const apiKeyMode = this.subscribeForm.value.apiKeyMode;
        if (apiKeyMode) {
          const selectedApp = this.getSelectedApplication();
          if (selectedApp.api_key_mode === ApiKeyModeEnum.UNSPECIFIED) {
            const appToUpdate = await this.applicationService.getApplicationByApplicationId({ applicationId: selectedApp.id }).toPromise();
            appToUpdate.api_key_mode = apiKeyMode;
            await this.applicationService
              .updateApplicationByApplicationId({ application: appToUpdate, applicationId: appToUpdate.id })
              .toPromise();
          }
        }

        let subscription = await this.subscriptionService
          .createSubscription({
            subscriptionInput: {
              application: this.subscribeForm.value.application,
              plan: this.subscribeForm.value.plan,
              request: this.subscribeForm.value.request,
              general_conditions_accepted: this.subscribeForm.value.general_conditions_accepted,
              general_conditions_content_revision: this.subscribeForm.value.general_conditions_content_revision,
            },
          })
          .toPromise();

        if (this.hasAutoValidation()) {
          subscription = await this.subscriptionService
            .getSubscriptionById({
              subscriptionId: subscription.id,
              include: ['keys'],
            })
            .toPromise();
          const currentPlan = this.getCurrentPlan();
          if (currentPlan.security.toUpperCase() === Plan.SecurityEnum.APIKEY) {
            const apikeyHeader = this.configurationService.get('portal.apikeyHeader');
            this.apiSample = formatCurlCommandLine(this.api.entrypoints[0], { name: apikeyHeader, value: subscription.keys[0].key });
          } else if (
            currentPlan.security.toUpperCase() === Plan.SecurityEnum.OAUTH2 ||
            currentPlan.security.toUpperCase() === Plan.SecurityEnum.JWT
          ) {
            this.apiSample = formatCurlCommandLine(this.api.entrypoints[0], { name: 'Authorization', value: 'Bearer xxxx-xxxx-xxxx-xxxx' });
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
          if (error.code === 'errors.plan.general_conditions_revision') {
            return this.ngOnInit();
          } else {
            const translation = await this.translateService.get(error.code).toPromise();
            this.subscriptionError = translation;
          }
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
    return this.plans.find(plan => plan.id === planId);
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

  displayGeneralConditions() {
    if (this.planHasGeneralConditions()) {
      if (this.hasSubscription() || this.hasSubscriptionError) {
        return false;
      }
      if (this.subscribeForm.valid) {
        return true;
      }
      const invalidControls = Object.values(this.subscribeForm.controls).filter(control => control.invalid);
      if (invalidControls.length === 1 && invalidControls[0] === this.subscribeForm.get('general_conditions_accepted')) {
        return true;
      }
    }
    return false;
  }

  getSubscriptionKey() {
    return this._subscription && this._subscription.keys && this._subscription.keys[0] ? this._subscription.keys[0].key : null;
  }

  private updateApplications() {
    if (this._allApiSubscriptions) {
      const subscribedApps = [];
      if (this._subscription) {
        this._allApiSubscriptions = [this._subscription].concat(this._allApiSubscriptions);
      }
      const plan = this.getCurrentPlan();
      if (plan) {
        let disabled = false;
        let title;
        this.availableApplications = this._applications
          .map(application => {
            disabled = false;
            title = undefined;
            const label = `${application.name} (${application.owner.display_name})`;
            const appSubscriptions = this._allApiSubscriptions.filter(sub => sub.application === application.id);
            if (appSubscriptions.length > 0) {
              const appPlansSubscriptions = appSubscriptions.filter(subscription => subscription.plan === plan.id);
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
          })
          .filter(app => app !== null);
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
      return appSubscriptions.find(subscription => subscription.plan === plan.id) == null;
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
    return this.subscriptionService
      .getSubscriptions({
        apiId: this.apiId,
        size: -1,
        statuses: [StatusEnum.ACCEPTED, StatusEnum.PENDING, StatusEnum.PAUSED],
      })
      .toPromise()
      .then(response => {
        this._allApiSubscriptions = response.data;
      })
      .catch(err => {
        if (err.status === 403) {
          this._canSubscribe = false;
        }
      });
  }

  canCreateApp() {
    return this.configurationService.hasFeature(FeatureEnum.applicationCreation);
  }

  canConfigureSharedApiKey() {
    return this.configurationService.hasFeature(FeatureEnum.sharedApiKey);
  }

  canDisplayApiKeyModeStep() {
    return (
      this.canConfigureSharedApiKey() &&
      this.isApiKey() &&
      this.getSelectedApplication()?.api_key_mode === ApiKeyModeEnum.UNSPECIFIED &&
      this.hasAtLeastOneApiKeySubscription()
    );
  }

  async updateSelectedAppSubscriptions() {
    const selectedApplication = this.getSelectedApplication();
    if (selectedApplication) {
      this._selectedApplicationSubscriptionsWithMetadata = this._allApplicationSubscriptionsWithMetadata?.find(
        item => item.applicationId === selectedApplication.id,
      )?.subscriptionsResponse;
      if (!this._selectedApplicationSubscriptionsWithMetadata) {
        const subscriptionsResponse = await this.subscriptionService
          .getSubscriptions({
            applicationId: selectedApplication.id,
            size: -1,
          })
          .toPromise();
        this._allApplicationSubscriptionsWithMetadata.push({ applicationId: selectedApplication.id, subscriptionsResponse });
        this._selectedApplicationSubscriptionsWithMetadata = subscriptionsResponse;
      }
    }
  }

  hasAtLeastOneApiKeySubscription() {
    if (this._selectedApplicationSubscriptionsWithMetadata) {
      const appSubscriptions: Subscription[] = this._selectedApplicationSubscriptionsWithMetadata.data;
      const appSubscriptionsMetadata: any = this._selectedApplicationSubscriptionsWithMetadata.metadata;
      const nbSubsOnApiKeyPlans = appSubscriptions.filter(
        sub => sub.api !== this.apiId && appSubscriptionsMetadata[sub.plan]?.securityType === SecurityEnum.APIKEY,
      ).length;
      return nbSubsOnApiKeyPlans >= 1;
    }
    return false;
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

  @HostListener(':gv-list:click', ['$event.detail'])
  onGvListClick(detail: any) {
    this.router.navigate([`/applications/${detail.item.id}`]);
  }
}
