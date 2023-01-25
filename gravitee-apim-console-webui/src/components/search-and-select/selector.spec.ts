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
import { noop } from 'lodash';

import { Identifiable, Selector } from './selector';

function toIdentifiable(ref, index): Identifiable {
  return {
    id: `${index}`,
    name: `${index}`,
  };
}

function initData(length): Identifiable[] {
  return Array.from({ length }).map(toIdentifiable);
}

describe('Selector', () => {
  test('Update options should populate the options list', () => {
    const selector = new Selector();
    const options = initData(3);
    selector.updateOptions(options);
    expect(selector.options).toEqual(options);
  });

  test('Select should remove from options list', () => {
    const selector = new Selector();
    const options = initData(3);
    selector.updateOptions(options);
    selector.updateSelection(['0', '1'], noop);
    const expectedOptions = [{ id: '2', name: '2' }];
    const expectedSelection = [
      { id: '0', name: '0' },
      { id: '1', name: '1' },
    ];
    expect(selector.options).toEqual(expectedOptions);
    expect(selector.selection).toEqual(expectedSelection);
  });

  test('Un-select should put back to options list', () => {
    const selector = new Selector();
    const options = initData(3);
    selector.updateOptions(options);
    selector.updateSelection(['0', '1'], noop);
    selector.updateSelection(['0'], noop);
    const expectedOptions = [
      { id: '1', name: '1' },
      { id: '2', name: '2' },
    ];
    const expectedSelection = [{ id: '0', name: '0' }];
    expect(selector.options).toEqual(expectedOptions);
    expect(selector.selection).toEqual(expectedSelection);
  });
});
