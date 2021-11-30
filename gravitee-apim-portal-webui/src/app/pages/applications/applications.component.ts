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
  Subscription,
  SubscriptionService,
} from '../../../../projects/portal-webclient-sdk/src/lib';
import '@gravitee/ui-components/wc/gv-card-list';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import StatusEnum = Subscription.StatusEnum;
import { Pagination } from 'src/app/model/pagination';
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
  paginationSize = 12;

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
        switchMap((params) =>
          this.applicationService.getApplications({
            size: Number(params.get('size') ?? 12),
            page: Number(params.get('page') ?? 1),
          }),
        ),
      )
      .subscribe((response) => {
        this.paginationData = response.metadata.pagination as unknown as Pagination;
        this.paginationSize = this.paginationData.size;

        this.nbApplications = this.paginationData.total;
        this.applications = response.data.map((application) => ({
          item: application,
          metrics: this._getMetrics(application),
        }));
        this.empty = this.applications.length === 0;
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  private _getMetrics(application: Application) {
    return this.permissionsService
      .getCurrentUserPermissions({ applicationId: application.id })
      .toPromise()
      .then((permissions) => {
        if (permissions.SUBSCRIPTION && permissions.SUBSCRIPTION.includes('R')) {
          return this.subscriptionService
            .getSubscriptions({ size: -1, applicationId: application.id, statuses: [StatusEnum.ACCEPTED] })
            .toPromise()
            .then(async (r) => {
              const count = r.data.length;
              const title = await this.translateService
                .get('applications.subscribers.title', {
                  count,
                  appName: application.name,
                })
                .toPromise();
              return {
                subscribers: {
                  value: r.data.length,
                  clickable: true,
                  title,
                },
              };
            });
        }
      });
  }

  @HostListener(':gv-card-full:click', ['$event.detail'])
  onClickToApp(application: Promise<Application>) {
    Promise.resolve(application).then((_application) => {
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
  onPaginate({ page }) {
    if (this.paginationData.current_page !== page) {
      const queryParams: GetApplicationsRequestParams = {
        page,
        size: this.paginationSize,
      };
      this.router.navigate([], { queryParams });
    }
  }

  onSelectSize(size) {
    if (this.paginationSize !== size) {
      const queryParams: GetApplicationsRequestParams = {
        page: 1,
        size,
      };
      this.router.navigate([], { queryParams });
    }
  }
}
