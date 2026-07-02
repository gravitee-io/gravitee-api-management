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

import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { CdkDragDrop, CdkDragMove, CdkDropList, DragDropModule } from '@angular/cdk/drag-drop';
import { By } from '@angular/platform-browser';

import { FlatTreeComponent, SectionNode } from './flat-tree.component';
import { FlatTreeComponentHarness } from './flat-tree.component.harness';

import { GioTestingModule } from '../../../shared/testing';
import {
  fakePortalNavigationApi,
  fakePortalNavigationFolder,
  fakePortalNavigationLink,
  fakePortalNavigationPage,
  PortalNavigationItem,
} from '../../../entities/management-api-v2';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';

describe('FlatTreeComponent', () => {
  let fixture: ComponentFixture<FlatTreeComponent>;
  let component: FlatTreeComponent;
  let harness: FlatTreeComponentHarness;
  let permissionService: GioPermissionService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FlatTreeComponent, MatIconTestingModule, GioTestingModule, NoopAnimationsModule, DragDropModule],
      providers: [
        {
          provide: GioPermissionService,
          useValue: {
            hasAnyMatching: jest.fn().mockReturnValue(true),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(FlatTreeComponent);
    component = fixture.componentInstance;
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, FlatTreeComponentHarness);
    permissionService = TestBed.inject(GioPermissionService);
  });

  const makeItem = (
    id: string,
    type: PortalNavigationItem['type'],
    title: string,
    order?: number,
    parentId?: string | null,
    published: boolean = true,
  ): PortalNavigationItem => {
    switch (type) {
      case 'FOLDER':
        return fakePortalNavigationFolder({ id, title, order, parentId, published });
      case 'LINK':
        return fakePortalNavigationLink({ id, title, order, parentId, published });
      case 'API':
        return fakePortalNavigationApi({ id, title, order, parentId, published });
      case 'PAGE':
      default:
        return fakePortalNavigationPage({ id, title, order, parentId, published });
    }
  };

  // As items are collapsed by default, expand so nested nodes are rendered and reachable.
  const expandTree = () => {
    component.expandAllNodes();
    fixture.detectChanges();
  };

  it('should build a sorted tree with proper types and parent/child relationships', () => {
    const links = [
      // root page with smallest order -> should be first
      makeItem('r1', 'PAGE', 'Root Page 1', 0),
      // folder root
      makeItem('f1', 'FOLDER', 'Folder 1', 1),
      // link root
      makeItem('l1', 'LINK', 'External Link', 3),
      // child page under folder
      makeItem('c1', 'PAGE', 'Child Page', 2, 'f1'),
    ];

    fixture.componentRef.setInput('links', links);
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

  describe('Indentation after move', () => {
    it('should stamp each node depth as its level', () => {
      const links = [
        makeItem('f1', 'FOLDER', 'Folder 1', 0),
        makeItem('f2', 'FOLDER', 'Folder 2', 0, 'f1'),
        makeItem('p1', 'PAGE', 'Page 1', 0, 'f2'),
      ];
      fixture.componentRef.setInput('links', links);
      fixture.detectChanges();

      const [folder1] = component.tree();
      expect(folder1.level).toBe(0);
      expect(folder1.children?.[0].level).toBe(1);
      expect(folder1.children?.[0].children?.[0].level).toBe(2);
    });

    it('should key trackBy by id and level so a moved node is re-rendered at its new depth', () => {
      // Same node id at two different depths must yield different keys, otherwise MatTree keeps the
      // stale indentation (it only updates the data, not the level, on an identity change).
      const atRoot = { id: 'p1', label: 'Page 1', type: 'PAGE' as const, level: 0 };
      const nested = { ...atRoot, level: 2 };

      expect(component.trackByNode(0, atRoot)).toBe('p1:0:0');
      expect(component.trackByNode(0, nested)).toBe('p1:2:0');
      expect(component.trackByNode(0, atRoot)).not.toBe(component.trackByNode(0, nested));
    });

    it('should change the trackBy key when a folder transitions empty <-> parent', () => {
      const empty = { id: 'f1', label: 'Folder 1', type: 'FOLDER' as const, level: 0, children: [] };
      const withChild = { ...empty, children: [{ id: 'p1', label: 'Page 1', type: 'PAGE' as const, level: 1 }] };

      expect(component.trackByNode(0, empty)).toBe('f1:0:0');
      expect(component.trackByNode(0, withChild)).toBe('f1:0:1');
      expect(component.trackByNode(0, empty)).not.toBe(component.trackByNode(0, withChild));
    });

    it('should keep the same trackBy key when a node is reordered at the same depth', () => {
      // A pure reorder among siblings keeps the depth, so the view is reused (no needless rebuild).
      const before = { id: 'p1', label: 'Page 1', type: 'PAGE' as const, level: 1 };
      const after = { ...before };

      expect(component.trackByNode(0, before)).toBe(component.trackByNode(3, after));
    });
  });

  it('should not auto-select when selectedId exists in tree', async () => {
    const selectSpy = jest.fn();
    component.nodeSelect.subscribe(selectSpy);

    const links = [makeItem('p1', 'PAGE', 'Page 1', 0)];

    fixture.componentRef.setInput('links', links);
    fixture.componentRef.setInput('selectedId', 'p1');
    fixture.detectChanges();

    // When selectedId exists, component should NOT emit default selection
    expect(selectSpy).not.toHaveBeenCalled();
  });

  it('should re-emit delete events from child nodes', () => {
    const links = [makeItem('p1', 'PAGE', 'Page 1', 0)];

    fixture.componentRef.setInput('links', links);
    fixture.detectChanges();

    const actionSpy = jest.fn();
    component.nodeMenuAction.subscribe(actionSpy);

    // Simulate delete event from child node
    component.nodeMenuAction.emit({ action: 'delete', itemType: 'PAGE', node: { id: 'p1', label: 'Page 1', type: 'PAGE' } });

    expect(actionSpy).toHaveBeenCalledTimes(1);
    expect(actionSpy).toHaveBeenCalledWith({ action: 'delete', itemType: 'PAGE', node: { id: 'p1', label: 'Page 1', type: 'PAGE' } });
  });

  it('should display empty state when no links provided', async () => {
    fixture.componentRef.setInput('links', null);
    fixture.detectChanges();

    const isEmpty = await harness.isEmptyStateDisplayed();
    expect(isEmpty).toBe(true);
  });

  it('should display empty state when links array is empty', async () => {
    fixture.componentRef.setInput('links', []);
    fixture.detectChanges();

    const isEmpty = await harness.isEmptyStateDisplayed();
    expect(isEmpty).toBe(true);
  });

  it('should get all item titles', async () => {
    const links = [
      makeItem('p1', 'PAGE', 'Page 1', 0),
      makeItem('f1', 'FOLDER', 'Folder 1', 1),
      makeItem('c1', 'PAGE', 'Child Page', 0, 'f1'),
    ];

    fixture.componentRef.setInput('links', links);
    fixture.detectChanges();

    const titles = await harness.getAllItemTitles();
    expect(titles.length).toBeGreaterThan(0);
    expect(titles).toContain('Page 1');
    expect(titles).toContain('Folder 1');
  });

  it('should select item by title', async () => {
    const selectSpy = jest.fn();
    component.nodeSelect.subscribe(selectSpy);

    const links = [makeItem('p1', 'PAGE', 'Page 1', 0), makeItem('p2', 'PAGE', 'Page 2', 1)];

    fixture.componentRef.setInput('links', links);
    fixture.detectChanges();

    await harness.selectItemByTitle('Page 1');
    fixture.detectChanges();

    expect(selectSpy).toHaveBeenCalledTimes(1);
    expect(selectSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        id: 'p1',
        label: 'Page 1',
        type: 'PAGE',
      }),
    );
  });

  it('should get selected item title', async () => {
    const links = [makeItem('p1', 'PAGE', 'Page 1', 0)];

    fixture.componentRef.setInput('links', links);
    fixture.componentRef.setInput('selectedId', 'p1');
    fixture.detectChanges();

    const selectedTitle = await harness.getSelectedItemTitle();
    expect(selectedTitle).toBe('Page 1');
  });

  it('should return null when no item is selected', async () => {
    const links = [makeItem('p1', 'PAGE', 'Page 1', 0)];

    fixture.componentRef.setInput('links', links);
    fixture.componentRef.setInput('selectedId', null);
    fixture.detectChanges();

    const selectedTitle = await harness.getSelectedItemTitle();
    expect(selectedTitle).toBeNull();
  });

  it('should get selected item type', async () => {
    const links = [makeItem('p1', 'PAGE', 'Page 1', 0)];

    fixture.componentRef.setInput('links', links);
    fixture.componentRef.setInput('selectedId', 'p1');
    fixture.detectChanges();

    const selectedType = await harness.getSelectedItemType();
    // getName() might return the full icon name like "gio:page" or just "page"
    expect(selectedType).toBeTruthy();
    expect(selectedType?.toLowerCase()).toContain('page');
  });

  it('should select edit action by id', async () => {
    const actionSpy = jest.fn();
    component.nodeMenuAction.subscribe(actionSpy);

    const links = [makeItem('p1', 'PAGE', 'Page 1', 0)];

    fixture.componentRef.setInput('links', links);
    fixture.detectChanges();
    await fixture.whenStable();

    await harness.selectEditById('p1');
    await fixture.whenStable();

    expect(actionSpy).toHaveBeenCalledTimes(1);
    expect(actionSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        action: 'edit',
        itemType: 'PAGE',
        node: expect.objectContaining({
          id: 'p1',
        }),
      }),
    );
  });

  it('should select delete action by id', async () => {
    const actionSpy = jest.fn();
    component.nodeMenuAction.subscribe(actionSpy);

    const links = [makeItem('p1', 'PAGE', 'Page 1', 0)];

    fixture.componentRef.setInput('links', links);
    fixture.detectChanges();
    await fixture.whenStable();

    await harness.selectDeleteById('p1');
    await fixture.whenStable();

    expect(actionSpy).toHaveBeenCalledTimes(1);
    expect(actionSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        action: 'delete',
        itemType: 'PAGE',
        node: expect.objectContaining({
          id: 'p1',
        }),
      }),
    );
  });

  it('should select publish action by id', async () => {
    const actionSpy = jest.fn();
    component.nodeMenuAction.subscribe(actionSpy);

    const links = [
      makeItem('parent-id', 'FOLDER', 'Parent Folder', 0, null, true),
      makeItem('p1', 'PAGE', 'Page 1', 0, 'parent-id', false),
    ];

    fixture.componentRef.setInput('links', links);
    fixture.detectChanges();
    await fixture.whenStable();

    expandTree();

    await harness.selectPublishById('p1');
    await fixture.whenStable();

    expect(actionSpy).toHaveBeenCalledTimes(1);
    expect(actionSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        action: 'publish',
        itemType: 'PAGE',
        node: expect.objectContaining({
          id: 'p1',
        }),
      }),
    );
  });

  it('should select unpublish action by id', async () => {
    const actionSpy = jest.fn();
    component.nodeMenuAction.subscribe(actionSpy);

    const links = [makeItem('p1', 'PAGE', 'Page 1', 0, 'parent-id', true)];

    fixture.componentRef.setInput('links', links);
    fixture.detectChanges();
    await fixture.whenStable();

    await harness.selectUnpublishById('p1');
    await fixture.whenStable();

    expect(actionSpy).toHaveBeenCalledTimes(1);
    expect(actionSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        action: 'unpublish',
        itemType: 'PAGE',
        node: expect.objectContaining({
          id: 'p1',
        }),
      }),
    );
  });

  describe('publish disabled state', () => {
    const findNode = (id: string, nodes: SectionNode[] = component.tree()): SectionNode | undefined => {
      for (const node of nodes) {
        if (node.id === id) {
          return node;
        }

        if (node.children) {
          const foundChild = findNode(id, node.children);
          if (foundChild) {
            return foundChild;
          }
        }
      }

      return undefined;
    };

    it('should not disable publish for root node', () => {
      const links = [makeItem('root-page', 'PAGE', 'Root Page', 0)];
      fixture.componentRef.setInput('links', links);
      fixture.detectChanges();

      const rootNode = findNode('root-page');
      expect(component.isPublishDisabled(rootNode)).toBe(false);
      expect(component.getPublishDisabledTooltip(rootNode)).toBe('');
    });

    it('should disable publish when parent is unpublished', () => {
      const links = [
        makeItem('folder-1', 'FOLDER', 'Folder 1', 0, null, false),
        makeItem('child-page-1', 'PAGE', 'Child Page 1', 0, 'folder-1', false),
      ];

      fixture.componentRef.setInput('links', links);
      fixture.detectChanges();

      const childNode = findNode('child-page-1');
      expect(component.isPublishDisabled(childNode)).toBe(true);
      expect(component.getPublishDisabledTooltip(childNode)).toBe('A navigation item cannot be published within an unpublished folder');
    });

    it('should not disable publish when parent is published', () => {
      const links = [
        makeItem('folder-1', 'FOLDER', 'Folder 1', 0, null, true),
        makeItem('child-page-1', 'PAGE', 'Child Page 1', 0, 'folder-1', false),
      ];

      fixture.componentRef.setInput('links', links);
      fixture.detectChanges();

      const childNode = findNode('child-page-1');
      expect(component.isPublishDisabled(childNode)).toBe(false);
      expect(component.getPublishDisabledTooltip(childNode)).toBe('');
    });

    it('should refresh parent lookup cache when links signal changes', () => {
      fixture.componentRef.setInput('links', [
        makeItem('folder-1', 'FOLDER', 'Folder 1', 0, null, false),
        makeItem('child-page-1', 'PAGE', 'Child Page 1', 0, 'folder-1', false),
      ]);
      fixture.detectChanges();

      let childNode = findNode('child-page-1');
      expect(component.isPublishDisabled(childNode)).toBe(true);

      fixture.componentRef.setInput('links', [
        makeItem('folder-1', 'FOLDER', 'Folder 1', 0, null, true),
        makeItem('child-page-1', 'PAGE', 'Child Page 1', 0, 'folder-1', false),
      ]);
      fixture.detectChanges();

      childNode = findNode('child-page-1');
      expect(component.isPublishDisabled(childNode)).toBe(false);
    });

    it('should disable publish when parent cannot be resolved', () => {
      const links = [makeItem('orphan-page', 'PAGE', 'Orphan Page', 0, 'missing-parent', false)];

      fixture.componentRef.setInput('links', links);
      fixture.detectChanges();

      const orphanNode = findNode('orphan-page');
      expect(component.isPublishDisabled(orphanNode)).toBe(true);
      expect(component.getPublishDisabledTooltip(orphanNode)).toBe(
        'A navigation item cannot be published because its parent is unavailable',
      );
    });

    it('should disable publish menu item when parent is unpublished', async () => {
      const links = [
        makeItem('folder-1', 'FOLDER', 'Folder 1', 0, null, false),
        makeItem('child-page-1', 'PAGE', 'Child Page 1', 0, 'folder-1', false),
      ];

      fixture.componentRef.setInput('links', links);
      fixture.detectChanges();
      await fixture.whenStable();

      expandTree();

      const publishButton = await harness.openPublishMenuAndGetItem('child-page-1');
      expect(publishButton).toBeTruthy();
      expect(await publishButton.isDisabled()).toBe(true);
    });

    it('should not emit publish action when clicking disabled publish menu item', async () => {
      const actionSpy = jest.fn();
      component.nodeMenuAction.subscribe(actionSpy);

      const links = [
        makeItem('folder-1', 'FOLDER', 'Folder 1', 0, null, false),
        makeItem('child-page-1', 'PAGE', 'Child Page 1', 0, 'folder-1', false),
      ];

      fixture.componentRef.setInput('links', links);
      fixture.detectChanges();
      await fixture.whenStable();

      expandTree();

      const publishButton = await harness.openPublishMenuAndGetItem('child-page-1');
      expect(publishButton).toBeTruthy();
      expect(await publishButton.isDisabled()).toBe(true);

      const disabledPublishButton = document.querySelector('[data-testid="publish-node-button"]') as HTMLButtonElement;
      expect(disabledPublishButton).toBeTruthy();

      disabledPublishButton.click();
      expect(actionSpy).not.toHaveBeenCalled();
    });

    it('should keep publish menu item enabled when parent is published', async () => {
      const links = [
        makeItem('folder-1', 'FOLDER', 'Folder 1', 0, null, true),
        makeItem('child-page-1', 'PAGE', 'Child Page 1', 0, 'folder-1', false),
      ];

      fixture.componentRef.setInput('links', links);
      fixture.detectChanges();
      await fixture.whenStable();

      expandTree();

      const publishButton = await harness.openPublishMenuAndGetItem('child-page-1');
      expect(publishButton).toBeTruthy();
      expect(await publishButton.isDisabled()).toBe(false);
    });
  });

  it('should handle nested folder structure', async () => {
    const links = [
      makeItem('f1', 'FOLDER', 'Folder 1', 0),
      makeItem('f2', 'FOLDER', 'Folder 2', 0, 'f1'),
      makeItem('p1', 'PAGE', 'Page 1', 0, 'f2'),
    ];

    fixture.componentRef.setInput('links', links);
    fixture.detectChanges();
    await fixture.whenStable();
    expandTree();

    const titles = await harness.getAllItemTitles();
    expect(titles).toContain('Folder 1');
    expect(titles).toContain('Folder 2');
    expect(titles).toContain('Page 1');
  });

  it('should handle nested api structure', async () => {
    const links = [
      makeItem('a1', 'API', 'API 1', 0),
      makeItem('f2', 'FOLDER', 'Folder 2', 0, 'f1'),
      makeItem('p1', 'PAGE', 'Page 1', 0, 'f1'),
    ];

    fixture.componentRef.setInput('links', links);
    fixture.detectChanges();
    const titles = await harness.getAllItemTitles();
    expect(titles).toContain('API 1');
    expect(titles).toContain('Folder 2');
    expect(titles).toContain('Page 1');
  });

  it('should handle different node types', async () => {
    const links = [makeItem('p1', 'PAGE', 'Page 1', 0), makeItem('f1', 'FOLDER', 'Folder 1', 1), makeItem('l1', 'LINK', 'Link 1', 2)];

    fixture.componentRef.setInput('links', links);
    fixture.detectChanges();

    const titles = await harness.getAllItemTitles();
    expect(titles.length).toBe(3);
    expect(titles).toContain('Page 1');
    expect(titles).toContain('Folder 1');
    expect(titles).toContain('Link 1');
  });

  describe('Auto-scroll', () => {
    const getDropList = (): CdkDropList => {
      fixture.componentRef.setInput('links', [makeItem('p1', 'PAGE', 'Page 1', 0)]);
      fixture.detectChanges();
      return fixture.debugElement.query(By.directive(CdkDropList)).injector.get(CdkDropList);
    };

    it('should use an auto-scroll step of 10 while dragging', () => {
      expect(getDropList().autoScrollStep).toBe(10);
    });
  });

  describe('Drag & Drop Scenarios', () => {
    let nodeMovedSpy: jest.SpyInstance;

    beforeEach(() => {
      nodeMovedSpy = jest.spyOn(component.nodeMoved, 'emit');
    });

    const findNode = (id: string, nodes = component.tree()): any => {
      for (const node of nodes) {
        if (node.id === id) return node;
        if (node.children) {
          const found = findNode(id, node.children);
          if (found) return found;
        }
      }
    };

    const dropWithIntent = (dragId: string, intent: { targetId: string; position: 'before' | 'inside' | 'after' }) => {
      component.dropIntent.set(intent);
      component.onDrop(createDropEvent(findNode(dragId)));
    };

    const dropScenarios = [
      {
        description: 'should move an item into an empty folder as its first child',
        links: [makeItem('p1', 'PAGE', 'Page 1', 0), makeItem('f1', 'FOLDER', 'Folder 1', 1)],
        dragId: 'p1',
        intent: { targetId: 'f1', position: 'inside' as const },
        expected: { newParentId: 'f1', newOrder: 0 },
      },
      {
        description: 'should append an item to a non-empty folder',
        links: [
          makeItem('p1', 'PAGE', 'Page 1', 0),
          makeItem('f1', 'FOLDER', 'Folder 1', 1),
          makeItem('c1', 'PAGE', 'Child 1', 0, 'f1'),
          makeItem('c2', 'PAGE', 'Child 2', 1, 'f1'),
        ],
        dragId: 'p1',
        intent: { targetId: 'f1', position: 'inside' as const },
        expected: { newParentId: 'f1', newOrder: 2 },
      },
      {
        description: 'should drop an item before a root sibling',
        links: [makeItem('p1', 'PAGE', '1', 0), makeItem('p2', 'PAGE', '2', 1)],
        dragId: 'p2',
        intent: { targetId: 'p1', position: 'before' as const },
        expected: { newParentId: null, newOrder: 0 },
      },
      {
        description: 'should drop an item after a root sibling',
        links: [makeItem('p1', 'PAGE', '1', 0), makeItem('p2', 'PAGE', '2', 1)],
        dragId: 'p1',
        intent: { targetId: 'p2', position: 'after' as const },
        expected: { newParentId: null, newOrder: 2 },
      },
      {
        description: 'should reparent an item before a child of another folder',
        links: [makeItem('f1', 'FOLDER', '1', 0), makeItem('c1', 'PAGE', '1.1', 0, 'f1'), makeItem('p2', 'PAGE', '2', 1)],
        dragId: 'p2',
        intent: { targetId: 'c1', position: 'before' as const },
        expected: { newParentId: 'f1', newOrder: 0 },
      },
      {
        description: 'should move a nested item back to the root after a root folder',
        links: [makeItem('f1', 'FOLDER', '1', 0), makeItem('c1', 'PAGE', '1.1', 0, 'f1'), makeItem('p2', 'PAGE', '2', 1)],
        dragId: 'c1',
        intent: { targetId: 'f1', position: 'after' as const },
        expected: { newParentId: null, newOrder: 1 },
      },
    ];

    it.each(dropScenarios)('$description', ({ links, dragId, intent, expected }) => {
      fixture.componentRef.setInput('links', links);
      fixture.detectChanges();

      dropWithIntent(dragId, intent);

      expect(nodeMovedSpy).toHaveBeenCalledWith({
        node: findNode(dragId),
        newParentId: expected.newParentId,
        newOrder: expected.newOrder,
      });
    });

    it('should expand the target folder on an inside drop so the moved item is visible', () => {
      const links = [makeItem('p1', 'PAGE', 'Page 1', 0), makeItem('f1', 'FOLDER', 'Folder 1', 1)];
      fixture.componentRef.setInput('links', links);
      fixture.detectChanges();
      const expandSpy = jest.spyOn(component.treeBase()!, 'expand');

      dropWithIntent('p1', { targetId: 'f1', position: 'inside' });

      expect(expandSpy).toHaveBeenCalledWith(findNode('f1'));
    });

    it('should not emit when the pointer resolved to no target', () => {
      const links = [makeItem('p1', 'PAGE', 'Page 1', 0)];
      fixture.componentRef.setInput('links', links);
      fixture.detectChanges();

      component.dropIntent.set(null);
      component.onDrop(createDropEvent(findNode('p1')));

      expect(nodeMovedSpy).not.toHaveBeenCalled();
    });

    it('should cancel the move when released outside the tree', () => {
      const links = [makeItem('p1', 'PAGE', '1', 0), makeItem('p2', 'PAGE', '2', 1)];
      fixture.componentRef.setInput('links', links);
      fixture.detectChanges();

      component.dropIntent.set({ targetId: 'p2', position: 'after' });
      component.onDrop(createDropEvent(findNode('p1'), false));

      expect(nodeMovedSpy).not.toHaveBeenCalled();
    });

    describe('drop position resolution', () => {
      const setup = (links: PortalNavigationItem[]) => {
        fixture.componentRef.setInput('links', links);
        fixture.detectChanges();
      };

      it('should resolve a container central band to "inside" and its edges to before/after', () => {
        setup([makeItem('f1', 'FOLDER', 'Folder 1', 0)]);

        expect(component['resolveDropPosition'](findNode('f1'), 0.5)).toBe('inside');
        expect(component['resolveDropPosition'](findNode('f1'), 0.1)).toBe('before');
        expect(component['resolveDropPosition'](findNode('f1'), 0.9)).toBe('after');
      });

      it('should resolve a leaf row to before/after only, never inside', () => {
        setup([makeItem('p1', 'PAGE', 'Page 1', 0)]);

        expect(component['resolveDropPosition'](findNode('p1'), 0.4)).toBe('before');
        expect(component['resolveDropPosition'](findNode('p1'), 0.6)).toBe('after');
      });

      it('should reject the dragged node itself and its own descendants as targets', () => {
        setup([makeItem('f1', 'FOLDER', 'Folder 1', 0), makeItem('f2', 'FOLDER', 'Folder 2', 0, 'f1')]);

        expect(component['canReorderRelativeTo'](findNode('f1'), findNode('f1'))).toBe(false);
        expect(component['canReorderRelativeTo'](findNode('f2'), findNode('f1'))).toBe(false);
        expect(component['canReorderRelativeTo'](findNode('f1'), findNode('f2'))).toBe(true);
      });
    });

    describe('onDragMoved hit-testing', () => {
      // A row element whose closest('mat-tree-node') is itself, exposing the node id and a fixed rect.
      const fakeRow = (nodeId: string | null, rect: Partial<DOMRect> = {}): HTMLElement => {
        const el = {
          getAttribute: (name: string) => (name === 'data-node-id' ? nodeId : null),
          getBoundingClientRect: () => ({ top: 0, height: 40, ...rect }) as DOMRect,
        } as unknown as HTMLElement;
        (el as any).closest = (selector: string) => (selector === 'mat-tree-node' ? el : null);
        return el;
      };

      const move = (clientY: number, draggedId: string): CdkDragMove<SectionNode> =>
        ({ event: new MouseEvent('mousemove', { clientX: 0, clientY }), source: { data: findNode(draggedId) } }) as any;

      beforeEach(() => {
        // jsdom doesn't implement elementFromPoint, so define it before the tests can spy on it.
        (document as any).elementFromPoint = () => null;
      });

      afterEach(() => {
        jest.restoreAllMocks();
        delete (document as any).elementFromPoint;
      });

      it('should set an "inside" intent when hovering the centre of a folder row', () => {
        fixture.componentRef.setInput('links', [makeItem('p1', 'PAGE', 'Page 1', 0), makeItem('f1', 'FOLDER', 'Folder 1', 1)]);
        fixture.detectChanges();
        jest.spyOn(document, 'elementFromPoint').mockReturnValue(fakeRow('f1'));

        component.onDragMoved(move(20, 'p1'));

        expect(component.dropIntent()).toEqual({ targetId: 'f1', position: 'inside' });
      });

      it('should set a "before" intent near the top edge of a folder row', () => {
        fixture.componentRef.setInput('links', [makeItem('p1', 'PAGE', 'Page 1', 0), makeItem('f1', 'FOLDER', 'Folder 1', 1)]);
        fixture.detectChanges();
        jest.spyOn(document, 'elementFromPoint').mockReturnValue(fakeRow('f1'));

        component.onDragMoved(move(4, 'p1'));

        expect(component.dropIntent()).toEqual({ targetId: 'f1', position: 'before' });
      });

      it('should clear the intent when the pointer is over no tree row', () => {
        fixture.componentRef.setInput('links', [makeItem('p1', 'PAGE', 'Page 1', 0), makeItem('f1', 'FOLDER', 'Folder 1', 1)]);
        fixture.detectChanges();
        component.dropIntent.set({ targetId: 'f1', position: 'inside' });
        jest.spyOn(document, 'elementFromPoint').mockReturnValue(null);

        component.onDragMoved(move(20, 'p1'));

        expect(component.dropIntent()).toBeNull();
      });

      it('should clear the intent when the resolved target is the dragged node itself', () => {
        fixture.componentRef.setInput('links', [makeItem('f1', 'FOLDER', 'Folder 1', 0)]);
        fixture.detectChanges();
        component.dropIntent.set({ targetId: 'f1', position: 'inside' });
        jest.spyOn(document, 'elementFromPoint').mockReturnValue(fakeRow('f1'));

        component.onDragMoved(move(20, 'f1'));

        expect(component.dropIntent()).toBeNull();
      });
    });

    it('should handle drag start and hide descendants', fakeAsync(async () => {
      const links = [
        makeItem('p1', 'PAGE', 'Page 1', 0),
        makeItem('f1', 'FOLDER', 'Folder 1', 1),
        makeItem('sp1', 'PAGE', 'Sub Page 1', 0, 'f1'),
      ];
      fixture.componentRef.setInput('links', links);
      fixture.detectChanges();
      tick();

      expandTree();

      const folderNode = findNode('f1');
      component.onDragStarted({ source: { data: folderNode } } as any);
      tick();
      fixture.detectChanges();

      const treeTitles = await harness.getAllItemTitles();
      expect(treeTitles).toEqual(['Page 1', 'Folder 1']);
    }));
  });

  describe('Permissions', () => {
    const setupPermissions = (permissions: string[]) => {
      (permissionService.hasAnyMatching as jest.Mock).mockImplementation((requestedPermissions: string[]) => {
        return requestedPermissions.some(p => permissions.includes(p));
      });
    };

    it('should show more actions button for folder if user has create permission', async () => {
      setupPermissions(['environment-documentation-c']);
      fixture = TestBed.createComponent(FlatTreeComponent);
      component = fixture.componentInstance;
      harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, FlatTreeComponentHarness);

      const links = [makeItem('f1', 'FOLDER', 'Folder 1', 0)];
      fixture.componentRef.setInput('links', links);

      fixture.detectChanges();
      await fixture.whenStable();

      const moreActionsButton = await harness['getMoreActionsButtonById']('f1')();
      expect(moreActionsButton).toBeTruthy();
    });

    it('should NOT show more actions button for page if user ONLY has create permission', async () => {
      setupPermissions(['environment-documentation-c']);
      fixture = TestBed.createComponent(FlatTreeComponent);
      component = fixture.componentInstance;
      harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, FlatTreeComponentHarness);

      const links = [makeItem('p1', 'PAGE', 'Page 1', 0)];
      fixture.componentRef.setInput('links', links);

      fixture.detectChanges();
      await fixture.whenStable();

      const moreActionsButton = await harness['getMoreActionsButtonById']('p1')();
      expect(moreActionsButton).toBeNull();
    });

    it('should show more actions button for page if user has update permission', async () => {
      setupPermissions(['environment-documentation-u']);
      fixture = TestBed.createComponent(FlatTreeComponent);
      component = fixture.componentInstance;
      harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, FlatTreeComponentHarness);

      const links = [makeItem('p2', 'PAGE', 'Page 2', 0)];
      fixture.componentRef.setInput('links', links);

      fixture.detectChanges();
      await fixture.whenStable();

      const moreActionsButton = await harness['getMoreActionsButtonById']('p2')();
      expect(moreActionsButton).toBeTruthy();
    });

    it('should show only "Add" options if user has only create permission', async () => {
      setupPermissions(['environment-documentation-c']);
      fixture = TestBed.createComponent(FlatTreeComponent);
      component = fixture.componentInstance;
      harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, FlatTreeComponentHarness);

      const links = [makeItem('f2', 'FOLDER', 'Folder 2', 0)];
      fixture.componentRef.setInput('links', links);

      fixture.detectChanges();
      await fixture.whenStable();

      const moreActionsButton = await harness['getMoreActionsButtonById']('f2')();
      await moreActionsButton.click();

      const addPageButton = await harness.getMenuItemByText('Add Page');
      const addFolderButton = await harness.getMenuItemByText('Add Folder');
      const addLinkButton = await harness.getMenuItemByText('Add Link');
      const editButton = await harness.getMenuItemByTestId('edit-node-button');
      const deleteButton = await harness.getMenuItemByTestId('delete-node-button');

      expect(addPageButton).toBeTruthy();
      expect(addFolderButton).toBeTruthy();
      expect(addLinkButton).toBeTruthy();
      expect(editButton).toBeNull();
      expect(deleteButton).toBeNull();
    });

    const testCases = [
      {
        description: 'should NOT show divider if user has only create permission',
        permissions: ['environment-documentation-c'],
        expected: false,
      },
      {
        description: 'should show divider if user has create and update permission',
        permissions: ['environment-documentation-c', 'environment-documentation-u'],
        expected: true,
      },
      {
        description: 'should show divider if user has create and delete permission',
        permissions: ['environment-documentation-c', 'environment-documentation-d'],
        expected: true,
      },
    ];

    testCases.forEach(({ description, permissions, expected }) => {
      it(description, async () => {
        setupPermissions(permissions);
        fixture = TestBed.createComponent(FlatTreeComponent);
        component = fixture.componentInstance;
        harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, FlatTreeComponentHarness);

        const links = [makeItem('f2', 'FOLDER', 'Folder 2', 0)];
        fixture.componentRef.setInput('links', links);

        fixture.detectChanges();
        await fixture.whenStable();

        const moreActionsButton = await harness['getMoreActionsButtonById']('f2')();
        await moreActionsButton.click();

        expect(await harness.hasDivider()).toBe(expected);
      });
    });

    it('should show only "Edit" and publishing options if user has only update permission', async () => {
      setupPermissions(['environment-documentation-u']);
      fixture = TestBed.createComponent(FlatTreeComponent);
      component = fixture.componentInstance;
      harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, FlatTreeComponentHarness);

      const links = [makeItem('p1', 'PAGE', 'Page 1', 0)];
      fixture.componentRef.setInput('links', links);

      fixture.detectChanges();
      await fixture.whenStable();

      const moreActionsButton = await harness['getMoreActionsButtonById']('p1')();
      await moreActionsButton.click();

      const editButton = await harness.getMenuItemByTestId('edit-node-button');
      const unpublishButton = await harness.getMenuItemByTestId('unpublish-node-button');
      const deleteButton = await harness.getMenuItemByTestId('delete-node-button');

      expect(editButton).toBeTruthy();
      expect(unpublishButton).toBeTruthy();
      expect(deleteButton).toBeNull();
    });

    it('should show only "Delete" option if user has only delete permission', async () => {
      setupPermissions(['environment-documentation-d']);
      fixture = TestBed.createComponent(FlatTreeComponent);
      component = fixture.componentInstance;
      harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, FlatTreeComponentHarness);

      const links = [makeItem('p1', 'PAGE', 'Page 1', 0)];
      fixture.componentRef.setInput('links', links);

      fixture.detectChanges();
      await fixture.whenStable();

      const moreActionsButton = await harness['getMoreActionsButtonById']('p1')();
      await moreActionsButton.click();

      const editButton = await harness.getMenuItemByTestId('edit-node-button');
      const deleteButton = await harness.getMenuItemByTestId('delete-node-button');

      expect(editButton).toBeNull();
      expect(deleteButton).toBeTruthy();
    });

    it('should enable "Delete" option for folder with children (cascade delete supported)', async () => {
      setupPermissions(['environment-documentation-d']);
      fixture = TestBed.createComponent(FlatTreeComponent);
      component = fixture.componentInstance;
      harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, FlatTreeComponentHarness);

      const links = [makeItem('f1', 'FOLDER', 'Folder 1', 0), makeItem('p1', 'PAGE', 'Child Page', 0, 'f1')];
      fixture.componentRef.setInput('links', links);

      fixture.detectChanges();
      await fixture.whenStable();

      const moreActionsButton = await harness['getMoreActionsButtonById']('f1')();
      await moreActionsButton.click();

      const deleteButton = await harness.getMenuItemByTestId('delete-node-button');

      expect(deleteButton).toBeTruthy();
      expect(await deleteButton!.isDisabled()).toBe(false);
    });
  });

  describe('Right-click context menu', () => {
    const setupPermissions = (permissions: string[]) => {
      (permissionService.hasAnyMatching as jest.Mock).mockImplementation((requestedPermissions: string[]) => {
        return requestedPermissions.some(p => permissions.includes(p));
      });
    };

    it('should preventDefault, update the active node, and position the anchor on right-click when actions are available', () => {
      const links = [makeItem('p1', 'PAGE', 'Page 1', 0)];
      fixture.componentRef.setInput('links', links);
      fixture.detectChanges();

      const node = component.tree()[0] as any;
      const event = new MouseEvent('contextmenu', { clientX: 123, clientY: 456 });
      const preventDefaultSpy = jest.spyOn(event, 'preventDefault');

      component.onContextMenu(event, node);

      expect(preventDefaultSpy).toHaveBeenCalledTimes(1);
      expect(component.contextMenuNode()?.id).toBe('p1');
      const anchor = fixture.nativeElement.querySelector('.context-menu-anchor') as HTMLElement;
      expect(anchor.style.left).toBe('123px');
      expect(anchor.style.top).toBe('456px');
    });

    it('should call openMenu on the trigger when actions are available', () => {
      const links = [makeItem('p1', 'PAGE', 'Page 1', 0)];
      fixture.componentRef.setInput('links', links);
      fixture.detectChanges();

      const trigger = component.contextMenuTrigger();
      expect(trigger).toBeTruthy();
      const openMenuSpy = jest.spyOn(trigger!, 'openMenu');

      const node = component.tree()[0] as any;
      component.onContextMenu(new MouseEvent('contextmenu', { clientX: 1, clientY: 2 }), node);

      expect(openMenuSpy).toHaveBeenCalledTimes(1);
    });

    it('should not open the custom one when the user has no actions on the node', () => {
      setupPermissions([]);
      fixture = TestBed.createComponent(FlatTreeComponent);
      component = fixture.componentInstance;

      const links = [makeItem('p1', 'PAGE', 'Page 1', 0)];
      fixture.componentRef.setInput('links', links);
      fixture.detectChanges();

      const trigger = component.contextMenuTrigger();
      const openMenuSpy = trigger ? jest.spyOn(trigger, 'openMenu') : null;

      const node = component.tree()[0] as any;
      const event = new MouseEvent('contextmenu', { clientX: 10, clientY: 20 });
      const preventDefaultSpy = jest.spyOn(event, 'preventDefault');

      component.onContextMenu(event, node);

      expect(preventDefaultSpy).toHaveBeenCalledTimes(1);
      expect(component.contextMenuNode()).toBeNull();
      expect(openMenuSpy).not.toHaveBeenCalled();
    });

    it('should be wired to the (contextmenu) event on tree rows', async () => {
      const links = [makeItem('p1', 'PAGE', 'Page 1', 0)];
      fixture.componentRef.setInput('links', links);
      fixture.detectChanges();
      await fixture.whenStable();

      const onContextMenuSpy = jest.spyOn(component, 'onContextMenu');
      const row = fixture.nativeElement.querySelector('mat-tree-node.tree__row') as HTMLElement;
      expect(row).toBeTruthy();

      row.dispatchEvent(new MouseEvent('contextmenu', { bubbles: true, clientX: 5, clientY: 6 }));

      expect(onContextMenuSpy).toHaveBeenCalledTimes(1);
    });
  });

  describe('collapse / expand all', () => {
    const nestedLinks = [
      makeItem('f1', 'FOLDER', 'Folder 1', 0),
      makeItem('f2', 'FOLDER', 'Folder 2', 0, 'f1'),
      makeItem('p1', 'PAGE', 'Page 1', 0, 'f2'),
    ];

    it('should collapse all items by default on initial load', async () => {
      fixture.componentRef.setInput('links', nestedLinks);
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();

      const titles = await harness.getAllItemTitles();
      expect(titles).toEqual(['Folder 1']);
      expect(component.hasExpandedNode()).toBe(false);
      expect(component.hasExpandableNode()).toBe(true);
    });

    it('should expand ancestors of the selected nested item', async () => {
      fixture.componentRef.setInput('links', nestedLinks);
      fixture.componentRef.setInput('selectedId', 'p1');
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();

      expect(await harness.getAllItemTitles()).toEqual(['Folder 1', 'Folder 2', 'Page 1']);
      expect(component.hasExpandedNode()).toBe(true);
    });

    it('should not reveal the selected nested item again after a data refresh when it was manually collapsed', async () => {
      fixture.componentRef.setInput('links', nestedLinks);
      fixture.componentRef.setInput('selectedId', 'p1');
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();

      expect(await harness.getAllItemTitles()).toEqual(['Folder 1', 'Folder 2', 'Page 1']);

      component.collapseAllNodes();
      fixture.detectChanges();

      expect(await harness.getAllItemTitles()).toEqual(['Folder 1']);
      expect(component.hasExpandedNode()).toBe(false);

      fixture.componentRef.setInput(
        'links',
        nestedLinks.map(link => ({ ...link })),
      );
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();

      expect(await harness.getAllItemTitles()).toEqual(['Folder 1']);
      expect(component.hasExpandedNode()).toBe(false);
    });

    it('should keep unrelated branches collapsed when revealing the selected nested item', async () => {
      const links = [
        makeItem('f1', 'FOLDER', 'Folder 1', 0),
        makeItem('p1', 'PAGE', 'Page 1', 0, 'f1'),
        makeItem('f2', 'FOLDER', 'Folder 2', 1),
        makeItem('p2', 'PAGE', 'Page 2', 0, 'f2'),
      ];

      fixture.componentRef.setInput('links', links);
      fixture.componentRef.setInput('selectedId', 'p2');
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();

      expect(await harness.getAllItemTitles()).toEqual(['Folder 1', 'Folder 2', 'Page 2']);
      expect(component.hasExpandedNode()).toBe(true);
    });

    it('should keep the tree collapsed when the selected item is a root item', async () => {
      fixture.componentRef.setInput('links', nestedLinks);
      fixture.componentRef.setInput('selectedId', 'f1');
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();

      expect(await harness.getAllItemTitles()).toEqual(['Folder 1']);
      expect(component.hasExpandedNode()).toBe(false);
    });

    it('should expand all items when expandAllNodes is called', async () => {
      fixture.componentRef.setInput('links', nestedLinks);
      fixture.detectChanges();
      await fixture.whenStable();

      component.expandAllNodes();
      fixture.detectChanges();

      const titles = await harness.getAllItemTitles();
      expect(titles).toContain('Folder 1');
      expect(titles).toContain('Folder 2');
      expect(titles).toContain('Page 1');
      expect(component.hasExpandedNode()).toBe(true);
    });

    it('should collapse all items when collapseAllNodes is called', async () => {
      fixture.componentRef.setInput('links', nestedLinks);
      fixture.detectChanges();
      await fixture.whenStable();

      component.expandAllNodes();
      fixture.detectChanges();
      expect(component.hasExpandedNode()).toBe(true);

      component.collapseAllNodes();
      fixture.detectChanges();

      const titles = await harness.getAllItemTitles();
      expect(titles).toEqual(['Folder 1']);
      expect(component.hasExpandedNode()).toBe(false);
    });

    it('should report no expandable node when the tree only contains leaf items', async () => {
      const leafLinks = [makeItem('p1', 'PAGE', 'Page 1', 0), makeItem('l1', 'LINK', 'Link 1', 1)];
      fixture.componentRef.setInput('links', leafLinks);
      fixture.detectChanges();
      await fixture.whenStable();

      expect(component.hasExpandableNode()).toBe(false);
      expect(component.hasExpandedNode()).toBe(false);
    });

    it('should sync the expansion state after a single node toggle via the DOM', async () => {
      fixture.componentRef.setInput('links', nestedLinks);
      fixture.detectChanges();
      await fixture.whenStable();
      expect(component.hasExpandedNode()).toBe(false);

      const folderNode = await harness.getNodeHarnessByTitle('Folder 1');
      await folderNode.toggle();

      expect(component.hasExpandedNode()).toBe(true);
    });

    it('should resync the expansion state when the tree data changes', fakeAsync(() => {
      const remainingFolder = [makeItem('f2', 'FOLDER', 'Folder 2', 1), makeItem('c2', 'PAGE', 'Child 2', 0, 'f2')];
      const links = [makeItem('f1', 'FOLDER', 'Folder 1', 0), makeItem('c1', 'PAGE', 'Child 1', 0, 'f1'), ...remainingFolder];
      fixture.componentRef.setInput('links', links);
      fixture.detectChanges();
      tick();

      component.treeBase()!.expand(component.tree()[0]);
      component.onNodeToggle();
      tick();
      expect(component.hasExpandedNode()).toBe(true);

      // The expanded folder is removed (e.g. deleted) and the list refreshes via [links].
      fixture.componentRef.setInput('links', remainingFolder);
      fixture.detectChanges();
      tick();

      expect(component.hasExpandedNode()).toBe(false);
    }));
  });

  describe('auto-scroll to selected node', () => {
    let scrolledNodeIds: string[];
    let originalScrollIntoView: typeof Element.prototype.scrollIntoView;

    beforeEach(() => {
      scrolledNodeIds = [];
      originalScrollIntoView = Element.prototype.scrollIntoView;
      Element.prototype.scrollIntoView = jest.fn(function (this: HTMLElement) {
        scrolledNodeIds.push(this.getAttribute('data-node-id') ?? '');
      });
    });

    afterEach(() => {
      Element.prototype.scrollIntoView = originalScrollIntoView;
    });

    it('should scroll the newly selected root item into view', async () => {
      const links = [makeItem('p1', 'PAGE', 'Page 1', 0), makeItem('p2', 'PAGE', 'Page 2', 1)];
      fixture.componentRef.setInput('links', links);
      fixture.detectChanges();
      await fixture.whenStable();

      fixture.componentRef.setInput('selectedId', 'p2');
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();
      await fixture.whenStable();

      expect(scrolledNodeIds).toContain('p2');
    });

    it('should scroll a nested item into view once its ancestors are expanded', async () => {
      const nestedLinks = [
        makeItem('f1', 'FOLDER', 'Folder 1', 0),
        makeItem('f2', 'FOLDER', 'Folder 2', 0, 'f1'),
        makeItem('p1', 'PAGE', 'Page 1', 0, 'f2'),
      ];
      fixture.componentRef.setInput('links', nestedLinks);
      fixture.componentRef.setInput('selectedId', 'p1');
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();
      await fixture.whenStable();

      expect(scrolledNodeIds).toContain('p1');
    });
  });

  function createDropEvent(itemData: any, isPointerOverContainer = true): CdkDragDrop<SectionNode[]> {
    return {
      item: { data: itemData },
      isPointerOverContainer,
      event: new MouseEvent('mouseup'),
    } as unknown as CdkDragDrop<SectionNode[]>;
  }
});
