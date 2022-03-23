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
import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import {
  Application,
  ApplicationService,
  GetApplicationsRequestParams,
  PermissionsService,
  SubscriptionService,
} from '../../../../projects/portal-webclient-sdk/src/lib';
import '@gravitee/ui-components/wc/gv-card-list';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Pagination } from '@gravitee/ui-components/wc/gv-pagination';
import { switchMap, takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

@Component({
  selector: 'app-applications',
  templateUrl: './applications.component.html',
  styleUrls: ['./applications.component.css'],
})
export class ApplicationsComponent implements OnInit, OnDestroy {
  nbApplications: number;
  applications: { item: Application; metrics: Promise<{ subscribers: { clickable: boolean; value: number } }> }[] = [];
  metrics: Array<any>;
  empty: boolean;
  paginationData: Pagination;
  paginationPageSizes = [6, 12, 24, 48, 96];
  initialPaginationSize = 12;

  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  constructor(
    private applicationService: ApplicationService,
    private subscriptionService: SubscriptionService,
    private permissionsService: PermissionsService,
    private router: Router,
    private translateService: TranslateService,
    private activatedRoute: ActivatedRoute,
  ) {
    this.metrics = [];
  }

  ngOnInit() {
    this.empty = false;

    this.activatedRoute.queryParamMap
      .pipe(
        takeUntil(this.unsubscribe$),
        switchMap(params =>
          this.applicationService.getApplications({
            size: Number(params.get('size') ?? this.initialPaginationSize),
            page: Number(params.get('page') ?? 1),
          }),
        ),
      )
      .subscribe(response => {
        const pagination = response.metadata.pagination as unknown as Pagination;
        if (pagination) {
          this.paginationData = {
            size: this.initialPaginationSize,
            sizes: this.paginationPageSizes,
            ...this.paginationData,
            ...pagination,
          };
          this.nbApplications = this.paginationData.total;
        } else {
          this.paginationData = null;
          this.nbApplications = 0;
        }

        this.applications = response.data.map(application => ({
          item: application,
          metrics: this._getMetrics(application, response.metadata),
        }));
        this.empty = this.applications.length === 0;
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  private async _getMetrics(application: Application, metadata: { [p: string]: { [p: string]: object } }) {
    let count = 0;
    if (metadata.subscriptions && metadata.subscriptions[application.id]) {
      const subscriptions = metadata.subscriptions[application.id] as Array<object>;
      count = subscriptions.length;
    }
    const title = await this.translateService
      .get('applications.subscribers.title', {
        count,
        appName: application.name,
      })
      .toPromise();
    return {
      subscribers: {
        value: count,
        clickable: true,
        title,
      },
    };
  }

  @HostListener(':gv-card-full:click', ['$event.detail'])
  onClickToApp(application: Promise<Application>) {
    Promise.resolve(application).then(_application => {
      this.router.navigate(['/applications', _application.id]);
    });
  }

  @HostListener(':gv-metrics:click', ['$event.detail'])
  onClickToAppSubscribers({ key, item }) {
    if (key === 'subscribers') {
      this.router.navigate(['/applications/' + item.id + '/subscriptions']);
    }
  }

  @HostListener(':gv-pagination:paginate', ['$event.detail'])
  onPaginate({ page, size }) {
    const queryParams: GetApplicationsRequestParams = {
      page,
      size,
    };
    this.router.navigate([], { queryParams });
  }
}
