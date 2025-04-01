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
import { Component, OnInit } from '@angular/core';
import { GioMenuSearchService } from '@gravitee/ui-particles-angular';

import {
  ORGANIZATION_MENU_GROUP_ID,
  OrganizationNavigationService,
} from './organization/configuration/navigation/organization-navigation.service';

@Component({
  selector: 'app-root',
  template: `<router-outlet></router-outlet>`,
  standalone: false,
})
export class AppComponent implements OnInit {
  constructor(
    private readonly organizationNavigationService: OrganizationNavigationService,
    private readonly gioMenuSearchService: GioMenuSearchService,
  ) {}

  public ngOnInit() {
    this.gioMenuSearchService.removeMenuSearchItems([ORGANIZATION_MENU_GROUP_ID]);
    this.gioMenuSearchService.addMenuSearchItems(this.organizationNavigationService.getOrganizationNavigationSearchItems());
  }
}
