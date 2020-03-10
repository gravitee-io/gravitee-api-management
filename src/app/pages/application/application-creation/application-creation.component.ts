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
import { ChangeDetectorRef, Component, OnInit, ViewChild } from '@angular/core';
import {
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  Validators
} from '@angular/forms';
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
  Api,
  ApiService,
  ApisResponse, Application,
  ApplicationInput,
  ApplicationService,
  ApplicationType,
  Plan,
  PortalService, SubscriptionService
} from '@gravitee/ng-portal-webclient';
import { SearchRequestParams } from '../../../utils/search-query-param.enum';
import { NotificationService } from '../../../services/notification.service';
import { distinctUntilChanged } from 'rxjs/operators';

@Component({
  selector: 'app-application-creation',
  templateUrl: './application-creation.component.html',
  styleUrls: ['./application-creation.component.css']
})
export class ApplicationCreationComponent implements OnInit {


  @ViewChild('searchApiAutocomplete', { static: false }) searchApiAutocomplete;
  @ViewChild('clientId', { static: false }) clientId;

  private _allSteps: any;
  steps: any;
  currentStep: number;
  applicationForm: FormGroup;
  allowedTypes: Array<ApplicationType>;
  allowedOptions: { icon: string; active: boolean; description: string; id: string; title: string }[];
  apiList: { data: any; id: string; value: string }[];
  plans: Array<Plan>;
  subscribeList: any[];
  subscriptionListOptions: any;
  validationListOptions: any;
  disabledPlans: number;
  private updateStepsTimer: any;
  private readSteps: number[];
  selectedApi: Api;
  allGrantTypes: { code?: string; responses_types?: Array<string>; name?: string; disabled: boolean; type?: string; value: boolean }[];
  planForm: FormGroup;
  creationInProgress: boolean;
  creationSuccess: boolean;
  creationError: boolean;
  createdApplication: Application;

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
    this.apiList = [];
    this.subscribeList = [];
    this.disabledPlans = 0;
    this.readSteps = [1];
    this.plans = [];
  }

  async ngOnInit() {
    this._allSteps = await Promise.all([
      i18n('applicationCreation.step.general'),
      i18n('applicationCreation.step.security'),
      i18n('applicationCreation.step.subscription'),
      i18n('applicationCreation.step.validate')
    ].map((_title) => this.translateService.get(_title).toPromise().then((title) => ({ title }))));
    this.steps = this._allSteps;
    const applicationTypes = await this.portalService.getEnabledApplicationTypes().toPromise();
    this.allowedTypes = applicationTypes.data;
    this.allowedOptions = await Promise.all(applicationTypes.data
      .map((type, index) => {
        return this.translateService.get([`applicationType.${type.id}.title`, `applicationType.${type.id}.description`])
          .toPromise()
          .then((translations) => {
            const [title, description] = Object.values(translations);
            return {
              id: type.id,
              icon: getApplicationTypeIcon(type.id),
              active: index === 0,
              title,
              description,
            };
          });
      }));

    this.planForm = this.formBuilder.group({
      apiId: new FormControl(null, [Validators.required]),
      planId: new FormControl(null, [Validators.required]),
    });

    this.applicationForm = this.formBuilder.group({
      name: new FormControl(null, [Validators.required]),
      description: new FormControl(null, [Validators.required]),
      picture: new FormControl(null),
      groups: new FormArray([]),
      settings: this.formBuilder.group({
        app: this.formBuilder.group({
          type: new FormControl('', null),
          client_id: new FormControl('', null),
        }),
        oauth: this.formBuilder.group({
          client_secret: new FormControl('', null),
          client_id: new FormControl('', null),
          redirect_uris: new FormArray([]),
          client_uri: new FormControl('', null),
          response_types: new FormArray([]),
          grant_types: new FormArray([]),
          application_type: new FormControl(this.allowedOptions[0].id, [Validators.required]),
          renew_client_secret_supported: new FormControl(false, null),
        })
      })
    });

    this.translateService.get([
      i18n('apiSubscribe.apps.comment'),
      i18n('applicationCreation.subscription.comment'),
      i18n('applicationCreation.subscription.remove'),
      i18n('applicationCreation.subscription.validation.type'),
      i18n('applicationCreation.subscription.validation.auto'),
      i18n('applicationCreation.subscription.validation.manual'),
    ]).toPromise().then(translations => {
      const values = Object.values(translations);
      this.subscriptionListOptions = {
        data: [
          { field: 'api.name', label: 'Api' },
          { field: 'plan.name', label: 'Plan' },
          {
            field: 'request',
            label: values[1],
            type: 'gv-text',
            attributes: {
              rows: 2,
              required: (item) => item.requiredComment,
              placeholder: (item) => item.plan.comment_question || values[0],
            },
            width: '350px'
          },
          {
            type: 'gv-icon',
            width: '25px',
            attributes: {
              shape: 'general:trash',
              clickable: true,
              onClick: (item) => this.removePlan(item.plan),
              title: values[2]
            },
          },
        ]
      };

      this.validationListOptions = {
        data: [
          { field: 'api.name', label: 'Api' },
          { field: 'plan.name', label: 'Plan' },
          {
            field: 'request',
            label: values[1],
          },
          {
            field: (item) => item.plan.validation.toUpperCase === Plan.ValidationEnum.AUTO ? values[4] : values[5],
            label: values[3],
          }
        ]
      };
    });

    this.applicationForm.valueChanges
      .pipe(distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr)))
      .subscribe(() => setTimeout(() => this.updateSteps(), 0));

    if (this.activatedRoute.snapshot.queryParamMap.has('api')) {
      const apiId = this.activatedRoute.snapshot.queryParamMap.get('api');
      this.apiService.getApiByApiId({ apiId })
        .toPromise()
        .then((api) => {
          this.loadPlans(api);
        });
    }
    this.updateGrantTypes();
  }

  get applicationType() {
    return this.applicationForm.get('settings.oauth.application_type') as FormControl;
  }

  get applicationTypeEntity() {
    return this.allowedTypes.find((type) => type.id === this.applicationType.value);
  }

  get redirectURIs() {
    return this.applicationForm.get('settings.oauth.redirect_uris') as FormArray;
  }

  get grantTypes() {
    return this.applicationForm.get('settings.oauth.grant_types') as FormArray;
  }

  get pictureSrc() {
    return this.applicationForm.get('picture').value;
  }

  get appName() {
    return this.applicationForm.get('name').value;
  }

  get appDescription() {
    return this.applicationForm.get('description').value;
  }

  get appClientId() {
    return this.applicationForm.get('settings.app.client_id').value;
  }

  get selectedPlan() {
    const id = this.planForm.get('planId').value;
    return this.plans.find((p) => p.id === id);
  }

  onSearchApi({ detail }) {
    this.plans = [];
    return this.apiService.searchApis(new SearchRequestParams(detail, 5))
      .toPromise()
      .then((apisResponse: ApisResponse) => {
        if (apisResponse.data.length) {
          this.apiList = apisResponse.data.map((a) => {
            const row = document.createElement('gv-row');
            // @ts-ignore
            row.item = a;
            return { value: a.name, element: row, id: a.id, data: a };
          });
        } else {
          this.apiList = [];
        }
      });
  }

  async onSelectApi({ detail }) {
    const api = this.apiList.find((a) => a.id === detail.id).data;
    this.planForm.get('apiId').setValue(api.id);
    this.loadPlans(api);
  }

  private async loadPlans(api) {
    if (api) {
      const plans = await this.apiService.getApiPlansByApiId({ apiId: api.id }).toPromise();
      this.plans = plans.data.filter((plan) => (plan.security.toUpperCase() !== Plan.SecurityEnum.KEYLESS));
      if (this.selectedPlan == null && this.plans.length > 0) {
        this.planForm.get('planId').setValue(this.plans[0].id);
      }
      this.planForm.get('apiId').setValue(api.id);
      this.selectedApi = api;
    }
  }

  isOAuthClient() {
    return this.applicationType.value !== 'simple';
  }

  get applicationTypeName() {
    const entity = this.applicationTypeEntity;
    return this.isOAuthClient() ? entity.name : entity.name + ' ' + this.applicationForm.get('settings.app.type').value;
  }

  setCurrentStep(step) {
    if (!(this.creationSuccess)) {
      if (!this.readSteps.includes(step)) {
        this.readSteps.push(step);
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

  updateGrantTypes() {
    this.subscribeList = [];
    const appTypeEntity = this.applicationTypeEntity;
    this.grantTypes.clear();
    this.allGrantTypes = appTypeEntity.allowed_grant_types.map((allowedGrantType) => {
      const value = appTypeEntity.default_grant_types
        .find((grant) => allowedGrantType.code === grant.code) != null;

      const disabled = appTypeEntity.mandatory_grant_types
        .find((grant) => allowedGrantType.code === grant.code) != null;

      if (value === true) {
        this.grantTypes.push(new FormControl(allowedGrantType.type));
      }
      return { ...allowedGrantType, disabled, value };
    });
    if (!this.requiresRedirectUris) {
      this.redirectURIs.clear();
    }
  }

  get canAddPlan() {
    return this.subscribeList.find((s) => s.plan.id === this.selectedPlan.id) != null;
  }

  get requireClientId() {
    return !this.hasValidClientId(this.selectedPlan);
  }

  hasValidClientId(plan) {
    if (!this.isOAuthClient()
      && (plan.security.toUpperCase() === Plan.SecurityEnum.OAUTH2 || plan.security.toUpperCase() === Plan.SecurityEnum.JWT)) {
      if (this.appClientId == null || this.appClientId.trim() === '') {
        return false;
      }
    }
    return true;
  }

  addPlan() {
    if (this.planForm.valid && this.selectedApi && this.selectedPlan) {
      this.subscribeList = [...this.subscribeList, {
        api: this.selectedApi,
        plan: this.selectedPlan,
        requiredComment: this.requireComment(),
        request: ''
      }];
      this.updateSteps();
    }
  }

  removePlan(plan) {
    this.subscribeList = this.subscribeList.filter((s) => !(s.plan.id === plan.id));
    this.ref.detectChanges();
    this.updateSteps();
  }

  isInvalid(...controlNames) {
    return controlNames.find((name) => this.applicationForm.get(name).errors != null) != null;
  }

  hasValidType() {
    if (this.readSteps.includes(2)) {
      if (this.applicationType.value === 'simple' || this.applicationType.value === 'backend_to_backend') {
        return true;
      } else {
        return this.redirectURIs.errors == null &&
          this.redirectURIs.value.length > 0 &&
          this.redirectURIs.value[0] !== null &&
          this.redirectURIs.value[0].trim() !== '';
      }
    }
    return false;
  }

  hasValidSubscriptions() {
    return this.readSteps.includes(3)
      && this.subscribeList.find((s) => this.hasRequireComment(s.plan) && (s.request == null || s.request.trim() === '')) == null;
  }

  private async updateSteps() {
    const appTypeDescription = this.readSteps.includes(2) ? this.applicationTypeEntity.name : '';
    const subscriptionDescription = this.readSteps.includes(3) ?
      await this.translateService
        .get(i18n('applicationCreation.subscription.description'), { count: this.subscribeList.length })
        .toPromise() : '';
    const createdAt = this.createdApplication ?
      new Date(this.createdApplication.created_at).toLocaleString(this.translateService.currentLang) : '';
    this.steps = [
      { description: this.applicationForm.get('name').value, validate: !this.isInvalid('name', 'description') },
      { description: appTypeDescription, validate: this.hasValidType() },
      {
        description: subscriptionDescription,
        validate: this.hasValidSubscriptions(),
      },
      { description: createdAt, validate: this.creationSuccess }
    ].map(({ description, validate }, index) => {
      const step = this._allSteps[index];
      step.description = description;
      step.validate = !!description && (typeof validate === 'boolean' ? validate : true);
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

  get requiresRedirectUris() {
    return this.applicationTypeEntity.requires_redirect_uris;
  }

  get selectedApiName() {
    return this.selectedApi ? this.selectedApi.name : '';
  }

  addRedirectUri(event) {
    if (event.target.valid) {
      const value = event.target.value;
      if (value && value.trim() !== '') {
        this.redirectURIs.push(new FormControl(value, Validators.required));
        event.target.value = '';
      }
    }
  }

  removeRedirectUri(index: number) {
    this.redirectURIs.removeAt(index);
  }

  get hasRedirectUris() {
    return this.redirectURIs.length > 0;
  }

  onClientIdChange() {
    const clientId = this.appClientId;
    if (clientId == null || clientId.trim() === '') {
      this.subscribeList = this.subscribeList.filter((s) => this.hasValidClientId(s.plan));
    }
  }

  focusClientId() {
    this.currentStep = 2;
    setTimeout(() => {
      this.clientId.nativeElement.focus();
    }, 0);
  }

  canValidate() {
    if (this.steps && !this.creationSuccess) {
      const firstThree = this.steps.filter((step, index) => (index <= 2 && step.validate));
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
    this.creationInProgress = true;
    const applicationInput = this.applicationForm.getRawValue() as ApplicationInput;
    if (this.isOAuthClient()) {
      delete applicationInput.settings.app;
    } else {
      delete applicationInput.settings.oauth;
    }

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

  requireComment() {
    return this.hasRequireComment(this.selectedPlan);
  }

  hasRequireComment(plan) {
    return plan && plan.comment_required;
  }

  onSwitchGrant(event, grantType) {
    if (event.target.value) {
      this.grantTypes.push(new FormControl(grantType.type));
    } else {
      let index = -1;
      this.grantTypes.controls.forEach((control, i) => {
        if (control.value === grantType.type) {
          index = i;
          return;
        }
      });
      this.grantTypes.removeAt(index);
    }
  }

  onRequestChange($event: any) {
    clearTimeout(this.updateStepsTimer);
    this.updateStepsTimer = setTimeout(() => this.updateSteps(), 200);
  }

}
