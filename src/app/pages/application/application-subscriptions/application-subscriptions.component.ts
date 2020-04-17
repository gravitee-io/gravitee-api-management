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
import { ChangeDetectorRef, Component, HostListener, OnInit } from '@angular/core';
import '@gravitee/ui-components/wc/gv-list';
import '@gravitee/ui-components/wc/gv-info';
import '@gravitee/ui-components/wc/gv-rating-list';
import '@gravitee/ui-components/wc/gv-confirm';
import {
  ApiService,
  SubscriptionService,
  Subscription, GetSubscriptionsRequestParams, ApplicationService, PermissionsService
} from '@gravitee/ng-portal-webclient';
import { ActivatedRoute, Router } from '@angular/router';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { TranslateService } from '@ngx-translate/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { LoaderService } from '../../../services/loader.service';
import StatusEnum = Subscription.StatusEnum;
import { NotificationService } from '../../../services/notification.service';

@Component({
  selector: 'app-application-subscriptions',
  templateUrl: './application-subscriptions.component.html',
  styleUrls: ['./application-subscriptions.component.css']
})
export class ApplicationSubscriptionsComponent implements OnInit {

  subscriptions: Array<Subscription>;
  options: any;
  format: any;
  form: FormGroup = this.formBuilder.group({
    api: '',
    status: [],
    apiKey: '',
  });
  apisOptions: any;
  statusOptions: any;
  miscellaneous: Array<any>;
  metadata: any;
  validApiKeys: Array<any>;
  expiredApiKeys: Array<any>;
  selectedSubscriptions: Array<string>;
  displayExpiredApiKeys = false;
  canDelete = false;
  canUpdate = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private applicationService: ApplicationService,
    private subscriptionService: SubscriptionService,
    private notificationService: NotificationService,
    private translateService: TranslateService,
    private apiService: ApiService,
    private formBuilder: FormBuilder,
    public loaderService: LoaderService,
    private permissionsService: PermissionsService,
    private ref: ChangeDetectorRef,
  ) {
  }

  ngOnInit() {
    const application = this.route.snapshot.data.application;
    if (application) {
      this.permissionsService.getCurrentUserPermissions({ applicationId: application.id }).toPromise()
        .then(permissions => {
          this.canDelete = permissions.SUBSCRIPTION && permissions.SUBSCRIPTION.includes('D');
          this.canUpdate = permissions.SUBSCRIPTION && permissions.SUBSCRIPTION.includes('U');
          this.format = (key) => this.translateService.get(key).toPromise();
          this.apisOptions = [];
          this.options = {
            selectable: true,
            data: [
              { field: 'api', label: i18n('application.subscriptions.api') },
              { field: 'plan', label: i18n('application.subscriptions.plan') },
              { field: 'created_at', type: 'date', label: i18n('application.subscriptions.created_at') },
              {
                field: 'subscribed_by', label: i18n('application.subscriptions.subscribed_by'),
                format: (item) => this.metadata[item] && this.metadata[item].name
              },
              { field: 'processed_at', type: 'date', label: i18n('application.subscriptions.processed_at') },
              { field: 'start_at', type: 'date', label: i18n('application.subscriptions.start_at') },
              {
                field: 'status', label: i18n('application.subscriptions.status'),
                format: (key) => {
                  const statusKey = 'common.status.' + key.toUpperCase();
                  return this.translateService.get(statusKey).toPromise();
                },
                style: (item) => {
                  let style = 'text-align: right;';
                  switch (item.status.toUpperCase()) {
                    case StatusEnum.ACCEPTED:
                      return style += 'color: #009B5B';
                    case StatusEnum.PAUSED:
                    case StatusEnum.PENDING:
                      return style += 'color: #FA8C16';
                    case StatusEnum.REJECTED:
                      return style += 'color: #F5222D';
                  }
                },
              },
              {
                type: 'gv-icon',
                width: '25px',
                style: () => 'text-align: right',
                confirmMessage: i18n('application.subscriptions.renew.message'),
                condition: (item) => this.canUpdate &&
                  [StatusEnum.ACCEPTED, StatusEnum.PAUSED].includes(item.status.toUpperCase()),
                attributes: {
                  onClick: (item) => this.renewSubscription(item.id),
                  shape: 'code:lock-overturning',
                  title: i18n('application.subscriptions.renew.title'),
                },
              },
              {
                type: 'gv-icon',
                width: '25px',
                style: () => 'text-align: right',
                confirmMessage: i18n('application.subscriptions.close.message'),
                condition: (item) => this.canDelete &&
                  [StatusEnum.ACCEPTED, StatusEnum.PAUSED, StatusEnum.PENDING].includes(item.status.toUpperCase()),
                attributes: {
                  onClick: (item) => this.closeSubscription(item.id),
                  shape: 'general:trash',
                  title: i18n('application.subscriptions.close.title'),
                },
              },
            ]
          };
        });
      this.translateService.get([
        i18n('application.miscellaneous.owner'),
        i18n('application.miscellaneous.type'),
        i18n('application.miscellaneous.createdDate'),
        i18n('application.miscellaneous.lastUpdate')])
        .subscribe(({
                      'application.miscellaneous.owner': owner,
                      'application.miscellaneous.type': type,
                      'application.miscellaneous.createdDate': createdDate,
                      'application.miscellaneous.lastUpdate': lastUpdate,
                    }) => {
          this.translateService.get('application.types', { type: application.applicationType })
            .toPromise()
            .then((applicationType => {
              this.miscellaneous = [
                { key: owner, value: application.owner.display_name },
                { key: type, value: applicationType },
                {
                  key: createdDate,
                  value: new Date(application.created_at),
                  date: 'short'
                },
                {
                  key: lastUpdate,
                  value: new Date(application.updated_at),
                  date: 'relative'
                },
              ];
            }));
        });

      this.applicationService.getSubscriberApisByApplicationId({ applicationId: application.id, size: -1 }).toPromise().then(apis => {
        this.apisOptions = [];
        apis.data.forEach(api => {
          this.apisOptions.push({ label: api.name + ' (' + api.version + ')', value: api.id });
        });
      });

      const statusKeys = Object.keys(StatusEnum).map(s => 'common.status.' + s);
      this.translateService.get(statusKeys).toPromise().then(translatedKeys => {
        this.statusOptions = Object.keys(StatusEnum).map((s, i) => {
          return { label: Object.values(translatedKeys)[i], value: s };
        });
        this.form.patchValue({ status: [StatusEnum.ACCEPTED, StatusEnum.PAUSED, StatusEnum.PENDING] });
        this.search(true);
      });
    }
  }

  search(displaySubscription?) {
    const applicationId = this.route.snapshot.params.applicationId;
    const requestParameters: GetSubscriptionsRequestParams = { applicationId };
    if (this.form.value.api) {
      requestParameters.apiId = this.form.value.api;
    }
    if (this.form.value.status) {
      requestParameters.statuses = this.form.value.status;
    }
    this.subscriptionService.getSubscriptions(requestParameters).toPromise().then(response => {
      this.subscriptions = response.data;
      this.metadata = response.metadata;
      this.subscriptions.forEach(subscription => {
        subscription.api = this.metadata[subscription.api] && this.metadata[subscription.api].name;
        subscription.plan = this.metadata[subscription.plan] && this.metadata[subscription.plan].name;
      });
      if (displaySubscription && this.route.snapshot.queryParams.subscription) {
        const subscription = this.subscriptions.find(s => s.id === this.route.snapshot.queryParams.subscription);
        this.selectedSubscriptions = [subscription.id];
        this.onSelectSubscription(subscription);
      } else {
        this.selectedSubscriptions = [];
        this.onSelectSubscription(null);
      }
    });
  }

  reset() {
    this.form.reset({
      status: [StatusEnum.ACCEPTED, StatusEnum.PAUSED, StatusEnum.PENDING]
    });
  }

  closeSubscription(subscriptionId) {
    this.subscriptionService.closeSubscription({ subscriptionId }).toPromise().then(() => {
      this.notificationService.success(i18n('application.subscriptions.success.close'));
      this.search(true);
    });
  }

  renewSubscription(subscriptionId) {
    this.subscriptionService.renewKeySubscription({ subscriptionId }).toPromise().then(() => {
      this.notificationService.success(i18n('application.subscriptions.success.renew'));
      this.search(true);
    });
  }

  revokeApiKey(subscriptionId, keyId) {
    this.subscriptionService.revokeKeySubscription({ subscriptionId, keyId }).toPromise().then(() => {
      this.notificationService.success(i18n('application.subscriptions.apiKey.success.revoke'));
      this.search(true);
    });
  }

  @HostListener(':gv-table:select', ['$event.detail.items[0]'])
  onSelectSubscription(subscription: Subscription) {
    this.router.navigate([], { queryParams: { subscription: subscription ? subscription.id : null }, fragment: 's' });
    if (subscription) {
      this.subscriptionService.getSubscriptionById({ subscriptionId: subscription.id, include: ['keys'] }).toPromise().then(sub => {
        this.validApiKeys = sub.keys.filter(apiKey => this.isApiKeyValid(apiKey));
        this.expiredApiKeys = sub.keys.filter(apiKey => !this.isApiKeyValid(apiKey));
        this.ref.detectChanges();
      });
    } else {
      delete this.validApiKeys;
      delete this.expiredApiKeys;
    }
  }

  getDateAsString(timestamp, long?) {
    return long ? new Date(timestamp).toLocaleString() : new Date(timestamp).toLocaleDateString();
  }

  endAt(apiKey) {
    return apiKey.revoked_at || apiKey.expire_at;
  }

  apiKeyEnded(apiKey) {
    const endAt = this.endAt(apiKey);
    return endAt && (new Date(endAt) < new Date());
  }

  isApiKeyValid(apiKey) {
    return !this.endAt(apiKey) || !this.apiKeyEnded(apiKey);
  }

  toggleDisplayExpired() {
    this.displayExpiredApiKeys = !this.displayExpiredApiKeys;
    this.ref.detectChanges();
  }
}
