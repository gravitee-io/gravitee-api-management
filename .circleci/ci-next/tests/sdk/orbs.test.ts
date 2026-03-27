import { describe, it, expect } from 'vitest';
import { orb, orbJob, useOrb, serializeOrbs } from '../../src/sdk/orbs.js';

describe('orbs', () => {
  const keeper = orb('keeper', 'gravitee-io/keeper@0.7.0', {
    commands: {
      'env-export': { 'secret-url': 'string', 'var-name': 'string' },
    },
  });

  it('serializes orb imports', () => {
    expect(serializeOrbs([keeper])).toMatchSnapshot();
  });

  it('useOrb creates a step', () => {
    expect(
      useOrb(keeper, 'env-export', { 'secret-url': 'keeper://xxx', 'var-name': 'TOKEN' }).serialize(),
    ).toMatchSnapshot();
  });

  it('orbJob creates a job reference', () => {
    const ref = orbJob(keeper, 'some-job');
    expect(ref.name).toBe('keeper/some-job');
    expect(ref.kind).toBe('orb-job');
  });
});
