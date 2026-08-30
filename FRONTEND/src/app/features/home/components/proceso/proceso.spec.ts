import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProcesoComponent } from './proceso';

describe('Proceso', () => {
  let component: ProcesoComponent;
  let fixture: ComponentFixture<ProcesoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProcesoComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ProcesoComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
