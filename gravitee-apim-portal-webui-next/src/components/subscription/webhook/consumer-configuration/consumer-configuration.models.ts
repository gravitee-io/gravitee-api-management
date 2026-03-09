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
import { FormControl, FormGroup } from '@angular/forms';

import { Header, RetryConfiguration, SslOptions, WebhookSubscriptionConfigurationAuth } from '../../../../entities/subscription';

export type ConsumerConfigurationForm = FormGroup<{
  channel: FormControl<string>;
  consumerConfiguration: FormGroup<{
    callbackUrl: FormControl<string>;
    headers: FormControl<Header[] | null>;
    retry: FormControl<RetryConfiguration>;
    ssl: FormControl<SslOptions>;
    auth: FormControl<WebhookSubscriptionConfigurationAuth>;
  }>;
}>;

export type ConsumerConfigurationValues = {
  channel: string;
  consumerConfiguration: {
    callbackUrl: string;
    headers?: Header[] | null;
    retry: RetryConfiguration;
    ssl: SslOptions;
    auth: WebhookSubscriptionConfigurationAuth;
  };
};

export interface ConsumerConfigurationFormData {
  value: ConsumerConfigurationValues | undefined;
  isValid: boolean;
}
