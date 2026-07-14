import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, catchError, map, of, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  LIMITE_MAXIMO_PEDIDOS,
  Pedido,
  PedidoCreateRequest,
  StatusPedido,
  podeTransicionar,
} from '../models/pedido.model';

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

  constructor(private http: HttpClient) {
    this.carregar();
  }

  carregar(): void {
    this.buscarTodos().subscribe((pedidos) => this.pedidosSubject.next(pedidos));
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
        if (totalConhecido >= LIMITE_MAXIMO_PEDIDOS) {
          return throwError(() => new Error(
            `Limite maximo de ${LIMITE_MAXIMO_PEDIDOS} pedidos cadastrados foi atingido`,
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
