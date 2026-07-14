import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { Subscription, combineLatest } from 'rxjs';
import {
  LIMITE_MAXIMO_PEDIDOS,
  Pedido,
  STATUS_LABELS,
  StatusPedido,
  podeTransicionar,
} from '../../../core/models/pedido.model';
import { PedidoService } from '../../../core/services/pedido.service';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-pedido-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
  ],
  templateUrl: './pedido-list.component.html',
  styleUrl: './pedido-list.component.scss',
})
export class PedidoListComponent implements OnInit, OnDestroy {
  private subscription?: Subscription;

  readonly displayedColumns = ['cliente', 'itens', 'peso', 'status', 'acoes'];
  readonly statusLabels = STATUS_LABELS;
  readonly limiteMaximo = LIMITE_MAXIMO_PEDIDOS;
  readonly StatusPedido = StatusPedido;

  pedidos: Pedido[] = [];
  carregando = true;
  apiIndisponivel = false;

  constructor(
    private pedidoService: PedidoService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.pedidoService.carregar();
    this.subscription = combineLatest([
      this.pedidoService.pedidos$,
      this.pedidoService.apiDisponivel$,
    ]).subscribe(([pedidos, apiDisponivel]) => {
      this.pedidos = pedidos;
      this.apiIndisponivel = !apiDisponivel;
      this.carregando = false;
    });
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  podeAtingirLimite(): boolean {
    return this.pedidos.length >= this.limiteMaximo;
  }

  acaoHabilitada(pedido: Pedido, novoStatus: StatusPedido): boolean {
    return podeTransicionar(pedido.status, novoStatus);
  }

  rotuloStatus(status: StatusPedido): string {
    return this.statusLabels[status];
  }

  classeBadge(status: StatusPedido): string {
    return `badge badge-${status.toLowerCase().replace(/_/g, '-')}`;
  }

  alterarStatus(pedido: Pedido, novoStatus: StatusPedido): void {
    this.pedidoService.alterarStatus(pedido, novoStatus).subscribe({
      next: () => this.notificar(`Pedido "${pedido.displayName}" atualizado para ${this.statusLabels[novoStatus]}`),
      error: (err) => this.notificar(this.extrairMensagemErro(err), true),
    });
  }

  excluir(pedido: Pedido): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        titulo: 'Excluir pedido',
        mensagem: `Tem certeza que deseja excluir o pedido "${pedido.displayName}"? Essa ação não pode ser desfeita.`,
      },
    });

    dialogRef.afterClosed().subscribe((confirmado) => {
      if (!confirmado) {
        return;
      }
      this.pedidoService.excluir(pedido).subscribe({
        next: () => this.notificar(`Pedido "${pedido.displayName}" excluído com sucesso`),
        error: (err) => this.notificar(this.extrairMensagemErro(err), true),
      });
    });
  }

  private notificar(mensagem: string, erro = false): void {
    this.snackBar.open(mensagem, 'Fechar', {
      duration: 4000,
      panelClass: erro ? 'snackbar-erro' : 'snackbar-sucesso',
    });
  }

  private extrairMensagemErro(err: unknown): string {
    const httpError = err as { error?: { message?: string }; message?: string };
    return httpError?.error?.message ?? httpError?.message ?? 'Ocorreu um erro inesperado';
  }
}
