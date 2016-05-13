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
function interceptorConfig($httpProvider) {
  'ngInject';
  $httpProvider.defaults.headers.common['X-Requested-With'] = 'XMLHttpRequest';

  var interceptorUnauthorized = function ($q, $injector) {
    return {
      responseError: function (error) {
        var unauthorizedError = !error || error.status === 401;
        var errorMessage = '';

        if (unauthorizedError) {
          if (error.config.headers['Authorization']) {
            errorMessage = 'Wrong user or password';
          } else {
            $injector.get('$rootScope').$broadcast('graviteeLogout');
          }
        } else if (error.status === 500) {
          errorMessage = 'Unexpected error';
        } else if (error.status === 503) {
          errorMessage = 'Server unavailable';
        }

        $injector.get('NotificationService').error(error, errorMessage);

        return $q.reject(error);
      }
    };
  };

  var interceptorTimeout = function ($q, $injector) {
    return {
      request: function (config) {
        config.timeout = 10000;
        return config;
      },
      responseError: function (error) {
        if (error && error.status <= 0) {
          $injector.get('NotificationService').error('Server unreachable');
        }
        return $q.reject(error);
      }
    };
  };


  if ($httpProvider.interceptors) {
    $httpProvider.interceptors.push(interceptorUnauthorized);
    $httpProvider.interceptors.push(interceptorTimeout);
  }
}

export default interceptorConfig;
