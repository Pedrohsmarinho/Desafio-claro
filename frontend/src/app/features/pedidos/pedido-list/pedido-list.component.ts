import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import {
  LIMITE_MAXIMO_PEDIDOS,
  Pedido,
  STATUS_LABELS,
  StatusPedido,
  podeTransicionar,
} from '../../../core/models/pedido.model';
import { PedidoService } from '../../../core/services/pedido.service';
import { extrairMensagemErro } from '../../../core/utils/http-error.util';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';

type FiltroStatus = StatusPedido | 'TODOS';

/** Mapeia o id da coluna ordenavel (mat-sort-header) para o nome do campo na entidade do backend. */
const CAMPO_ORDENACAO_BACKEND: Record<string, string> = {
  cliente: 'displayName',
  itens: 'itens',
  pesoKg: 'peso',
  status: 'status',
};

const DEBOUNCE_BUSCA_MS = 300;

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
export class PedidoListComponent implements OnInit, OnDestroy {
  private readonly subscriptions = new Subscription();
  private readonly termoBuscaSubject = new Subject<string>();

  readonly displayedColumns = ['cliente', 'itens', 'peso', 'status', 'acoes'];
  readonly statusLabels = STATUS_LABELS;
  /** Valor inicial seguro; atualizado para o real (app.pedidos.limite-maximo) assim que PedidoService o carrega. */
  limiteMaximo = LIMITE_MAXIMO_PEDIDOS;
  readonly StatusPedido = StatusPedido;
  readonly opcoesFiltroStatus: FiltroStatus[] = ['TODOS', ...Object.values(StatusPedido)];
  readonly pageSizeOptions = [5, 10, 25];

  /** Página atual retornada por GET /api/pedidos/busca - reflete filtro/paginação/ordenação vigentes. */
  pedidos: Pedido[] = [];
  totalElements = 0;
  carregando = true;

  /**
   * Total de pedidos do usuário sem nenhum filtro aplicado (via pedidos$, o
   * cache compartilhado com o dashboard/cadastro) - usado para o cabeçalho
   * ("X de 5 pedidos") e para habilitar/desabilitar o botão "Adicionar",
   * que precisa do limite real, não do total já filtrado pela busca/status.
   */
  totalPedidosUsuario = 0;
  apiIndisponivel = false;

  filtroStatus: FiltroStatus = 'TODOS';
  termoBusca = '';

  /** Vinculados a [pageIndex]/[pageSize] no template - o paginador so reflete a pagina/tamanho de verdade se esses bindings existirem e apontarem para campos reativos (nao uma expressao estatica como pageSizeOptions[0]). */
  pageIndex = 0;
  pageSize = this.pageSizeOptions[0];

  private sortActive = '';
  private sortDirection: 'asc' | 'desc' | '' = '';

  constructor(
    private pedidoService: PedidoService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.subscriptions.add(
      this.termoBuscaSubject.pipe(debounceTime(DEBOUNCE_BUSCA_MS), distinctUntilChanged()).subscribe(() => {
        this.pageIndex = 0;
        this.buscar();
      }),
    );

    this.subscriptions.add(
      this.pedidoService.pedidos$.subscribe((pedidos) => (this.totalPedidosUsuario = pedidos.length)),
    );
    this.subscriptions.add(
      this.pedidoService.apiDisponivel$.subscribe((disponivel) => (this.apiIndisponivel = !disponivel)),
    );
    this.subscriptions.add(
      this.pedidoService.limiteMaximo$.subscribe((limite) => (this.limiteMaximo = limite)),
    );

    this.pedidoService.carregar();
    this.buscar();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  onBuscaChange(): void {
    this.termoBuscaSubject.next(this.termoBusca);
  }

  onFiltroStatusChange(): void {
    this.pageIndex = 0;
    this.buscar();
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.buscar();
  }

  onSortChange(sort: Sort): void {
    this.sortActive = sort.active;
    this.sortDirection = sort.direction;
    this.pageIndex = 0;
    this.buscar();
  }

  rotuloFiltroStatus(status: FiltroStatus): string {
    return status === 'TODOS' ? 'Todos os status' : this.statusLabels[status];
  }

  podeAtingirLimite(): boolean {
    return this.totalPedidosUsuario >= this.limiteMaximo;
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
      next: () => {
        this.notificar(`Pedido "${pedido.displayName}" atualizado para ${this.statusLabels[novoStatus]}`);
        this.buscar();
      },
      error: (err) => this.notificar(extrairMensagemErro(err, 'Ocorreu um erro inesperado'), true),
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
        next: () => {
          this.notificar(`Pedido "${pedido.displayName}" excluído com sucesso`);
          this.buscar();
        },
        error: (err) => this.notificar(extrairMensagemErro(err, 'Ocorreu um erro inesperado'), true),
      });
    });
  }

  private buscar(): void {
    this.carregando = true;

    const status = this.filtroStatus === 'TODOS' ? null : this.filtroStatus;
    const campoBackend = this.sortActive ? CAMPO_ORDENACAO_BACKEND[this.sortActive] ?? this.sortActive : null;
    const sort = campoBackend && this.sortDirection ? `${campoBackend},${this.sortDirection}` : null;

    this.pedidoService
      .buscarPagina({
        status,
        busca: this.termoBusca || null,
        page: this.pageIndex,
        size: this.pageSize,
        sort,
      })
      .subscribe({
        next: (pagina) => {
          this.pedidos = pagina.content;
          this.totalElements = pagina.totalElements;
          this.carregando = false;
        },
        error: (err) => {
          this.carregando = false;
          this.pedidos = [];
          this.totalElements = 0;
          this.notificar(extrairMensagemErro(err, 'Ocorreu um erro inesperado'), true);
        },
      });
  }

  private notificar(mensagem: string, erro = false): void {
    this.snackBar.open(mensagem, 'Fechar', {
      duration: 4000,
      panelClass: erro ? 'snackbar-erro' : 'snackbar-sucesso',
    });
  }
}
