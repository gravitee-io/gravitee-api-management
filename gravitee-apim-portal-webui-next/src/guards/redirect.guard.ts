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
import { DOCUMENT } from '@angular/common';
import { inject } from '@angular/core';

import { ConfigService } from '../services/config.service';

export const redirectGuard = (): boolean => {
  const enabled = inject(ConfigService).configuration?.portalNext?.access?.enabled;

  if (!enabled) {
    // Through DOCUMENT rather than the window global, which is not replaceable under jsdom 26.
    const location = inject(DOCUMENT).location;
    location.href = location.href.substring(0, location.href.indexOf('/next')) + '/404';
  }
  return true;
};
