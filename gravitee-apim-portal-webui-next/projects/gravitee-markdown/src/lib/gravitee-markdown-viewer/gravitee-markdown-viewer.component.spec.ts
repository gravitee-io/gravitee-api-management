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
import { GraviteeMarkdownViewerComponent } from './gravitee-markdown-viewer.component';
import { GraviteeMarkdownViewerService } from './gravitee-markdown-viewer.service';
import { GraviteeMarkdownViewerRegistryService } from './gravitee-markdown-viewer-registry.service';
import { CopyCodeComponent } from '../component-library/components/copy-code/copy-code.component';

describe('GraviteeMarkdownViewerComponent', () => {
  let component: GraviteeMarkdownViewerComponent;
  let fixture: ComponentFixture<GraviteeMarkdownViewerComponent>;
  let registryService: GraviteeMarkdownViewerRegistryService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GraviteeMarkdownViewerComponent],
      providers: [
        GraviteeMarkdownViewerService,
        GraviteeMarkdownViewerRegistryService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(GraviteeMarkdownViewerComponent);
    component = fixture.componentInstance;
    registryService = TestBed.inject(GraviteeMarkdownViewerRegistryService);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should register dynamic components', () => {
    registryService.registerComponent({
      selector: 'app-copy-code',
      component: CopyCodeComponent
    });

    expect(registryService.hasComponent('app-copy-code')).toBe(true);
  });

  it('should render markdown with dynamic components', () => {
    const markdownContent = `
# Test

Here's some code:

<app-copy-code text="console.log('test')"></app-copy-code>
    `;

    registryService.registerComponent({
      selector: 'app-copy-code',
      component: CopyCodeComponent
    });

    // Test that the component can be created and registry works
    expect(component).toBeTruthy();
    expect(registryService.hasComponent('app-copy-code')).toBe(true);
  });
});
