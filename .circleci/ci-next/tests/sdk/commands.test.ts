import { describe, it, expect } from 'vitest';
import { run, checkout, cache, workspace, storeArtifacts, storeTestResults, addSshKeys, setupRemoteDocker, when, unless } from '../../src/sdk/commands.js';

describe('commands', () => {
  it('run with name and command string', () => {
    expect(run('Install', 'npm ci').serialize()).toMatchSnapshot();
  });

  it('run with name and options object', () => {
    expect(run('Build', {
      command: 'mvn install',
      workingDirectory: 'backend',
      environment: { CI: 'true' },
    }).serialize()).toMatchSnapshot();
  });

  it('run with options only', () => {
    expect(run({ command: 'echo hello', name: 'Greet', when: 'always' }).serialize()).toMatchSnapshot();
  });

  it('checkout default', () => {
    expect(checkout().serialize()).toMatchSnapshot();
  });

  it('checkout with path', () => {
    expect(checkout('/tmp/src').serialize()).toMatchSnapshot();
  });

  it('cache restore', () => {
    expect(cache.restore({ keys: ['v1-{{ checksum "pom.xml" }}', 'v1-'] }).serialize()).toMatchSnapshot();
  });

  it('cache save', () => {
    expect(cache.save({ key: 'v1-{{ checksum "pom.xml" }}', paths: ['~/.m2'], when: 'always' }).serialize()).toMatchSnapshot();
  });

  it('workspace persist', () => {
    expect(workspace.persist({ root: '.', paths: ['target/'] }).serialize()).toMatchSnapshot();
  });

  it('workspace attach', () => {
    expect(workspace.attach({ at: '.' }).serialize()).toMatchSnapshot();
  });

  it('store artifacts', () => {
    expect(storeArtifacts({ path: 'dist/', destination: 'build' }).serialize()).toMatchSnapshot();
  });

  it('store test results', () => {
    expect(storeTestResults('~/test-results').serialize()).toMatchSnapshot();
  });

  it('add ssh keys', () => {
    expect(addSshKeys(['ab:cd:ef']).serialize()).toMatchSnapshot();
  });

  it('setup remote docker', () => {
    expect(setupRemoteDocker({ version: '20.10.24', dockerLayerCaching: true }).serialize()).toMatchSnapshot();
  });

  it('when conditional', () => {
    expect(when('<< parameters.deploy >>', [run('Deploy', 'deploy.sh')]).serialize()).toMatchSnapshot();
  });

  it('unless conditional', () => {
    expect(unless('<< parameters.skip >>', [run('Test', 'npm test')]).serialize()).toMatchSnapshot();
  });
});
