import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, catchError, map, of, switchMap, timer } from 'rxjs';
import { environment } from '../../../environments/environment';

export type StatusApi = 'verificando' | 'up' | 'down';

/**
 * Consome /actuator/health de verdade (nao apenas infere disponibilidade a
 * partir de falhas em chamadas de negocio, como o PedidoService faz para o
 * fallback offline). Faz polling simples a cada 15s - o mesmo intervalo
 * documentado para o scrape do Prometheus - o suficiente para um indicador
 * de saude no topo da aplicacao, sem exigir WebSocket/SSE.
 */
@Injectable({ providedIn: 'root' })
export class HealthService {
  private readonly INTERVALO_MS = 15_000;

  private readonly statusSubject = new BehaviorSubject<StatusApi>('verificando');
  readonly status$ = this.statusSubject.asObservable();

  constructor(private http: HttpClient) {
    timer(0, this.INTERVALO_MS)
      .pipe(
        switchMap(() =>
          this.http.get<{ status: string }>(`${environment.actuatorUrl}/health`).pipe(
            map((resposta): StatusApi => (resposta.status === 'UP' ? 'up' : 'down')),
            catchError(() => of<StatusApi>('down')),
          ),
        ),
      )
      .subscribe((status) => this.statusSubject.next(status));
  }
}
