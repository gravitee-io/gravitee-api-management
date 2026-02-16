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
import { toPairs } from 'lodash';
import { ActivatedRoute, Router } from '@angular/router';

import AnalyticsService from '../../../../services/analytics.service';
import { ApiService } from '../../../../services/api.service';
import NotificationService from '../../../../services/notification.service';

import '@gravitee/ui-components/wc/gv-code';

interface IQueryParam {
  key: string;
  value: string;
}

class ApiAnalyticsLogControllerAjs {
  public log: {
    clientRequest: any;
    proxyRequest: any;
    clientResponse: any;
    proxyResponse: any;
  };
  public backStateParams: {
    q: any;
    size: any;
    from: any;
    to: any;
    page: any;
  };
  public activatedRoute: ActivatedRoute;

  public previousLog: string;
  public nextLog: string;

  private static headersAsList(obj) {
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

      (obj.headersAsList as Array<[string, string]>).sort(([key1], [key2]) => key1.localeCompare(key2));
    }
  }
  constructor(
    public readonly Constants: any,
    private readonly ngRouter: Router,
    private readonly NotificationService: NotificationService,
    private readonly AnalyticsService: AnalyticsService,
    private readonly ApiService: ApiService,
  ) {}

  $onInit() {
    this.backStateParams = {
      from: this.activatedRoute.snapshot.queryParams.from,
      to: this.activatedRoute.snapshot.queryParams.to,
      q: this.activatedRoute.snapshot.queryParams.q,
      page: this.activatedRoute.snapshot.queryParams.page,
      size: this.activatedRoute.snapshot.queryParams.size,
    };

    this.ApiService.getLog(
      this.activatedRoute.snapshot.params.apiId,
      this.activatedRoute.snapshot.params.logId,
      this.activatedRoute.snapshot.queryParams.timestamp,
    ).then(response => {
      this.log = response.data;
      if (this.log.clientRequest != null) {
        ApiAnalyticsLogControllerAjs.headersAsList(this.log.clientRequest);
        this.log.clientRequest = {
          ...this.log.clientRequest,
          queryParams: this.extractQueryParams(this.log.clientRequest.uri),
        };
      }

      if (this.log.proxyRequest != null) {
        ApiAnalyticsLogControllerAjs.headersAsList(this.log.proxyRequest);
        this.log.proxyRequest = {
          ...this.log.proxyRequest,
          queryParams: this.extractQueryParams(this.log.proxyRequest.uri),
        };
      }

      if (this.log.clientResponse != null) {
        ApiAnalyticsLogControllerAjs.headersAsList(this.log.clientResponse);
      }

      if (this.log.proxyResponse != null) {
        ApiAnalyticsLogControllerAjs.headersAsList(this.log.proxyResponse);
      }

      if (this.AnalyticsService.getFetchedLogs()) {
        this.fillPreviousNext();
      } else {
        const query = this.AnalyticsService.buildQueryFromState(this.activatedRoute.snapshot.queryParams);
        this.ApiService.findLogs(this.activatedRoute.snapshot.params.apiId, query).then(logs => {
          this.AnalyticsService.setFetchedLogs(logs.data.logs);
          this.fillPreviousNext();
        });
      }
    });
  }

  codeMirrorOptions(log) {
    if (log) {
      const codeMirrorOptions = {
        lineNumbers: true,
        lineWrapping: true,
        matchBrackets: true,
        mode: this.getMimeType(log),
      };
      return codeMirrorOptions;
    }

    // codeMirrorOptions for error block
    return {
      lineNumbers: true,
      mode: 'text/x-java',
    };
  }

  goToLog(logId) {
    this.ngRouter.navigate(['../', logId], {
      relativeTo: this.activatedRoute,
      queryParams: {
        ...this.backStateParams,
        timestamp: this.activatedRoute.snapshot.queryParams.timestamp,
      },
    });
    this.ApiService.getLog(this.activatedRoute.snapshot.params.apiId, logId, this.activatedRoute.snapshot.queryParams.timestamp).then(
      response => {
        this.log = response.data;
        this.$onInit();
      },
    );
  }

  fillPreviousNext() {
    const logs = this.AnalyticsService.getFetchedLogs();
    for (let i = 0; i < logs.length; i++) {
      if (logs[i] === this.activatedRoute.snapshot.params.logId) {
        this.previousLog = logs[i - 1];
        this.nextLog = logs[i + 1];
        break;
      }
    }
  }

  getMimeType(log): string | null {
    if (log.headers) {
      let contentType;
      if (log.headers['content-type']) {
        contentType = log.headers['content-type'][0];
      } else if (log.headers['Content-Type']) {
        contentType = log.headers['Content-Type'][0];
      }
      if (contentType) {
        const splitElement = contentType.split(';', 1)[0];
        // hack to manage some "application/xxx" content-type.
        if (splitElement.includes('json') || splitElement.includes('javascript')) {
          return 'javascript';
        }
        if (splitElement.includes('xml')) {
          return 'xml';
        }
        return splitElement;
      }
    }
    return null;
  }

  /**
   * Convert an input uri (path + query params) to an array of IQueryParam
   *
   * Example:
   * `/gme?type=monthly&bucket=status_repartition&bucket=status_repartition-2`
   * -->
   * [
   *   { key: 'type', value: 'monthly'},
   *   { key: 'bucket', value: '[ status_repartition, status_repartition-2]'},
   * ]
   *
   * @param uri
   */
  extractQueryParams(uri: string): IQueryParam[] {
    if (!uri.includes('?')) {
      return [];
    }

    // Slice the interesting part of the uri and split to get all the query params
    const queryParamsMap = uri
      .slice(uri.indexOf('?') + 1)
      .split('&')
      .map(queryParamsAsString => {
        // A simple `.split` is not enough as query param values can contains `=` themselves
        const indexOfEqualChar = queryParamsAsString.indexOf('=');
        return [queryParamsAsString.substring(0, indexOfEqualChar), queryParamsAsString.substring(indexOfEqualChar + 1)];
      })
      // Convert in a map to group query params with the same key (it can happen when sending an array), for example:
      // {
      //   type: ['monthly'],
      //   bucket: ['status_repartition', 'status_repartition-2'],
      // }
      .reduce(
        (acc, [key, value]) => {
          const currentValues = acc[key] || [];
          acc[key] = [...currentValues, value];
          return acc;
        },
        {} as { [key in string]: string[] },
      );

    // Convert the map to an array and join the header values if needed
    return toPairs(queryParamsMap).map(([key, values]) => ({
      key,
      value: values.length === 1 ? values[0] : `[ ${values.join(', ')} ]`,
    }));
  }

  onCopyBodySuccess() {
    this.NotificationService.show('Body has been copied to clipboard');
  }

  configureLogging() {
    this.ngRouter.navigate(['../', 'configuration'], {
      relativeTo: this.activatedRoute,
    });
  }

  backToLogs() {
    this.ngRouter.navigate(['../'], {
      relativeTo: this.activatedRoute,
      queryParams: {
        ...this.backStateParams,
      },
    });
  }

  goToSubscription(subscriptionId: string) {
    this.ngRouter.navigate(['../', '../', 'subscriptions', subscriptionId], {
      relativeTo: this.activatedRoute,
    });
  }
}
ApiAnalyticsLogControllerAjs.$inject = ['Constants', 'ngRouter', 'NotificationService', 'AnalyticsService', 'ApiService'];

export const ApiAnalyticsLogComponentAjs: ng.IComponentOptions = {
  controller: ApiAnalyticsLogControllerAjs,
  bindings: {
    activatedRoute: '<',
  },
  template: require('html-loader!./analytics-log.html').default, // eslint-disable-line @typescript-eslint/no-var-requires
};
