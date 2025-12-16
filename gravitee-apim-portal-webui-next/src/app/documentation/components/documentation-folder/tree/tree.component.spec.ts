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

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { TreeComponent } from './tree.component';
import { PortalNavigationItem } from '../../../../../entities/portal-navigation/portal-navigation-item';
import { makeItem, MOCK_ITEMS } from '../../../../../mocks/portal-navigation-item.mocks';
import { DocumentationTreeService, TreeNode } from '../../../services/documentation-tree.service';

describe('TreeComponent', () => {
  const documentationTreeService = new DocumentationTreeService();
  let fixture: ComponentFixture<TreeComponent>;
  let component: TreeComponent;
  const items: PortalNavigationItem[] = MOCK_ITEMS;
  let itemsTree: TreeNode[];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TreeComponent, MatIconTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(TreeComponent);
    component = fixture.componentInstance;

    itemsTree = documentationTreeService.mapItemsToNodes(items);
  });

  it('should build a sorted tree with proper types and parent/child relationships', () => {
    const items = documentationTreeService.mapItemsToNodes([
      // root page with smallest order -> should be first
      makeItem('r1', 'PAGE', 'Root Page 1', 0),
      // folder root
      makeItem('f1', 'FOLDER', 'Folder 1', 1),
      // link root
      makeItem('l1', 'LINK', 'External Link', 3),
      // child page under folder
      makeItem('c1', 'PAGE', 'Child Page', 2, 'f1'),
    ]);

    fixture.componentRef.setInput('tree', items);
    fixture.detectChanges();

    const tree = component.tree();
    expect(Array.isArray(tree)).toBe(true);
    expect(tree.length).toBe(3);

    // roots sorted by order: r1 (0), f1 (1), l1 (3)
    expect(tree[0].id).toBe('r1');
    expect(tree[0].type).toBe('PAGE');
    expect(tree[0].children).toBeUndefined();

    expect(tree[1].id).toBe('f1');
    expect(tree[1].type).toBe('FOLDER');
    expect(tree[1].children?.length).toBe(1);
    expect(tree[1].children?.[0].id).toBe('c1');
    expect(tree[1].children?.[0].type).toBe('PAGE');
    expect(tree[1].children?.[0].children).toBeUndefined();

    expect(tree[2].id).toBe('l1');
    expect(tree[2].type).toBe('LINK');
    expect(tree[2].children).toBeUndefined();
  });

  describe('first page selection', () => {
    it('should autoselect the deepest page', () => {
      const selectSpy = jest.fn();
      component.selectNode.subscribe(selectSpy);

      fixture.componentRef.setInput('tree', itemsTree);
      fixture.detectChanges();

      expect(selectSpy).toHaveBeenCalledWith('p1');
    });

    it('should autoselect intermediary page', () => {
      const selectSpy = jest.fn();
      component.selectNode.subscribe(selectSpy);

      items[1].order = 1;
      items[3].order = 0;

      fixture.componentRef.setInput('tree', documentationTreeService.mapItemsToNodes(items));
      fixture.detectChanges();

      expect(selectSpy).toHaveBeenCalledWith('p2');
    });

    it('should autoselect top level page', () => {
      const selectSpy = jest.fn();
      component.selectNode.subscribe(selectSpy);

      items[0].order = 1;
      items[4].order = 0;

      fixture.componentRef.setInput('tree', documentationTreeService.mapItemsToNodes(items));
      fixture.detectChanges();

      expect(selectSpy).toHaveBeenCalledWith('p3');
    });

    it('should select the page by pageId', () => {
      const selectSpy = jest.fn();
      component.selectNode.subscribe(selectSpy);

      fixture.componentRef.setInput('tree', itemsTree);
      fixture.componentRef.setInput('selectedId', 'p3');
      fixture.detectChanges();

      expect(selectSpy).toHaveBeenCalledWith('p3');
    });
  });

  it('should scroll into view on page load', () => {
    const scrollIntoViewSpy = jest.spyOn(HTMLElement.prototype, 'scrollIntoView').mockImplementation(() => {});

    fixture.componentRef.setInput('tree', itemsTree);
    fixture.componentRef.setInput('selectedId', 'p3');
    fixture.detectChanges();

    expect(scrollIntoViewSpy).toHaveBeenCalledWith({ behavior: 'smooth', block: 'center' });
    scrollIntoViewSpy.mockRestore();
  });
});
