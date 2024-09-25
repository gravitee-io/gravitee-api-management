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

import NotificationService from './services/notification.service';
import { CsrfInterceptor } from './shared/interceptors/csrf.interceptor';
import { IfMatchEtagInterceptor } from './shared/interceptors/if-match-etag.interceptor';

function interceptorConfig($httpProvider: angular.IHttpProvider, Constants) {
  $httpProvider.defaults.headers.common['X-Requested-With'] = 'XMLHttpRequest';

  $httpProvider.defaults.withCredentials = true;

  // Explicitly disable automatic csrf handling as it will not work for cross-domain (using custom csrf interceptor).
  $httpProvider.defaults.xsrfCookieName = 'none';
  $httpProvider.defaults.xsrfHeaderName = 'none';

  const interceptorTimeout = function ($q: angular.IQService, $injector: angular.auto.IInjectorService): angular.IHttpInterceptor {
    return {
      request: function (config) {
        // Use defined HTTP timeout or default value
        config.timeout = config.timeout || 10000;
        return config;
      },
      responseError: function (error) {
        const notificationService = $injector.get('NotificationService') as NotificationService;
        if (!error.config || !error.config.silentCall) {
          if (error.config && !error.config.tryItMode) {
            if (error && error.status <= 0 && error.xhrStatus !== 'abort') {
              notificationService.showError('Server unreachable');
            }
          } else {
            notificationService.showError('Unable to call the remote service.');
          }
        }
        return $q.reject(error);
      },
    };
  };
  interceptorTimeout.$inject = ['$q', '$injector'];

  const csrfInterceptor = function ($q: angular.IQService): angular.IHttpInterceptor {
    return {
      request: function (config) {
        if (CsrfInterceptor.xsrfToken) {
          config.headers['X-Xsrf-Token'] = CsrfInterceptor.xsrfToken;
        }
        return config;
      },
      response: function (response) {
        if (response.headers('X-Xsrf-Token')) {
          CsrfInterceptor.xsrfToken = response.headers('X-Xsrf-Token');
        }
        return response;
      },
      responseError: function (response) {
        if (response.headers('X-Xsrf-Token')) {
          CsrfInterceptor.xsrfToken = response.headers('X-Xsrf-Token');
        }
        return $q.reject(response);
      },
    };
  };
  csrfInterceptor.$inject = ['$q'];

  // This interceptor aims to resolve the problem with Satellizer which, after exchanging oauth provider's token with apim's one, adds the apim token on all requests (through Authorization bearer header).
  // It caused logout problems between Portal and Management console because session Cookie is successfully removed but token is still sent by Satellizer using Authorization header.
  // See https://github.com/sahat/satellizer#question-how-can-i-avoid-sending-authorization-header-on-all-http-requests
  const noSatellizerAuthorizationInterceptor = function (): angular.IHttpInterceptor {
    return {
      request: function (config) {
        if (config.url.startsWith(Constants.baseURL)) {
          (config as any).skipAuthorization = true;
        }
        return config;
      },
    };
  };

  const replaceEnvInterceptor = function ($q: angular.IQService, $injector: angular.auto.IInjectorService): angular.IHttpInterceptor {
    return {
      request: function (config) {
        const constants: any = $injector.get('Constants');
        if (config.url.includes('{:envId}')) {
          config.url = config.url.replace('{:envId}', constants.org.currentEnv.id);
        }
        return config;
      },
    };
  };
  replaceEnvInterceptor.$inject = ['$q', '$injector'];

  const ifMatchEtagInterceptor = function (ngIfMatchEtagInterceptor: IfMatchEtagInterceptor): angular.IHttpInterceptor {
    return {
      request: function (config) {
        ngIfMatchEtagInterceptor.interceptRequest(config.method, config.url, (etagValue) => {
          config.headers[IfMatchEtagInterceptor.ETAG_HEADER_IF_MATCH] = etagValue;
        });
        return config;
      },
      response: function (response) {
        ngIfMatchEtagInterceptor.interceptResponse(response.config.url, response.headers(IfMatchEtagInterceptor.ETAG_HEADER));
        return response;
      },
    };
  };
  ifMatchEtagInterceptor.$inject = ['ngIfMatchEtagInterceptor'];

  if ($httpProvider.interceptors) {
    // Add custom noSatellizerAuthorizationInterceptor at the beginning of the list to make sure they are activated before others interceptors such as Satellizer's interceptors.
    $httpProvider.interceptors.unshift(noSatellizerAuthorizationInterceptor);
    $httpProvider.interceptors.push(csrfInterceptor);
    $httpProvider.interceptors.push(interceptorTimeout);
    $httpProvider.interceptors.push(replaceEnvInterceptor);
    $httpProvider.interceptors.push(ifMatchEtagInterceptor);
  }
}
interceptorConfig.$inject = ['$httpProvider', 'Constants'];

export default interceptorConfig;
