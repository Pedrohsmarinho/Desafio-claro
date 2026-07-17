import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideAnimations } from '@angular/platform-browser/animations';
import { Router, provideRouter } from '@angular/router';
import { environment } from '../../../../environments/environment';
import { Pedido, StatusPedido } from '../../../core/models/pedido.model';
import { PedidoService } from '../../../core/services/pedido.service';
import { PedidoFormComponent } from './pedido-form.component';

describe('PedidoFormComponent', () => {
  let component: PedidoFormComponent;
  let fixture: ComponentFixture<PedidoFormComponent>;
  let httpMock: HttpTestingController;
  let router: Router;

  const flushCargaInicial = (pedidos: Pedido[]) => {
    httpMock.match(`${environment.apiUrl}/pedidos`).forEach((req) => req.flush(pedidos));
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PedidoFormComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideAnimations(), provideRouter([])],
    });

    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
    fixture = TestBed.createComponent(PedidoFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    flushCargaInicial([]);
    httpMock.match(`${environment.apiUrl}/dashboard/metricas`).forEach((req) =>
      req.flush({ totalPedidos: 0, porStatus: {}, limiteMaximo: 5 }),
    );
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  it('comeca com o formulario invalido e sem limite atingido', () => {
    expect(component.form.invalid).toBeTrue();
    expect(component.limiteAtingido).toBeFalse();
  });

  it('reporta nome do cliente curto demais', () => {
    component.form.setValue({ displayName: 'Ana', pesoKg: 1, itens: 2 });

    expect(component.form.get('displayName')?.hasError('minlength')).toBeTrue();
    expect(component.form.invalid).toBeTrue();
  });

  it('reporta itens nao inteiros', () => {
    component.form.setValue({ displayName: 'Cliente Valido', pesoKg: 1, itens: 1.5 });

    expect(component.form.get('itens')?.hasError('pattern')).toBeTrue();
  });

  it('nao envia quando o limite de 5 pedidos ja foi atingido', () => {
    const cincoPedidos: Pedido[] = Array.from({ length: 5 }, (_, i) => ({
      id: i + 1,
      displayName: `Pedido ${i}`,
      itens: 1,
      peso: 500,
      pesoKg: 0.5,
      status: StatusPedido.EM_PROCESSAMENTO,
    }));
    TestBed.inject(PedidoService).carregar();
    httpMock.expectOne(`${environment.apiUrl}/pedidos`).flush(cincoPedidos);
    fixture.detectChanges();

    component.form.setValue({ displayName: 'Cliente Valido', pesoKg: 1, itens: 2 });
    component.salvar();

    expect(component.limiteAtingido).toBeTrue();
    httpMock.expectNone((req) => req.method === 'POST');
  });

  it('converte o peso de kg para gramas ao enviar', () => {
    component.form.setValue({ displayName: 'Cliente Valido', pesoKg: 1.5, itens: 2 });

    component.salvar();

    const req = httpMock.expectOne(`${environment.apiUrl}/pedidos`);
    expect(req.request.body.peso).toBe(1500);
    req.flush({ id: 10, displayName: 'Cliente Valido', itens: 2, peso: 1500, pesoKg: 1.5, status: StatusPedido.EM_PROCESSAMENTO });
    flushCargaInicial([]);
  });

  it('navega para a listagem apos cadastrar com sucesso', () => {
    component.form.setValue({ displayName: 'Cliente Valido', pesoKg: 1, itens: 2 });

    component.salvar();

    httpMock
      .expectOne(`${environment.apiUrl}/pedidos`)
      .flush({ id: 10, displayName: 'Cliente Valido', itens: 2, peso: 1000, pesoKg: 1, status: StatusPedido.EM_PROCESSAMENTO });
    flushCargaInicial([]);

    expect(router.navigate).toHaveBeenCalledWith(['/pedidos']);
    expect(component.enviando).toBeFalse();
  });

  it('exibe a mensagem de erro da API quando a criacao falha por regra de negocio', () => {
    component.form.setValue({ displayName: 'Cliente Valido', pesoKg: 1, itens: 2 });

    component.salvar();

    httpMock
      .expectOne(`${environment.apiUrl}/pedidos`)
      .flush({ message: 'Limite maximo de 5 pedidos cadastrados foi atingido' }, { status: 422, statusText: 'Unprocessable Entity' });

    expect(component.erroEnvio).toBe('Limite maximo de 5 pedidos cadastrados foi atingido');
    expect(component.enviando).toBeFalse();
  });
});
