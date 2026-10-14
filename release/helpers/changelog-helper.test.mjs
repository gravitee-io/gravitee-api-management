import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import {
  BoardComponents,
  ComponentTypes,
  ProductSections,
  getChangelogFor,
  getTicketSections,
  getTicketsFor,
  getTicketsForProduct,
  productKeysOf,
} from './changelog-helper.mjs';

const issue = (overrides) => ({
  project: 'APIM',
  type: 'Public Bug',
  summary: 'something broke',
  githubIssue: '10',
  components: [],
  ...overrides,
});

describe('getChangelogFor', () => {
  it('sorts lines by GitHub issue number with unlinked tickets last, and escapes [ for GitBook', () => {
    const rendered = getChangelogFor([
      issue({ summary: 'no github link', githubIssue: null }),
      issue({ summary: 'later fix', githubIssue: '20' }),
      issue({ summary: 'first [regression]', githubIssue: '3' }),
    ]);

    assert.equal(
      rendered,
      '* first \\[regression] [#3](https://github.com/gravitee-io/issues/issues/3)\n' +
        '* later fix [#20](https://github.com/gravitee-io/issues/issues/20)\n' +
        '* no github link',
    );
  });
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

  it('merges a Portal-board ticket into the Portal component section, they are the same product', () => {
    const tickets = getTicketsFor(
      [
        issue({ components: [{ name: 'Portal' }], summary: 'apim ticket filed under the Portal component' }),
        issue({ project: 'PORTAL', summary: 'ticket raised on the Portal board' }),
      ],
      'Portal',
      'Public Bug',
    );

    assert.match(tickets, /apim ticket filed under the Portal component/);
    assert.match(tickets, /ticket raised on the Portal board/);
  });

  it('keeps a Portal-board ticket that carries a component in that component section only', () => {
    // The board mapping must not pick it up a second time: a component already claims the ticket.
    const portalTicket = [issue({ project: 'PORTAL', components: [{ name: 'Gateway' }] })];

    assert.match(getTicketsFor(portalTicket, 'Gateway', 'Public Bug'), /something broke/);
    assert.equal(getTicketsFor(portalTicket, 'Portal', 'Public Bug'), undefined);
  });

  it('leaves a Portal-board ticket out of Other, the Portal component section already covers it', () => {
    assert.equal(getTicketsFor([issue({ project: 'PORTAL', components: [] })], 'Other', 'Public Bug'), undefined);
  });

  it('takes a Portal-board ticket whose components mean nothing to APIM under Portal', () => {
    const tickets = getTicketsFor([issue({ project: 'PORTAL', components: [{ name: 'Router' }] })], 'Portal', 'Public Bug');

    assert.match(tickets, /\*\*Portal\*\*/);
  });

  it('lists a Portal-board ticket carrying the Portal component once, through the direct match', () => {
    // The direct component match claims it; the board mapping must not add a second line.
    const tickets = getTicketsFor([issue({ project: 'PORTAL', components: [{ name: 'Portal' }] })], 'Portal', 'Public Bug');

    assert.equal(tickets.match(/something broke/g).length, 1);
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

  it('renders nothing for a board with no product section, rather than an undefined heading', () => {
    // PORTAL's componentless tickets live in the Portal component section; it has no product section.
    assert.equal(getTicketsForProduct([issue({ project: 'PORTAL' })], 'PORTAL', 'Public Bug'), undefined);
  });
});

describe('section maps', () => {
  it('keeps the heading namespaces disjoint, so no heading can ever print twice', () => {
    // A product section named like a component would print the same heading from two loops.
    const componentNames = new Set(ComponentTypes);
    assert.deepEqual(
      Object.values(ProductSections).filter((name) => componentNames.has(name)),
      [],
    );

    // A board in both maps would get its componentless tickets listed once per path.
    const productBoards = new Set(Object.keys(ProductSections));
    assert.deepEqual(
      Object.keys(BoardComponents).filter((board) => productBoards.has(board)),
      [],
    );

    // A mapped component that is no real component would swallow the board's tickets silently:
    // no component section prints it, no product section exists, and Other excludes the board.
    assert.deepEqual(
      Object.values(BoardComponents).filter((name) => !componentNames.has(name)),
      [],
    );
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

  it('leaves out the Portal board, which the Portal component section already covers', () => {
    assert.deepEqual(productKeysOf([issue({ project: 'PORTAL' }), issue({ project: 'OBS' })]), ['OBS']);
  });
});

describe('getTicketSections', () => {
  it('prints a single Portal heading for an APIM Portal ticket and a Portal-board ticket', () => {
    const sections = getTicketSections(
      [
        issue({ components: [{ name: 'Portal' }], summary: 'apim ticket filed under the Portal component' }),
        issue({ project: 'PORTAL', summary: 'ticket raised on the Portal board' }),
      ],
      'Public Bug',
    );

    const rendered = sections.join('');
    assert.equal(rendered.split('**Portal**').length - 1, 1);
    assert.match(rendered, /apim ticket filed under the Portal component/);
    assert.match(rendered, /ticket raised on the Portal board/);
  });

  it('puts the component sections first, the product sections next and Other last', () => {
    const sections = getTicketSections(
      [
        issue({ components: [{ name: 'Gateway' }], summary: 'gateway ticket' }),
        issue({ project: 'OBS', summary: 'observability ticket' }),
        issue({ summary: 'componentless apim ticket' }),
      ],
      'Public Bug',
    );

    const headings = sections.map((section) => section.match(/\*\*(.+)\*\*/)[1]);
    assert.deepEqual(headings, ['Gateway', 'Observability', 'Other']);
  });

  it('keeps Other last when no product board released, exactly as before', () => {
    const sections = getTicketSections(
      [issue({ components: [{ name: 'Console' }], summary: 'console ticket' }), issue({ summary: 'componentless apim ticket' })],
      'Public Bug',
    );

    const headings = sections.map((section) => section.match(/\*\*(.+)\*\*/)[1]);
    assert.deepEqual(headings, ['Console', 'Other']);
  });

  it('returns nothing when no ticket matches the type', () => {
    assert.deepEqual(getTicketSections([issue({})], 'Public Improvement'), []);
  });

  it('renders no product section for a board whose tickets all carry a known component', () => {
    // The board is present, so productKeysOf names it, but its only ticket is claimed by Gateway:
    // an empty Observability heading must not render.
    const sections = getTicketSections([issue({ project: 'OBS', components: [{ name: 'Gateway' }] })], 'Public Bug');

    assert.deepEqual(
      sections.map((section) => section.match(/\*\*(.+)\*\*/)[1]),
      ['Gateway'],
    );
  });
});

describe('the grouping rule', () => {
  const BOARDS = ['APIM', 'PORTAL', 'OBS', 'ESM', 'AIAM', 'AUTHZ', 'FOUND', 'BX', 'NEWBOARD'];
  const COMPONENT_SETS = [
    [],
    ...ComponentTypes.map((name) => [{ name }]),
    [{ name: 'Router' }], // a component unknown to APIM
    [{ name: 'Gateway' }, { name: 'Portal' }], // two known components claim the ticket once each
    [{ name: 'Router' }, { name: 'Console' }], // the known component claims it, the unknown one is ignored
    [{ name: 'Gateway' }, { name: 'Gateway' }], // a repeated component claims the ticket once
  ];
  const TYPES = ['Public Bug', 'Public Improvement', 'Story'];

  // The grouping rule, stated independently of the implementation under test.
  const expectedHeadings = (ticket, ticketType) => {
    if (ticket.type !== ticketType) {
      return [];
    }
    const known = [...new Set(ticket.components.map((cmp) => cmp.name).filter((name) => ComponentTypes.includes(name)))];
    if (known.length > 0) {
      return known;
    }
    if (BoardComponents[ticket.project]) {
      return [BoardComponents[ticket.project]];
    }
    if (ProductSections[ticket.project]) {
      return [ProductSections[ticket.project]];
    }
    return ['Other'];
  };

  const tickets = BOARDS.flatMap((project) =>
    COMPONENT_SETS.flatMap((components) =>
      TYPES.map((type) =>
        issue({ project, components, type, summary: `<${project}|${type}|${components.map((cmp) => cmp.name).join('+') || 'none'}>` }),
      ),
    ),
  );

  for (const ticketType of ['Public Bug', 'Public Improvement']) {
    it(`places every ${ticketType} under exactly the headings the rule gives it, for every board and component mix`, () => {
      const sections = getTicketSections(tickets, ticketType);

      for (const ticket of tickets) {
        const expected = expectedHeadings(ticket, ticketType);
        const claiming = sections.filter((section) => section.includes(`* ${ticket.summary}`));
        assert.equal(claiming.length, expected.length, `"${ticket.summary}" rendered ${claiming.length}x, expected ${expected.length}x`);
        for (const section of claiming) {
          const heading = section.match(/\*\*(.+)\*\*/)[1];
          assert.ok(expected.includes(heading), `"${ticket.summary}" rendered under wrong heading **${heading}**`);
        }
      }

      const headings = sections.map((section) => section.match(/\*\*(.+)\*\*/)[1]);
      assert.deepEqual(
        headings,
        [...ComponentTypes, ...Object.values(ProductSections), 'Other'],
        'with every board present, sections render once each, in display order, Other last',
      );
    });
  }
});
