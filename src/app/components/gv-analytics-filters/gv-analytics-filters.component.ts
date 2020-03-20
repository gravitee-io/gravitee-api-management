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
import { AfterViewInit, Component, EventEmitter, HostListener, Input, OnInit, Output } from '@angular/core';
import { Dashboard } from '@gravitee/ng-portal-webclient';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup } from '@angular/forms';
import { LoaderService } from '../../services/loader.service';
import { AnalyticsService } from '../../services/analytics.service';

@Component({
  selector: 'app-gv-analytics-filters',
  templateUrl: './gv-analytics-filters.component.html',
  styleUrls: ['./gv-analytics-filters.component.css']
})
export class GvAnalyticsFiltersComponent implements OnInit, AfterViewInit {

  @Input() dashboard: Dashboard;
  @Output() refreshDashboard: EventEmitter<any> = new EventEmitter();

  analyticsForm: FormGroup;
  tags: Array<any>;

  constructor(
    private router: Router,
    public route: ActivatedRoute,
    private formBuilder: FormBuilder,
    public loaderService: LoaderService,
    public analyticsService: AnalyticsService,
  ) { }

  ngOnInit(): void {
    this.initForm();
    if (!this.route.snapshot.queryParams.timeframe && !(this.route.snapshot.queryParams.from && this.route.snapshot.queryParams.to)) {
      this.router.navigate([], {
        queryParams: { timeframe: '1d' },
        queryParamsHandling: 'merge',
        fragment: 'dashboard'
      }).then(() => {
        this.initFilters();
      });
    } else {
      this.initFilters();
    }
  }

  private initForm() {
    this.analyticsForm = this.formBuilder.group({
      from: this.route.snapshot.queryParams.from,
      to: this.route.snapshot.queryParams.to,
    });
  }

  ngAfterViewInit() {
    setTimeout(() => {
      this.refreshDashboard.emit();
    });
  }

  initFilters() {
    this.initForm();
    this.analyticsService.timeframes.forEach((t) => t.active = t.id === this.route.snapshot.queryParams.timeframe);

    this.tags = [];
    Object.keys(this.route.snapshot.queryParams)
      .filter((q) => !this.analyticsService.queryParams.includes(q))
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
      fragment: 'dashboard'
    }).then(() => {
      this.initForm();
      this.initFilters();
      this.refreshDashboard.emit();
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
      fragment: 'dashboard'
    }).then(() => {
      this.initFilters();
      this.refreshDashboard.emit();
    });
  }

  formValid() {
    return (!this.analyticsForm.value.from && !this.analyticsForm.value.to)
      || (this.analyticsForm.value.from && this.analyticsForm.value.to);
  }

  search() {
    if (this.formValid()) {
      const queryParams: any = {};
      if (this.analyticsForm.value.from && this.analyticsForm.value.to) {
        queryParams.from = this.analyticsForm.value.from;
        queryParams.to = this.analyticsForm.value.to;
        queryParams.timeframe = null;
      }
      this.router.navigate([], {
        queryParams,
        queryParamsHandling: 'merge',
        fragment: 'dashboard'
      }).then(() => {
        this.initFilters();
        this.refreshDashboard.emit();
      });
    }
  }

  reset() {
    const queryParams: any = {
      from: this.analyticsForm.value.from,
      to: this.analyticsForm.value.to,
    };
    if (!this.analyticsForm.value.from && !this.analyticsForm.value.to) {
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
      fragment: 'dashboard'
    }).then(() => {
      this.initForm();
      this.initFilters();
      this.refreshDashboard.emit();
    });
  }
}
