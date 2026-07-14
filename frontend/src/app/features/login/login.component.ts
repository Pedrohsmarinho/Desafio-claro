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
import { AuthService } from '../../core/services/auth.service';

type ModoAcesso = 'login' | 'cadastro';

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

    this.carregandoLogin = true;
    this.erroLogin = null;

    const { email, senha } = this.formLogin.getRawValue();
    this.authService.login({ email: email!, senha: senha! }).subscribe({
      next: () => {
        this.carregandoLogin = false;
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.carregandoLogin = false;
        this.erroLogin = err.status === 401 ? 'Email ou senha inválidos' : this.extrairErro(err);
      },
    });
  }

  criarConta(): void {
    if (this.formCadastro.invalid) {
      return;
    }

    this.carregandoCadastro = true;
    this.erroCadastro = null;

    const { nome, email, senha } = this.formCadastro.getRawValue();
    this.authService.registrar({ nome: nome!, email: email!, senha: senha! }).subscribe({
      next: () => {
        this.carregandoCadastro = false;
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.carregandoCadastro = false;
        this.erroCadastro = err.status === 409
          ? 'Já existe uma conta cadastrada com esse email'
          : this.extrairErro(err);
      },
    });
  }

  private extrairErro(err: unknown): string {
    const httpError = err as { error?: { message?: string } };
    return httpError?.error?.message ?? 'Ocorreu um erro inesperado. Tente novamente.';
  }
}
