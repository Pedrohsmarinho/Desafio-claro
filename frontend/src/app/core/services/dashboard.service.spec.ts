import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { DashboardMetricas, StatusPedido } from '../models/pedido.model';
import { DashboardService } from './dashboard.service';

describe('DashboardService', () => {
  let service: DashboardService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DashboardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('busca as metricas do usuario autenticado em GET /api/dashboard/metricas', () => {
    const metricas: DashboardMetricas = {
      totalPedidos: 3,
      porStatus: {
        [StatusPedido.EM_PROCESSAMENTO]: 1,
        [StatusPedido.PAUSADO]: 1,
        [StatusPedido.CANCELADO]: 1,
      },
      limiteMaximo: 5,
    };

    let resultado: DashboardMetricas | undefined;
    service.buscarMetricas().subscribe((r) => (resultado = r));

    const req = httpMock.expectOne(`${environment.apiUrl}/dashboard/metricas`);
    expect(req.request.method).toBe('GET');
    req.flush(metricas);

    expect(resultado).toEqual(metricas);
  });
});
