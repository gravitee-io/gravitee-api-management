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
import { ComponentHarness } from '@angular/cdk/testing';

export class ApiRuntimeLogsNativeDetailsHarness extends ComponentHarness {
  static hostSelector = 'api-runtime-logs-native-details';

  private readonly notFoundBanner = this.locatorForOptional('[data-testid=native_log_not_found]');
  private readonly loadFailedBanner = this.locatorForOptional('[data-testid=native_log_load_failed]');
  private readonly title = this.locatorForOptional('[data-testid=native_log_title]');
  private readonly connectionCard = this.locatorForOptional('[data-testid=native_log_connection_card]');
  private readonly clientCard = this.locatorForOptional('[data-testid=native_log_client_card]');
  private readonly serverCard = this.locatorForOptional('[data-testid=native_log_server_card]');
  private readonly errorCard = this.locatorForOptional('[data-testid=native_log_error_card]');
  private readonly backLink = this.locatorForOptional('[data-testid=native_log_back]');

  isNotFoundBannerVisible(): Promise<boolean> {
    return this.notFoundBanner().then(el => el != null);
  }

  isLoadFailedBannerVisible(): Promise<boolean> {
    return this.loadFailedBanner().then(el => el != null);
  }

  isTitleVisible(): Promise<boolean> {
    return this.title().then(el => el != null);
  }

  isConnectionCardVisible(): Promise<boolean> {
    return this.connectionCard().then(el => el != null);
  }

  isClientCardVisible(): Promise<boolean> {
    return this.clientCard().then(el => el != null);
  }

  isServerCardVisible(): Promise<boolean> {
    return this.serverCard().then(el => el != null);
  }

  isErrorCardVisible(): Promise<boolean> {
    return this.errorCard().then(el => el != null);
  }

  isBackLinkVisible(): Promise<boolean> {
    return this.backLink().then(el => el != null);
  }
}
