import { keeper } from './keeper';
import { github } from './github';
import { slack } from './slack';
import { awsS3 } from './aws-s3';
import { artifactory } from './artifactory';
import { awsCli } from './aws-cli';
import { helm } from './helm';
import { snyk } from './snyk';

export const orbs = {
  artifactory,
  awsCli,
  awsS3,
  keeper,
  github,
  helm,
  slack,
  snyk,
};
