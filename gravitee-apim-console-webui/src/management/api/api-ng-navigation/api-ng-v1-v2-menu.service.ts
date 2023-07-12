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
import { Injectable } from '@angular/core';

import { MenuGroupItem, MenuItem } from './MenuGroupItem';
import { ApiMenuService } from './ApiMenuService';

import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';

@Injectable()
export class ApiNgV1V2MenuService implements ApiMenuService {
  constructor(private readonly permissionService: GioPermissionService) {}

  public getMenu(): {
    subMenuItems: MenuItem[];
    groupItems: MenuGroupItem[];
  } {
    const subMenuItems: MenuItem[] = [
      // TODO: To be completed
    ];
    const groupItems: MenuGroupItem[] = [
      // TODO: To be completed
    ].filter((group) => !!group);

    return { subMenuItems, groupItems };
  }
}
