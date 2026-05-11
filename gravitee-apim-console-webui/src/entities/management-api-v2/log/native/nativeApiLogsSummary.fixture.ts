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
import { isFunction } from 'lodash';

import { NativeApiLogsSummary } from './nativeApiLogsSummary';

export function fakeNativeApiLogsSummary(
  modifier?: Partial<NativeApiLogsSummary> | ((base: NativeApiLogsSummary) => NativeApiLogsSummary),
): NativeApiLogsSummary {
  const base: NativeApiLogsSummary = {
    countByConnectionStatus: {
      CONNECTED: 184,
      SESSION_ERROR: 32,
      CONNECTION_ERROR: 28,
      INTERNAL_ERROR: 4,
    },
  };

  if (isFunction(modifier)) return modifier(base);
  return { ...base, ...modifier };
}
