import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
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

type FiltroStatus = StatusPedido | 'TODOS';

@Component({
  selector: 'app-pedido-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSortModule,
    MatPaginatorModule,
  ],
  templateUrl: './pedido-list.component.html',
  styleUrl: './pedido-list.component.scss',
})
export class PedidoListComponent implements OnInit, AfterViewInit, OnDestroy {
  private subscription?: Subscription;

  @ViewChild(MatSort) sort!: MatSort;
  @ViewChild(MatPaginator) paginator!: MatPaginator;

  readonly displayedColumns = ['cliente', 'itens', 'peso', 'status', 'acoes'];
  readonly statusLabels = STATUS_LABELS;
  readonly limiteMaximo = LIMITE_MAXIMO_PEDIDOS;
  readonly StatusPedido = StatusPedido;
  readonly opcoesFiltroStatus: FiltroStatus[] = ['TODOS', ...Object.values(StatusPedido)];

  pedidos: Pedido[] = [];
  dataSource = new MatTableDataSource<Pedido>([]);
  carregando = true;
  apiIndisponivel = false;

  filtroStatus: FiltroStatus = 'TODOS';
  termoBusca = '';

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
      this.aplicarFiltros();
    });
  }

  ngAfterViewInit(): void {
    this.dataSource.sort = this.sort;
    this.dataSource.paginator = this.paginator;
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  aplicarFiltros(): void {
    const termo = this.termoBusca.trim().toLowerCase();
    this.dataSource.data = this.pedidos.filter(
      (pedido) =>
        (this.filtroStatus === 'TODOS' || pedido.status === this.filtroStatus) &&
        (!termo || pedido.displayName.toLowerCase().includes(termo)),
    );
    if (this.paginator) {
      this.paginator.firstPage();
    }
  }

  rotuloFiltroStatus(status: FiltroStatus): string {
    return status === 'TODOS' ? 'Todos os status' : this.statusLabels[status];
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
