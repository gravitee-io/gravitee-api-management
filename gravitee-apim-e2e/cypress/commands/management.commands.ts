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
import { RequestInfo, RequestInfoHolder } from '@model/technical';
import { ApiManagementCommands } from './management/apis.management.commands';
import { PortalSettingsManagementCommands } from './management/portal-settings.management.commands';
import { EnvironmentConfigurationManagementCommands } from './management/environment-configuration.management.commands';
import { UserManagementCommands } from './management/user.management.commands';
import { ApisQualityManagementCommands } from './management/apis-quality.management.commands';
import { ApisRatingsManagementCommands } from './management/apis-ratings.management.commands';
import { ApplicationsManagementCommands } from './management/applications.management.commands';
import { ApisPlansManagementCommands } from './management/apis-plans.management.commands';
import { ApisSubscriptionsManagementCommands } from './management/apis-subscriptions.management.commands';

export class ManagementCommands extends RequestInfoHolder {
  constructor(requestInfo: RequestInfo) {
    super(requestInfo);
  }

  apis(): ApiManagementCommands {
    return new ApiManagementCommands(this.requestInfo);
  }

  apisPlans(): ApisPlansManagementCommands {
    return new ApisPlansManagementCommands(this.requestInfo);
  }

  apisQuality(): ApisQualityManagementCommands {
    return new ApisQualityManagementCommands(this.requestInfo);
  }

  apisRating(): ApisRatingsManagementCommands {
    return new ApisRatingsManagementCommands(this.requestInfo);
  }

  apisSubscriptions(): ApisSubscriptionsManagementCommands {
    return new ApisSubscriptionsManagementCommands(this.requestInfo);
  }

  applications(): ApplicationsManagementCommands {
    return new ApplicationsManagementCommands(this.requestInfo);
  }

  portalSettings(): PortalSettingsManagementCommands {
    return new PortalSettingsManagementCommands(this.requestInfo);
  }

  environmentConfiguration(): EnvironmentConfigurationManagementCommands {
    return new EnvironmentConfigurationManagementCommands(this.requestInfo);
  }

  users(): UserManagementCommands {
    return new UserManagementCommands(this.requestInfo);
  }
}
