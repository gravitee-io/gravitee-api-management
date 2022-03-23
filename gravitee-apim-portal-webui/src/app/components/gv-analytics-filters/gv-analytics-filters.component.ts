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
import { ApplicationService, Dashboard } from '../../../../projects/portal-webclient-sdk/src/lib';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormControl, FormGroup } from '@angular/forms';
import { AnalyticsService } from '../../services/analytics.service';
import { GvValidators } from '../../utils/gv-validators';
import '@gravitee/ui-components/wc/gv-button';
import '@gravitee/ui-components/wc/gv-tag';
import '@gravitee/ui-components/wc/gv-input';
import '@gravitee/ui-components/wc/gv-select';
import '@gravitee/ui-components/wc/gv-option';
import '@gravitee/ui-components/wc/gv-date-picker';
import { NavRouteService } from '../../services/nav-route.service';

@Component({
  selector: 'app-gv-analytics-filters',
  templateUrl: './gv-analytics-filters.component.html',
  styleUrls: ['./gv-analytics-filters.component.css'],
})
export class GvAnalyticsFiltersComponent implements OnInit, AfterViewInit, OnDestroy {
  @Input() dashboard: Dashboard;
  @Input() withURI: boolean;
  @Input() link: { label: string; relativePath: string; icon: string };
  @Output() export: EventEmitter<any> = new EventEmitter();
  @Input() exportDisabled: boolean;
  @Input() exportLoading: boolean;
  @Input() searchLoading: boolean;

  private maxDateTimer: any;
  analyticsForm: FormGroup;
  tags: Array<any>;
  apisOptions: Array<any>;
  maxDate: number;
  advancedFiltersDisplayed;

  constructor(
    private router: Router,
    private formBuilder: FormBuilder,
    private applicationService: ApplicationService,
    public route: ActivatedRoute,
    public analyticsService: AnalyticsService,
    private navRouteService: NavRouteService,
  ) {}

  ngOnInit(): void {
    this.analyticsForm = this.formBuilder.group({
      timeframe: new FormControl(null),
      range: new FormControl([null, null]),
      requestId: new FormControl(null),
      transactionId: new FormControl(null),
      methods: new FormControl(null),
      path: new FormControl(null),
      responseTimes: new FormControl(null),
      status: new FormControl(null),
      api: new FormControl(null),
      payloads: new FormControl(null),
    });

    this.analyticsForm.get('timeframe').setValidators(GvValidators.oneRequired(this.analyticsForm.get('range')));
    this.analyticsForm.get('range').setValidators([GvValidators.dateRange, GvValidators.oneRequired(this.analyticsForm.get('timeframe'))]);

    this.analyticsForm.get('range').valueChanges.subscribe(range => {
      // Reset timeframe if range is set
      if (range !== null && range.filter(v => v != null).length === 2) {
        this.analyticsForm.get('timeframe').setValue(null);
      }
    });

    this.analyticsForm.get('timeframe').valueChanges.subscribe(timeframe => {
      // Reset range if timeframe is set
      if (timeframe) {
        this.analyticsForm.get('range').setValue([null, null]);
      }
    });

    this.route.queryParams.subscribe(queryParams => {
      if (!this.route.snapshot.queryParams.timeframe && !(this.route.snapshot.queryParams.from && this.route.snapshot.queryParams.to)) {
        this.router.navigate([], {
          queryParams: { timeframe: '1d' },
          queryParamsHandling: 'merge',
          fragment: this.analyticsService.fragment,
        });
      }

      const formValues = {
        timeframe: queryParams.timeframe,
        range: [queryParams.from, queryParams.to],
        requestId: queryParams._id,
        transactionId: queryParams.transaction,
        methods: queryParams.method,
        path: queryParams.uri,
        responseTimes: queryParams['response-time'],
        api: queryParams.api,
        payloads: queryParams.body,
      };

      this.analyticsForm.reset(formValues);

      this.tags = [];
      Object.keys(queryParams)
        .filter(q => !this.analyticsService.queryParams.includes(q))
        .filter(q => !this.analyticsService.advancedQueryParams.includes(q))
        .forEach(q => {
          const queryParam = queryParams[q];
          if (typeof queryParam === 'string') {
            this.tags.push(q + ': ' + queryParam);
          } else {
            queryParam.forEach(qp => {
              this.tags.push(q + ': ' + qp);
            });
          }
        });
      if (
        queryParams._id ||
        queryParams.transaction ||
        queryParams.method ||
        queryParams.uri ||
        queryParams['response-time'] ||
        queryParams.status ||
        queryParams.api ||
        queryParams.body
      ) {
        this.advancedFiltersDisplayed = true;
      }
    });

    this.maxDate = new Date().getTime();
    this.maxDateTimer = setInterval(() => {
      this.maxDate = new Date().getTime();
    }, 30000);
  }

  ngOnDestroy(): void {
    clearInterval(this.maxDateTimer);
  }

  ngAfterViewInit() {
    if (this.route.snapshot.params.applicationId) {
      this.applicationService
        .getSubscriberApisByApplicationId({ applicationId: this.route.snapshot.params.applicationId, size: -1 })
        .toPromise()
        .then(apis => {
          this.apisOptions = apis.data.map(api => {
            return { label: api.name + ' (' + api.version + ')', value: api.id };
          });
        });
    }
  }

  @HostListener(':gv-option:select', ['$event.detail'])
  _onChangeDisplay({ id }) {
    const queryParams: any = { timeframe: id, ...this.analyticsService.getRemovableQueryParams() };
    this.router
      .navigate([], {
        queryParams,
        queryParamsHandling: 'merge',
        fragment: this.analyticsService.fragment,
      })
      .then(() => {
        this.search();
      });
  }

  onTagRemove(tag) {
    const queryParams: any = { ...this.route.snapshot.queryParams };
    const tagSplit = tag.split(': ');
    const key = tagSplit[0];
    const value = tagSplit[1];
    Object.keys(queryParams)
      .filter(q => q === key)
      .forEach(q => {
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
      fragment: this.analyticsService.fragment,
    });
  }

  search() {
    if (this.analyticsForm.valid) {
      const queryParams: any = {
        _id: this.analyticsForm.value.requestId || null,
        transaction: this.analyticsForm.value.transactionId || null,
        method: this.analyticsForm.value.methods || null,
        uri: this.analyticsForm.value.path || null,
        'response-time': this.analyticsForm.value.responseTimes || null,
        status: this.analyticsForm.value.status || null,
        api: this.analyticsForm.value.api || null,
        body: this.analyticsForm.value.payloads || null,
        from: this.analyticsForm.value.range[0],
        to: this.analyticsForm.value.range[1],
        timeframe: this.analyticsForm.value.timeframe,
      };

      this.navRouteService.navigateForceRefresh([], {
        queryParams,
        queryParamsHandling: 'merge',
        fragment: this.analyticsService.fragment,
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
      fragment: this.analyticsService.fragment,
    });
  }

  goTo(relativePath: string) {
    this.router.navigate([relativePath], {
      relativeTo: this.route,
      queryParamsHandling: 'merge',
      queryParams: this.analyticsService.getRemovableQueryParams(),
    });
  }

  toggleFilters() {
    this.advancedFiltersDisplayed = !this.advancedFiltersDisplayed;
  }
}
