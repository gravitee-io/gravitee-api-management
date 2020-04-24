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
import { AfterViewInit, Component, HostListener, Input, OnDestroy, OnInit } from '@angular/core';
import { ApplicationService, Dashboard } from '@gravitee/ng-portal-webclient';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormControl, FormGroup } from '@angular/forms';
import { LoaderService } from '../../services/loader.service';
import { AnalyticsService } from '../../services/analytics.service';
import '@gravitee/ui-components/wc/gv-date-picker';
import { NavRouteService } from '../../services/nav-route.service';

@Component({
  selector: 'app-gv-analytics-filters',
  templateUrl: './gv-analytics-filters.component.html',
  styleUrls: ['./gv-analytics-filters.component.css']
})
export class GvAnalyticsFiltersComponent implements OnInit, AfterViewInit, OnDestroy {

  @Input() dashboard: Dashboard;
  @Input() withURI: boolean;
  @Input() link: { label: string, relativePath: string };

  private maxDateTimer: any;
  analyticsForm: FormGroup;
  tags: Array<any>;
  apisOptions: Array<any>;
  invalidDates: boolean;
  maxDate: number;
  advancedFiltersDisplayed;

  constructor(
    private router: Router,
    private formBuilder: FormBuilder,
    private applicationService: ApplicationService,
    public route: ActivatedRoute,
    public loaderService: LoaderService,
    public analyticsService: AnalyticsService,
    private navRouteService: NavRouteService,
  ) {
  }

  ngOnInit(): void {

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

    this.route.queryParams.subscribe((queryParams) => {
      this.analyticsForm.get('range').setValue([queryParams.from, queryParams.to]);
      this.analyticsForm.get('requestId').setValue(queryParams._id);
      this.analyticsForm.get('transactionId').setValue(queryParams.transaction);
      this.analyticsForm.get('methods').setValue(queryParams.method);
      this.analyticsForm.get('path').setValue(queryParams.uri);
      this.analyticsForm.get('responseTimes').setValue(queryParams['response-time']);
      this.analyticsForm.get('api').setValue(queryParams.api);
      this.analyticsForm.get('payloads').setValue(queryParams.body);

      this.analyticsService.timeframes.forEach((t) => t.active = t.id === queryParams.timeframe);

      this.tags = [];
      Object.keys(queryParams)
        .filter((q) => !this.analyticsService.queryParams.includes(q))
        .filter((q) => !this.analyticsService.advancedQueryParams.includes(q))
        .forEach((q) => {
          const queryParam = queryParams[q];
          if (typeof queryParam === 'string') {
            this.tags.push(q + ': ' + queryParam);
          } else {
            queryParam.forEach(qp => {
              this.tags.push(q + ': ' + qp);
            });
          }
        });
      if (queryParams._id || queryParams.transaction || queryParams.method ||
        queryParams.uri || queryParams['response-time'] || queryParams.status ||
        queryParams.api || queryParams.body) {
        this.advancedFiltersDisplayed = true;
      }
    });


    if (!this.route.snapshot.queryParams.timeframe && !(this.route.snapshot.queryParams.from && this.route.snapshot.queryParams.to)) {
      this.router.navigate([], {
        queryParams: { timeframe: '1d' },
        queryParamsHandling: 'merge',
        fragment: this.analyticsService.fragment
      });
    }

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
      this.applicationService.getSubscriberApisByApplicationId({ applicationId: this.route.snapshot.params.applicationId, size: -1 })
        .toPromise().then(apis => {
        this.apisOptions = apis.data.map(api => {
          return { label: api.name + ' (' + api.version + ')', value: api.id };
        });
      });
    }
  }

  @HostListener(':gv-option:select', ['$event.detail'])
  _onChangeDisplay({ id }) {
    const queryParams: any = { timeframe: id, ...this.analyticsService.getRemovableQueryParams() };
    this.router.navigate([], {
      queryParams,
      queryParamsHandling: 'merge',
      fragment: this.analyticsService.fragment
    }).then(() => {
      this.analyticsForm.patchValue({ range: null });
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
      if (this.analyticsForm.value.range && this.analyticsForm.value.range[0]) {
        queryParams.from = this.analyticsForm.value.range[0];
        queryParams.to = this.analyticsForm.value.range[1];
        queryParams.timeframe = null;
      }
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
      fragment: this.analyticsService.fragment
    });
  }

  goTo(relativePath: string) {
    this.router.navigate([relativePath], {
      relativeTo: this.route, queryParamsHandling: 'merge', queryParams: this.analyticsService.getRemovableQueryParams()
    });
  }

  toggleFilters() {
    this.advancedFiltersDisplayed = !this.advancedFiltersDisplayed;
  }
}
