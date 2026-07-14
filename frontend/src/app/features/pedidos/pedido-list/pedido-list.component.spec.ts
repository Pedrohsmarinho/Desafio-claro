import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { environment } from '../../../../environments/environment';
import { Pedido, StatusPedido } from '../../../core/models/pedido.model';
import { PedidoListComponent } from './pedido-list.component';

describe('PedidoListComponent - filtros', () => {
  let component: PedidoListComponent;
  let fixture: ComponentFixture<PedidoListComponent>;
  let httpMock: HttpTestingController;

  const pedidos: Pedido[] = [
    { id: 1, displayName: 'Pedido João Silva', itens: 1, peso: 500, pesoKg: 0.5, status: StatusPedido.EM_PROCESSAMENTO },
    { id: 2, displayName: 'Pedido Maria Souza', itens: 1, peso: 500, pesoKg: 0.5, status: StatusPedido.PAUSADO },
    { id: 3, displayName: 'Pedido Carlos Lima', itens: 1, peso: 500, pesoKg: 0.5, status: StatusPedido.CANCELADO },
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PedidoListComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideAnimations(), provideRouter([])],
    });

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(PedidoListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    // PedidoService dispara um GET no proprio construtor, e o ngOnInit do
    // componente chama carregar() de novo - por isso pode haver mais de uma
    // requisicao pendente nesse ponto.
    httpMock.match(`${environment.apiUrl}/pedidos`).forEach((req) => req.flush(pedidos));
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  it('mostra todos os pedidos sem filtro aplicado', () => {
    expect(component.dataSource.data.length).toBe(3);
  });

  it('filtra por status', () => {
    component.filtroStatus = StatusPedido.PAUSADO;
    component.aplicarFiltros();

    expect(component.dataSource.data.length).toBe(1);
    expect(component.dataSource.data[0].displayName).toBe('Pedido Maria Souza');
  });

  it('filtra por termo de busca (case-insensitive)', () => {
    component.termoBusca = 'CARLOS';
    component.aplicarFiltros();

    expect(component.dataSource.data.length).toBe(1);
    expect(component.dataSource.data[0].displayName).toBe('Pedido Carlos Lima');
  });

  it('combina filtro de status e busca', () => {
    component.filtroStatus = StatusPedido.CANCELADO;
    component.termoBusca = 'joão';
    component.aplicarFiltros();

    expect(component.dataSource.data.length).toBe(0);
  });
});
