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
import { Component, computed, input, Input, OnInit } from '@angular/core';
import { NgOptimizedImage } from '@angular/common';
import { MatTooltip } from '@angular/material/tooltip';

import { ApiDocumentationV4BreadcrumbComponent } from '../api-documentation-v4-breadcrumb/api-documentation-v4-breadcrumb.component';
import { Breadcrumb, getLogoForPageType, getTooltipForPageType, PageType } from '../../../../../entities/management-api-v2';

@Component({
  selector: 'api-documentation-v4-page-header',
  standalone: true,
  imports: [ApiDocumentationV4BreadcrumbComponent, NgOptimizedImage, MatTooltip],
  templateUrl: './api-documentation-v4-page-header.component.html',
  styleUrl: './api-documentation-v4-page-header.component.scss',
})
export class ApiDocumentationV4PageHeaderComponent implements OnInit {
  @Input()
  pageType: PageType;

  @Input()
  breadcrumbs: Breadcrumb[];

  @Input()
  isHomepage: boolean = false;

  name = input<string>();
  headerPageName = computed(() => this.name() ?? 'Add new page');

  iconUrl: string;
  iconTooltip: string;

  ngOnInit() {
    this.iconUrl = getLogoForPageType(this.pageType);
    this.iconTooltip = getTooltipForPageType(this.pageType);
  }
}
