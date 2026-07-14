import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { filter } from 'rxjs';
import { AuthService } from './core/services/auth.service';
import { HealthService, StatusApi } from './core/services/health.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, MatToolbarModule, MatButtonModule, MatIconModule, MatTooltipModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  autenticado = false;
  readonly saudeApi$ = this.healthService.status$;

  private readonly rotulosSaude: Record<StatusApi, string> = {
    verificando: 'Verificando status da API...',
    up: 'API disponível',
    down: 'API indisponível',
  };

  constructor(
    private authService: AuthService,
    private healthService: HealthService,
    private router: Router,
  ) {
    this.autenticado = this.authService.isAuthenticated();
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => {
        this.autenticado = this.authService.isAuthenticated();
      });
  }

  rotuloSaude(status: StatusApi): string {
    return this.rotulosSaude[status];
  }

  sair(): void {
    this.authService.logout();
    this.autenticado = false;
    this.router.navigate(['/login']);
  }
}
