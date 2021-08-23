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

import UserService from '../../services/user.service';

class DashboardController {
  canViewAnalytics: boolean;
  private selectedIndex;
  private alertsEnabled;

  constructor(private $state: StateService, private UserService: UserService, private Constants) {
    'ngInject';

    this.alertsEnabled = Constants.org.settings.alert.enabled && UserService.isUserHasPermissions(['environment-alert-r']);
    if (this.$state.is('management.dashboard.alerts')) {
      this.selectedIndex = 3;
    } else if (this.$state.is('management.dashboard.analytics')) {
      this.selectedIndex = 2;
    } else if (this.$state.is('management.dashboard.apis-status')) {
      this.selectedIndex = 1;
    } else {
      this.selectedIndex = 0;
    }

    this.canViewAnalytics = UserService.isUserHasAllPermissions(['environment-platform-r']);
  }
}

export default DashboardController;
