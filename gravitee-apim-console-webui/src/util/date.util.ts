import { Moment } from 'moment';

export function endOfDay(moment: Moment | undefined | null): undefined | number {
  if (!moment) {
    return undefined;
  }

  const endOfDay: Moment = moment.clone();
  endOfDay.add(1, 'day');
  endOfDay.subtract(1, 'millisecond');

  return endOfDay.valueOf();
}
