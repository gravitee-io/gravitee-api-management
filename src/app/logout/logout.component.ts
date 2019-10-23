import { Component, OnInit } from '@angular/core';
import { AuthenticationService } from '@gravitee/clients-sdk/dist';
import { Router } from '@angular/router';
import { CurrentUserService } from '../currentUser.service';

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
        this.router.navigate(['/']);
      }
    );
  }

}
