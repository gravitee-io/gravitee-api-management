/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { OverlayContainer, OverlayModule } from '@angular/cdk/overlay';
import { ComponentFixture, fakeAsync, flushMicrotasks, TestBed, tick } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { KeywordValueInputComponent } from './keyword-value-input.component';
import { FILTER_VALUES_PROVIDER, FilterValueItem, FilterValuesProvider, FilterValuesQuery } from '../../filter-providers';
import { FilterDefinition } from '../../filter.model';

const KEYWORD_DEFINITION: FilterDefinition = {
  name: 'API',
  label: 'API',
  type: 'KEYWORD',
  operators: ['EQ', 'IN'],
  apiTypes: ['HTTP_PROXY'],
};

function makeItems(prefix: string, count: number): FilterValueItem[] {
  return Array.from({ length: count }, (_, i) => ({
    id: `${prefix}-${i}`,
    value: `${prefix}-${i}`,
    label: `${prefix} ${i}`,
  }));
}

function appendFakeValuesScrollSurface(overlay: OverlayContainer): HTMLElement {
  let host: HTMLElement;
  try {
    host = overlay.getContainerElement();
  } catch {
    host = document.body;
  }
  const shell = document.createElement('div');
  shell.className = 'gd-keyword-values-overlay-panel';
  const scroll = document.createElement('div');
  scroll.className = 'gd-keyword-values-overlay__scroll';
  scroll.style.height = '80px';
  scroll.style.overflow = 'auto';
  const inner = document.createElement('div');
  inner.style.height = '400px';
  inner.appendChild(document.createTextNode('\u00a0'));
  scroll.appendChild(inner);
  shell.appendChild(scroll);
  host.appendChild(shell);
  Object.defineProperty(scroll, 'scrollHeight', { configurable: true, value: 480 });
  Object.defineProperty(scroll, 'clientHeight', { configurable: true, value: 80 });
  return scroll;
}

function scrollPanelToBottom(panel: HTMLElement): void {
  panel.scrollTop = panel.scrollHeight - panel.clientHeight;
  panel.dispatchEvent(new Event('scroll', { bubbles: false }));
}

describe('KeywordValueInputComponent', () => {
  let fixture: ComponentFixture<KeywordValueInputComponent>;
  let getValuesSpy: jest.MockedFunction<FilterValuesProvider['getValues']>;

  afterEach(() => {
    try {
      const host = TestBed.inject(OverlayContainer).getContainerElement();
      host.querySelectorAll('.gd-keyword-values-overlay-panel').forEach(el => el.remove());
      host.querySelectorAll('.mat-mdc-autocomplete-panel.gd-value-input-autocomplete-panel').forEach(el => el.remove());
    } catch {
      document.body.querySelectorAll('.gd-keyword-values-overlay-panel').forEach(el => el.remove());
    }
  });

  function configureBed(getValuesImpl: FilterValuesProvider['getValues']): void {
    getValuesSpy = jest.fn(getValuesImpl);
    TestBed.configureTestingModule({
      imports: [KeywordValueInputComponent, OverlayModule],
      providers: [
        provideNoopAnimations(),
        { provide: FILTER_VALUES_PROVIDER, useValue: { getValues: getValuesSpy } as FilterValuesProvider },
      ],
    });
    fixture = TestBed.createComponent(KeywordValueInputComponent);
    fixture.componentRef.setInput('definition', KEYWORD_DEFINITION);
    fixture.componentRef.setInput('selectedOperator', 'IN');
    fixture.detectChanges();
  }

  it('should request only page 1 for a fixed list (hasNextPage false) and ignore scroll', fakeAsync(() => {
    TestBed.resetTestingModule();
    const data = makeItems('fixed', 3);
    configureBed(() => of({ data, hasNextPage: false }));

    tick(200);
    expect(getValuesSpy).toHaveBeenCalledTimes(1);
    expect(getValuesSpy).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1, perPage: 10, filterName: 'API' }));

    const panel = appendFakeValuesScrollSurface(TestBed.inject(OverlayContainer));
    fixture.componentInstance.attachAutocompletePanelScrollListener();
    flushMicrotasks();
    tick(16);

    scrollPanelToBottom(panel);
    tick(120);

    expect(getValuesSpy).toHaveBeenCalledTimes(1);
  }));

  it('should load the next page when the user scrolls near the bottom and hasNextPage is true', fakeAsync(() => {
    TestBed.resetTestingModule();
    const page1 = makeItems('p1', 10);
    const page2 = makeItems('p2', 5);
    configureBed((q: FilterValuesQuery) => {
      if (q.page === 1) {
        return of({ data: page1, hasNextPage: true });
      }
      if (q.page === 2) {
        return of({ data: page2, hasNextPage: false });
      }
      return of({ data: [], hasNextPage: false });
    });

    tick(200);
    expect(getValuesSpy).toHaveBeenCalledTimes(1);
    expect(getValuesSpy).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1 }));

    const panel = appendFakeValuesScrollSurface(TestBed.inject(OverlayContainer));
    fixture.componentInstance.attachAutocompletePanelScrollListener();
    flushMicrotasks();
    tick(16);

    scrollPanelToBottom(panel);
    tick(120);

    expect(getValuesSpy).toHaveBeenCalledTimes(2);
    expect(getValuesSpy).toHaveBeenLastCalledWith(expect.objectContaining({ page: 2 }));
  }));
});
