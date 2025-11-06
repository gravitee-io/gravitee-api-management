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
import {Component, OnDestroy, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MatIconModule} from '@angular/material/icon';
import {MatButtonModule} from '@angular/material/button';
import {MatDialog, MatDialogModule} from '@angular/material/dialog';
import {MatMenuModule} from '@angular/material/menu';
import {ActivatedRoute, Router} from '@angular/router';
import {EMPTY, of, Subject} from 'rxjs';
import {catchError, filter, switchMap, takeUntil, tap} from 'rxjs/operators';
import {GioConfirmDialogComponent, GioConfirmDialogData} from '@gravitee/ui-particles-angular';

import {SnackBarService} from '../../services-ngx/snack-bar.service';
import {UiPortalMenuLinksService} from '../../services-ngx/ui-portal-menu-links.service';
import {PortalMenuLink} from '../../entities/management-api-v2';
import {
  MenuLinkAddDialogComponent,
  MenuLinkAddDialogData,
  MenuLinkAddDialogResult,
} from '../top-bar/menu-link-dialog/menu-link-add-dialog.component';

export type SectionNodeType = 'page' | 'folder' | 'link';

export interface SectionNode {
  id: string;
  label: string;
  type: SectionNodeType;
  data?: PortalMenuLink;
  unpublished?: boolean;
  link?: boolean;
  children?: SectionNode[];
}

@Component({
  selector: 'portal-sections-tree',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule, MatDialogModule, MatMenuModule],
  templateUrl: './tree.component.html',
  styleUrls: ['./tree.component.scss'],
})
export class TreeComponent implements OnInit, OnDestroy {
  tree = signal<SectionNode[]>([]);

  expanded = signal<Record<string, boolean>>({});
  selectedId = signal<string | null>(null);
  apiExpanded = signal<boolean>(false);
  private unsubscribe$ = new Subject<void>();

  constructor(
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
    private readonly uiPortalMenuLinksService: UiPortalMenuLinksService,
    private readonly router: Router,
    private readonly route: ActivatedRoute,
  ) {
  }

  ngOnInit(): void {
    this.initializeTree();
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }

  isExpanded = (id: string) => this.expanded()[id];

  toggleApi() {
    this.apiExpanded.update((v) => !v);
  }

  toggle(node: SectionNode) {
    if (node.type !== 'folder') return;
    this.expanded.update((e) => ({...e, [node.id]: !e[node.id]}));
  }

  select(node: SectionNode) {
    this.selectedId.set(node.id);
  }

  onAddLinkClick(): void {
    this.matDialog
      .open<MenuLinkAddDialogComponent, MenuLinkAddDialogData, MenuLinkAddDialogResult>(MenuLinkAddDialogComponent, {
        data: {},
        role: 'dialog',
        id: 'addMenuLinkDialog',
        width: '500px',
      })
      .afterClosed()
      .pipe(
        filter((menuLink) => !!menuLink),
        switchMap((menuLink) => this.uiPortalMenuLinksService.create({...menuLink})),
        tap((menuLink) => {
          this.snackBarService.success(`Menu link '${menuLink.name}' created successfully`);
          this.initializeTree();
        }),
        catchError(({error}) => {
          this.snackBarService.error(error?.message ? error.message : 'Error during creation');
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  addSection(node?: SectionNode) {
    this.onAddLinkClick();
  }

  onAddSectionMenu() {
    // TODO: Implement real Section creation when backend is available
    this.onAddLinkClick();
  }

  onAddPageMenu() {
    // TODO: Implement real Page creation when backend is available
    this.onAddLinkClick();
  }

  onAddLinkMenu() {
    this.onAddLinkClick();
  }

  addFolder(node?: SectionNode) {
    this.onAddLinkClick();
  }

  edit(node?: SectionNode) {
    if (!node) return;
    this.router.navigate(['../top-bar', node.id], {relativeTo: this.route});
  }

  link(node?: SectionNode) {
    this.onAddLinkClick();
  }

  delete(node?: SectionNode) {
    if (!node) return;

    const title = 'Delete menu link';
    const content = 'Please note that once your menu link is deleted, it cannot be restored.';
    const confirmButton = 'Delete';
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        data: {title, content, confirmButton},
        role: 'alertdialog',
        id: 'confirmDialog',
      })
      .afterClosed()
      .pipe(
        filter((confirmed) => confirmed),
        switchMap(() => this.uiPortalMenuLinksService.delete(node.id)),
        tap(() => {
          this.snackBarService.success(`Menu link deleted successfully`);
          this.initializeTree();
        }),
        catchError(({error}) => {
          this.snackBarService.error(error?.message ?? 'Error during deletion');
          return of(false);
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  trackById = (_: number, n: SectionNode) => n.id;

  private initializeTree() {
    this.uiPortalMenuLinksService
      .list(1, 9999)
      .pipe(
        tap((response) => {
          const nodes: SectionNode[] = response.data.map((link: PortalMenuLink) => ({
            id: link.id,
            label: link.name,
            type: 'link',
            data: link,
            link: true,
          }));
          this.tree.set(nodes);
        }),
        catchError(() => {
          this.tree.set([]);
          return of();
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }
}
