import { describe, it, expect } from 'vitest';
import { generatePullRequestsConfig } from '../../src/workflows/pull-requests.js';
import type { CircleCIEnvironment } from '../../src/config/environment.js';
import { validateCircleCIConfig } from '../helpers/schema-validator.js';

function makeEnv(overrides: Partial<CircleCIEnvironment> = {}): CircleCIEnvironment {
  return {
    action: 'pull_requests',
    apimVersionPath: '',
    baseBranch: 'master',
    branch: 'APIM-1234-my-custom-branch',
    sha1: '784ff35ca',
    changedFiles: ['pom.xml'],
    buildNum: '1234',
    buildId: '1234',
    graviteeioVersion: '',
    isDryRun: false,
    ...overrides,
  };
}

describe('Pull requests workflow', () => {
  it('generates config for feature branch with all changes (pom.xml)', async () => {
    const cfg = generatePullRequestsConfig(makeEnv());
    await expect(cfg.toYAML()).toMatchFileSnapshot('snapshots/feature-branch-all-changes.yml');
  });

  it('generates config for master branch', async () => {
    const cfg = generatePullRequestsConfig(makeEnv({
      branch: 'master',
    }));
    await expect(cfg.toYAML()).toMatchFileSnapshot('snapshots/master-branch.yml');
  });

  it('generates config for support branch', async () => {
    const cfg = generatePullRequestsConfig(makeEnv({
      baseBranch: '4.1.x',
      branch: '4.1.x',
    }));
    await expect(cfg.toYAML()).toMatchFileSnapshot('snapshots/support-branch.yml');
  });

  it('generates config for E2E branch', async () => {
    const cfg = generatePullRequestsConfig(makeEnv({
      branch: 'APIM-1234-run-e2e',
    }));
    await expect(cfg.toYAML()).toMatchFileSnapshot('snapshots/e2e-branch.yml');
  });

  it('generates config for console-only changes', async () => {
    const cfg = generatePullRequestsConfig(makeEnv({
      changedFiles: ['gravitee-apim-console-webui'],
    }));
    await expect(cfg.toYAML()).toMatchFileSnapshot('snapshots/console-only-changes.yml');
  });

  it('generates config for portal-only changes', async () => {
    const cfg = generatePullRequestsConfig(makeEnv({
      changedFiles: ['gravitee-apim-portal-webui'],
    }));
    await expect(cfg.toYAML()).toMatchFileSnapshot('snapshots/portal-only-changes.yml');
  });

  it('generates config for portal-next-only changes', async () => {
    const cfg = generatePullRequestsConfig(makeEnv({
      changedFiles: ['gravitee-apim-portal-webui-next'],
    }));
    await expect(cfg.toYAML()).toMatchFileSnapshot('snapshots/portal-next-only-changes.yml');
  });

  it('generates config for backend-only changes (gateway)', async () => {
    const cfg = generatePullRequestsConfig(makeEnv({
      changedFiles: ['gravitee-apim-gateway'],
    }));
    await expect(cfg.toYAML()).toMatchFileSnapshot('snapshots/backend-only-gateway.yml');
  });

  it('generates config for helm-only changes', async () => {
    const cfg = generatePullRequestsConfig(makeEnv({
      changedFiles: ['helm'],
    }));
    await expect(cfg.toYAML()).toMatchFileSnapshot('snapshots/helm-only-changes.yml');
  });

  it('generates config for backend definition-only changes', async () => {
    const cfg = generatePullRequestsConfig(makeEnv({
      changedFiles: ['gravitee-apim-definition'],
    }));
    await expect(cfg.toYAML()).toMatchFileSnapshot('snapshots/backend-definition-only.yml');
  });

  it('generates config for distribution-only changes', async () => {
    const cfg = generatePullRequestsConfig(makeEnv({
      changedFiles: ['gravitee-apim-distribution'],
    }));
    await expect(cfg.toYAML()).toMatchFileSnapshot('snapshots/distribution-only-changes.yml');
  });
});

describe('Schema validation', () => {
  it('feature branch config is valid against CircleCI schema', async () => {
    const cfg = generatePullRequestsConfig(makeEnv());
    const { valid, errors } = await validateCircleCIConfig(cfg.toJSON());
    expect(errors).toBeNull();
    expect(valid).toBe(true);
  });

  it('master branch config is valid against CircleCI schema', async () => {
    const cfg = generatePullRequestsConfig(makeEnv({ branch: 'master' }));
    const { valid, errors } = await validateCircleCIConfig(cfg.toJSON());
    expect(errors).toBeNull();
    expect(valid).toBe(true);
  });

  it('support branch config is valid against CircleCI schema', async () => {
    const cfg = generatePullRequestsConfig(makeEnv({ baseBranch: '4.1.x', branch: '4.1.x' }));
    const { valid, errors } = await validateCircleCIConfig(cfg.toJSON());
    expect(errors).toBeNull();
    expect(valid).toBe(true);
  });

  it('E2E branch config is valid against CircleCI schema', async () => {
    const cfg = generatePullRequestsConfig(makeEnv({ branch: 'APIM-1234-run-e2e' }));
    const { valid, errors } = await validateCircleCIConfig(cfg.toJSON());
    expect(errors).toBeNull();
    expect(valid).toBe(true);
  });

  it('console-only changes config is valid against CircleCI schema', async () => {
    const cfg = generatePullRequestsConfig(makeEnv({ changedFiles: ['gravitee-apim-console-webui'] }));
    const { valid, errors } = await validateCircleCIConfig(cfg.toJSON());
    expect(errors).toBeNull();
    expect(valid).toBe(true);
  });

  it('portal-only changes config is valid against CircleCI schema', async () => {
    const cfg = generatePullRequestsConfig(makeEnv({ changedFiles: ['gravitee-apim-portal-webui'] }));
    const { valid, errors } = await validateCircleCIConfig(cfg.toJSON());
    expect(errors).toBeNull();
    expect(valid).toBe(true);
  });

  it('portal-next-only changes config is valid against CircleCI schema', async () => {
    const cfg = generatePullRequestsConfig(makeEnv({ changedFiles: ['gravitee-apim-portal-webui-next'] }));
    const { valid, errors } = await validateCircleCIConfig(cfg.toJSON());
    expect(errors).toBeNull();
    expect(valid).toBe(true);
  });

  it('backend-only changes config is valid against CircleCI schema', async () => {
    const cfg = generatePullRequestsConfig(makeEnv({ changedFiles: ['gravitee-apim-gateway'] }));
    const { valid, errors } = await validateCircleCIConfig(cfg.toJSON());
    expect(errors).toBeNull();
    expect(valid).toBe(true);
  });

  it('helm-only changes config is valid against CircleCI schema', async () => {
    const cfg = generatePullRequestsConfig(makeEnv({ changedFiles: ['helm'] }));
    const { valid, errors } = await validateCircleCIConfig(cfg.toJSON());
    expect(errors).toBeNull();
    expect(valid).toBe(true);
  });

  it('backend definition-only changes config is valid against CircleCI schema', async () => {
    const cfg = generatePullRequestsConfig(makeEnv({ changedFiles: ['gravitee-apim-definition'] }));
    const { valid, errors } = await validateCircleCIConfig(cfg.toJSON());
    expect(errors).toBeNull();
    expect(valid).toBe(true);
  });

  it('distribution-only changes config is valid against CircleCI schema', async () => {
    const cfg = generatePullRequestsConfig(makeEnv({ changedFiles: ['gravitee-apim-distribution'] }));
    const { valid, errors } = await validateCircleCIConfig(cfg.toJSON());
    expect(errors).toBeNull();
    expect(valid).toBe(true);
  });
});
