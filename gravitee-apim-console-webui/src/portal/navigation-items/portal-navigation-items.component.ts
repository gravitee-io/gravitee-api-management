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
import { GraviteeMarkdownEditorModule } from '@gravitee/gravitee-markdown';

import { GioCardEmptyStateModule } from '@gravitee/ui-particles-angular';
import { Component, inject, OnInit } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { map } from 'rxjs/operators';

import { PortalHeaderComponent } from '../components/header/portal-header.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { SectionNode, TreeComponent } from '../components/tree-component/tree.component';
import { PortalMenuLink } from '../../entities/management-api-v2';
import { SnackBarService } from '../../services-ngx/snack-bar.service';

@Component({
  selector: 'portal-navigation-items',
  templateUrl: './portal-navigation-items.component.html',
  styleUrls: ['./portal-navigation-items.component.scss'],
  imports: [
    PortalHeaderComponent,
    GraviteeMarkdownEditorModule,
    ReactiveFormsModule,
    EmptyStateComponent,
    GioCardEmptyStateModule,
    MatButton,
    TreeComponent,
    HttpClientModule,
  ],
})
export class PortalNavigationItemsComponent implements OnInit {
  contentControl = new FormControl({
    value: '',
    disabled: true,
  });

  menuLinks: PortalMenuLink[] | null = null;
  isEmpty = true;
  pageNotFound = false;

  navId = toSignal(inject(ActivatedRoute).queryParams.pipe(map((params) => params['navId'] ?? null)));

  constructor(
    private readonly http: HttpClient,
    private readonly snackBarService: SnackBarService,
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    // TODO replace mock with real backend call when available
    this.http.get<{ data: PortalMenuLink[] }>('assets/mocks/portal-menu-links.json').subscribe({
      next: ({ data }) => {
        this.menuLinks = data ?? [];
        this.isEmpty = (this.menuLinks?.length ?? 0) === 0;
      },
      error: (err) => {
        this.menuLinks = [];
        this.isEmpty = true;
        this.snackBarService.error('Failed to load portal navigation items: ' + err);
      },
    });
  }

  onSelect($event: SectionNode) {
    this.pageNotFound = false;
    this.router
      .navigate(['.'], {
        relativeTo: this.activatedRoute,
        queryParams: { navId: $event.id },
        queryParamsHandling: 'merge',
      })
      .catch((err) => this.snackBarService.error('Failed to navigate to portal navigation items: ' + err));
  }

  onPageNotFound() {
    this.pageNotFound = true;
    this.snackBarService.error('The requested Navigation Item does not exist.');
  }
}
