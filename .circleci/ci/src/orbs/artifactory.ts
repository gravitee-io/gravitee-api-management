import { orb } from '@circleci/circleci-config-sdk';
import { config } from '../config';

export const artifactory = new orb.OrbImport('artifactory-orb', 'jfrog', 'artifactory-orb', config.orbs.artifactory);
