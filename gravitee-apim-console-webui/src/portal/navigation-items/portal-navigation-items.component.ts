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
import { Component, effect, inject, OnInit } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { map } from 'rxjs/operators';

import { PortalHeaderComponent } from '../components/header/portal-header.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { SectionNode, TreeComponent } from '../components/tree-component/tree.component';
import { PortalMenuLink } from '../../entities/management-api-v2';
import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { PortalContentService } from '../../services/portal-content.service';
import { PortalEditorHelperService } from '../../services/portal-editor-helper.service';
import { PortalTreeHelperService } from '../../services/portal-tree-helper.service';

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
    private readonly contentService: PortalContentService,
    private readonly editorHelper: PortalEditorHelperService,
    private readonly snackBarService: SnackBarService,
    private readonly activatedRoute: ActivatedRoute,
    private readonly treeHelper: PortalTreeHelperService,
  ) {
    effect(() => {
      const id = this.navId();
      if (id) {
        this.validateNavSelection();
      } else {
        this.pageNotFound = false;
      }
    });
  }

  ngOnInit(): void {
    this.contentService.getMenuLinks().subscribe({
      next: (links) => {
        this.menuLinks = links ?? [];
        this.isEmpty = (this.menuLinks?.length ?? 0) === 0;
        this.validateNavSelection();
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

    if ($event.type !== 'page') {
      this.editorHelper.clearEditor(this.contentControl);
      this.treeHelper
        .navigateToNavId(this.activatedRoute, $event.id)
        .catch((err) => this.snackBarService.error('Failed to navigate to portal navigation items: ' + err));
      return;
    }

    this.editorHelper.resetEditor(this.contentControl);

    this.contentService.getPageContent($event.id, $event.label).subscribe({
      next: (content) => {
        this.editorHelper.enableEditor(this.contentControl);
        this.editorHelper.setEditorContent(this.contentControl, content);
      },
      error: (err) => {
        this.snackBarService.error('Failed to load page content' + err);
        this.editorHelper.enableEditor(this.contentControl);
      },
    });
    this.treeHelper
      .navigateToNavId(this.activatedRoute, $event.id)
      .catch((err) => this.snackBarService.error('Failed to navigate to portal navigation items: ' + err));
  }

  onPageNotFound() {
    this.pageNotFound = true;
    this.editorHelper.clearEditor(this.contentControl);
    this.snackBarService.error('The requested Navigation Item does not exist.');
  }

  private validateNavSelection(): void {
    if (!this.menuLinks || this.menuLinks.length === 0) {
      return;
    }
    const id = this.navId();
    if (!id) {
      return;
    }
    const tree = this.treeHelper.mapLinksToNodes(this.menuLinks);
    const exists = this.treeHelper.findNode(tree, (n) => n.id === id);
    if (!exists) {
      this.onPageNotFound();
    } else {
      this.pageNotFound = false;
    }
  }
}
