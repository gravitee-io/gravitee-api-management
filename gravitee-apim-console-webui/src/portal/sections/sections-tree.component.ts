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
import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatMenuModule } from '@angular/material/menu';
import { ActivatedRoute, Router } from '@angular/router';
import { of, Subject } from 'rxjs';
import { catchError, filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';

import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { UiPortalMenuLinksService } from '../../services-ngx/ui-portal-menu-links.service';
import { PortalMenuLink } from '../../entities/management-api-v2';

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
  templateUrl: './sections-tree.component.html',
  styleUrls: ['./sections-tree.component.scss'],
})
export class SectionsTreeComponent implements OnInit, OnDestroy {
  tree = signal<SectionNode[]>([]);
  expanded = signal<Record<string, boolean>>({});
  selectedId = signal<string | null>(null);
  apiExpanded = signal<boolean>(false);

  private readonly unsubscribe$ = new Subject<void>();

  constructor(
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
    private readonly uiPortalMenuLinksService: UiPortalMenuLinksService,
    private readonly router: Router,
    private readonly route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.loadTree();
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }

  isExpanded = (id: string) => this.expanded()[id];

  toggleApi(): void {
    this.apiExpanded.update((v) => !v);
  }

  toggle(node: SectionNode): void {
    if (node.type !== 'folder') return;
    this.expanded.update((e) => ({ ...e, [node.id]: !e[node.id] }));
  }

  select(node: SectionNode): void {
    this.selectedId.set(node.id);
    this.navigateToNode(node);
  }

  // Actions
  onAddSectionMenu(): void {
    // TODO: Implement real Section creation when backend is available
    // console.log('Add section - not implemented yet');
  }

  onAddPageMenu(): void {
    // TODO: Implement real Page creation when backend is available
    // console.log('Add page - not implemented yet');
  }

  onAddLinkMenu(): void {
    // console.log('Add link - not implemented yet');
  }

  edit(node: SectionNode): void {
    this.navigateToNode(node);
  }

  delete(node: SectionNode): void {
    this.confirmAndDelete(node);
  }

  trackById = (_: number, n: SectionNode) => n.id;

  private loadTree(): void {
    this.uiPortalMenuLinksService
      .list(1, 9999)
      .pipe(
        tap((response) => {
          const nodes = this.mapLinksToNodes(response.data);
          this.tree.set(nodes);
        }),
        catchError((error) => {
          // console.error('Failed to load tree:', error);
          this.snackBarService.error('Failed to load tree : ' + error);
          this.tree.set([]);
          return of();
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  private mapLinksToNodes(links: PortalMenuLink[]): SectionNode[] {
    return links.map((link) => ({
      id: link.id,
      label: link.name,
      type: 'link' as const,
      data: link,
      link: true,
    }));
  }

  private navigateToNode(node: SectionNode): void {
    this.router.navigate(['../sections', node.id], { relativeTo: this.route });
  }

  private confirmAndDelete(node: SectionNode): void {
    const dialogRef = this.matDialog.open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
      data: {
        title: 'Delete menu link',
        content: 'Please note that once your menu link is deleted, it cannot be restored.',
        confirmButton: 'Delete',
      },
      role: 'alertdialog',
      id: 'confirmDialog',
    });

    dialogRef
      .afterClosed()
      .pipe(
        filter(Boolean),
        switchMap(() => this.uiPortalMenuLinksService.delete(node.id)),
        tap(() => {
          this.snackBarService.success('Menu link deleted successfully');
          this.loadTree();
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error?.message ?? 'Error during deletion');
          return of(false);
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }
}
