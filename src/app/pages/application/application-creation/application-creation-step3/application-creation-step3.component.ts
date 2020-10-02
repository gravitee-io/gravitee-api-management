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
import { Api, ApiService, ApisResponse, Plan } from '../../../../../../projects/portal-webclient-sdk/src/lib';
import { TranslateService } from '@ngx-translate/core';
import { SearchRequestParams } from '../../../../utils/search-query-param.enum';

@Component({
  selector: 'app-application-creation-step3',
  templateUrl: './application-creation-step3.component.html',
  styleUrls: ['../application-creation.component.css'],
})
export class ApplicationCreationStep3Component implements OnInit {

  @Output() updated = new EventEmitter<any[]>();
  @Input() subscribeList: any[];
  @Output() changeStep = new EventEmitter<{step: number, fragment: string}>();
  // tslint:disable-next-line:ban-types
  @Input() hasValidClientId: Function;

  @ViewChild('searchApiAutocomplete') searchApiAutocomplete;
  planForm: FormGroup;
  apiList: { data: any; id: string; value: string }[];
  plans: Array<Plan>;
  selectedApi: Api;
  disabledPlans: number;

  subscriptionListOptions: any;

  private updateStepsTimer: any;

  constructor(private formBuilder: FormBuilder,
              private apiService: ApiService,
              private activatedRoute: ActivatedRoute,
              private translateService: TranslateService,
              private ref: ChangeDetectorRef
  ) {
    this.apiList = [];
    this.subscribeList = [];
    this.disabledPlans = 0;
    this.plans = [];
  }

  ngOnInit(): void {
    this.planForm = this.formBuilder.group({
      apiId: new FormControl(null, [Validators.required]),
      planId: new FormControl(null, [Validators.required]),
    });

    if (this.activatedRoute.snapshot.queryParamMap.has('api')) {
      const apiId = this.activatedRoute.snapshot.queryParamMap.get('api');
      this.apiService.getApiByApiId({ apiId })
        .toPromise()
        .then((api) => this.loadPlans(api));
    }

    this.translateService.get([
      i18n('apiSubscribe.apps.comment'),
      i18n('applicationCreation.subscription.comment'),
      i18n('applicationCreation.subscription.remove'),
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
            width: '300px'
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
    });
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


  get selectedApiName() {
    return this.selectedApi ? this.selectedApi.name : '';
  }

  goToStep2() {
    this.changeStep.emit({ step: 2 , fragment: 'appClientId' })
  }

  onRequestChange($event: any) {
    clearTimeout(this.updateStepsTimer);
    this.updateStepsTimer = setTimeout(() => this.updated.emit(this.subscribeList), 200);
  }

  get selectedPlan() {
    const id = this.planForm.get('planId').value;
    return this.plans.find((p) => p.id === id);
  }

  async onSelectApi({ detail }) {
    const api = this.apiList.find((a) => a.id === detail.id).data;
    this.planForm.get('apiId').setValue(api.id);
    this.loadPlans(api);
  }

  requireComment() {
    return this.hasRequireComment(this.selectedPlan);
  }

  hasRequireComment(plan) {
    return plan && plan.comment_required;
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

  get canAddPlan() {
    return this.subscribeList.find((s) => s.plan.id === this.selectedPlan.id) != null;
  }

  get requireClientId() {
    return !this.hasValidClientId(this.selectedPlan);
  }

  addPlan() {
    if (this.planForm.valid && this.selectedApi && this.selectedPlan) {
      this.subscribeList = [...this.subscribeList, {
        api: this.selectedApi,
        plan: this.selectedPlan,
        requiredComment: this.requireComment(),
        request: ''
      }];
      this.updated.emit(this.subscribeList);
    }
  }

  removePlan(plan) {
    this.subscribeList = this.subscribeList.filter((s) => !(s.plan.id === plan.id));
    this.updated.emit(this.subscribeList);
    this.ref.detectChanges();
  }

}
