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
import { Observable } from 'rxjs';
import { LicenseOptions } from '@gravitee/ui-particles-angular';

export interface MenuItem {
  routerLink?: string;
  routerLinkActiveOptions?: { exact: boolean };
  displayName: string;
  license?: LicenseOptions;
  iconRight$?: Observable<string>;
  header?: MenuItemHeader;
  tabs?: MenuItem[];
  icon?: string;
  category?: string;
}

export interface MenuGroupItem {
  title: string;
  items: MenuItem[];
}

export interface MenuItemHeader {
  title?: string;
  subtitle?: string;
  action?: MenuItemHeaderAction;
}

export interface MenuItemHeaderAction {
  text: string;
  targetUrl?: string;
  disabled: boolean;
  disabledTooltip?: string;
}
