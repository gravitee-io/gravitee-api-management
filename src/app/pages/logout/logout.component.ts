import { Component, OnInit } from '@angular/core';
import { AuthenticationService } from 'ng-portal-webclient/dist';
import { Router } from '@angular/router';
import { CurrentUserService } from '../../services/currentUser.service';

@Component({
  selector: 'app-logout',
  templateUrl: './logout.component.html',
  styleUrls: ['./logout.component.css']
})
export class LogoutComponent implements OnInit {

  constructor(
    private authService: AuthenticationService,
    private router: Router,
    private currentUserService: CurrentUserService
  ) { }

  ngOnInit() {
    this.authService.logout().subscribe(
      () => {
        this.currentUserService.revokeUser();
        this.router.navigate(['']);
      }
    );
  }

}
