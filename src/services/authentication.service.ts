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

import { AuthProvider } from "satellizer";

class AuthenticationService {

  private providers: {
    id: string;
    name: string;
    icon: string
  }[] = [];

  constructor(
    private $auth: AuthProvider,
    private Constants) {
    'ngInject';

    if (Constants.authentication) {
      let googleConfig = Constants.authentication.google;
      if (googleConfig && googleConfig.clientId) {
        this.providers.push({
          id: 'google',
          name: 'Google',
          icon: 'google-plus'
        });
      }

      let githubConfig = Constants.authentication.github;
      if (githubConfig && githubConfig.clientId) {
        this.providers.push({
          id: 'github',
          name: 'GitHub',
          icon: 'github-circle'
        });
      }
    }
  }

  authenticate(provider) {
    return this.$auth.authenticate(provider);
  }

  getProviders() {
    return this.providers;
  }

}

export default AuthenticationService;
