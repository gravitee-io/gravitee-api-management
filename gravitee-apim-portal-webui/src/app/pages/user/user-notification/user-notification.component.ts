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
import { ChangeDetectorRef, Component, HostListener, OnDestroy, OnInit } from '@angular/core';

import { PortalNotification, UserService } from '../../../../../projects/portal-webclient-sdk/src/lib';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { TranslateService } from '@ngx-translate/core';
import { SearchQueryParam } from '../../../utils/search-query-param.enum';
import { ActivatedRoute, Router } from '@angular/router';
import { ConfigurationService } from '../../../services/configuration.service';
import { EventService, GvEvent } from '../../../services/event.service';

import '@gravitee/ui-components/wc/gv-table';

@Component({
  selector: 'app-user-notification',
  templateUrl: './user-notification.component.html',
  styleUrls: ['./user-notification.component.css'],
})
export class UserNotificationComponent implements OnInit, OnDestroy {
  static NEW = 'gv-notifications:onNew';
  static REMOVE = 'gv-notifications:onRemove';

  notifications: Array<PortalNotification>;
  options: any;
  total: any;
  format: any;
  paginationData: any = {};
  pageSizes: Array<any>;
  size: number;

  constructor(
    private userService: UserService,
    private translateService: TranslateService,
    private route: ActivatedRoute,
    private router: Router,
    private config: ConfigurationService,
    private eventService: EventService,
    private ref: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    this.pageSizes = this.config.get('pagination.size.values');
    this.size = this.route.snapshot.queryParams[SearchQueryParam.SIZE]
      ? parseInt(this.route.snapshot.queryParams[SearchQueryParam.SIZE], 10)
      : this.config.get('pagination.size.default');
    this.format = key => this.translateService.get(key).toPromise();
    this.options = {
      data: [
        { field: 'title', width: '150px' },
        { field: 'message' },
        {
          type: 'gv-relative-time',
          width: '150px',
          attributes: {
            datetime: item => item.created_at,
          },
          style: () => 'text-align: right',
        },
        {
          type: 'gv-icon',
          width: '30px',
          style: () => 'text-align: right',
          attributes: {
            onClick: item => this.markAsRead(item.id),
            shape: 'code:check',
            title: i18n('user.notifications.read'),
          },
        },
      ],
    };
    this.loadNotifications();
    this.eventService.subscribe(({ type }) => {
      if (type === UserNotificationComponent.NEW) {
        this.loadNotifications();
      }
    });
  }

  ngOnDestroy() {
    this.eventService.unsubscribe();
  }

  loadNotifications() {
    return this.userService
      .getCurrentUserNotifications({
        size: this.size,
        page: this.route.snapshot.queryParams[SearchQueryParam.PAGE] || 1,
      })
      .toPromise()
      .then(response => {
        this.notifications = response.data;
        this.total = response.metadata.pagination ? response.metadata.pagination.total : 0;
        this.buildPaginationData();
      });
  }

  private buildPaginationData() {
    const totalPages = this.total / this.size;
    this.paginationData = {
      first: 1,
      last: totalPages,
      current_page: this.route.snapshot.queryParams[SearchQueryParam.PAGE] || 1,
      total_pages: totalPages,
      total: this.total,
    };
  }

  markAsRead(notificationId) {
    this.userService
      .deleteCurrentUserNotificationByNotificationId({ notificationId })
      .toPromise()
      .then(() => {
        this.eventService.dispatch(new GvEvent(UserNotificationComponent.REMOVE));
        this.loadNotifications().then(() => {
          this.ref.detectChanges();
        });
      });
  }

  @HostListener(':gv-pagination:paginate', ['$event.detail'])
  _onPaginate({ page }) {
    const queryParams: any = {};
    queryParams[SearchQueryParam.PAGE] = page;
    queryParams[SearchQueryParam.SIZE] = this.size;
    queryParams.log = null;
    this.router
      .navigate([], {
        queryParams,
        queryParamsHandling: 'merge',
      })
      .then(() => {
        this.loadNotifications();
      });
  }

  onSelectSize(size) {
    this.router
      .navigate([], {
        queryParams: { size, page: null, log: null },
        queryParamsHandling: 'merge',
      })
      .then(() => {
        this.size = size;
        this.loadNotifications();
      });
  }
}
