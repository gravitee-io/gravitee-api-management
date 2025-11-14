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
import { Component, computed, effect, input, output } from '@angular/core';

import { TreeNodeComponent } from './tree-node.component';

import { PortalMenuLink } from '../../../entities/management-api-v2';
import { PortalTreeHelperService } from '../../../services/portal-tree-helper.service';

export type SectionNodeType = 'page' | 'folder' | 'link';

export interface SectionNode {
  id: string;
  label: string;
  type: SectionNodeType;
  data?: PortalMenuLink;
  children?: SectionNode[];
}

@Component({
  selector: 'portal-tree-component',
  standalone: true,
  imports: [TreeNodeComponent],
  templateUrl: './tree.component.html',
  styleUrls: ['./tree.component.scss'],
})
export class TreeComponent {
  links = input<PortalMenuLink[] | null>(null);
  tree = computed(() => {
    const links = this.links();
    return links && Array.isArray(links) ? this.helper.mapLinksToNodes(links) : [];
  });

  selectedId = input<string | null>(null);
  select = output<SectionNode>();

  constructor(private readonly helper: PortalTreeHelperService) {
    effect(() => {
      const currentTree = this.tree();
      const currentSelectedId = this.selectedId();

      if (currentTree.length > 0) {
        const isPageIdProvided = currentSelectedId !== null;
        isPageIdProvided && this.helper.findNode(currentTree, (node) => node.id === currentSelectedId);
        if (!isPageIdProvided) {
          const firstPageNode = this.helper.findNode(currentTree, (node) => node.type === 'page');
          if (firstPageNode) {
            this.select.emit(firstPageNode);
          }
        }
      }
    });
  }
}
