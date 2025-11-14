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
import { ActivatedRoute, Router } from '@angular/router';

import { PortalMenuLink } from '../entities/management-api-v2';

export type SectionNodeType = 'page' | 'folder' | 'link';

export interface SectionNode {
  id: string;
  label: string;
  type: SectionNodeType;
  data?: PortalMenuLink;
  children?: SectionNode[];
}

type ProcessingNode = SectionNode & { __order: number; __parentId: string | null };

@Injectable({ providedIn: 'root' })
export class PortalTreeHelperService {
  constructor(private readonly router: Router) {}

  mapLinksToNodes(links: PortalMenuLink[]): SectionNode[] {
    const nodesById = this.createNodesMap(links);
    const roots = this.connectNodes(nodesById);
    return this.sortAndCleanTree(roots);
  }

  createNodesMap(links: PortalMenuLink[]): Map<string, ProcessingNode> {
    const nodes = new Map<string, ProcessingNode>();
    for (const link of links) {
      const type = this.getNodeType(link);
      nodes.set(
        link.id as string,
        {
          id: link.id as string,
          label: link.name as string,
          type,
          data: link,
          children: type === 'folder' ? [] : undefined,
          __order: (link as any).order ?? 0,
          __parentId: ((link as any).parentId ?? null) as string | null,
        } as ProcessingNode,
      );
    }
    return nodes;
  }

  connectNodes(nodes: Map<string, ProcessingNode>): ProcessingNode[] {
    const roots: ProcessingNode[] = [];
    for (const node of nodes.values()) {
      const parent = node.__parentId ? nodes.get(node.__parentId) : null;
      if (parent) {
        parent.children ??= [];
        parent.children.push(node);
      } else {
        roots.push(node);
      }
    }
    return roots;
  }

  sortAndCleanTree(nodes: ProcessingNode[]): SectionNode[] {
    return nodes
      .sort((a, b) => a.__order - b.__order)
      .map(({ __order, __parentId, ...node }) => ({
        ...node,
        children: node.children ? this.sortAndCleanTree(node.children as ProcessingNode[]) : undefined,
      }));
  }

  getNodeType(link: PortalMenuLink): 'page' | 'folder' | 'link' {
    const type: string = (link as any).type;
    const target = (link as any).target;
    if (target === null || type === 'FOLDER') {
      return 'folder';
    }
    if (['LINK', 'EXTERNAL'].includes(type)) {
      return 'link';
    }
    return 'page';
  }

  findNode(nodes: SectionNode[], predicate: (node: SectionNode) => boolean): SectionNode | null {
    for (const node of nodes) {
      if (predicate(node)) {
        return node;
      }
      if (node.children) {
        const found = this.findNode(node.children, predicate);
        if (found) return found;
      }
    }
    return null;
  }

  navigateToNavId(activatedRoute: ActivatedRoute, id: string): Promise<boolean> {
    return this.router.navigate(['.'], {
      relativeTo: activatedRoute,
      queryParams: { navId: id },
      queryParamsHandling: 'merge',
    });
  }
}
