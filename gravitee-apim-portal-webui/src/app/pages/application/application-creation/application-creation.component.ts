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
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import '@gravitee/ui-components/wc/gv-stepper';
import '@gravitee/ui-components/wc/gv-option';
import '@gravitee/ui-components/wc/gv-switch';
import '@gravitee/ui-components/wc/gv-file-upload';
import { TranslateService } from '@ngx-translate/core';
import { ActivatedRoute, Router } from '@angular/router';
import { getApplicationTypeIcon } from '@gravitee/ui-components/src/lib/theme';

import { ConfigurationService } from '../../../services/configuration.service';
import {
  Api,
  ApiKeyModeEnum,
  Application,
  ApplicationInput,
  ApplicationService,
  ApplicationType,
  Plan,
  SubscriptionService,
} from '../../../../../projects/portal-webclient-sdk/src/lib';
import { NotificationService } from '../../../services/notification.service';
import { FeatureEnum } from '../../../model/feature.enum';

import { AppFormType, OAuthFormType } from './application-creation-step2/application-creation-step2.component';
import { CreationFormType } from './application-creation-step1/application-creation-step1.component';

const SecurityEnum = Plan.SecurityEnum;

export interface ApplicationTypeOption extends ApplicationType {
  icon: string;
  description: string;
  title: string;
}

interface StepState {
  description: string;
  valid: boolean;
  invalid: boolean;
}

type ApplicationFormType = FormGroup<{
  name: FormControl<string>;
  description: FormControl<string>;
  domain: FormControl<string>;
  picture: FormControl<string>;
  settings: FormControl<
    (
      | { app: { type: string; client_id: string } }
      | { oauth: { redirect_uris: any[]; grant_types: any[]; application_type: string; additionalClientMetadata: any[] } }
    ) & {
      tls: { client_certificate: string };
    }
  >;
}>;

function mapToApplicationInput(rawValue): ApplicationInput {
  const result = rawValue as ApplicationInput;
  if (rawValue.oauth !== undefined) {
    result.settings.oauth.additional_client_metadata = rawValue.oauth.additionalClientMetadata.reduce((acc, { key, value }) => {
      acc[key] = value;
      return acc;
    }, {});
  }
  return result;
}

@Component({
  selector: 'app-application-creation',
  templateUrl: './application-creation.component.html',
  styleUrls: ['./application-creation.component.css'],
})
export class ApplicationCreationComponent implements OnInit {
  private _allSteps: any;
  steps: any;
  currentStep: number;
  applicationForm: ApplicationFormType;
  allowedTypes: Array<ApplicationTypeOption>;
  plans: Array<Plan>;
  subscribeList: {
    api: Api;
    plan: Plan;
    requiredComment: boolean;
    request: string;
    general_conditions_accepted?: boolean;
    general_conditions_content_revision?: string;
    channel?: string;
    entrypoint?: string;
    entrypointConfiguration?: object;
  }[];
  apiKeyMode: ApiKeyModeEnum = ApiKeyModeEnum.UNSPECIFIED;
  apiKeyModeTitle: string;

  private readSteps: number[];

  creationInProgress: boolean;
  creationSuccess: boolean;
  creationError: boolean;
  createdApplication: Application;
  applicationType: ApplicationTypeOption;
  private stepOneForm: CreationFormType;
  private stepTwoForm: AppFormType | OAuthFormType;

  subscriptionErrors: { api: Api; message: string }[];

  constructor(
    private translateService: TranslateService,
    private configurationService: ConfigurationService,
    private formBuilder: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private notificationService: NotificationService,
    private subscriptionService: SubscriptionService,
    private applicationService: ApplicationService,
    private ref: ChangeDetectorRef,
  ) {
    this.currentStep = 1;
    this.readSteps = [1];
    this.subscriptionErrors = [];
  }

  async ngOnInit() {
    const stepsTitle: any[] = [
      'applicationCreation.step.general',
      'applicationCreation.step.security',
      'applicationCreation.step.subscription',
      'applicationCreation.step.validate',
    ];
    if (this.canConfigureSharedApiKey()) {
      stepsTitle.splice(3, 0, 'applicationCreation.step.apiKeyMode');
    }

    this._allSteps = await Promise.all(
      stepsTitle.map(_title =>
        this.translateService
          .get(_title)
          .toPromise()
          .then(title => ({ title })),
      ),
    );
    this.steps = this._allSteps;
    this.allowedTypes = await Promise.all(
      this.route.snapshot.data.enabledApplicationTypes.map(type => {
        return this.translateService
          .get([`applicationType.${type.id}.title`, `applicationType.${type.id}.description`])
          .toPromise()
          .then(translations => {
            const [title, description] = Object.values(translations);
            return {
              ...type,
              icon: getApplicationTypeIcon(type.id),
              title,
              description,
            };
          });
      }),
    );

    this.applicationForm = this.formBuilder.group({
      name: new FormControl(null, [Validators.required]),
      description: new FormControl(null, [Validators.required]),
      domain: new FormControl(null),
      picture: new FormControl(null),
      settings: new FormControl(null, [Validators.required]),
    });
  }

  setCurrentStep(step: number) {
    if (!this.creationSuccess) {
      if (!this.readSteps.includes(step)) {
        this.readSteps.push(step);
        if (step === 3) {
          this.subscribeList = [];
        }
      }
      this.ref.detectChanges();
      this.updateSteps();
      this.currentStep = step;
    }
  }

  onChangeStep({ detail: { current } }) {
    if (!this.canDisplayApiKeyModeStep() && current === 4) {
      current += 1;
    }
    this.notificationService.reset();
    this.creationError = false;
    this.setCurrentStep(current);
  }

  get requireClientId() {
    if (this.applicationType && this.isSimpleApp && this.subscribeList) {
      const subscription = this.subscribeList.find(s => !this.hasValidClientId(s.plan));
      if (subscription) {
        return true;
      }
    }
    return false;
  }

  hasValidClientId(plan) {
    if (
      this.isSimpleApp &&
      (plan.security?.toUpperCase() === Plan.SecurityEnum.OAUTH2 || plan.security?.toUpperCase() === Plan.SecurityEnum.JWT)
    ) {
      const { settings } = this.applicationForm.getRawValue();
      if ((settings as any).app) {
        if ((settings as any).app.client_id == null || (settings as any).app.client_id.trim() === '') {
          return false;
        }
      }
    }
    return true;
  }

  hasValidSubscriptions() {
    return (
      this.readSteps.includes(3) &&
      this.subscribeList.find(s => this.hasRequireComment(s.plan) && (s.request == null || s.request.trim() === '')) == null
    );
  }

  hasRequireComment(plan) {
    return plan && plan.comment_required;
  }

  onStepOneUpdated(stepOneForm: CreationFormType) {
    this.stepOneForm = stepOneForm;
    this.applicationForm.patchValue(this.stepOneForm.getRawValue());
    this.updateSteps();
  }

  get stepOneState(): StepState {
    if (this.stepOneForm) {
      return {
        description: this.stepOneForm.get('name').value,
        valid: this.stepOneForm.valid,
        invalid: this.readSteps.includes(2) && this.stepOneForm.invalid,
      };
    }
    return { description: '', valid: false, invalid: false };
  }

  onStepTwoUpdated(stepTwoForm: AppFormType | OAuthFormType) {
    this.stepTwoForm = stepTwoForm;
    this.applicationForm.get('settings').patchValue(this.stepTwoForm.getRawValue());
    this.updateSteps();
  }

  get stepTwoState(): StepState {
    if (this.stepTwoForm) {
      const description = this.readSteps.includes(2) && this.applicationType ? this.applicationType.name : '';
      return { description, valid: this.stepTwoForm.valid, invalid: this.stepTwoForm.invalid };
    }
    return { description: '', valid: false, invalid: false };
  }

  onStepThreeUpdated(subscribeList: any[]) {
    this.subscribeList = subscribeList;
    this.updateSteps().then(() => this.ref.detectChanges());
  }

  async stepThreeState(): Promise<StepState> {
    if (this.subscribeList) {
      const description = await this.translateService
        .get('applicationCreation.subscription.description', { count: this.subscribeList.length })
        .toPromise();
      const valid = this.hasValidSubscriptions();
      return { description, valid, invalid: !valid };
    }
    return { description: '', valid: false, invalid: false };
  }

  onStepFourUpdated(apiKeyMode: ApiKeyModeEnum) {
    this.apiKeyMode = apiKeyMode;
    this.updateSteps();
  }

  async stepFourState(): Promise<StepState> {
    if (this.apiKeyMode !== ApiKeyModeEnum.UNSPECIFIED) {
      this.apiKeyModeTitle = await this.translateService.get(`apiKeyMode.${this.apiKeyMode.toLowerCase()}.title`).toPromise();
      const valid = [ApiKeyModeEnum.SHARED, ApiKeyModeEnum.EXCLUSIVE].includes(this.apiKeyMode);
      return { description: this.apiKeyModeTitle, valid, invalid: !valid };
    }
    return { description: '', valid: false, invalid: false };
  }

  private async updateSteps() {
    const createdAt = this.createdApplication
      ? new Date(this.createdApplication.created_at).toLocaleString(this.translateService.currentLang)
      : '';

    const stepThreeStep = await this.stepThreeState();
    const stepsStates = [
      this.stepOneState,
      this.stepTwoState,
      stepThreeStep,
      { description: createdAt, valid: this.creationSuccess, invalid: false },
    ];
    if (this.canConfigureSharedApiKey()) {
      const stepFourStep = await this.stepFourState();
      stepsStates.splice(3, 0, stepFourStep);
    }

    this.steps = stepsStates.map(({ description, valid, invalid }, index) => {
      const step = this._allSteps[index];
      step.description = description;
      step.valid = this.readSteps.includes(index + 1) && valid;
      step.invalid = this.readSteps.includes(index + 1) && invalid;
      return step;
    });
    if (!this.canDisplayApiKeyModeStep()) {
      this.steps.splice(3, 1);
    }
  }

  onNext() {
    if (this.canNext()) {
      let nextStep = this.currentStep + 1;
      if (!this.canDisplayApiKeyModeStep() && nextStep === 4) {
        nextStep += 1;
      }
      this.setCurrentStep(nextStep);
    }
  }

  canNext() {
    return this._allSteps && this.currentStep < this._allSteps.length && !this.creationSuccess;
  }

  canPrevious() {
    return this.currentStep > 1 && !this.creationSuccess;
  }

  onPrevious() {
    if (this.canPrevious()) {
      let previousStep = this.currentStep - 1;
      if (!this.canDisplayApiKeyModeStep() && previousStep === 4) {
        previousStep -= 1;
      }
      this.setCurrentStep(previousStep);
    }
  }

  onExit() {
    this.router.navigate(['../'], { relativeTo: this.route });
  }

  canValidate() {
    if (this.steps && !this.creationSuccess) {
      const indexMax = this.canDisplayApiKeyModeStep() ? 3 : 2;
      const firstSteps = this.steps.filter((step, index) => index <= indexMax && step.valid);
      if (firstSteps.length === indexMax + 1) {
        return this.applicationForm.valid && this.hasValidSubscriptions();
      }
    }
    return false;
  }

  hasNext() {
    return this.currentStep <= 4;
  }

  hasCreate() {
    return this.currentStep === 5 && !this.creationSuccess;
  }

  createApp() {
    this.creationSuccess = false;
    this.creationError = false;
    this.creationInProgress = true;
    this.subscriptionErrors = [];
    const applicationInput = mapToApplicationInput(this.applicationForm.getRawValue());

    applicationInput.api_key_mode = this.apiKeyMode;

    this.applicationService
      .createApplication({ applicationInput })
      .toPromise()
      .then(async application => {
        this.createdApplication = application;

        for (const subscription of this.subscribeList) {
          const subscriptionInput: any = {
            application: application.id,
            plan: subscription.plan.id,
            request: subscription.request,
            ...(subscription.plan.mode === 'PUSH'
              ? {
                  configuration: {
                    channel: subscription.channel ?? undefined,
                    entrypointId: subscription.entrypoint ?? undefined,
                    entrypointConfiguration: subscription.entrypointConfiguration ?? undefined,
                  },
                }
              : undefined),
          };

          if (subscription.general_conditions_accepted) {
            subscriptionInput.general_conditions_accepted = subscription.general_conditions_accepted;
            subscriptionInput.general_conditions_content_revision = subscription.general_conditions_content_revision;
          }

          try {
            await this.subscriptionService.createSubscription({ subscriptionInput }).toPromise();
          } catch (exception) {
            let message = null;
            if (exception.error && exception.error.errors) {
              const error = exception.error.errors[0];
              message = await this.translateService.get(error.code, error.parameters).toPromise();
            }
            this.subscriptionErrors.push({ api: subscription.api, message });
          }
        }
        this.creationSuccess = true;
        this.creationInProgress = false;
        setTimeout(this.updateSteps.bind(this), 200);
      })
      .catch(() => {
        this.creationError = true;
        this.creationInProgress = false;
      });
  }

  onApplicationTypeSelected(applicationTypeOption: ApplicationTypeOption) {
    setTimeout(() => {
      this.applicationType = applicationTypeOption;
      this.updateSteps();
    }, 0);
  }

  get isSimpleApp() {
    return this.applicationType.id.toLowerCase() === 'simple';
  }

  onRequireChangeStep($event: { step; fragment }) {
    this.setCurrentStep($event.step);
    if ($event.fragment) {
      setTimeout(() => {
        const element = document.getElementById($event.fragment);
        if (element) {
          element.focus();
        }
      });
    }
  }

  canConfigureSharedApiKey() {
    return this.configurationService.hasFeature(FeatureEnum.sharedApiKey);
  }

  canDisplayApiKeyModeStep() {
    return this.canConfigureSharedApiKey() && this.hasAtLeastTwoApiKeySubscriptionsNotOnSameApi();
  }

  hasAtLeastTwoApiKeySubscriptionsNotOnSameApi() {
    const subscriptionsApi = [];

    const apiKeySubscriptionList = this.subscribeList?.filter(sub => sub.plan.security === SecurityEnum.APIKEY);
    if (!apiKeySubscriptionList || apiKeySubscriptionList.length < 2) {
      return false;
    }

    for (const subscription of apiKeySubscriptionList) {
      if (subscriptionsApi.includes(subscription.api.id)) {
        return false;
      }
      subscriptionsApi.push(subscription.api.id);
    }
    return true;
  }
}
