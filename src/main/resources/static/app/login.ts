/// <reference path="../typings/_custom.d.ts" />

import {Component, View, NgIf} from 'angular2/angular2';
import {bootstrap} from 'angular2/angular2';

@Component({
  selector: 'login'
})

@View({
  template: `
  <div class="ui middle aligned center aligned grid" style="height:100%">
    <div class="column" style="max-width:450px">
      <form class="ui large form" action="/login" method="post" (submit)="onSubmit()">
        <div class="ui segment">
          <div class="field">
            <div class="ui left icon input">
              <i class="user icon"></i>
              <input type="text" id="personalNumber" name="personalNumber" placeholder="Personal Number" pattern="[0-9]*" required autofocus>
            </div>
          </div>
          <div class="field">
            <div class="ui left icon input">
              <i class="lock icon"></i>
              <input type="password" id="password" name="password" placeholder="Password" required>
            </div>
          </div>
          <button type="submit" [class.loading]="loading" class="ui fluid large primary button">Login</button>
        </div>

        <div class="ui error message"></div>

      </form>

      <div class="ui message">
        Forgot your password? <a href="#">Help Me</a>.
        New to us? <a href="#">Sign Up</a>.
      </div>
    </div>
  </div>
  `,
})

export class Login {

    loading: Boolean = false;

    constructor() {
    }

    onSubmit() {
        this.loading = true;
    }
}

bootstrap(Login);