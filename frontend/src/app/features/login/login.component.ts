import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Observable } from 'rxjs';
import { LoginResponse } from '../../core/models/auth.model';
import { AuthService } from '../../core/services/auth.service';
import { extrairMensagemErro } from '../../core/utils/http-error.util';

type ModoAcesso = 'login' | 'cadastro';

/** Parametros que diferem entre entrar()/criarConta() - o resto do fluxo (loading, subscribe, navegacao) e identico. */
interface OpcoesAutenticacao {
  setCarregando: (valor: boolean) => void;
  setErro: (mensagem: string | null) => void;
  statusComMensagemEspecial: { codigo: number; mensagem: string };
  mensagemPadrao: string;
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  modo: ModoAcesso = 'login';

  carregandoLogin = false;
  erroLogin: string | null = null;

  carregandoCadastro = false;
  erroCadastro: string | null = null;

  formLogin = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    senha: ['', [Validators.required]],
  });

  formCadastro = this.fb.group({
    nome: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    senha: ['', [Validators.required, Validators.minLength(8)]],
  });

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
  ) {}

  selecionarModo(modo: ModoAcesso): void {
    this.modo = modo;
    this.erroLogin = null;
    this.erroCadastro = null;
  }

  entrar(): void {
    if (this.formLogin.invalid) {
      return;
    }

    const { email, senha } = this.formLogin.getRawValue();
    this.autenticar(this.authService.login({ email: email!, senha: senha! }), {
      setCarregando: (v) => (this.carregandoLogin = v),
      setErro: (v) => (this.erroLogin = v),
      statusComMensagemEspecial: { codigo: 401, mensagem: 'Email ou senha inválidos' },
      mensagemPadrao: 'Ocorreu um erro inesperado. Tente novamente.',
    });
  }

  criarConta(): void {
    if (this.formCadastro.invalid) {
      return;
    }

    const { nome, email, senha } = this.formCadastro.getRawValue();
    this.autenticar(this.authService.registrar({ nome: nome!, email: email!, senha: senha! }), {
      setCarregando: (v) => (this.carregandoCadastro = v),
      setErro: (v) => (this.erroCadastro = v),
      statusComMensagemEspecial: { codigo: 409, mensagem: 'Já existe uma conta cadastrada com esse email' },
      mensagemPadrao: 'Ocorreu um erro inesperado. Tente novamente.',
    });
  }

  /** Miolo comum a entrar()/criarConta(): loading -> subscribe -> navega no sucesso ou reporta erro. */
  private autenticar(request$: Observable<LoginResponse>, opcoes: OpcoesAutenticacao): void {
    opcoes.setCarregando(true);
    opcoes.setErro(null);

    request$.subscribe({
      next: () => {
        opcoes.setCarregando(false);
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        opcoes.setCarregando(false);
        opcoes.setErro(
          err.status === opcoes.statusComMensagemEspecial.codigo
            ? opcoes.statusComMensagemEspecial.mensagem
            : extrairMensagemErro(err, opcoes.mensagemPadrao),
        );
      },
    });
  }
}
