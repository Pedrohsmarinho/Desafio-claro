import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DashboardMetricas } from '../models/pedido.model';

/**
 * Metricas do usuario autenticado para os graficos do dashboard (pedidos
 * por status, pedidos vs. limite maximo) - consulta GET /api/dashboard/metricas,
 * que o backend resolve com uma query escopada por usuarioId (nao le do
 * MeterRegistry do Micrometer, que so tem metricas globais - ver README).
 */
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly apiUrl = `${environment.apiUrl}/dashboard`;

  constructor(private http: HttpClient) {}

  buscarMetricas(): Observable<DashboardMetricas> {
    return this.http.get<DashboardMetricas>(`${this.apiUrl}/metricas`);
  }
}
