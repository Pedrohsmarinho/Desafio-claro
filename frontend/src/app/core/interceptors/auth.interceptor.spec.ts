import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let httpMock: HttpTestingController;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['getToken', 'logout']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    });

    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('anexa Authorization: Bearer <token> quando ha token', () => {
    authServiceSpy.getToken.and.returnValue('token-fake');

    httpClient.get(`${environment.apiUrl}/pedidos`).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/pedidos`);
    expect(req.request.headers.get('Authorization')).toBe('Bearer token-fake');
    req.flush([]);
  });

  it('nao anexa Authorization quando nao ha token', () => {
    authServiceSpy.getToken.and.returnValue(null);

    httpClient.get(`${environment.apiUrl}/pedidos`).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/pedidos`);
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush([]);
  });

  it('em 401, limpa a sessao e redireciona para /login', () => {
    authServiceSpy.getToken.and.returnValue('token-expirado');

    httpClient.get(`${environment.apiUrl}/pedidos`).subscribe({
      error: () => {
        expect(authServiceSpy.logout).toHaveBeenCalled();
        expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
      },
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/pedidos`);
    req.flush({ message: 'Token invalido' }, { status: 401, statusText: 'Unauthorized' });
  });
});
