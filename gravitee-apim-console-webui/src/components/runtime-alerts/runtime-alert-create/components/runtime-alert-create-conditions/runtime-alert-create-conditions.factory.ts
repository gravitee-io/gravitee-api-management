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

import { FormControl, FormGroup, Validators } from '@angular/forms';

import { Metrics } from '../../../../../entities/alert';

export class RuntimeAlertCreateConditionsFactory {
  static create(rule: string) {
    if (rule.endsWith('@MISSING_DATA')) {
      return new FormGroup({
        duration: new FormControl<number>(null, [Validators.required, Validators.min(1)]),
        timeUnit: new FormControl<string>(null, [Validators.required]),
        type: new FormControl<string>('MISSING_DATA', [Validators.required]),
      });
    }

    switch (rule) {
      case 'REQUEST@METRICS_SIMPLE_CONDITION':
        return new FormGroup({
          metric: new FormControl<Metrics>(null, [Validators.required]),
          type: new FormControl<string>({ value: null, disabled: true }, [Validators.required]),
        });
      case 'REQUEST@METRICS_AGGREGATION':
        return new FormGroup({
          metric: new FormControl<Metrics>(null, [Validators.required]),
          type: new FormControl<string>('AGGREGATION', [Validators.required]),
          function: new FormControl<string>(null, [Validators.required]),
          operator: new FormControl<string>(null, [Validators.required]),
          threshold: new FormControl<number>(null, [Validators.required]),
          duration: new FormControl<number>(null, [Validators.required, Validators.min(1)]),
          timeUnit: new FormControl<string>(null, [Validators.required]),
          projections: new FormGroup({
            property: new FormControl<string>(null),
          }),
        });
      case 'REQUEST@METRICS_RATE':
        return new FormGroup({
          comparison: new FormGroup({
            metric: new FormControl<Metrics>(null, [Validators.required]),
            type: new FormControl<string>({ value: null, disabled: true }, [Validators.required]),
          }),
          type: new FormControl<string>('RATE', [Validators.required]),
          operator: new FormControl<string>(null, [Validators.required]),
          threshold: new FormControl<number>(null, [Validators.required]),
          duration: new FormControl<number>(null, [Validators.required, Validators.min(1), Validators.max(100)]),
          timeUnit: new FormControl<string>(null, [Validators.required]),
          projections: new FormGroup({
            property: new FormControl<string>(null),
          }),
        });
      case 'ENDPOINT_HEALTH_CHECK@API_HC_ENDPOINT_STATUS_CHANGED':
        return new FormGroup({
          projections: new FormGroup({
            property: new FormControl<string>(null),
          }),
          type: new FormControl<string>('API_HC_ENDPOINT_STATUS_CHANGED', [Validators.required]),
        });
      default:
        return null;
    }
  }
}
