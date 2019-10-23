import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { User } from '@gravitee/clients-sdk/dist';

@Injectable()
export class CurrentUserService {

  private currentUserSource = new BehaviorSubject<User>(undefined);
  currentUser = this.currentUserSource.asObservable();

  constructor() { }

  changeUser(newCurrentUser: User) {
    this.currentUserSource.next(newCurrentUser);
  }

  revokeUser() {
    this.currentUserSource.next(undefined);
  }
}