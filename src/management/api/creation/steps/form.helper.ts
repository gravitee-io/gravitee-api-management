import { INgModelController } from 'angular';

export const shouldDisplayHint = (model: INgModelController) => {
  if (model.$untouched) {
    return true;
  }

  return model.$valid;
};
