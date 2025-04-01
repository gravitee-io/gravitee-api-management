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

import { Component, Inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';

import { GroupItem, OrganizationNavigationService } from './organization-navigation.service';

import { Constants } from '../../../entities/Constants';

@Component({
  selector: 'org-navigation',
  styleUrls: ['./org-navigation.component.scss'],
  templateUrl: './org-navigation.component.html',
  standalone: false,
})
export class OrgNavigationComponent implements OnInit {
  public groupItems: GroupItem[] = [];

  constructor(
    private readonly router: Router,
    @Inject(Constants) private readonly constants: Constants,
    private readonly organizationNavigationService: OrganizationNavigationService,
  ) {}

  ngOnInit(): void {
    this.groupItems = this.organizationNavigationService.getOrganizationNavigationRoutes();
  }

  goBack() {
    // Reloads current environment if exists, otherwise loads the default root path
    this.router.navigate([this.constants.org.currentEnv?.id ?? '/']);
  }
}
