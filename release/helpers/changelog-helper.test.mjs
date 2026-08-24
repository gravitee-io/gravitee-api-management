import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { getTicketsFor, getTicketsForProduct, productKeysOf } from './changelog-helper.mjs';

const issue = (overrides) => ({
  project: 'APIM',
  type: 'Public Bug',
  summary: 'something broke',
  githubIssue: '10',
  components: [],
  ...overrides,
});

describe('getTicketsFor', () => {
  it('groups an APIM ticket under its component', () => {
    const tickets = getTicketsFor([issue({ components: [{ name: 'Gateway' }] })], 'Gateway', 'Public Bug');

    assert.match(tickets, /\*\*Gateway\*\*/);
    assert.match(tickets, /something broke \[#10\]/);
  });

  it('puts a ticket with an unknown component under Other', () => {
    const tickets = getTicketsFor([issue({ components: [{ name: 'Router' }] })], 'Other', 'Public Bug');

    assert.match(tickets, /\*\*Other\*\*/);
  });

  it('returns nothing for a component with no tickets of that type', () => {
    assert.equal(getTicketsFor([issue({ components: [{ name: 'Gateway' }] })], 'Gateway', 'Public Improvement'), undefined);
  });

  it('leaves a product ticket out of Other, so it is not listed twice', () => {
    // The board has a section of its own, so catching it here too would print it under both headings.
    assert.equal(getTicketsFor([issue({ project: 'OBS', components: [] })], 'Other', 'Public Bug'), undefined);
  });

  it('keeps a product ticket that carries a component in that component section, exactly as today', () => {
    // Product sections replace 'Other' for these boards, not the component breakdown.
    const tickets = getTicketsFor([issue({ project: 'OBS', components: [{ name: 'Gateway' }] })], 'Gateway', 'Public Bug');

    assert.match(tickets, /\*\*Gateway\*\*/);
  });

  it('still lists a product ticket once per component it carries, as today', () => {
    const twoComponents = [issue({ project: 'OBS', components: [{ name: 'Gateway' }, { name: 'Console' }] })];

    assert.match(getTicketsFor(twoComponents, 'Gateway', 'Public Bug'), /something broke/);
    assert.match(getTicketsFor(twoComponents, 'Console', 'Public Bug'), /something broke/);
  });

  it('keeps a board with no product name in the component breakdown rather than naming it', () => {
    // FOUND and BX are APIM's own working boards, so they are named nowhere and fall where APIM does.
    const tickets = getTicketsFor([issue({ project: 'BX', components: [{ name: 'Console' }] })], 'Console', 'Public Bug');
    assert.match(tickets, /\*\*Console\*\*/);

    assert.match(getTicketsFor([issue({ project: 'FOUND', components: [] })], 'Other', 'Public Bug'), /\*\*Other\*\*/);
    assert.match(getTicketsFor([issue({ project: 'NEWBOARD', components: [] })], 'Other', 'Public Bug'), /\*\*Other\*\*/);
  });
});

describe('getTicketsForProduct', () => {
  it('groups a product ticket under its product name rather than a board key', () => {
    const tickets = getTicketsForProduct([issue({ project: 'ESM', summary: 'topic acl sync' })], 'ESM', 'Public Bug');

    assert.match(tickets, /\*\*Event Stream Management\*\*/);
    assert.match(tickets, /topic acl sync \[#10\]/);
  });

  it('takes only that board, not every non-APIM ticket', () => {
    const tickets = getTicketsForProduct(
      [issue({ project: 'OBS', summary: 'reporter lag' }), issue({ project: 'ESM', summary: 'topic acl sync' })],
      'OBS',
      'Public Bug',
    );

    assert.match(tickets, /reporter lag/);
    assert.doesNotMatch(tickets, /topic acl sync/);
  });

  it('returns nothing when the board has no ticket of that type', () => {
    assert.equal(getTicketsForProduct([issue({ project: 'OBS' })], 'OBS', 'Public Improvement'), undefined);
  });

  it('takes a ticket whose components mean nothing to APIM, which would otherwise land in Other', () => {
    const tickets = getTicketsForProduct([issue({ project: 'OBS', components: [{ name: 'Router' }] })], 'OBS', 'Public Bug');

    assert.match(tickets, /\*\*Observability\*\*/);
  });

  it('leaves out a ticket already listed under its component, so it is not listed twice', () => {
    // A component section already covers it; taking it here too would add a second line.
    assert.equal(getTicketsForProduct([issue({ project: 'OBS', components: [{ name: 'Gateway' }] })], 'OBS', 'Public Bug'), undefined);
  });
});

describe('productKeysOf', () => {
  it('lists the product boards present, in the declared order rather than the order tickets arrived', () => {
    const keys = productKeysOf([issue({ project: 'OBS' }), issue({ project: 'ESM' }), issue({ project: 'OBS' })]);

    assert.deepEqual(keys, ['ESM', 'OBS']);
  });

  it('leaves out APIM and its team boards, which the component sections already cover', () => {
    assert.deepEqual(productKeysOf([issue({ project: 'APIM' }), issue({ project: 'FOUND' }), issue({ project: 'BX' })]), []);
  });

  it('leaves out a board with no product name, which the component sections and Other already cover', () => {
    assert.deepEqual(productKeysOf([issue({ project: 'NEWBOARD' }), issue({ project: 'ESM' })]), ['ESM']);
  });
});
