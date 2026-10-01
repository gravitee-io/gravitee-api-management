/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { afterRenderEffect, Component, ElementRef, ErrorHandler, inject, input, output } from '@angular/core';

import { TreeNodeComponent } from './tree-node.component';
import { TreeNode } from '../../../services/tree.service';

@Component({
  selector: 'app-tree-component',
  standalone: true,
  imports: [TreeNodeComponent],
  templateUrl: './tree.component.html',
  styleUrls: ['./tree.component.scss'],
})
export class TreeComponent {
  private readonly element = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly errorHandler = inject(ErrorHandler);

  tree = input.required<TreeNode[]>();
  selectedId = input<string | null>(null);
  expandedContainerIds = input<ReadonlySet<string> | null>(null);
  selectNode = output<string>();

  constructor() {
    afterRenderEffect(onCleanup => {
      // Read as dependencies: both can move the selected row, so both have to re-run the scroll.
      this.selectedId();
      this.expandedContainerIds();

      let canceled = false;
      onCleanup(() => {
        canceled = true;
      });

      // A revealed branch is still animating to its full height, so its row has no final position yet.
      const animations = this.element.nativeElement.getAnimations?.({ subtree: true }) ?? [];
      Promise.allSettled(animations.map(animation => animation.finished))
        .then(() => {
          if (!canceled) {
            this.scrollIntoView();
          }
        })
        .catch((error: unknown) => this.errorHandler.handleError(error));
    });
  }

  onNodeSelected(id: string) {
    this.selectNode.emit(id);
  }

  private scrollIntoView() {
    const selectedItem = this.element.nativeElement.querySelector<HTMLElement>('[role="treeitem"][aria-selected="true"]');
    if (selectedItem && !selectedItem.closest('[inert]')) {
      selectedItem.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }
}
