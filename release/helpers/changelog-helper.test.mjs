import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { getTicketsFor } from './changelog-helper.mjs';

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
});
