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
import { buildFilterFields, FilterableItem, matchesFilters, pruneSelection } from './catalog-filters';

describe('catalog filters', () => {
  const names = {
    category: new Map([
      ['cat-it', 'IT'],
      ['cat-hr', 'HR'],
    ]),
    access: new Map([
      ['NO_KEY', 'No key needed'],
      ['CREDENTIALS', 'Credentials needed'],
    ]),
  };

  const items: FilterableItem[] = [
    { access: 'NO_KEY', categoryIds: ['cat-it'], labels: ['incident', 'on-call'], publisher: 'platform' },
    { access: 'CREDENTIALS', categoryIds: ['cat-it'], labels: ['incident'], publisher: 'platform' },
    { access: 'CREDENTIALS', categoryIds: ['cat-hr'], labels: ['hr'], publisher: 'people' },
  ];

  describe('buildFilterFields', () => {
    it('offers a field only when its values tell items apart', () => {
      const fields = buildFilterFields(items, names);

      expect(fields.map(field => field.key)).toEqual(['access', 'category', 'tag', 'publisher']);

      const single = buildFilterFields([items[0], { ...items[0], labels: ['incident', 'on-call'] }], names);
      expect(single.map(field => field.key)).toEqual([]);
    });

    it('counts each value, names it, and keeps the order the caller gave', () => {
      const access = buildFilterFields(items, names).find(field => field.key === 'access');

      expect(access?.values).toEqual([
        { value: 'NO_KEY', label: 'No key needed', count: 1 },
        { value: 'CREDENTIALS', label: 'Credentials needed', count: 2 },
      ]);
    });

    it('puts the commonest first where no order is given', () => {
      const label = buildFilterFields(items, names).find(field => field.key === 'tag');

      expect(label?.values.map(value => `${value.label} ${value.count}`)).toEqual(['incident 2', 'hr 1', 'on-call 1']);
    });
  });

  describe('pruneSelection', () => {
    it('drops a value the fields no longer offer, and the field with it', () => {
      const fields = buildFilterFields(items, names);

      expect(pruneSelection({ access: ['NO_KEY'], tag: ['gone'] }, fields)).toEqual({ access: ['NO_KEY'] });
    });

    it('drops a value whose whole field is gone', () => {
      expect(pruneSelection({ protocol: ['A2A'] }, buildFilterFields(items, names))).toEqual({});
    });

    it('keeps what is still offered', () => {
      const fields = buildFilterFields(items, names);

      expect(pruneSelection({ access: ['NO_KEY', 'CREDENTIALS'], category: ['cat-hr'] }, fields)).toEqual({
        access: ['NO_KEY', 'CREDENTIALS'],
        category: ['cat-hr'],
      });
    });
  });

  describe('matchesFilters', () => {
    it('lets everything through when nothing is selected', () => {
      expect(items.filter(item => matchesFilters(item, {}))).toHaveLength(3);
      expect(items.filter(item => matchesFilters(item, { tag: [] }))).toHaveLength(3);
    });

    it('treats values within a field as alternatives', () => {
      expect(items.filter(item => matchesFilters(item, { access: ['NO_KEY', 'CREDENTIALS'] }))).toHaveLength(3);
    });

    it('treats separate fields as conditions that must all hold', () => {
      expect(items.filter(item => matchesFilters(item, { access: ['CREDENTIALS'], category: ['cat-hr'] }))).toHaveLength(1);
      expect(items.filter(item => matchesFilters(item, { access: ['NO_KEY'], category: ['cat-hr'] }))).toHaveLength(0);
    });

    it('matches an item that carries any of the selected labels', () => {
      expect(items.filter(item => matchesFilters(item, { tag: ['on-call'] }))).toHaveLength(1);
      expect(items.filter(item => matchesFilters(item, { tag: ['incident'] }))).toHaveLength(2);
    });
  });
});
