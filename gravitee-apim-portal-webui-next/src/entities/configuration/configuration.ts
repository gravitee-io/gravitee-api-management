/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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

import { ConfigurationAnalytics } from './configuration-analytics';
import { ConfigurationApplication } from './configuration-application';
import { ConfigurationAuthentication } from './configuration-authentication';
import { ConfigurationDocumentation } from './configuration-documentation';
import { ConfigurationPlan } from './configuration-plan';
import { ConfigurationPortalNext } from './configuration-portal-next';
import { ConfigurationReCaptcha } from './configuration-re-captcha';
import { ConfigurationScheduler } from './configuration-scheduler';
import { Enabled } from './enabled';

export interface Configuration {
  portalNext?: ConfigurationPortalNext;
  authentication?: ConfigurationAuthentication;
  scheduler?: ConfigurationScheduler;
  documentation?: ConfigurationDocumentation;
  plan?: ConfigurationPlan;
  apiReview?: Enabled;
  analytics?: ConfigurationAnalytics;
  application?: ConfigurationApplication;
  recaptcha?: ConfigurationReCaptcha;
  alert?: Enabled;
}
