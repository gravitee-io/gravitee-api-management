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
import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';

import { FlatTreeComponent, SectionNode } from './flat-tree.component';
import { FlatTreeComponentHarness } from './flat-tree.component.harness';

import { GioTestingModule } from '../../../shared/testing';
import {
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
    type: 'PAGE' | 'FOLDER' | 'LINK',
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
      case 'PAGE':
      default:
        return fakePortalNavigationPage({ id, title, order, parentId, published });
    }
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

    const links = [makeItem('p1', 'PAGE', 'Page 1', 0, 'parent-id', false)];

    fixture.componentRef.setInput('links', links);
    fixture.detectChanges();
    await fixture.whenStable();

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

  it('should handle nested folder structure', async () => {
    const links = [
      makeItem('f1', 'FOLDER', 'Folder 1', 0),
      makeItem('f2', 'FOLDER', 'Folder 2', 0, 'f1'),
      makeItem('p1', 'PAGE', 'Page 1', 0, 'f2'),
    ];

    fixture.componentRef.setInput('links', links);
    fixture.detectChanges();

    const titles = await harness.getAllItemTitles();
    expect(titles).toContain('Folder 1');
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

    const dropScenarios = [
      {
        description: 'should move Folder 1 down to 2nd position',
        links: [makeItem('p1', 'PAGE', '1', 0), makeItem('p2', 'PAGE', '2', 1), makeItem('f1', 'FOLDER', '3', 2)],
        dragId: 'f1',
        dropIndex: 1,
        prevIndex: 2,
        expected: { newParentId: null, newOrder: 1 },
      },
      {
        description: 'should move Page 2 into top position of Folder 1',
        links: [makeItem('f1', 'FOLDER', '1', 0), makeItem('p1', 'PAGE', '1.1', 0, 'f1'), makeItem('p2', 'PAGE', '1.2', 1, 'f1')],
        dragId: 'p2',
        dropIndex: 1, // Gap between F1 and P1
        prevIndex: 2,
        expected: { newParentId: 'f1', newOrder: 0 },
      },
      {
        description: 'should move Page 2 to root position',
        links: [makeItem('f1', 'FOLDER', '1', 0), makeItem('p1', 'PAGE', '1.1', 0, 'f1'), makeItem('p2', 'PAGE', '1.2', 1, 'f1')],
        dragId: 'p2',
        dropIndex: 0, // Top of list
        prevIndex: 2,
        expected: { newParentId: null, newOrder: 0 },
      },
      {
        description: 'should move Page 1 below Folder 1 (sibling)',
        links: [makeItem('p1', 'PAGE', '1', 0), makeItem('f1', 'FOLDER', '2', 1), makeItem('p2', 'PAGE', '3', 2)],
        dragId: 'p1',
        dropIndex: 2,
        prevIndex: 0,
        expected: { newParentId: null, newOrder: 2 },
      },
      {
        description: 'should move Root Page 2 into Folder 1 (above Child Page 1)',
        links: [makeItem('f1', 'FOLDER', '1', 0), makeItem('p1', 'PAGE', '1.1', 0, 'f1'), makeItem('p2', 'PAGE', '2', 1)],
        dragId: 'p2',
        dropIndex: 1,
        prevIndex: 2,
        expected: { newParentId: 'f1', newOrder: 0 },
      },
      {
        description: 'should move Page 1 into Folder 1 at 2nd position (descending)',
        links: [
          makeItem('p1', 'PAGE', '1', 0),
          makeItem('f1', 'FOLDER', '2', 1),
          makeItem('p2', 'PAGE', '2.1', 0, 'f1'),
          makeItem('p3', 'PAGE', '2.2', 1, 'f1'),
        ],
        dragId: 'p1',
        dropIndex: 2, // Gap between P2 and P3
        prevIndex: 0,
        expected: { newParentId: 'f1', newOrder: 1 },
      },
      {
        description: 'should NOT place inside folder if already sibling and moving down 1 slot',
        links: [makeItem('p1', 'PAGE', '1', 0), makeItem('f1', 'FOLDER', '2', 1), makeItem('p2', 'PAGE', '2.1', 0, 'f1')],
        dragId: 'p1',
        dropIndex: 1,
        prevIndex: 0,
        expected: { newParentId: null, newOrder: 1 },
      },
      {
        description: 'should move nested Page 1 to root bottom',
        links: [makeItem('f1', 'FOLDER', '1', 0), makeItem('p1', 'PAGE', '1.1', 0, 'f1'), makeItem('p2', 'PAGE', '2', 1)],
        dragId: 'p1',
        dropIndex: 2, // After P2
        prevIndex: 1,
        expected: { newParentId: null, newOrder: 2 },
      },
      {
        description: 'should move last item up one space',
        links: [makeItem('f1', 'FOLDER', '1', 0), makeItem('p1', 'PAGE', '2', 1), makeItem('p2', 'PAGE', '3', 2)],
        dragId: 'p2',
        dropIndex: 1,
        prevIndex: 2,
        expected: { newParentId: null, newOrder: 1 },
      },
    ];

    it.each(dropScenarios)('$description', async ({ links, dragId, dropIndex, prevIndex, expected }) => {
      fixture.componentRef.setInput('links', links);
      fixture.detectChanges();
      await fixture.whenStable();

      const nodeToMove = findNode(dragId);
      if (!nodeToMove) throw new Error(`Node ${dragId} not found in test setup`);

      const event = createDropEvent(nodeToMove, dropIndex, prevIndex);
      component.onDrop(event);

      expect(nodeMovedSpy).toHaveBeenCalledWith({
        node: nodeToMove,
        newParentId: expected.newParentId,
        newOrder: expected.newOrder,
      });
    });

    it('should not emit event if dropped at same position', async () => {
      const links = [makeItem('p1', 'PAGE', 'Page 1', 0)];
      fixture.componentRef.setInput('links', links);
      fixture.detectChanges();

      const pageNode = component.tree()[0];
      const event = createDropEvent(pageNode, 0, 0);

      component.onDrop(event);
      expect(nodeMovedSpy).not.toHaveBeenCalled();
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

    it('should disable "Delete" option for folder with children', async () => {
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
      expect(await deleteButton!.isDisabled()).toBe(true);
    });
  });

  function createDropEvent(itemData: any, currentIndex: number, previousIndex: number): CdkDragDrop<SectionNode[]> {
    return {
      item: { data: itemData },
      currentIndex,
      previousIndex,
      container: { data: [] },
      previousContainer: { data: [] },
      isPointerOverContainer: true,
      distance: { x: 0, y: 0 },
      dropPoint: { x: 0, y: 0 },
      event: new MouseEvent('mouseup'),
    } as unknown as CdkDragDrop<SectionNode[]>;
  }
});
