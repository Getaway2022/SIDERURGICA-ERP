import { Component } from '@angular/core';
import { NavbarComponent } from './components/navbar/navbar';
import { HeroComponent } from './components/hero/hero';
import { NosotrosComponent } from './components/nosotros/nosotros';
import { ProductosComponent } from './components/productos/productos';
import { ServiciosComponent } from './components/servicios/servicios';
import { StatsComponent } from './components/stats/stats';
import { ProcesoComponent } from './components/proceso/proceso';
import { ContactoComponent } from './components/contacto/contacto';
import { FooterComponent } from './components/footer/footer';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    NavbarComponent,
    HeroComponent,
    NosotrosComponent,
    ProductosComponent,
    ServiciosComponent,
    StatsComponent,
    ProcesoComponent,
    ContactoComponent,
    FooterComponent,
  ],
  template: `
    <app-navbar></app-navbar>
    <app-hero></app-hero>
    <app-nosotros></app-nosotros>
    <app-productos></app-productos>
    <app-servicios></app-servicios>
    <app-stats></app-stats>
    <app-proceso></app-proceso>
    <app-contacto></app-contacto>
    <app-footer></app-footer>
  `,
})
export class HomeComponent {}
