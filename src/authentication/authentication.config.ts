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

import * as _ from 'lodash';

import {AuthProvider} from 'satellizer';

function authenticationConfig ($authProvider: AuthProvider, Constants) {
  'ngInject';

  if (Constants.authentication) {
    $authProvider.withCredentials = true;
    
    // Google
    let googleConfig = Constants.authentication.google;
    if (googleConfig && googleConfig.clientId) {
      $authProvider.google({
        url: Constants.baseURL + 'auth/google',
        clientId: googleConfig.clientId
      });
    }

    // GitHub
    let githubConfig = Constants.authentication.github;
    if (githubConfig && githubConfig.clientId) {
      $authProvider.github({
        url: Constants.baseURL + 'auth/github',
        clientId: githubConfig.clientId
      });
    }

    // Custom
    let customConfig = Constants.authentication.oauth2;
    if (customConfig) {
      $authProvider.oauth2(_.merge(customConfig, {
        url: Constants.baseURL + 'auth/oauth2',
        oauthType: '2.0',
        redirectUri: window.location.origin + window.location.pathname,
        requiredUrlParams: ['scope'],
        scopeDelimiter: ' '
      }));
    }
  }
}

export default authenticationConfig;
