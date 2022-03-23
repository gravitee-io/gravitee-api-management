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
import { ChangeDetectorRef, Component, HostListener, NgZone, OnDestroy, OnInit } from '@angular/core';
import {
  Api,
  ApiService,
  Application,
  ApplicationService,
  GetApplicationsRequestParams,
  GetSubscriptionsRequestParams,
  Subscription,
  SubscriptionService,
} from '../../../../projects/portal-webclient-sdk/src/lib';
import '@gravitee/ui-components/wc/gv-table';
import { TranslateService } from '@ngx-translate/core';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { ActivatedRoute, Router } from '@angular/router';
import { getApplicationTypeIcon } from '@gravitee/ui-components/src/lib/theme';
import { ConfigurationService } from '../../services/configuration.service';
import { getPictureDisplayName } from '@gravitee/ui-components/src/lib/item';
import { switchMap, takeUntil } from 'rxjs/operators';
import { Pagination } from '@gravitee/ui-components/wc/gv-pagination';
import { Subject } from 'rxjs';
import StatusEnum = Subscription.StatusEnum;
import { formatCurlCommandLine } from '../../utils/utils';

@Component({
  selector: 'app-subscriptions',
  templateUrl: './subscriptions.component.html',
  styleUrls: ['./subscriptions.component.css'],
})
export class SubscriptionsComponent implements OnInit, OnDestroy {
  applications: Array<Application>;
  subscriptions: Array<Subscription>;
  subscriptionsMetadata: Map<string, any> = new Map<string, any>();
  apis: Map<string, Api> = new Map<string, Api>();
  format: (key) => Promise<any>;
  options: object;
  optionsSubscriptions: object;
  subs: Array<any>;
  subsByApplication: any;
  emptyKeyApplications: string;
  emptyKeySubscriptions: string;
  selectedApplicationId: string;
  selectedSubscriptions: Array<any>;
  apikeyHeader: string;
  skeleton = true;
  public curlExample: string;

  paginationData: Pagination;
  paginationPageSizes: Array<number>;
  paginationSize: number;
  nbApplications: number;
  empty: boolean;
  fragments: any = {
    pagination: 'pagination',
  };

  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  constructor(
    private applicationService: ApplicationService,
    private subscriptionService: SubscriptionService,
    private apiService: ApiService,
    private translateService: TranslateService,
    private router: Router,
    private configurationService: ConfigurationService,
    private ngZone: NgZone,
    private ref: ChangeDetectorRef,
    private activatedRoute: ActivatedRoute,
    private config: ConfigurationService,
  ) {}

  ngOnInit() {
    this.applications = [];
    this.subsByApplication = {};
    this.subs = [];
    this.emptyKeyApplications = i18n('subscriptions.applications.init');
    this.emptyKeySubscriptions = i18n('subscriptions.subscriptions.init');
    this.apikeyHeader = this.configurationService.get('portal.apikeyHeader');
    this.options = {
      selectable: true,
      data: [
        { field: '_links.picture', type: 'image', alt: item => getPictureDisplayName(item) },
        {
          field: 'name',
          type: 'gv-icon',
          width: '30px',
          attributes: {
            shape: item => getApplicationTypeIcon(item.applicationType),
            title: item => item.applicationType,
          },
        },
        {
          field: 'name',
          label: i18n('subscriptions.applications.name'),
        },
        { field: 'owner.display_name', label: i18n('subscriptions.applications.owner') },
        {
          type: 'gv-button',
          width: '30px',
          attributes: {
            link: true,
            href: item => `/applications/${item.id}`,
            title: i18n('subscriptions.applications.navigate'),
            icon: 'communication:share',
            onClick: item => this.goToApplication(item.id),
          },
        },
      ],
    };

    this.paginationPageSizes = this.config.get('pagination.size.values', [5, 10, 25, 50, 100]);
    this.paginationSize = this.config.get('pagination.size.default', 10);
    this.optionsSubscriptions = {
      selectable: true,
      data: [
        {
          field: item => {
            return this.subscriptionsMetadata[item.subscription.api].pictureUrl;
          },
          type: 'image',
          alt: item =>
            this.subscriptionsMetadata[item.subscription.api] && getPictureDisplayName(this.subscriptionsMetadata[item.subscription.api]),
        },
        {
          field: item => this.subscriptionsMetadata[item.subscription.api].name,
          tag: item => this.subscriptionsMetadata[item.subscription.api] && this.subscriptionsMetadata[item.subscription.api].version,
          label: i18n('subscriptions.subscriptions.api'),
        },
        { field: 'plan.name', label: i18n('subscriptions.subscriptions.plan') },
        { field: 'subscription.start_at', type: 'date', label: i18n('subscriptions.subscriptions.start_date') },
        { field: 'subscription.end_at', type: 'date', label: i18n('subscriptions.subscriptions.end_date') },
        {
          type: 'gv-button',
          width: '25px',
          attributes: {
            link: true,
            href: item => `/applications/${this.selectedApplicationId}/subscriptions?subscription=${item.subscription.id}`,
            title: i18n('subscriptions.subscriptions.navigate'),
            icon: 'communication:share',
            onClick: item => this.goToSubscription(item.subscription.id),
          },
        },
      ],
    };
    this.format = key => this.translateService.get(key).toPromise();

    this.activatedRoute.queryParamMap
      .pipe(
        takeUntil(this.unsubscribe$),
        switchMap(params =>
          this.applicationService.getApplications({
            size: Number(params.get('size') ?? this.paginationSize),
            page: Number(params.get('page') ?? 1),
          }),
        ),
      )
      .subscribe(response => {
        this.applications = response.data;
        this.skeleton = false;

        const pagination = response.metadata.pagination as unknown as Pagination;
        if (pagination) {
          this.paginationData = {
            size: this.paginationSize,
            sizes: this.paginationPageSizes,
            ...this.paginationData,
            ...pagination,
          };
          this.nbApplications = this.paginationData.total;
        } else {
          this.paginationData = null;
          this.nbApplications = 0;
        }

        this.empty = this.applications.length === 0;
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  async displayCurlExample(sub: any) {
    delete this.curlExample;
    this.ref.detectChanges();
    let keys;
    if (!sub.subscription.keys || !sub.subscription.keys[0]) {
      const subscriptionDetail = await this.subscriptionService
        .getSubscriptionById({
          subscriptionId: sub.subscription.id,
          include: ['keys'],
        })
        .toPromise();
      keys = subscriptionDetail.keys;
      sub.subscription.keys = subscriptionDetail.keys;
    } else {
      keys = sub.subscription.keys;
    }

    const entrypoints = this.subscriptionsMetadata[sub.subscription.api].entrypoints;
    const entrypoint = entrypoints && entrypoints[0] && entrypoints[0].target;
    if (entrypoint && keys[0]) {
      this.curlExample = formatCurlCommandLine(entrypoint, { name: this.apikeyHeader, value: keys[0].key });
    }
  }

  goToApplication(appId: string) {
    this.ngZone.run(() => this.router.navigate(['/applications', appId]));
  }

  goToSubscription(subId: string) {
    this.ngZone.run(() =>
      this.router.navigate(['/applications', this.selectedApplicationId, 'subscriptions'], { queryParams: { subscription: subId } }),
    );
  }

  onApplicationMouseEnter({ detail }) {
    this.selectSubscriptions(detail.item);
  }

  onApplicationClick({ detail }) {
    this.selectSubscriptions(detail.items[0]);
  }

  onSubscriptionClick({ detail }) {
    if (detail.items[0]) {
      this.displayCurlExample(detail.items[0]);
    } else {
      this.curlExample = null;
    }
  }

  onFocusOut() {
    this.emptyKeySubscriptions = i18n('subscriptions.subscriptions.init');
    this.subs = [];
  }

  get rowsHeight() {
    return this.curlExample ? '25vh' : '45vh';
  }

  async selectSubscriptions(application) {
    this.selectedApplicationId = application?.id;
    this.curlExample = null;
    this.selectedSubscriptions = [];
    if (this.selectedApplicationId) {
      if (this.subsByApplication[this.selectedApplicationId] == null) {
        const params: GetSubscriptionsRequestParams = { applicationId: this.selectedApplicationId, statuses: [StatusEnum.ACCEPTED] };
        const { data, metadata } = await this.subscriptionService.getSubscriptions(params).toPromise();
        this.subscriptionsMetadata = { ...this.subscriptionsMetadata, ...metadata };
        const subscription = await Promise.all(
          data.map(async applicationSubscription => {
            return {
              subscription: applicationSubscription,
              api: this.subscriptionsMetadata[applicationSubscription.api],
              plan: this.subscriptionsMetadata[applicationSubscription.plan],
            };
          }),
        );
        this.subsByApplication[this.selectedApplicationId] = subscription;
      }
      this.subs = this.subsByApplication[this.selectedApplicationId];
    } else {
      this.subs = [];
    }
    if (!this.subs || !this.subs.length) {
      this.emptyKeySubscriptions = i18n('subscriptions.subscriptions.empty');
    }
  }

  @HostListener(':gv-pagination:paginate', ['$event.detail'])
  onPaginate({ page, size }) {
    const queryParams: GetApplicationsRequestParams = {
      page,
      size,
    };
    this.router.navigate([], { queryParams, fragment: this.fragments.pagination });
  }
}
