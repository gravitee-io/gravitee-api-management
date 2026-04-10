/**
 * Orb definitions used in Gravitee APIM CI.
 */
import { orb } from '../sdk/orbs.js';
import { orbVersions } from '../config/config.js';

export const keeper = orb('keeper', `gravitee-io/keeper@${orbVersions.keeper}`, {
  commands: {
    'env-export': { 'secret-url': 'string', 'var-name': 'string' },
    install: {},
    exec: { 'step-name': 'string', command: 'string' },
  },
});

export const aquasec = orb('aquasec', `gravitee-io/aquasec@${orbVersions.aquasec}`, {
  commands: {
    install_billy: {},
    pull_aqua_scanner_image: {},
    register_artifact: { artifact_to_register: 'string' },
    scan_docker_image: { docker_image_to_scan: 'string', scanner_url: 'string' },
  },
  jobs: {
    fs_scan: {},
  },
});

export const slack = orb('slack', `circleci/slack@${orbVersions.slack}`, {
  commands: {
    notify: { channel: 'string', branch_pattern: 'string', event: 'string', template: 'string' },
  },
});

export const gravitee = orb('gravitee', `gravitee-io/gravitee@${orbVersions.gravitee}`, {
  commands: {
    'docker-load-image-from-workspace': {},
  },
});

export const helm = orb('helm', `circleci/helm@${orbVersions.helm}`, {
  commands: {
    install_helm_client: { version: 'string' },
  },
});
