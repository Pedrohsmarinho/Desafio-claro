import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, catchError, map, of, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  FiltroPedidos,
  LIMITE_MAXIMO_PEDIDOS,
  PaginaPedidos,
  Pedido,
  PedidoCreateRequest,
  StatusPedido,
  podeTransicionar,
} from '../models/pedido.model';
import { DashboardService } from './dashboard.service';

const FALLBACK_KEY = 'pedidos_fallback_local';

/**
 * Alem do CRUD contra a API, mantem um cache compartilhado (pedidos$) usado
 * pela listagem, pelo dashboard e pelo formulario de cadastro (para checar o
 * limite de 5 sem duplicar chamadas), e implementa o fallback em LocalStorage
 * exigido para o cadastro quando a API esta indisponivel.
 */
@Injectable({ providedIn: 'root' })
export class PedidoService {
  private readonly apiUrl = `${environment.apiUrl}/pedidos`;

  private readonly pedidosSubject = new BehaviorSubject<Pedido[]>([]);
  readonly pedidos$ = this.pedidosSubject.asObservable();

  /** false quando o ultimo carregamento falhou (API indisponivel) - usado para exibir aviso na listagem/dashboard. */
  private readonly apiDisponivelSubject = new BehaviorSubject<boolean>(true);
  readonly apiDisponivel$ = this.apiDisponivelSubject.asObservable();

  /**
   * LIMITE_MAXIMO_PEDIDOS e usado so como valor inicial seguro, ate a
   * primeira resposta de GET /api/dashboard/metricas chegar - dali em diante,
   * este e o valor real configurado no backend (app.pedidos.limite-maximo),
   * usado tanto para a checagem de limite no fallback offline (criar())
   * quanto para o texto exibido nas telas (via limiteMaximo$).
   */
  private readonly limiteMaximoSubject = new BehaviorSubject<number>(LIMITE_MAXIMO_PEDIDOS);
  readonly limiteMaximo$ = this.limiteMaximoSubject.asObservable();

  constructor(
    private http: HttpClient,
    private dashboardService: DashboardService,
  ) {
    this.carregar();
    this.dashboardService.buscarMetricas().subscribe((metricas) => this.limiteMaximoSubject.next(metricas.limiteMaximo));
  }

  carregar(): void {
    this.buscarTodos().subscribe((pedidos) => this.pedidosSubject.next(pedidos));
  }

  /**
   * Filtro (status/nome), paginacao e ordenacao resolvidos no backend
   * (GET /api/pedidos/busca) - usado pela listagem, que dispara uma nova
   * requisicao a cada mudanca de filtro/pagina/coluna ordenada, em vez de
   * carregar tudo uma vez e filtrar no navegador (como pedidos$/buscarTodos
   * ainda fazem, para o dashboard e a checagem de limite no cadastro).
   */
  buscarPagina(filtro: FiltroPedidos): Observable<PaginaPedidos> {
    let params = new HttpParams().set('page', filtro.page).set('size', filtro.size);
    if (filtro.status) {
      params = params.set('status', filtro.status);
    }
    if (filtro.busca) {
      params = params.set('busca', filtro.busca);
    }
    if (filtro.sort) {
      params = params.set('sort', filtro.sort);
    }

    return this.http.get<PaginaPedidos>(`${this.apiUrl}/busca`, { params });
  }

  buscarTodos(): Observable<Pedido[]> {
    return this.http.get<Pedido[]>(this.apiUrl).pipe(
      map((pedidos) => {
        this.apiDisponivelSubject.next(true);
        return [...pedidos, ...this.lerFallbackLocal()];
      }),
      catchError(() => {
        this.apiDisponivelSubject.next(false);
        return of(this.lerFallbackLocal());
      }),
    );
  }

  criar(request: PedidoCreateRequest): Observable<Pedido> {
    return this.http.post<Pedido>(this.apiUrl, request).pipe(
      tap(() => this.carregar()),
      catchError((error: HttpErrorResponse) => {
        // status 0 = falha de rede/conexao (API fora do ar); demais status
        // (400/422) sao respostas reais da API e devem ser propagados para
        // que o formulario exiba a mensagem de validacao/regra de negocio.
        if (error.status !== 0) {
          return throwError(() => error);
        }

        const totalConhecido = this.pedidosSubject.value.length;
        const limiteMaximo = this.limiteMaximoSubject.value;
        if (totalConhecido >= limiteMaximo) {
          return throwError(() => new Error(
            `Limite maximo de ${limiteMaximo} pedidos cadastrados foi atingido`,
          ));
        }

        const pedidoLocal = this.criarLocal(request);
        this.carregar();
        return of(pedidoLocal);
      }),
    );
  }

  alterarStatus(pedido: Pedido, novoStatus: StatusPedido): Observable<Pedido> {
    if (this.ehLocal(pedido.id)) {
      try {
        const atualizado = this.atualizarStatusLocal(pedido.id, novoStatus);
        this.carregar();
        return of(atualizado);
      } catch (err) {
        return throwError(() => err);
      }
    }

    return this.http
      .patch<Pedido>(`${this.apiUrl}/${pedido.id}/status`, { status: novoStatus })
      .pipe(tap(() => this.carregar()));
  }

  excluir(pedido: Pedido): Observable<void> {
    if (this.ehLocal(pedido.id)) {
      this.removerLocal(pedido.id);
      this.carregar();
      return of(void 0);
    }

    return this.http.delete<void>(`${this.apiUrl}/${pedido.id}`).pipe(tap(() => this.carregar()));
  }

  private ehLocal(id: number | string): boolean {
    return typeof id === 'string' && id.startsWith('local-');
  }

  private lerFallbackLocal(): Pedido[] {
    const raw = localStorage.getItem(FALLBACK_KEY);
    return raw ? (JSON.parse(raw) as Pedido[]) : [];
  }

  private salvarFallbackLocal(pedidos: Pedido[]): void {
    localStorage.setItem(FALLBACK_KEY, JSON.stringify(pedidos));
  }

  private criarLocal(request: PedidoCreateRequest): Pedido {
    const novo: Pedido = {
      id: `local-${Date.now()}`,
      displayName: request.displayName,
      itens: request.itens,
      peso: request.peso,
      pesoKg: request.peso / 1000,
      status: StatusPedido.EM_PROCESSAMENTO,
      origemLocal: true,
    };

    const atuais = this.lerFallbackLocal();
    atuais.push(novo);
    this.salvarFallbackLocal(atuais);
    return novo;
  }

  private atualizarStatusLocal(id: number | string, novoStatus: StatusPedido): Pedido {
    const atuais = this.lerFallbackLocal();
    const pedido = atuais.find((p) => p.id === id);
    if (!pedido) {
      throw new Error('Pedido local nao encontrado');
    }
    if (!podeTransicionar(pedido.status, novoStatus)) {
      throw new Error(`Transicao invalida de ${pedido.status} para ${novoStatus}`);
    }
    pedido.status = novoStatus;
    this.salvarFallbackLocal(atuais);
    return pedido;
  }

  private removerLocal(id: number | string): void {
    const atuais = this.lerFallbackLocal().filter((p) => p.id !== id);
    this.salvarFallbackLocal(atuais);
  }
}
