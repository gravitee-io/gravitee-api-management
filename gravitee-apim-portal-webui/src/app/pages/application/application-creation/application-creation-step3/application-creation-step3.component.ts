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
import { ChangeDetectorRef, Component, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { TranslateService } from '@ngx-translate/core';
import { distinctUntilChanged } from 'rxjs/operators';

import { Api, ApiService, ApisResponse, Page, Plan } from '../../../../../../projects/portal-webclient-sdk/src/lib';
import { SearchRequestParams } from '../../../../utils/search-query-param.enum';

@Component({
  selector: 'app-application-creation-step3',
  templateUrl: './application-creation-step3.component.html',
  styleUrls: ['../application-creation.component.css'],
})
export class ApplicationCreationStep3Component implements OnInit {
  @Output() updated = new EventEmitter<any[]>();
  @Input() subscribeList: any[];
  @Output() changeStep = new EventEmitter<{ step: number; fragment: string }>();
  // eslint-disable-next-line @typescript-eslint/ban-types
  @Input() hasValidClientId: Function;

  @ViewChild('searchApiAutocomplete') searchApiAutocomplete;
  planForm: FormGroup;
  apiList: { data: any; id: string; value: string }[];
  plans: Array<Plan>;
  selectedApi: Api;
  disabledPlans: number;

  generalConditions: Map<string, Page> = new Map();

  currentGeneralConditions: Page;
  _generalConditionsAccepted = false;

  subscriptionListOptions: any;

  private updateStepsTimer: any;

  constructor(
    private formBuilder: FormBuilder,
    private apiService: ApiService,
    private activatedRoute: ActivatedRoute,
    private translateService: TranslateService,
    private ref: ChangeDetectorRef,
  ) {
    this.apiList = [];
    this.subscribeList = [];
    this.disabledPlans = 0;
    this.plans = [];
  }

  refeshGeneralCondition() {
    this.ref.detectChanges();
  }

  ngOnInit(): void {
    this.planForm = this.formBuilder.group({
      apiId: new FormControl(null, [Validators.required]),
      planId: new FormControl(null, [Validators.required]),
      general_conditions_accepted: new FormControl(null),
    });

    this.planForm.valueChanges.pipe(distinctUntilChanged((prev, curr) => prev.planId === curr.planId)).subscribe(() => {
      if (this.hasGeneralConditions) {
        this.planForm.get('general_conditions_accepted').setValidators(Validators.requiredTrue);

        const pageId = this.selectedPlan.general_conditions;
        if (this.generalConditions.get(pageId) === undefined) {
          let _api = this.planForm.get('apiId').value;
          if (!_api && this.activatedRoute.snapshot.queryParamMap.has('api')) {
            _api = this.activatedRoute.snapshot.queryParamMap.get('api');
          }

          this.apiService
            .getPageByApiIdAndPageId({
              apiId: _api,
              pageId: this.selectedPlan.general_conditions,
              include: ['content'],
            })
            .toPromise()
            .then(page => {
              this.generalConditions.set(page.id, page);
              this.currentGeneralConditions = page;
            });
        } else {
          this.currentGeneralConditions = this.generalConditions.get(pageId);
        }
      } else {
        this.planForm.get('general_conditions_accepted').clearValidators();
      }

      this.planForm.get('general_conditions_accepted').setValue(false);
      this._generalConditionsAccepted = false;
      this.ref.detectChanges();
    });

    this.planForm.valueChanges
      .pipe(distinctUntilChanged((prev, curr) => prev.general_conditions_accepted === curr.general_conditions_accepted))
      .subscribe(() => {
        this._generalConditionsAccepted = this.planForm.get('general_conditions_accepted').value;
        this.ref.detectChanges();
      });

    if (this.activatedRoute.snapshot.queryParamMap.has('api')) {
      const apiId = this.activatedRoute.snapshot.queryParamMap.get('api');
      this.apiService
        .getApiByApiId({ apiId })
        .toPromise()
        .then(api => this.loadPlans(api));
    }

    this.translateService
      .get([
        i18n('apiSubscribe.apps.comment'),
        i18n('applicationCreation.subscription.comment'),
        i18n('applicationCreation.subscription.remove'),
      ])
      .toPromise()
      .then(translations => {
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
                required: item => item.requiredComment,
                placeholder: item => item.plan.comment_question || values[0],
              },
              width: '300px',
            },
            {
              type: 'gv-icon',
              width: '25px',
              attributes: {
                shape: 'general:trash',
                clickable: true,
                onClick: item => this.removePlan(item.plan),
                title: values[2],
              },
            },
          ],
        };
      });
  }

  onSearchApi({ detail }) {
    this.plans = [];
    return this.apiService
      .searchApis(new SearchRequestParams(detail, 5))
      .toPromise()
      .then((apisResponse: ApisResponse) => {
        if (apisResponse.data.length) {
          this.apiList = apisResponse.data.map(a => {
            const row = document.createElement('gv-row');
            // eslint-disable-next-line @typescript-eslint/ban-ts-comment
            // @ts-ignore
            row.item = a;
            return { value: a.name, element: row, id: a.id, data: a };
          });
        } else {
          this.apiList = [];
        }
      });
  }

  get selectedApiName() {
    return this.selectedApi ? this.selectedApi.name : '';
  }

  goToStep2() {
    this.changeStep.emit({ step: 2, fragment: 'appClientId' });
  }

  onRequestChange() {
    clearTimeout(this.updateStepsTimer);
    this.updateStepsTimer = setTimeout(() => this.updated.emit(this.subscribeList), 200);
  }

  get selectedPlan() {
    const id = this.planForm.get('planId').value;
    return this.plans.find(p => p.id === id);
  }

  async onSelectApi({ detail }) {
    const api = this.apiList.find(a => a.id === detail.id).data;
    this.planForm.get('apiId').setValue(api.id);
    this.loadPlans(api);
  }

  get generalConditionsAccepted() {
    return !this.hasGeneralConditions || this._generalConditionsAccepted;
  }

  get generalConditionsAcceptedAndPlanSubscribed() {
    // 'canAddPlan' is used to keep the GCU displayed until the user click on subscribe button
    return !this.hasGeneralConditions || (this.canAddPlan && this._generalConditionsAccepted);
  }

  requireComment() {
    return this.hasRequireComment(this.selectedPlan);
  }

  hasRequireComment(plan) {
    return plan && plan.comment_required;
  }

  getCurrentGeneralConditions() {
    return this.currentGeneralConditions;
  }

  get hasGeneralConditions() {
    const plan = this.selectedPlan;
    return plan !== undefined && plan.general_conditions !== undefined && plan.general_conditions !== '';
  }

  private async loadPlans(api) {
    if (api) {
      const plans = await this.apiService.getApiPlansByApiId({ apiId: api.id, size: -1 }).toPromise();
      this.plans = plans.data.filter(plan => plan.security.toUpperCase() !== Plan.SecurityEnum.KEYLESS);
      if (this.selectedPlan == null && this.plans.length > 0) {
        this.planForm.get('planId').setValue(this.plans[0].id);
      }
      this.planForm.get('apiId').setValue(api.id);
      this.selectedApi = api;
    }
  }

  get canAddPlan() {
    return this.subscribeList && this.subscribeList.find(s => s.plan.id === this.selectedPlan.id) != null;
  }

  get requireClientId() {
    return !this.hasValidClientId(this.selectedPlan);
  }

  addPlan() {
    if (this.planForm.valid && this.selectedApi && this.selectedPlan) {
      const _subscription: any = {
        api: this.selectedApi,
        plan: this.selectedPlan,
        requiredComment: this.requireComment(),
        request: '',
      };

      if (this.hasGeneralConditions) {
        _subscription.general_conditions_accepted = this.planForm.get('general_conditions_accepted').value;
        _subscription.general_conditions_content_revision = this.currentGeneralConditions.contentRevisionId;
      }

      this.subscribeList = [...this.subscribeList, _subscription];
      this.updated.emit(this.subscribeList);
    }
  }

  removePlan(plan) {
    if (this.planForm.get('planId').value === plan.id && this.hasGeneralConditions) {
      this.planForm.get('general_conditions_accepted').setValue(false);
      this._generalConditionsAccepted = false;
    }
    this.subscribeList = this.subscribeList.filter(s => !(s.plan.id === plan.id));
    this.updated.emit(this.subscribeList);
    this.ref.detectChanges();
  }
}
