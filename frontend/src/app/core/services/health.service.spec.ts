import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, discardPeriodicTasks, fakeAsync, tick } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { HealthService } from './health.service';

describe('HealthService', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('reporta "up" quando /actuator/health responde UP', fakeAsync(() => {
    const service = TestBed.inject(HealthService);
    tick();
    let ultimoStatus: string | undefined;
    service.status$.subscribe((status) => (ultimoStatus = status));

    httpMock.expectOne(`${environment.actuatorUrl}/health`).flush({ status: 'UP' });

    expect(ultimoStatus).toBe('up');
    discardPeriodicTasks();
  }));

  it('reporta "down" quando /actuator/health responde DOWN', fakeAsync(() => {
    const service = TestBed.inject(HealthService);
    tick();
    let ultimoStatus: string | undefined;
    service.status$.subscribe((status) => (ultimoStatus = status));

    httpMock.expectOne(`${environment.actuatorUrl}/health`).flush({ status: 'DOWN' });

    expect(ultimoStatus).toBe('down');
    discardPeriodicTasks();
  }));

  it('reporta "down" quando a chamada falha (API fora do ar)', fakeAsync(() => {
    const service = TestBed.inject(HealthService);
    tick();
    let ultimoStatus: string | undefined;
    service.status$.subscribe((status) => (ultimoStatus = status));

    httpMock.expectOne(`${environment.actuatorUrl}/health`).error(new ProgressEvent('network error'), {
      status: 0,
      statusText: 'Unknown Error',
    });

    expect(ultimoStatus).toBe('down');
    discardPeriodicTasks();
  }));

  it('faz polling periodico a cada 30 segundos', fakeAsync(() => {
    const service = TestBed.inject(HealthService);
    tick();

    httpMock.expectOne(`${environment.actuatorUrl}/health`).flush({ status: 'UP' });

    tick(30_000);
    httpMock.expectOne(`${environment.actuatorUrl}/health`).flush({ status: 'UP' });

    tick(30_000);
    httpMock.expectOne(`${environment.actuatorUrl}/health`).flush({ status: 'UP' });

    let ultimoStatus: string | undefined;
    service.status$.subscribe((status) => (ultimoStatus = status));
    expect(ultimoStatus).toBe('up');

    discardPeriodicTasks();
  }));
});
