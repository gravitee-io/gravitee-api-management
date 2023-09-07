import { keeper } from './keeper';
import { github } from './github';
import { slack } from './slack';
import { awsS3 } from './aws-s3';
import { artifactory } from './artifactory';
import { awsCli } from './aws-cli';
import { snyk } from './snyk';
import { helm } from './helm';

export const orbs = {
  artifactory,
  awsCli,
  awsS3,
  keeper,
  github,
  slack,
  snyk,
  helm,
};
