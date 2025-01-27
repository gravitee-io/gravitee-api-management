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

import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatTreeHarness } from '@angular/material/tree/testing';

import { PageTreeComponent } from './page-tree.component';

describe('PageTreeComponent', () => {
  let component: PageTreeComponent;
  let fixture: ComponentFixture<PageTreeComponent>;
  let harnessLoader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PageTreeComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(PageTreeComponent);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    component = fixture.componentInstance;
    component.pages = [
      {
        id: 'parent',
        name: 'Parent',
        children: [
          { id: 'child', name: 'Child' },
          {
            id: 'second-child',
            name: 'Second Child',
            children: [
              {
                id: 'grandchild',
                name: 'Grandchild',
              },
            ],
          },
        ],
      },
      {
        id: 'lone-node',
        name: 'Lone Node',
      },
    ];
    fixture.detectChanges();
  });

  it('should be expanded by default', async () => {
    const fileTree = await harnessLoader.getHarness(MatTreeHarness);
    expect(await fileTree.getNodes().then(nodes => nodes[0].isExpanded())).toEqual(true);
  });

  it('should create a node for each page', async () => {
    const fileTree = await harnessLoader.getHarness(MatTreeHarness);
    expect(await fileTree.getNodes().then(nodes => nodes.length)).toEqual(5);
  });

  it('should select file when page has no children', async () => {
    let emitted = '';
    component.openFile.subscribe((event: string) => {
      emitted = event;
    });
    const fileTree = await harnessLoader.getHarness(MatTreeHarness);
    const clickableNode = await fileTree.getNodes().then(nodes => nodes[1].host());
    await clickableNode.click();

    expect(emitted).toEqual('child');
  });

  it('should not select file when a page has children', async () => {
    let emitted = 'never-called';
    component.openFile.subscribe((event: string) => {
      emitted = event;
    });
    const fileTree = await harnessLoader.getHarness(MatTreeHarness);
    const parentNode = await fileTree.getNodes().then(nodes => nodes[0].host());
    await parentNode.click();

    expect(emitted).toEqual('never-called');
  });
});
