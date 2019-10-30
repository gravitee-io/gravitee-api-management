import { Component, OnInit } from '@angular/core';

import {UserService, User} from 'ng-portal-webclient/dist';
import { CurrentUserService } from '../../services/currentUser.service';

@Component({
  selector: 'app-user',
  templateUrl: './user.component.html',
  styleUrls: ['./user.component.css']
})

export class UserComponent implements OnInit {
  user: User;

  constructor(
    private userService: UserService,
    private currentUserService: CurrentUserService
  ) { }

  ngOnInit() {
    this.currentUserService.currentUser.subscribe(newCurrentUser => this.user = newCurrentUser);

    this.userService.getCurrentUser().subscribe(
      (user) => {
        const loggedUser = user;
        this.userService.getCurrentUserAvatar().subscribe(
          (avatar) => {
            const reader = new FileReader();
            reader.addEventListener('loadend', (e) => {
              loggedUser.avatar = reader.result.toString();
            });
            reader.readAsDataURL(avatar);
            this.currentUserService.changeUser(loggedUser);
          }
        );
      }
    );

  }

}
