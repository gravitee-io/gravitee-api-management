import { CustomParametersList, MachineExecutor, ReusableExecutor } from '../../sdk/index.mjs';

import { createClassParameter } from '../parameters/class.mjs';
import { createDlcParameter } from '../parameters/dlc.mjs';
import { createTagParameter } from '../parameters/tag.mjs';
import { createVersionParameter } from '../parameters/version.mjs';

const image = 'ubuntu-<< parameters.version >>:<< parameters.tag >>';

const machine = new MachineExecutor('<< parameters.class >>', image);

const params = new CustomParametersList([
  createVersionParameter('2204'),
  createClassParameter('medium'),
  createTagParameter('current'),
  createDlcParameter(false),
]);

export const Ubuntu = new ReusableExecutor('ubuntu', machine, params);
