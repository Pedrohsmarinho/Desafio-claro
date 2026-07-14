import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';

/**
 * Anexa "Authorization: Bearer <token>" em toda requisicao para a API. Se a
 * API responder 401 (token ausente/invalido/expirado do lado do backend),
 * limpa a sessao local e redireciona para /login - cobre o caso do token
 * expirar em segundo plano (usuario deixou a aba aberta) mesmo que o
 * relogio do navegador diga que ainda e valido.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const token = authService.getToken();
  const requisicaoParaApi = req.url.startsWith(environment.apiUrl);

  const requisicao = token && requisicaoParaApi
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(requisicao).pipe(
    catchError((error) => {
      if (error.status === 401 && requisicaoParaApi) {
        authService.logout();
        router.navigate(['/login']);
      }
      return throwError(() => error);
    }),
  );
};
