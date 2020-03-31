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
import { AfterViewInit, Component, EventEmitter, HostListener, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { ApplicationService, Dashboard } from '@gravitee/ng-portal-webclient';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormControl, FormGroup } from '@angular/forms';
import { LoaderService } from '../../services/loader.service';
import { AnalyticsService } from '../../services/analytics.service';
import '@gravitee/ui-components/wc/gv-date-picker';

@Component({
  selector: 'app-gv-analytics-filters',
  templateUrl: './gv-analytics-filters.component.html',
  styleUrls: ['./gv-analytics-filters.component.css']
})
export class GvAnalyticsFiltersComponent implements OnInit, AfterViewInit, OnDestroy {

  @Input() dashboard: Dashboard;
  @Output() refreshDashboard: EventEmitter<any> = new EventEmitter();
  @Input() withURI: boolean;

  analyticsForm: FormGroup;
  tags: Array<any>;
  apisOptions: Array<any>;
  invalidDates: boolean;
  maxDate: number;
  private maxDateTimer: any;

  constructor(
    private router: Router,
    public route: ActivatedRoute,
    private formBuilder: FormBuilder,
    public loaderService: LoaderService,
    public analyticsService: AnalyticsService,
    public applicationService: ApplicationService,
  ) {
  }

  ngOnInit(): void {
    this.initForm();
    if (!this.route.snapshot.queryParams.timeframe && !(this.route.snapshot.queryParams.from && this.route.snapshot.queryParams.to)) {
      this.router.navigate([], {
        queryParams: { timeframe: '1d' },
        queryParamsHandling: 'merge',
        fragment: this.analyticsService.fragment
      }).then(() => {
        this.initFilters();
      });
    } else {
      this.initFilters();
    }
    this.maxDate = new Date().getTime();
    this.maxDateTimer = setInterval(() => {
      this.maxDate = new Date().getTime();
    }, 30000);

  }

  ngOnDestroy(): void {
    clearInterval(this.maxDateTimer);
  }

  private initForm() {
    this.analyticsForm = this.formBuilder.group({
      range: new FormControl([this.route.snapshot.queryParams.from, this.route.snapshot.queryParams.to]),
      requestId: new FormControl(this.route.snapshot.queryParams._id),
      transactionId: new FormControl(this.route.snapshot.queryParams.transaction),
      methods: new FormControl(this.route.snapshot.queryParams.method),
      path: new FormControl(this.route.snapshot.queryParams.uri),
      responseTimes: new FormControl(this.route.snapshot.queryParams['response-time']),
      status: new FormControl(this.route.snapshot.queryParams.status),
      api: new FormControl(this.route.snapshot.queryParams.api),
      payloads: new FormControl(this.route.snapshot.queryParams.body),
    });
  }

  ngAfterViewInit() {
    if (this.route.snapshot.params.applicationId) {
      this.applicationService.getSubscriberApisByApplicationId({ applicationId: this.route.snapshot.params.applicationId, size: -1 })
        .toPromise().then(apis => {
        this.apisOptions = apis.data.map(api => {
          return { label: api.name + ' (' + api.version + ')', value: api.id };
        });
      });
      setTimeout(() => {
        this.refreshDashboard.emit();
      });
    }
  }

  initFilters() {
    this.initForm();
    this.analyticsService.timeframes.forEach((t) => t.active = t.id === this.route.snapshot.queryParams.timeframe);

    this.tags = [];
    Object.keys(this.route.snapshot.queryParams)
      .filter((q) => !this.analyticsService.queryParams.includes(q))
      .filter((q) => !this.analyticsService.advancedQueryParams.includes(q))
      .forEach((q) => {
        const queryParam = this.route.snapshot.queryParams[q];
        if (typeof queryParam === 'string') {
          this.tags.push(q + ': ' + queryParam);
        } else {
          queryParam.forEach(qp => {
            this.tags.push(q + ': ' + qp);
          });
        }
      });
  }

  @HostListener(':gv-option:select', ['$event.detail'])
  _onChangeDisplay({ id }) {
    const queryParams: any = { timeframe: id, from: null, to: null };
    this.router.navigate([], {
      queryParams,
      queryParamsHandling: 'merge',
      fragment: this.analyticsService.fragment
    }).then(() => {
      this.analyticsForm.patchValue({ range: null });
      // this.analyticsForm.patchValue({to: null});
      this.search();
    });
  }

  onTagRemove(tag) {
    const queryParams: any = { ...this.route.snapshot.queryParams };
    const tagSplit = tag.split(': ');
    const key = tagSplit[0];
    const value = tagSplit[1];
    Object.keys(queryParams)
      .filter((q) => q === key)
      .forEach((q) => {
        if (queryParams[q].includes(value)) {
          if (typeof queryParams[q] === 'string') {
            queryParams[q] = queryParams[q].replace(value, '');
            if (!queryParams[q]) {
              queryParams[q] = null;
            }
          } else {
            queryParams[q].splice(queryParams[q].indexOf(value), 1);
            if (!queryParams[q].length) {
              queryParams[q] = null;
            }
          }
        }
      });
    this.router.navigate([], {
      queryParams,
      queryParamsHandling: 'merge',
      fragment: this.analyticsService.fragment
    }).then(() => {
      this.initFilters();
      this.refreshDashboard.emit();
    });
  }

  formValid() {
    return !this.invalidDates;
  }

  search() {
    if (this.formValid()) {
      const queryParams: any = {
        _id: this.analyticsForm.value.requestId || null,
        transaction: this.analyticsForm.value.transactionId || null,
        method: this.analyticsForm.value.methods || null,
        uri: this.analyticsForm.value.path || null,
        'response-time': this.analyticsForm.value.responseTimes || null,
        status: this.analyticsForm.value.status || null,
        api: this.analyticsForm.value.api || null,
        body: this.analyticsForm.value.payloads || null,
      };

      if (this.analyticsForm.value.range) {
        queryParams.from = this.analyticsForm.value.range[0];
        queryParams.to = this.analyticsForm.value.range[1];
        queryParams.timeframe = null;
      }
      this.router.navigate([], {
        queryParams,
        queryParamsHandling: 'merge',
        fragment: this.analyticsService.fragment
      }).then(() => {
        this.initFilters();
        this.refreshDashboard.emit();
      });
    }
  }

  reset() {
    const queryParams: any = {
      from: this.analyticsForm.value.range[0],
      to: this.analyticsForm.value.range[1],
    };
    if (this.analyticsForm.value.range == null || this.analyticsForm.value.range.length === 0) {
      queryParams.timeframe = this.route.snapshot.queryParams.timeframe || '1d';
    }
    // keep analytics params
    Object.keys(this.route.snapshot.queryParams)
      .filter(q => this.analyticsService.queryParams.includes(q))
      .forEach(q => {
        queryParams[q] = this.route.snapshot.queryParams[q];
      });
    this.router.navigate([], {
      queryParams,
      fragment: this.analyticsService.fragment
    }).then(() => {
      this.initFilters();
      this.refreshDashboard.emit();
    });
  }
}
