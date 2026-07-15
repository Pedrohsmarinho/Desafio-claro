import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { environment } from '../../../environments/environment';
import { DashboardMetricas, Pedido, StatusPedido } from '../../core/models/pedido.model';
import { DashboardService } from '../../core/services/dashboard.service';
import { PedidoService } from '../../core/services/pedido.service';
import { DashboardComponent } from './dashboard.component';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let httpMock: HttpTestingController;

  const pedidos: Pedido[] = [
    { id: 1, displayName: 'Pedido A', itens: 2, peso: 1000, pesoKg: 1, status: StatusPedido.EM_PROCESSAMENTO },
    { id: 2, displayName: 'Pedido B', itens: 3, peso: 2000, pesoKg: 2, status: StatusPedido.EM_PROCESSAMENTO },
    { id: 3, displayName: 'Pedido C', itens: 1, peso: 500, pesoKg: 0.5, status: StatusPedido.PAUSADO },
    { id: 4, displayName: 'Pedido D', itens: 4, peso: 1500, pesoKg: 1.5, status: StatusPedido.CANCELADO },
  ];

  const metricas: DashboardMetricas = {
    totalPedidos: 4,
    porStatus: {
      [StatusPedido.EM_PROCESSAMENTO]: 2,
      [StatusPedido.PAUSADO]: 1,
      [StatusPedido.CANCELADO]: 1,
    },
    limiteMaximo: 5,
  };

  const metricasUrl = `${environment.apiUrl}/dashboard/metricas`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideAnimations(), provideRouter([])],
    });

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    // PedidoService dispara um GET no proprio construtor, e o ngOnInit do
    // componente reage a esse mesmo Observable compartilhado (cards) e
    // dispara GET /api/dashboard/metricas separadamente (graficos).
    httpMock.match(`${environment.apiUrl}/pedidos`).forEach((req) => req.flush(pedidos));
    httpMock.expectOne(metricasUrl).flush(metricas);
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  it('calcula os cards de resumo a partir dos pedidos carregados', () => {
    expect(component.totalPedidos).toBe(4);
    expect(component.totalEmProcessamento).toBe(2);
    expect(component.pesoTotalKg).toBe(5);
    expect(component.itensTotais).toBe(10);
    expect(component.carregando).toBeFalse();
  });

  it('monta o grafico de barras a partir das metricas do backend (GET /api/dashboard/metricas)', () => {
    expect(component.barChartData.labels?.length).toBe(3);
    expect(component.barChartData.datasets[0].data).toEqual([2, 1, 1]);
  });

  it('monta o grafico de pizza com pedidos cadastrados vs. vagas restantes, a partir das metricas do backend', () => {
    expect(component.pieChartData.datasets[0].data).toEqual([4, 1]);
  });

  it('faz polling periodico recarregando os pedidos e as metricas', fakeAsync(() => {
    // o componente da instancia criada no beforeEach ja agendou seu timer de
    // polling fora da zona fakeAsync (tick() nao o rastreia); por isso essa
    // instancia e destruida e uma nova e criada aqui dentro, para que o
    // timer nasca dentro da zona controlada pelo tick().
    fixture.destroy();

    const pedidoService = TestBed.inject(PedidoService);
    const dashboardService = TestBed.inject(DashboardService);
    const carregarSpy = spyOn(pedidoService, 'carregar').and.callThrough();
    const metricasSpy = spyOn(dashboardService, 'buscarMetricas').and.callThrough();

    const pollFixture = TestBed.createComponent(DashboardComponent);
    pollFixture.detectChanges();
    httpMock.match(`${environment.apiUrl}/pedidos`).forEach((req) => req.flush(pedidos));
    httpMock.expectOne(metricasUrl).flush(metricas);
    carregarSpy.calls.reset();
    metricasSpy.calls.reset();

    tick(20_000);
    expect(carregarSpy).toHaveBeenCalledTimes(1);
    expect(metricasSpy).toHaveBeenCalledTimes(1);
    httpMock.match(`${environment.apiUrl}/pedidos`).forEach((req) => req.flush(pedidos));
    httpMock.expectOne(metricasUrl).flush(metricas);

    pollFixture.destroy();
  }));
});
