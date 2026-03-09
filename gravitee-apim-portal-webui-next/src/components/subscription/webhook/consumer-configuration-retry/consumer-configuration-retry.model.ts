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

import { RetryOptionsType, RetryStrategiesType } from '../../../../entities/subscription';

export type RetryFormType = FormGroup<{
  retryOption: FormControl<RetryOptionsType>;
  retryStrategy?: FormControl<RetryStrategiesType | null>;
  maxAttempts?: FormControl<number | null>;
  initialDelaySeconds?: FormControl<number | null>;
  maxDelaySeconds?: FormControl<number | null>;
}>;

export interface RetryFormValues {
  retryOption: RetryOptionsType;
  retryStrategy: RetryStrategiesType | null;
  maxAttempts: number | null;
  initialDelaySeconds: number | null;
  maxDelaySeconds: number | null;
}
