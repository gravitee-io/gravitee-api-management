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
import { ChangeDetectorRef, Component, HostListener, NgZone, OnInit } from '@angular/core';
import '@gravitee/ui-components/wc/gv-input';
import '@gravitee/ui-components/wc/gv-list';
import '@gravitee/ui-components/wc/gv-rating-list';
import '@gravitee/ui-components/wc/gv-confirm';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { FormBuilder, FormControl, FormGroup } from '@angular/forms';
import { getPictureDisplayName } from '@gravitee/ui-components/src/lib/item';
import { Pagination } from '@gravitee/ui-components/wc/gv-pagination';

import { NotificationService } from '../../../services/notification.service';
import {
  ApiKeyModeEnum,
  ApiService,
  Application,
  ApplicationService,
  GetSubscriptionsRequestParams,
  Key,
  PermissionsResponse,
  PermissionsService,
  Plan,
  Subscription,
  SubscriptionService,
} from '../../../../../projects/portal-webclient-sdk/src/lib';
import { ConfigurationService } from '../../../services/configuration.service';

const StatusEnum = Subscription.StatusEnum;
const SecurityEnum = Plan.SecurityEnum;

type ApplicationSubscriptionsFormType = FormGroup<{
  api: FormControl<string>;
  status: FormControl<GetSubscriptionsRequestParams['statuses']>;
  apiKey: FormControl<string>;
}>;

@Component({
  selector: 'app-application-subscriptions',
  templateUrl: './application-subscriptions.component.html',
  styleUrls: ['./application-subscriptions.component.css'],
  standalone: false,
})
export class ApplicationSubscriptionsComponent implements OnInit {
  subscriptions: Array<Subscription>;
  options: any;
  format: any;
  form: ApplicationSubscriptionsFormType = this.formBuilder.group({
    api: '',
    status: [],
    apiKey: '',
  });
  apisOptions: any;
  statusOptions: any;
  metadata: any;
  selectedSubscriptions: Array<string>;
  selectedSubscription: Subscription;
  selectedSubscriptionFederated: boolean;
  displayExpiredApiKeys: boolean;
  canDelete: boolean;
  canUpdate: boolean;
  isSearching: boolean;
  application: Application;
  sharedAPIKey: Key | null;
  sharedAPIKeyLoaded: boolean;
  paginationData: Pagination;
  paginationPageSizes: Array<number>;
  paginationSize: number;
  selectedPageSize: number;
  fragments: { pagination: string } = {
    pagination: 'pagination',
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private applicationService: ApplicationService,
    private subscriptionService: SubscriptionService,
    private notificationService: NotificationService,
    private translateService: TranslateService,
    private apiService: ApiService,
    private formBuilder: FormBuilder,
    private permissionsService: PermissionsService,
    private ref: ChangeDetectorRef,
    private ngZone: NgZone,
    private config: ConfigurationService,
  ) {}

  ngOnInit() {
    this.application = this.route.snapshot.data.application;
    const permissions: PermissionsResponse = this.route.snapshot.data.permissions;
    if (this.application) {
      this.canDelete = permissions?.SUBSCRIPTION?.includes('D');
      this.canUpdate = permissions?.SUBSCRIPTION?.includes('U');
      this.format = key => this.translateService.get(key).toPromise();
      this.paginationPageSizes = this.config.get('pagination.size.values', [5, 10, 25, 50, 100]);
      this.paginationSize = this.config.get('pagination.size.default', 10);
      this.apisOptions = [];
      this.options = {
        selectable: true,
        data: [
          {
            field: 'api',
            type: 'image',
            alt: item => this.metadata[item.api] && getPictureDisplayName(this.metadata[item.api]),
            format: item => this.metadata[item] && this.metadata[item].pictureUrl,
          },
          {
            field: 'api',
            label: 'application.subscriptions.api',
            tag: item => this.metadata[item.api] && this.metadata[item.api].version,
            format: item => this.metadata[item] && this.metadata[item].name,
          },
          {
            field: 'plan',
            label: 'application.subscriptions.plan',
            format: item => this.metadata[item] && this.metadata[item].name,
          },
          {
            field: 'plan',
            label: 'application.subscriptions.security_type',
            type: () => 'div',
            attributes: {
              innerHTML: item => {
                const securityType = this.metadata[item.plan]?.securityType?.toLocaleLowerCase().replace('_', ' ');
                if (this.isAPIKeySubscription(item.plan) && this.applicationHasSharedKey()) {
                  return `${securityType} <gv-state> ${this.application.api_key_mode} </gv-state>`;
                }
                return securityType;
              },
            },
          },
          { field: 'created_at', type: 'date', label: 'application.subscriptions.created_at', width: '160px' },
          {
            field: 'subscribed_by',
            label: 'application.subscriptions.subscribed_by',
            format: item => this.metadata[item] && this.metadata[item].name,
            width: '190px',
          },
          {
            field: 'status',
            label: 'application.subscriptions.status',
            width: '80px',
            format: key => {
              const statusKey = 'common.status.' + key.toUpperCase();
              return this.translateService.get(statusKey).toPromise();
            },
            style: item => {
              switch (item.status.toUpperCase()) {
                case StatusEnum.ACCEPTED:
                  return 'color: #009B5B';
                case StatusEnum.PAUSED:
                case StatusEnum.PENDING:
                  return 'color: #FA8C16';
                case StatusEnum.REJECTED:
                  return 'color: #F5222D';
              }
            },
          },
          {
            type: 'gv-button',
            width: '25px',
            condition: item => this.metadata[item.api] && this.metadata[item.api].state === 'published',
            attributes: {
              link: true,
              href: item => `/catalog/api/${item.api}`,
              title: 'application.subscriptions.navigateToApi',
              icon: 'communication:share',
              onClick: item => this.goToApi(item.api),
            },
          },
        ],
      };

      this.applicationService
        .getSubscriberApisByApplicationId({ applicationId: this.application.id, size: -1 })
        .toPromise()
        .then(apis => {
          this.apisOptions = [];
          apis.data.forEach(api => {
            this.apisOptions.push({ label: api.name + ' (' + api.version + ')', value: api.id });
          });
        });

      const statusKeys = Object.keys(StatusEnum).map(s => 'common.status.' + s);

      this.translateService
        .get(statusKeys)
        .toPromise()
        .then(translatedKeys => {
          this.statusOptions = Object.keys(StatusEnum).map((s, i) => {
            return { label: Object.values(translatedKeys)[i], value: s };
          });
          this.form.patchValue({ status: [StatusEnum.ACCEPTED, StatusEnum.PAUSED, StatusEnum.PENDING] });
        })
        .then(() => this.search(true));
    }
  }

  computeSharedApiKey() {
    // FIXME: what if there are shared apikeys among not shared apikeys ?
    if (this.applicationHasSharedKey()) {
      this.sharedAPIKeyLoaded = false;
      delete this.sharedAPIKey;
      const sharedAPIKeySub = this.subscriptions?.find(subscription => {
        return (
          this.isAPIKeySubscription(subscription.plan) &&
          [StatusEnum.ACCEPTED, StatusEnum.PAUSED, StatusEnum.CLOSED].includes(subscription.status)
        );
      });
      if (sharedAPIKeySub) {
        this.loadSubscriptionKeys(sharedAPIKeySub.id)
          .then(keys => {
            // First find sharedKey without end date.
            this.sharedAPIKey = keys.find(key => !this.endAt(key));
            if (!this.sharedAPIKey) {
              // If none exist, find a valid one with an end date.
              this.sharedAPIKey = keys.find(key => this.isApiKeyValid(key));
            }
          })
          .finally(() => (this.sharedAPIKeyLoaded = true));
      }
    }
  }

  canRenewApiKey(subscription: Subscription) {
    return (
      subscription &&
      this.canUpdate &&
      [`${StatusEnum.ACCEPTED}`, `${StatusEnum.PAUSED}`].includes(subscription.status.toUpperCase()) &&
      this.isAPIKeySubscription(subscription.plan) &&
      !this.applicationHasSharedKey()
    );
  }

  canRevokeApiKey(subscription: Subscription) {
    return (
      subscription &&
      this.canUpdate &&
      [`${StatusEnum.ACCEPTED}`, `${StatusEnum.PAUSED}`].includes(subscription.status.toUpperCase()) &&
      this.isAPIKeySubscription(subscription.plan) &&
      !this.applicationHasSharedKey()
    );
  }

  canRenewSharedApiKey() {
    return this.canUpdate && this.applicationHasSharedKey();
  }

  canRevokeSharedApiKey() {
    return this.canUpdate && this.applicationHasSharedKey();
  }

  canCloseSubscription(subscription: Subscription) {
    return (
      subscription &&
      this.canDelete &&
      subscription.origin !== 'KUBERNETES' &&
      [`${StatusEnum.ACCEPTED}`, `${StatusEnum.PAUSED}`, `${StatusEnum.PENDING}`].includes(subscription.status.toUpperCase())
    );
  }

  goToApi(apiId: string) {
    this.ngZone.run(() => this.router.navigate(['/catalog/api/', apiId]));
  }

  search(displaySubscription?, size?, page?): Promise<void> {
    const applicationId = this.route.snapshot.params.applicationId;
    const requestParameters: GetSubscriptionsRequestParams = { applicationId };
    if (this.form.value.api) {
      requestParameters.apiId = this.form.value.api;
    }
    if (this.form.value.status) {
      requestParameters.statuses = this.form.value.status;
    }
    if (size) {
      requestParameters.size = size;
      requestParameters.page = page;
      this.selectedPageSize = size;
    } else if (this.selectedPageSize) {
      requestParameters.size = this.selectedPageSize;
    }
    this.isSearching = true;
    return this.subscriptionService
      .getSubscriptions(requestParameters)
      .toPromise()
      .then(response => {
        const pagination = response.metadata.pagination as unknown as Pagination;
        if (pagination) {
          this.paginationData = {
            size: this.paginationSize,
            sizes: this.paginationPageSizes,
            ...this.paginationData,
            ...pagination,
          };
        } else {
          this.paginationData = null;
        }
        this.subscriptions = response.data;
        this.metadata = response.metadata;
        if (displaySubscription && this.route.snapshot.queryParams.subscription) {
          const subscription = this.subscriptions.find(s => s.id === this.route.snapshot.queryParams.subscription);
          this.selectedSubscriptions = [subscription.id];
          this.onSelectSubscription(subscription);
        } else {
          this.selectedSubscriptions = [];
          this.onSelectSubscription(null);
        }
      })
      .then(() => this.computeSharedApiKey())
      .finally(() => (this.isSearching = false));
  }

  reset() {
    this.form.reset({
      status: [StatusEnum.ACCEPTED, StatusEnum.PAUSED, StatusEnum.PENDING],
    });
  }

  closeSubscription(subscriptionId) {
    this.subscriptionService
      .closeSubscription({ subscriptionId })
      .toPromise()
      .then(() => {
        this.notificationService.success('application.subscriptions.success.close');
        this.search(false);
      });
  }

  renewApiKey(subscriptionId) {
    this.subscriptionService
      .renewKeySubscription({ subscriptionId })
      .toPromise()
      .then(() => {
        this.notificationService.success('application.subscriptions.success.renew');
        this.search(true);
      });
  }

  revokeApiKey(subscriptionId, apiKey) {
    this.subscriptionService
      .revokeKeySubscription({ subscriptionId, apiKey })
      .toPromise()
      .then(() => {
        this.notificationService.success('application.subscriptions.apiKey.success.revoke');
        this.search(true);
      });
  }

  renewSharedApiKey() {
    this.applicationService
      .renewSharedKey({ applicationId: this.application.id })
      .toPromise()
      .then(() => {
        this.notificationService.success('application.shared-key.renew.success');
        this.search(true);
      });
  }

  revokeSharedApiKey() {
    this.applicationService
      .revokeSharedKey({ applicationId: this.application.id, apiKey: this.sharedAPIKey.id })
      .toPromise()
      .then(() => {
        this.notificationService.success('application.shared-key.revoke.success');
        this.search(true);
      });
  }

  onSelectSubscription(subscription: Subscription) {
    this.router.navigate([], { queryParams: { subscription: subscription ? subscription.id : null }, fragment: 's' });
    if (subscription) {
      this.selectedSubscription = subscription;
      this.selectedSubscriptionFederated = this.metadata[subscription.api].definitionVersion === 'FEDERATED';
      if (!this.selectedSubscription.keys || !this.selectedSubscription.keys[0]) {
        this.loadSubscriptionKeys(subscription.id);
      }
    } else {
      delete this.selectedSubscription;
      delete this.selectedSubscriptionFederated;
    }
  }

  loadSubscriptionKeys(subscriptionId: string): Promise<Key[]> {
    return this.subscriptionService
      .getSubscriptionById({ subscriptionId, include: ['keys'] })
      .toPromise()
      .then(subscription => {
        this.subscriptions.find(s => s.id === subscriptionId).keys = subscription.keys;
        this.ref.detectChanges();
        return subscription.keys;
      });
  }

  getValidApiKeys(sub: Subscription) {
    if (sub && sub.keys) {
      const validApiKeys = sub.keys.filter(apiKey => this.isApiKeyValid(apiKey));
      if (validApiKeys && validApiKeys.length > 0) {
        return validApiKeys;
      }
    }
  }

  getExpiredApiKeys(sub: Subscription) {
    if (sub && sub.keys) {
      const expiredApiKeys = sub.keys.filter(apiKey => !this.isApiKeyValid(apiKey));
      if (expiredApiKeys && expiredApiKeys.length > 0) {
        return expiredApiKeys;
      }
    }
  }

  endAt(apiKey) {
    return apiKey.revoked_at || apiKey.expire_at;
  }

  apiKeyEnded(apiKey) {
    const endAt = this.endAt(apiKey);
    return endAt && new Date(endAt) < new Date();
  }

  isApiKeyValid(apiKey) {
    return !this.endAt(apiKey) || !this.apiKeyEnded(apiKey);
  }

  toggleDisplayExpired() {
    this.displayExpiredApiKeys = !this.displayExpiredApiKeys;
    this.ref.detectChanges();
  }

  isAPIKeySubscription(planId: string) {
    return this.metadata[planId].securityType === SecurityEnum.APIKEY;
  }

  applicationHasSharedKey() {
    return this.application?.api_key_mode === ApiKeyModeEnum.SHARED;
  }

  @HostListener(':gv-pagination:paginate', ['$event.detail'])
  onPaginate({ page, size }) {
    this.search(true, size, page);
  }
}
