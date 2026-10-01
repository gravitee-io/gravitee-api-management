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

import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ErrorHandler } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { TreeComponent } from './tree.component';
import { TreeComponentHarness } from './tree.component.harness';
import { fakePortalNavigationFolder } from '../../../../../entities/portal-navigation/portal-navigation-item.fixture';
import { makeItem } from '../../../../../mocks/portal-navigation-item.mocks';
import { AppTestingModule } from '../../../../../testing/app-testing.module';
import { TreeService, TreeNode } from '../../../services/tree.service';

describe('TreeComponent', () => {
  const documentationTreeService = new TreeService();
  const parentItem = fakePortalNavigationFolder();

  let fixture: ComponentFixture<TreeComponent>;
  let component: TreeComponent;
  let harness: TreeComponentHarness;
  let itemsTree: TreeNode[];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TreeComponent, MatIconTestingModule, AppTestingModule],
    }).compileComponents();

    /**
     *  Creates this tree
     *
     *  - Page 1
     *  - Folder 1
     *  - External Link 1
     *  - Folder 2
     *      - Page 2
     *      - Folder 3
     *      - External Link 2
     *  - API 1
     *      - Page 3
     */
    documentationTreeService.init(parentItem, [
      makeItem('p1', 'PAGE', 'Page 1', 0),
      makeItem('f1', 'FOLDER', 'Folder 1', 1),
      makeItem('l1', 'LINK', 'External Link 1', 3),
      makeItem('f2', 'FOLDER', 'Folder 2', 4),
      makeItem('p2', 'PAGE', 'Page 2', 0, 'f2'),
      makeItem('f3', 'FOLDER', 'Folder 3', 1, 'f2'),
      makeItem('l2', 'LINK', 'External Link 2', 2, 'f2'),
      makeItem('a1', 'API', 'API 1', 4),
      makeItem('p3', 'PAGE', 'Page 3', 5, 'a1'),
    ]);
    itemsTree = documentationTreeService.getTree();
    fixture = TestBed.createComponent(TreeComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('tree', itemsTree);
    fixture.componentRef.setInput('selectedId', 'p2');
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, TreeComponentHarness);
  });

  it('should build tree', async () => {
    const topLevelItems = await harness.getTopLevelNodes();
    expect(topLevelItems.length).toBe(5);

    const topLevelLabels = await Promise.all(topLevelItems?.map(node => node.getText()) || []);
    expect(topLevelLabels).toEqual(['Page 1', 'Folder 1', 'open_in_new External Link 1', 'Folder 2', 'API 1']);

    expect(await topLevelItems[0].getChildren()).toHaveLength(0);
    expect(await topLevelItems[1].getChildren()).toHaveLength(0);
    expect(await topLevelItems[2].getChildren()).toHaveLength(0);

    const nestedItems = await topLevelItems[3].getChildren();
    expect(nestedItems).toHaveLength(3);

    const nestedLabels = await Promise.all(nestedItems?.map(node => node.getText()) || []);
    expect(nestedLabels).toEqual(['Page 2', 'Folder 3', 'open_in_new External Link 2']);
  });

  it('should scroll to a selected page after its path is revealed', async () => {
    const scrollIntoViewSpy = jest.spyOn(HTMLElement.prototype, 'scrollIntoView').mockReturnValue().mockClear();
    fixture.componentRef.setInput('expansionRequest', { mode: 'reveal-path', pathIds: new Set(['f2']) });
    fixture.detectChanges();
    await fixture.whenStable();
    expect(scrollIntoViewSpy).toHaveBeenCalledWith({ behavior: 'smooth', block: 'center' });
    scrollIntoViewSpy.mockRestore();
  });

  it('should not scroll to the automatically selected page inside a collapsed branch', async () => {
    const scrollIntoViewSpy = jest.spyOn(HTMLElement.prototype, 'scrollIntoView').mockReturnValue().mockClear();
    fixture.componentRef.setInput('expansionRequest', { mode: 'collapse-all' });
    fixture.componentRef.setInput('selectedId', 'p3');
    fixture.detectChanges();
    await fixture.whenStable();
    expect(scrollIntoViewSpy).not.toHaveBeenCalled();
    scrollIntoViewSpy.mockRestore();
  });

  it('should wait for expansion animations before scrolling to the selected page', async () => {
    let finishAnimation!: () => void;
    const finished = new Promise<void>(resolve => {
      finishAnimation = resolve;
    });
    fixture.nativeElement.getAnimations = () => [{ finished }];
    const scrollIntoViewSpy = jest.spyOn(HTMLElement.prototype, 'scrollIntoView').mockReturnValue().mockClear();
    fixture.componentRef.setInput('expansionRequest', { mode: 'reveal-path', pathIds: new Set(['f2']) });
    fixture.detectChanges();
    await fixture.whenStable();
    expect(scrollIntoViewSpy).not.toHaveBeenCalled();
    finishAnimation();
    await Promise.allSettled([finished]);
    await fixture.whenStable();
    expect(scrollIntoViewSpy).toHaveBeenCalledWith({ behavior: 'smooth', block: 'center' });
    scrollIntoViewSpy.mockRestore();
  });

  it('should not scroll if the user collapses the selected branch before its animation finishes', async () => {
    let finishAnimation!: () => void;
    const finished = new Promise<void>(resolve => {
      finishAnimation = resolve;
    });
    fixture.nativeElement.getAnimations = () => [{ finished }];
    const scrollIntoViewSpy = jest.spyOn(HTMLElement.prototype, 'scrollIntoView').mockReturnValue().mockClear();
    fixture.componentRef.setInput('expansionRequest', { mode: 'reveal-path', pathIds: new Set(['f2']) });
    await harness.clickItemByTitle('Folder 2');
    finishAnimation();
    await Promise.allSettled([finished]);
    await fixture.whenStable();
    expect(scrollIntoViewSpy).not.toHaveBeenCalled();
    scrollIntoViewSpy.mockRestore();
  });

  it('should report scrolling errors to ErrorHandler', async () => {
    const error = new Error('Unable to scroll to the selected page');
    const errorHandlerSpy = jest.spyOn(TestBed.inject(ErrorHandler), 'handleError').mockReturnValue();
    const scrollIntoViewSpy = jest.spyOn(HTMLElement.prototype, 'scrollIntoView').mockImplementationOnce(() => {
      throw error;
    });

    try {
      fixture.componentRef.setInput('expansionRequest', { mode: 'reveal-path', pathIds: new Set(['f2']) });
      fixture.detectChanges();
      await fixture.whenStable();
      expect(errorHandlerSpy).toHaveBeenCalledTimes(1);
      expect(errorHandlerSpy).toHaveBeenCalledWith(error);
    } finally {
      scrollIntoViewSpy.mockRestore();
      errorHandlerSpy.mockRestore();
    }
  });

  it('should scroll to the selected page even if an animation is canceled', async () => {
    let cancelAnimation!: (reason: unknown) => void;
    const finished = new Promise<void>((_, reject) => {
      cancelAnimation = reject;
    });
    fixture.nativeElement.getAnimations = () => [{ finished }];
    const scrollIntoViewSpy = jest.spyOn(HTMLElement.prototype, 'scrollIntoView').mockReturnValue().mockClear();
    const errorHandlerSpy = jest.spyOn(TestBed.inject(ErrorHandler), 'handleError').mockReturnValue();

    try {
      fixture.componentRef.setInput('expansionRequest', { mode: 'reveal-path', pathIds: new Set(['f2']) });
      fixture.detectChanges();
      await fixture.whenStable();
      expect(scrollIntoViewSpy).not.toHaveBeenCalled();
      cancelAnimation(new DOMException('Animation canceled', 'AbortError'));
      await Promise.allSettled([finished]);
      await fixture.whenStable();
      expect(scrollIntoViewSpy).toHaveBeenCalledTimes(1);
      expect(scrollIntoViewSpy).toHaveBeenCalledWith({ behavior: 'smooth', block: 'center' });
      expect(errorHandlerSpy).not.toHaveBeenCalled();
    } finally {
      scrollIntoViewSpy.mockRestore();
      errorHandlerSpy.mockRestore();
    }
  });

  it('should cancel a pending scroll when the tree is destroyed', async () => {
    let finishAnimation!: () => void;
    const finished = new Promise<void>(resolve => {
      finishAnimation = resolve;
    });
    fixture.nativeElement.getAnimations = () => [{ finished }];
    const scrollIntoViewSpy = jest.spyOn(HTMLElement.prototype, 'scrollIntoView').mockReturnValue().mockClear();
    fixture.componentRef.setInput('expansionRequest', { mode: 'reveal-path', pathIds: new Set(['f2']) });
    fixture.detectChanges();
    fixture.destroy();
    finishAnimation();
    await finished;
    await Promise.resolve();
    expect(scrollIntoViewSpy).not.toHaveBeenCalled();
    scrollIntoViewSpy.mockRestore();
  });

  describe('item click', () => {
    it('should select a page on click', async () => {
      const selectSpy = jest.fn();
      component.selectNode.subscribe(selectSpy);

      await harness.clickItemByTitle('Page 1');
      expect(selectSpy).toHaveBeenCalledWith('p1');
    });

    it('should select a nested page on click', async () => {
      const selectSpy = jest.fn();
      component.selectNode.subscribe(selectSpy);

      await harness.clickItemByTitle('Folder 2');
      await harness.clickItemByTitle('Page 2');
      expect(selectSpy).toHaveBeenCalledWith('p2');
    });

    it('should expand folder on click', async () => {
      const selectSpy = jest.fn();
      component.selectNode.subscribe(selectSpy);

      let folder = await harness.getFolderByTitle('Folder 1');
      expect(folder?.expanded).toEqual(false);

      await harness.clickItemByTitle('Folder 1');
      expect(selectSpy).not.toHaveBeenCalledWith('f1');

      folder = await harness.getFolderByTitle('Folder 1');
      expect(folder?.expanded).toEqual(true);
    });

    it('should expand api on click', async () => {
      const selectSpy = jest.fn();
      component.selectNode.subscribe(selectSpy);

      let api = await harness.getApiByTitle('API 1');
      expect(api?.expanded).toEqual(false);

      await harness.clickItemByTitle('API 1');
      expect(selectSpy).not.toHaveBeenCalledWith('a1');

      api = await harness.getApiByTitle('API 1');
      expect(api?.expanded).toEqual(true);
    });
  });
});
