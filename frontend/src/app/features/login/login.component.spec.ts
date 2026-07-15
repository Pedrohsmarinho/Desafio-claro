import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideAnimations } from '@angular/platform-browser/animations';
import { Router, provideRouter } from '@angular/router';
import { environment } from '../../../environments/environment';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideAnimations(), provideRouter([])],
    });

    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  it('inicia no modo login, com os dois formularios invalidos', () => {
    expect(component.modo).toBe('login');
    expect(component.formLogin.invalid).toBeTrue();
    expect(component.formCadastro.invalid).toBeTrue();
  });

  it('nao envia o login enquanto o formulario estiver invalido', () => {
    component.entrar();

    expect(component.carregandoLogin).toBeFalse();
    httpMock.expectNone(`${environment.apiUrl}/auth/login`);
  });

  it('reporta email invalido no formulario de login', () => {
    component.formLogin.setValue({ email: 'nao-e-um-email', senha: 'qualquer' });

    expect(component.formLogin.get('email')?.hasError('email')).toBeTrue();
    expect(component.formLogin.invalid).toBeTrue();
  });

  it('navega para o dashboard apos login bem sucedido', () => {
    const navigateSpy = spyOn(router, 'navigate');
    component.formLogin.setValue({ email: 'usuario@teste.com', senha: 'senha12345' });

    component.entrar();

    httpMock.expectOne(`${environment.apiUrl}/auth/login`).flush({ email: 'usuario@teste.com', token: 'token-fake' });

    expect(navigateSpy).toHaveBeenCalledWith(['/dashboard']);
    expect(component.carregandoLogin).toBeFalse();
  });

  it('exibe mensagem de credenciais invalidas quando o login retorna 401', () => {
    component.formLogin.setValue({ email: 'usuario@teste.com', senha: 'senha-errada' });

    component.entrar();

    httpMock.expectOne(`${environment.apiUrl}/auth/login`).flush(
      { message: 'Credenciais invalidas' },
      { status: 401, statusText: 'Unauthorized' },
    );

    expect(component.erroLogin).toBe('Email ou senha inválidos');
    expect(component.carregandoLogin).toBeFalse();
  });

  it('reporta senha curta no formulario de cadastro', () => {
    component.selecionarModo('cadastro');
    component.formCadastro.setValue({ nome: 'Fulano', email: 'fulano@teste.com', senha: '123' });

    expect(component.formCadastro.get('senha')?.hasError('minlength')).toBeTrue();
    expect(component.formCadastro.invalid).toBeTrue();
  });

  it('navega para o dashboard apos cadastro bem sucedido (login automatico)', () => {
    const navigateSpy = spyOn(router, 'navigate');
    component.selecionarModo('cadastro');
    component.formCadastro.setValue({ nome: 'Fulano de Tal', email: 'fulano@teste.com', senha: 'senha12345' });

    component.criarConta();

    httpMock
      .expectOne(`${environment.apiUrl}/auth/registrar`)
      .flush({ email: 'fulano@teste.com', token: 'token-fake' });

    expect(navigateSpy).toHaveBeenCalledWith(['/dashboard']);
  });

  it('exibe mensagem de email duplicado quando o cadastro retorna 409', () => {
    component.selecionarModo('cadastro');
    component.formCadastro.setValue({ nome: 'Fulano de Tal', email: 'fulano@teste.com', senha: 'senha12345' });

    component.criarConta();

    httpMock
      .expectOne(`${environment.apiUrl}/auth/registrar`)
      .flush({ message: 'Email ja cadastrado' }, { status: 409, statusText: 'Conflict' });

    expect(component.erroCadastro).toBe('Já existe uma conta cadastrada com esse email');
  });
});
