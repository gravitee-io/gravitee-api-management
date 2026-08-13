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
import { Locator } from '@playwright/test';
import { BasePage } from './base.page';

export class HomePage extends BasePage {
  get overviewTab(): Locator {
    return this.byTestId('home-tab-overview');
  }

  get apiHealthCheckTab(): Locator {
    return this.byTestId('home-tab-api-health-check');
  }

  get tasksTab(): Locator {
    return this.byTestId('home-tab-tasks');
  }

  get broadcastsTab(): Locator {
    return this.byTestId('home-tab-broadcasts');
  }

  get apiEventsHeading(): Locator {
    return this.page.getByRole('heading', { name: 'API Events' });
  }
}
