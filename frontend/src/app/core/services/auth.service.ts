import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginRequest, LoginResponse, RegistroRequest } from '../models/auth.model';
import { jwtExpirado } from '../utils/jwt.util';

const STORAGE_KEY = 'pedidos_auth';

/**
 * Token guardado em sessionStorage (nao localStorage): expira junto com a
 * aba/sessao do navegador, reduzindo a janela de exposicao caso o
 * dispositivo seja compartilhado ou comprometido (localStorage persiste
 * indefinidamente entre sessoes). A alternativa mais segura de verdade
 * seria um cookie httpOnly setado pelo backend (inacessivel a JavaScript,
 * portanto imune a XSS) - deixada de fora do escopo deste desafio porque
 * exigiria reconfigurar CORS (credentials) e adicionar protecao CSRF, ja
 * que cookies sao enviados automaticamente pelo navegador.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = `${environment.apiUrl}/auth`;

  constructor(private http: HttpClient) {}

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, request).pipe(
      tap((response) => this.armazenarSessao(response)),
    );
  }

  registrar(request: RegistroRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/registrar`, request).pipe(
      tap((response) => this.armazenarSessao(response)),
    );
  }

  logout(): void {
    sessionStorage.removeItem(STORAGE_KEY);
  }

  isAuthenticated(): boolean {
    const token = this.getToken();
    return token !== null && !jwtExpirado(token);
  }

  getEmailLogado(): string | null {
    return this.lerSessao()?.email ?? null;
  }

  getToken(): string | null {
    return this.lerSessao()?.token ?? null;
  }

  private lerSessao(): LoginResponse | null {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as LoginResponse) : null;
  }

  private armazenarSessao(response: LoginResponse): void {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(response));
  }
}
