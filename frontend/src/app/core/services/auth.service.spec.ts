import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

const STORAGE_KEY = 'pedidos_auth';

function criarTokenFake(exp: number): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const payload = btoa(JSON.stringify({ sub: 'usuario@teste.com', exp }));
  return `${header}.${payload}.assinatura-fake`;
}

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    sessionStorage.removeItem(STORAGE_KEY);
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.removeItem(STORAGE_KEY);
  });

  it('nao esta autenticado quando nao ha sessao', () => {
    expect(service.isAuthenticated()).toBeFalse();
  });

  it('armazena a sessao em sessionStorage (nao localStorage) apos login', () => {
    const expFuturo = Math.floor(Date.now() / 1000) + 3600;
    const token = criarTokenFake(expFuturo);

    service.login({ email: 'usuario@teste.com', senha: '12345678' }).subscribe();
    httpMock.expectOne(`${environment.apiUrl}/auth/login`).flush({ email: 'usuario@teste.com', token });

    expect(service.isAuthenticated()).toBeTrue();
    expect(sessionStorage.getItem(STORAGE_KEY)).toContain(token);
    expect(localStorage.getItem(STORAGE_KEY)).toBeNull();
  });

  it('considera nao autenticado quando o token ja expirou', () => {
    const expPassado = Math.floor(Date.now() / 1000) - 3600;
    const token = criarTokenFake(expPassado);

    service.login({ email: 'usuario@teste.com', senha: '12345678' }).subscribe();
    httpMock.expectOne(`${environment.apiUrl}/auth/login`).flush({ email: 'usuario@teste.com', token });

    expect(service.isAuthenticated()).toBeFalse();
  });

  it('logout remove a sessao', () => {
    const expFuturo = Math.floor(Date.now() / 1000) + 3600;
    const token = criarTokenFake(expFuturo);

    service.login({ email: 'usuario@teste.com', senha: '12345678' }).subscribe();
    httpMock.expectOne(`${environment.apiUrl}/auth/login`).flush({ email: 'usuario@teste.com', token });

    service.logout();

    expect(service.isAuthenticated()).toBeFalse();
    expect(sessionStorage.getItem(STORAGE_KEY)).toBeNull();
  });

  it('registrar tambem autentica automaticamente', () => {
    const expFuturo = Math.floor(Date.now() / 1000) + 3600;
    const token = criarTokenFake(expFuturo);

    service.registrar({ nome: 'Fulano', email: 'fulano@teste.com', senha: '12345678' }).subscribe();
    httpMock.expectOne(`${environment.apiUrl}/auth/registrar`).flush({ email: 'fulano@teste.com', token });

    expect(service.isAuthenticated()).toBeTrue();
    expect(service.getEmailLogado()).toBe('fulano@teste.com');
  });
});
