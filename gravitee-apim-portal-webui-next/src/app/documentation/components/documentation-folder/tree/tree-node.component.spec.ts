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
import { TestKey } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { TreeNodeComponent } from './tree-node.component';
import { TreeNodeComponentHarness } from './tree-node.component.harness';
import { AppTestingModule } from '../../../../../testing/app-testing.module';
import { TreeNode } from '../../../services/tree.service';

describe('TreeNodeComponent', () => {
  let fixture: ComponentFixture<TreeNodeComponent>;
  let component: TreeNodeComponent;
  let harness: TreeNodeComponentHarness;

  const init = async (params: Partial<{ node: TreeNode }> = {}) => {
    await TestBed.configureTestingModule({
      imports: [TreeNodeComponent, AppTestingModule],
      providers: [],
    }).compileComponents();

    fixture = TestBed.createComponent(TreeNodeComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('node', params.node);
    fixture.componentRef.setInput('level', 0);
    fixture.componentRef.setInput('selectedId', null);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, TreeNodeComponentHarness);
    fixture.detectChanges();
  };

  describe('test link node', () => {
    const node: TreeNode = {
      id: 'n3',
      label: 'Link 1',
      type: 'LINK',
    };

    it('should render node', async () => {
      await init({ node });

      const icon = fixture.debugElement.query(By.css('.tree__link__icon'));
      const iconTextContent = icon.nativeElement.textContent.trim();
      expect(iconTextContent).toEqual('open_in_new');

      const labelBtn = fixture.debugElement.query(By.css('.tree__link'));
      expect(labelBtn.nativeElement.textContent.trim()).toBe(`${iconTextContent} ${node.label}`);
    });

    it('should redirect on click', async () => {
      await init({ node });

      const redirectToLink = jest.spyOn(component, 'redirectToLink');

      const row = fixture.debugElement.query(By.css('.tree__row'));
      row.triggerEventHandler('click');
      fixture.detectChanges();

      expect(redirectToLink).toHaveBeenCalled();
    });
  });

  describe('test page node', () => {
    const node: TreeNode = {
      id: 'n1',
      label: 'Page 1',
      type: 'PAGE',
    };

    it('should render node', async () => {
      await init({ node });

      const labelBtn = fixture.debugElement.query(By.css('.tree__label'));
      expect(labelBtn.nativeElement.textContent.trim()).toBe(node.label);

      const icon = fixture.debugElement.query(By.css('.tree__icon'));
      expect(icon).toBeNull();
    });

    it('should select node on click', async () => {
      await init({ node });

      const selectNode = jest.spyOn(component, 'selectNode');
      const nodeSelected = jest.fn();
      component.nodeSelected.subscribe(nodeSelected);

      const row = fixture.debugElement.query(By.css('.tree__row'));
      row.triggerEventHandler('click');
      fixture.detectChanges();

      expect(selectNode).toHaveBeenCalled();
      expect(nodeSelected).toHaveBeenCalledWith(node.id);
    });
  });

  describe('test folder node', () => {
    const node: TreeNode = {
      id: 'f1',
      label: 'Folder 1',
      type: 'FOLDER',
      children: [
        {
          id: 'p1',
          label: 'Page 1',
          type: 'PAGE',
        },
        {
          id: 'l1',
          label: 'Link 1',
          type: 'LINK',
        },
      ],
    };

    it('should render node', async () => {
      await init({ node });

      const labelBtn = fixture.debugElement.query(By.css('.tree__label'));
      expect(labelBtn.nativeElement.textContent.trim()).toBe(node.label);

      const icon = fixture.debugElement.query(By.css('.tree__icon'));
      expect(icon.nativeElement.textContent.trim()).toEqual('keyboard_arrow_down');

      const children = fixture.debugElement.queryAll(By.css('app-tree-node'));
      expect(children.length).toEqual(2);

      const innerPage = children[0].query(By.css('.tree__label'));
      expect(innerPage).toBeTruthy();
      expect(innerPage.nativeElement.textContent.trim()).toBe(node.children![0].label);

      const innerLinkIcon = children[1].query(By.css('.tree__link__icon'));
      const iconTextContent = innerLinkIcon.nativeElement.textContent.trim();
      expect(iconTextContent).toEqual('open_in_new');

      const innerLink = children[1].query(By.css('.tree__link'));
      expect(innerLink).toBeTruthy();
      expect(innerLink.nativeElement.textContent.trim()).toBe(`${iconTextContent} ${node.children![1].label}`);
    });

    it('should toggle expansion on click', async () => {
      await init({ node });

      const toggleNode = jest.spyOn(component, 'toggleNode');

      const row = fixture.debugElement.query(By.css('.tree__row'));
      row.triggerEventHandler('click');
      fixture.detectChanges();

      expect(toggleNode).toHaveBeenCalled();

      const icon = fixture.debugElement.query(By.css('.tree__icon'));
      expect(icon.nativeElement.classList).toContain('expanded');
    });
  });

  describe('test API Product node', () => {
    const node: TreeNode = {
      id: 'product1',
      label: 'API Product 1',
      type: 'API_PRODUCT',
      children: [
        {
          id: 'product-page1',
          label: 'Product Overview',
          type: 'PAGE',
        },
      ],
    };

    it('should render as a collapsed container', async () => {
      await init({ node });

      expect(await harness.getText()).toBe(node.label);
      expect(await harness.getChildren()).toHaveLength(1);
      expect(await harness.getAriaExpanded()).toBe('false');
      expect(await harness.isExpanded()).toBe(false);
    });

    it('should toggle expansion without selecting the node', async () => {
      await init({ node });
      const nodeSelected = jest.fn();
      component.nodeSelected.subscribe(nodeSelected);

      await harness.click();

      expect(await harness.getAriaExpanded()).toBe('true');
      expect(await harness.isExpanded()).toBe(true);
      expect(nodeSelected).not.toHaveBeenCalled();
    });

    it('should support keyboard expansion', async () => {
      await init({ node });

      await harness.sendKeys(TestKey.LEFT_ARROW);
      expect(await harness.getAriaExpanded()).toBe('false');

      await harness.sendKeys(TestKey.RIGHT_ARROW);
      expect(await harness.getAriaExpanded()).toBe('true');
    });
  });

  describe('expansion requests', () => {
    const node: TreeNode = {
      id: 'product',
      label: 'Product',
      type: 'API_PRODUCT',
      children: [{ id: 'api', label: 'API', type: 'API', children: [{ id: 'page', label: 'Page', type: 'PAGE' }] }],
    };

    it('should apply a focus path to nested containers and make collapsed children inert', async () => {
      await init({ node });
      const children = fixture.nativeElement.querySelector('.tree__children');
      expect(children.hasAttribute('inert')).toBe(true);

      fixture.componentRef.setInput('expansionRequest', { mode: 'focus-path', pathIds: new Set(['product', 'api']) });
      fixture.detectChanges();
      expect(await harness.isExpanded()).toBe(true);
      expect(children.hasAttribute('inert')).toBe(false);
      expect(await (await harness.getChildren())[0].isExpanded()).toBe(true);

      fixture.componentRef.setInput('expansionRequest', { mode: 'focus-path', pathIds: new Set(['product']) });
      expect(await (await harness.getChildren())[0].isExpanded()).toBe(false);
    });

    it('should preserve manual expansion when selection or node metadata changes', async () => {
      await init({ node });
      fixture.componentRef.setInput('expansionRequest', { mode: 'collapse-all' });
      await harness.click();
      fixture.componentRef.setInput('selectedId', 'page');
      fixture.componentRef.setInput('node', { ...node, label: 'Renamed product' });
      expect(await harness.isExpanded()).toBe(true);
    });

    it('should reveal a path without closing manually expanded siblings', async () => {
      await init({ node });
      await harness.click();
      fixture.componentRef.setInput('expansionRequest', { mode: 'reveal-path', pathIds: new Set(['other-product']) });
      expect(await harness.isExpanded()).toBe(true);

      fixture.componentRef.setInput('expansionRequest', { mode: 'collapse-all' });
      expect(await harness.isExpanded()).toBe(false);
      fixture.componentRef.setInput('expansionRequest', { mode: 'reveal-path', pathIds: new Set(['product']) });
      expect(await harness.isExpanded()).toBe(true);
    });

    it('should retain child expansion when a parent is collapsed and reopened', async () => {
      await init({ node });
      await harness.click();
      const api = (await harness.getChildren())[0];
      await api.click();
      await harness.click();
      await harness.click();
      expect(await api.isExpanded()).toBe(true);
    });
  });

  it('should compute selected state from selectedId input', async () => {
    const node: TreeNode = {
      id: 'n1',
      label: 'Folder 1',
      type: 'FOLDER',
    };
    await init({ node });

    const row = fixture.debugElement.query(By.css('.tree__row'));
    expect(row.nativeElement.classList.contains('selected')).toBe(false);

    fixture.componentRef.setInput('selectedId', 'n1');
    fixture.detectChanges();

    expect(row.nativeElement.classList.contains('selected')).toBe(true);
  });
});
