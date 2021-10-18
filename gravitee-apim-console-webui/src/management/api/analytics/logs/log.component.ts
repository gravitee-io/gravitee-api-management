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
import { StateService } from '@uirouter/core';
import NotificationService from '../../../../services/notification.service';
import '@gravitee/ui-components/wc/gv-code';

const LogComponent: ng.IComponentOptions = {
  bindings: {
    log: '<',
  },
  controller: function ($state: StateService, NotificationService: NotificationService, Constants: any) {
    'ngInject';
    this.Constants = Constants;
    this.NotificationService = NotificationService;

    this.backStateParams = {
      from: $state.params.from,
      to: $state.params.to,
      q: $state.params.q,
      page: $state.params.page,
      size: $state.params.size,
    };

    this.codeMirrorOptions = (log) => {
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
    };

    this.$onInit = () => {
      this.headersAsList(this.log.clientRequest);
      this.headersAsList(this.log.proxyRequest);
      this.headersAsList(this.log.clientResponse);
      this.headersAsList(this.log.proxyResponse);
    };

    this.headersAsList = (obj) => {
      if (obj) {
        obj.headersAsList = [];
        for (const k in obj.headers) {
          if (obj.headers.hasOwnProperty(k)) {
            for (const v in obj.headers[k]) {
              if (obj.headers[k].hasOwnProperty(v)) {
                obj.headersAsList.push([k, obj.headers[k][v]]);
              }
            }
          }
        }
      }
    };

    this.getMimeType = function (log) {
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
    };

    this.onCopyBodySuccess = function (evt) {
      this.NotificationService.show('Body has been copied to clipboard');
    };
  },
  template: require('./log.html'),
};

export default LogComponent;
