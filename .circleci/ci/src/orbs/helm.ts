import { orb } from '@circleci/circleci-config-sdk';
import { config } from '../config';

export const helm = new orb.OrbImport('helm', 'circleci', 'helm', config.orbs.helm);
