import { Component, OnInit, HostListener } from '@angular/core';
import { FormGroup, FormBuilder } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthenticationService, PortalService, IdentityProvider } from '@gravitee/clients-sdk/dist';

import '@gravitee/ui-components/wc/gv-button'
import '@gravitee/ui-components/wc/gv-icon'
import '@gravitee/ui-components/wc/gv-input'
import '@gravitee/ui-components/wc/gv-message'
import '@gravitee/ui-components/src/icons/shapes/thirdparty-shapes'


@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})

export class LoginComponent implements OnInit {

  loginForm : FormGroup;
  notification: {
    message: String,
    type: String,
  };
  
  providers: IdentityProvider[];

  constructor(
    private authService: AuthenticationService,
    private portalService: PortalService,
    private formBuilder: FormBuilder,
    private router: Router,
  ) { 
    this.portalService.configurationGet().subscribe(
      (configuration) => {
        if (configuration.authentication.localLogin.enabled) {
          this.loginForm = this.formBuilder.group({
            username: '',
            password: '',
          });
        }
      }
    );
  }

  ngOnInit() {
    this.portalService.configurationIdentitiesGet()
    .subscribe(
      (configurationIdentitiesResponse) => {
        this.providers = configurationIdentitiesResponse.data;
      },
      (error) => { 
        console.error('something wrong occurred with identity providers: ' + error.statusText);
      }
    )
  }
  
  authenticate(provider) {
    console.log('Authentication asked for \ ' + provider.name + ' (id = ' +  provider.id + ')');
  }

  isFormValid() {
    return !!this.loginForm.value.username && !!this.loginForm.value.password;
  }

  login() {
    if (this.isFormValid()) {
      //create basic authorization header
      var authorization = 'Basic ' + btoa(this.loginForm.value.username+":"+this.loginForm.value.password);

      //call the login resource from the API.
      this.authService.defaultHeaders = this.authService.defaultHeaders.set("X-Requested-With", "XMLHttpRequest"); //avoid browser to prompt for credentials if 401
      this.authService.login(authorization).subscribe(
        () => { 
          this.notification = {
            message: "Log in successful", 
            type : "success"
          };

          // add routing to main page.
          this.router.navigate(['/user']);
        },
        (error) => { 
          this.notification = {
            message: 'Username or password is incorrect.', 
            type: "error"
          };
          console.error(error)
          this.loginForm.reset();
        }
      );
    }
  }
}