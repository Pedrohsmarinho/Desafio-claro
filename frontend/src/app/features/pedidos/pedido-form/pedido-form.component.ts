import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subscription } from 'rxjs';
import { LIMITE_MAXIMO_PEDIDOS } from '../../../core/models/pedido.model';
import { PedidoService } from '../../../core/services/pedido.service';

@Component({
  selector: 'app-pedido-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './pedido-form.component.html',
  styleUrl: './pedido-form.component.scss',
})
export class PedidoFormComponent implements OnInit, OnDestroy {
  private subscription?: Subscription;

  readonly limiteMaximo = LIMITE_MAXIMO_PEDIDOS;
  totalPedidos = 0;
  enviando = false;
  erroEnvio: string | null = null;

  form = this.fb.group({
    displayName: ['', [Validators.required, Validators.minLength(5)]],
    pesoKg: [null as number | null, [Validators.required, Validators.min(0.001)]],
    itens: [null as number | null, [Validators.required, Validators.min(1), Validators.pattern(/^\d+$/)]],
  });

  constructor(
    private fb: FormBuilder,
    private pedidoService: PedidoService,
    private router: Router,
    private snackBar: MatSnackBar,
  ) {}

  ngOnInit(): void {
    this.subscription = this.pedidoService.pedidos$.subscribe((pedidos) => (this.totalPedidos = pedidos.length));
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  get limiteAtingido(): boolean {
    return this.totalPedidos >= this.limiteMaximo;
  }

  salvar(): void {
    if (this.form.invalid || this.limiteAtingido) {
      return;
    }

    this.enviando = true;
    this.erroEnvio = null;

    const { displayName, pesoKg, itens } = this.form.getRawValue();
    const pesoGramas = Math.round(pesoKg! * 1000);

    this.pedidoService.criar({ displayName: displayName!, itens: itens!, peso: pesoGramas }).subscribe({
      next: (pedido) => {
        this.enviando = false;
        const mensagem = pedido.origemLocal
          ? 'API indisponível: pedido salvo localmente e será sincronizado depois'
          : 'Pedido cadastrado com sucesso';
        this.snackBar.open(mensagem, 'Fechar', { duration: 4000 });
        this.router.navigate(['/pedidos']);
      },
      error: (err) => {
        this.enviando = false;
        this.erroEnvio = this.extrairMensagemErro(err);
      },
    });
  }

  private extrairMensagemErro(err: unknown): string {
    const httpError = err as { error?: { message?: string }; message?: string };
    return httpError?.error?.message ?? httpError?.message ?? 'Ocorreu um erro inesperado ao cadastrar o pedido';
  }
}
