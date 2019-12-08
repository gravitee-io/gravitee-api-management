import { TimeTooLongError } from '../exceptions/TimeTooLongError';

export function delay(time) {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      reject(new TimeTooLongError());
    }, time);
  });
}
