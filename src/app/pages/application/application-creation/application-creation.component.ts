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
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { ActivatedRoute, Router } from '@angular/router';
import { ConfigurationService } from '../../../services/configuration.service';

import { getApplicationTypeIcon } from '@gravitee/ui-components/src/lib/theme';
import {
  ApiService,
  Application,
  ApplicationInput,
  ApplicationService,
  ApplicationType,
  Plan,
  PortalService,
  SubscriptionService
} from '../../../../../projects/portal-webclient-sdk/src/lib';
import { NotificationService } from '../../../services/notification.service';

export interface ApplicationTypeOption extends ApplicationType {
  icon: string;
  description: string;
  title: string;
}

interface StepState {
  description: string,
  valid: boolean,
  invalid: boolean
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
  applicationForm: FormGroup;
  allowedTypes: Array<ApplicationTypeOption>;
  plans: Array<Plan>;
  subscribeList: any[];

  private readSteps: number[];

  creationInProgress: boolean;
  creationSuccess: boolean;
  creationError: boolean;
  createdApplication: Application;
  applicationType: ApplicationTypeOption;
  private stepOneForm: FormGroup;
  private stepTwoForm: FormGroup;

  constructor(private translateService: TranslateService,
              private configurationService: ConfigurationService,
              private formBuilder: FormBuilder,
              private router: Router,
              private route: ActivatedRoute,
              private portalService: PortalService,
              private notificationService: NotificationService,
              private apiService: ApiService,
              private activatedRoute: ActivatedRoute,
              private subscriptionService: SubscriptionService,
              private applicationService: ApplicationService,
              private ref: ChangeDetectorRef) {
    this.currentStep = 1;
    this.readSteps = [1];
  }

  async ngOnInit() {
    this._allSteps = await Promise.all([
      i18n('applicationCreation.step.general'),
      i18n('applicationCreation.step.security'),
      i18n('applicationCreation.step.subscription'),
      i18n('applicationCreation.step.validate')
    ].map((_title) => this.translateService.get(_title).toPromise().then((title) => ({ title }))));
    this.steps = this._allSteps;
    this.allowedTypes = await Promise.all(this.route.snapshot.data.enabledApplicationTypes
      .map((type, index) => {
        return this.translateService.get([`applicationType.${type.id}.title`, `applicationType.${type.id}.description`])
          .toPromise()
          .then((translations) => {
            const [title, description] = Object.values(translations);
            return {
              ...type,
              icon: getApplicationTypeIcon(type.id),
              title,
              description,
            };
          });
      }));

    this.applicationForm = this.formBuilder.group({
      name: new FormControl(null, [Validators.required]),
      description: new FormControl(null, [Validators.required]),
      picture: new FormControl(null),
      settings: new FormControl(null, [Validators.required]),
    });
  }

  setCurrentStep(step) {
    if (!(this.creationSuccess)) {
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
    this.notificationService.reset();
    this.creationError = false;
    this.setCurrentStep(current);
  }

  get requireClientId() {
    if (this.applicationType && this.isSimpleApp && this.subscribeList) {
      const subscription = this.subscribeList.find((s) => !this.hasValidClientId(s.plan));
      if (subscription) {
        return true;
      }
    }
    return false;
  }

  hasValidClientId(plan) {
    if (this.isSimpleApp
      && (plan.security.toUpperCase() === Plan.SecurityEnum.OAUTH2 || plan.security.toUpperCase() === Plan.SecurityEnum.JWT)) {
      const { settings } = this.applicationForm.getRawValue();
      if (settings.app) {
        if (settings.app.client_id == null || settings.app.client_id.trim() === '') {
          return false;
        }
      }
    }
    return true;
  }

  hasValidSubscriptions() {
    return this.readSteps.includes(3)
      && this.subscribeList.find((s) => this.hasRequireComment(s.plan) && (s.request == null || s.request.trim() === '')) == null;
  }

  hasRequireComment(plan) {
    return plan && plan.comment_required;
  }

  onStepOneUpdated(stepOneForm: FormGroup) {
    this.stepOneForm = stepOneForm;
    this.applicationForm.patchValue(this.stepOneForm.getRawValue());
    this.updateSteps();
  }

  get stepOneState(): StepState {
    if (this.stepOneForm) {
      return {
        description: this.stepOneForm.get('name').value,
        valid: this.stepOneForm.valid,
        invalid: this.readSteps.includes(2) && this.stepOneForm.invalid
      };
    }
    return { description: '', valid: false, invalid: false };
  }

  onStepTwoUpdated(stepTwoForm: FormGroup) {
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
      const description = await this.translateService.get(
        i18n('applicationCreation.subscription.description'), { count: this.subscribeList.length }).toPromise();
      const valid = this.hasValidSubscriptions();
      return { description, valid, invalid: !valid };
    }
    return { description: '', valid: false, invalid: false };
  }

  private async updateSteps() {
    const createdAt = this.createdApplication ?
      new Date(this.createdApplication.created_at).toLocaleString(this.translateService.currentLang) : '';

    const stepThreeStep = await this.stepThreeState();
    this.steps = [
      this.stepOneState,
      this.stepTwoState,
      stepThreeStep,
      { description: createdAt, valid: this.creationSuccess, invalid: false }
    ].map(({ description, valid, invalid }, index) => {
      const step = this._allSteps[index];
      step.description = description;
      step.valid = this.readSteps.includes(index + 1) && valid;
      step.invalid = this.readSteps.includes(index + 1) && invalid;
      return step;
    });
  }

  onNext() {
    if (this.canNext()) {
      this.setCurrentStep(this.currentStep + 1);
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
      this.setCurrentStep(this.currentStep - 1);
    }
  }

  onExit() {
    this.router.navigate(['../'], { relativeTo: this.route });
  }

  canValidate() {
    if (this.steps && !this.creationSuccess) {
      const firstThree = this.steps.filter((step, index) => (index <= 2 && step.valid));
      if (firstThree.length === 3) {
        return this.applicationForm.valid && this.hasValidSubscriptions();
      }
    }
    return false;
  }

  hasNext() {
    return this.currentStep <= 3;
  }

  hasCreate() {
    return this.currentStep === 4 && !this.creationSuccess;
  }

  createApp() {
    this.creationSuccess = false;
    this.creationError = false;
    this.creationInProgress = true;
    const applicationInput = this.applicationForm.getRawValue() as ApplicationInput;

    this.applicationService.createApplication({ ApplicationInput: applicationInput })
      .toPromise()
      .then((application) => {
        this.createdApplication = application;
        const subscriptions = this.subscribeList.map(async (s) => {
          return this.subscriptionService.createSubscription({
            SubscriptionInput: {
              application: application.id,
              plan: s.plan.id,
              request: s.request
            }
          }).toPromise();
        });

        Promise.all(subscriptions)
          .then(() => {
            this.creationSuccess = true;
            this.creationInProgress = false;
            setTimeout(() => {
              this.updateSteps();
            }, 200);
          }).catch((e) => {
          this.creationError = true;
          this.creationInProgress = false;
        });
      }).catch(() => {
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

  onRequireChangeStep($event: { step, fragment }) {
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

}
