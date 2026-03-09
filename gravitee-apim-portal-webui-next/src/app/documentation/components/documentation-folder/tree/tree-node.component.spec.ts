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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { TreeNodeComponent } from './tree-node.component';
import { AppTestingModule } from '../../../../../testing/app-testing.module';
import { TreeNode } from '../../../services/tree.service';

describe('TreeNodeComponent', () => {
  let fixture: ComponentFixture<TreeNodeComponent>;
  let component: TreeNodeComponent;

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
      expect(icon.nativeElement.classList).not.toContain('expanded');
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
    expect(row.nativeElement.classList.contains('m3-selected-nav-item')).toBe(false);

    fixture.componentRef.setInput('selectedId', 'n1');
    fixture.detectChanges();

    expect(row.nativeElement.classList.contains('m3-selected-nav-item')).toBe(true);
  });
});
