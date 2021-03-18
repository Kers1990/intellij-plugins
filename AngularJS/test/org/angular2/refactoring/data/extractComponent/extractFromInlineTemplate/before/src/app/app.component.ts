import { Component } from '@angular/core';

@Component({
  selector: 'app-root',
  template: `
    <footer class="text-muted py-5">
      <div class="container">
        <p<caret> class="float-end mb-1">
          <a href="#">Back to top of {{titleContent}}</a>
        </p>
        <p class="mb-1">Album example is &copy; Bootstrap, but please download and customize it for yourself!</p>
      </div>
    </footer>
  `,
})
export class AppComponent {
  titleContent = 'angular-extract-component';
}
