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

import { LoggingV4 } from './loggingV4';
import { Sampling } from './sampling';
import { TracingV4 } from './tracingV4';

export interface Analytics {
  /**
   * Whether analytics is enabled.
   */
  enabled?: boolean;
  sampling?: Sampling;
  logging?: LoggingV4;
  tracing?: TracingV4;
}
