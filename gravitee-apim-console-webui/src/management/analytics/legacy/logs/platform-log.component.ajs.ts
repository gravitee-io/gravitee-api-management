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
import { Router } from '@angular/router';

import NotificationService from '../../../../services/notification.service';
import AnalyticsService from '../../../../services/analytics.service';

const PlatformLogComponentAjs: ng.IComponentOptions = {
  bindings: {
    activatedRoute: '<',
  },
  controller: [
    'ngRouter',
    'NotificationService',
    'Constants',
    'AnalyticsService',
    function (ngRouter: Router, NotificationService: NotificationService, Constants: any, AnalyticsService: AnalyticsService) {
      this.Constants = Constants;
      this.NotificationService = NotificationService;
      this.ngRouter = ngRouter;
      this.AnalyticsService = AnalyticsService;

      this.$onInit = () => {
        this.AnalyticsService.getLog(this.activatedRoute.snapshot.params.logId, this.activatedRoute.snapshot.queryParams.timestamp).then(
          response => {
            this.log = response.data;

            this.headersAsList(this.log.clientRequest);
            this.headersAsList(this.log.proxyRequest);
            this.headersAsList(this.log.clientResponse);
            this.headersAsList(this.log.proxyResponse);
          },
        );
      };

      this.headersAsList = obj => {
        if (obj) {
          obj.headersAsList = [];
          for (const k in obj.headers) {
            // eslint-disable-next-line no-prototype-builtins
            if (obj.headers.hasOwnProperty(k)) {
              for (const v in obj.headers[k]) {
                // eslint-disable-next-line no-prototype-builtins
                if (obj.headers[k].hasOwnProperty(v)) {
                  obj.headersAsList.push([k, obj.headers[k][v]]);
                }
              }
            }
          }
        }
      };

      this.goToLogs = () => {
        this.ngRouter.navigate(['../'], {
          relativeTo: this.activatedRoute,
          queryParams: {
            from: this.activatedRoute.snapshot.queryParams.from,
            to: this.activatedRoute.snapshot.queryParams.to,
            q: this.activatedRoute.snapshot.queryParams.q,
            page: this.activatedRoute.snapshot.queryParams.page,
            size: this.activatedRoute.snapshot.queryParams.size,
          },
        });
      };

      this.goToSubscription = (apiId: string, subscriptionId: string) => {
        this.ngRouter.navigate(['../../apis', apiId, 'subscriptions', subscriptionId], {
          relativeTo: this.activatedRoute,
        });
      };

      this.getMimeType = function (log) {
        if (log.headers['Content-Type'] !== undefined) {
          const contentType = log.headers['Content-Type'][0];
          return contentType.split(';', 1)[0];
        }

        return null;
      };

      this.onCopyBodySuccess = function (evt) {
        this.NotificationService.show('Body has been copied to clipboard');
        evt.clearSelection();
      };
    },
  ],
  template: require('html-loader!./platform-log.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
};

export default PlatformLogComponentAjs;
