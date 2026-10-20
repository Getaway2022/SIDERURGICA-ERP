import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { vi } from 'vitest';
import { ReposicionesComponent } from './reposiciones';
import { environment } from '../../../../environments/environment';

describe('Reposiciones', () => {
  let fixture: ComponentFixture<ReposicionesComponent>;
  let http: HttpTestingController;
  const base = environment.apiUrl;
  const solicitud = {
    id: 1,
    inventarioId: 2,
    producto: 'Acero',
    cantidad: 12,
    estado: 'PENDIENTE',
    mensaje: 'Stock disponible 8',
    creadaEn: '2026-09-23',
  };
  function cargar() {
    http.expectOne(`${base}/reposiciones`).flush([solicitud]);
    http.expectOne(`${base}/proveedor`).flush([{ id: 3, nombre: 'Proveedor', estado: 'Activo' }]);
  }
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ReposicionesComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    fixture = TestBed.createComponent(ReposicionesComponent);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    cargar();
  });
  afterEach(() => {
    fixture.destroy();
    http.verify();
    vi.restoreAllMocks();
  });

  it('muestra el aviso y exige proveedor y precio antes de habilitar aprobación', async () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Stock disponible 8');
    const boton: HTMLButtonElement = fixture.nativeElement.querySelector('.btn-primary');
    expect(boton.disabled).toBe(true);
    const select: HTMLSelectElement = fixture.nativeElement.querySelector('select');
    select.selectedIndex = 1;
    select.dispatchEvent(new Event('change'));
    const precio: HTMLInputElement = fixture.nativeElement.querySelector('input');
    precio.value = '10';
    precio.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    expect(boton.disabled).toBe(false);
  });
  it('no aprueba si el responsable cancela la confirmación', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    fixture.componentInstance.aprobar(solicitud);
    http.expectNone(`${base}/reposiciones/1/aprobar`);
  });
  it('envía la aprobación una sola vez mientras está pendiente y actualiza la bandeja', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const component = fixture.componentInstance;
    component.proveedor[1] = 3;
    component.precio[1] = 10;
    component.aprobar(solicitud);
    component.aprobar(solicitud);
    const request = http.expectOne(`${base}/reposiciones/1/aprobar`);
    expect(request.request.body).toEqual({
      proveedorId: 3,
      precioUnitario: 10,
      cantidadEsperada: 12,
    });
    request.flush({ ...solicitud, estado: 'APROBADA', mensaje: 'Orden creada' });
    cargar();
    expect(component.ocupado).toBe(false);
    expect(component.mensaje).toBe('Orden creada');
  });
  it('muestra un fallo de presupuesto sin simular aprobación', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const component = fixture.componentInstance;
    component.aprobar(solicitud);
    http
      .expectOne(`${base}/reposiciones/1/aprobar`)
      .flush({ error: 'Capital insuficiente' }, { status: 400, statusText: 'Bad Request' });
    expect(component.mensaje).toBe('Capital insuficiente');
    expect(component.solicitudes[0].estado).toBe('PENDIENTE');
    expect(component.ocupado).toBe(false);
  });
});
