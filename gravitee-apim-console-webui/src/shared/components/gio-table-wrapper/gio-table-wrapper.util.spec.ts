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

import { sortBy } from 'lodash';

import { gioTableFilterCollection } from './gio-table-wrapper.util';

describe('gioTableFilterCollection', () => {
  const ELEMENT_DATA: unknown[] = [
    { position: 1, name: 'Hydrogen', weight: 1.0079, symbol: 'H' },
    { position: 2, name: 'Helium', weight: 4.0026, symbol: 'He' },
    { position: 3, name: 'Lithium', weight: 6.941, symbol: 'Li' },
    { position: 4, name: 'Beryllium', weight: 9.0122, symbol: 'Be' },
    { position: 5, name: 'Boron', weight: 10.811, symbol: 'B' },
    { position: 6, name: 'Carbon', weight: 12.0107, symbol: 'C' },
    { position: 7, name: 'Nitrogen', weight: 14.0067, symbol: 'N' },
    { position: 8, name: 'Oxygen', weight: 15.9994, symbol: 'O' },
    { position: 9, name: 'Fluorine', weight: 18.9984, symbol: 'F' },
    { position: 10, name: 'Neon', weight: 20.1797, symbol: 'Ne' },
  ];

  it('should return all collection without filter', () => {
    const collectionFiltered = gioTableFilterCollection(ELEMENT_DATA, {});

    expect(collectionFiltered).toEqual(ELEMENT_DATA);
  });

  it('should filter by pagination', () => {
    const collectionFilteredPage1 = gioTableFilterCollection(ELEMENT_DATA, {
      pagination: {
        index: 1,
        size: 2,
      },
    });

    expect(collectionFilteredPage1).toEqual([ELEMENT_DATA[0], ELEMENT_DATA[1]]);

    const collectionFilteredPage2 = gioTableFilterCollection(ELEMENT_DATA, {
      pagination: {
        index: 2,
        size: 2,
      },
    });

    expect(collectionFilteredPage2).toEqual([ELEMENT_DATA[2], ELEMENT_DATA[3]]);
  });

  it('should filter by searchTerm in any key', () => {
    const collectionFiltered1 = gioTableFilterCollection(ELEMENT_DATA, {
      searchTerm: 'Be',
    });

    expect(collectionFiltered1).toEqual([ELEMENT_DATA[3]]);

    const collectionFiltered2 = gioTableFilterCollection(ELEMENT_DATA, {
      searchTerm: '.99',
    });

    expect(collectionFiltered2).toEqual([ELEMENT_DATA[7], ELEMENT_DATA[8]]);
  });

  it('should filter by searchTerm with searchTermIgnoreKeys', () => {
    const collectionFiltered = gioTableFilterCollection(
      ELEMENT_DATA,
      {
        searchTerm: '4',
      },
      { searchTermIgnoreKeys: ['weight'] },
    );

    expect(collectionFiltered).toEqual([ELEMENT_DATA[3]]);
  });

  it('should sort by name asc', () => {
    const collectionSorted = gioTableFilterCollection(ELEMENT_DATA, {
      sort: {
        active: 'name',
        direction: 'asc',
      },
    });

    expect(collectionSorted).toEqual(sortBy(ELEMENT_DATA, 'name'));
  });

  it('should sort by symbol desc', () => {
    const collectionSorted = gioTableFilterCollection(ELEMENT_DATA, {
      sort: {
        active: 'symbol',
        direction: 'desc',
      },
    });

    expect(collectionSorted).toEqual(sortBy(ELEMENT_DATA, 'symbol').reverse());
  });
});
