# Guía de Integración SSE con Angular

Esta guía explica cómo integrar Server-Sent Events (SSE) para recibir notificaciones en tiempo real en tu aplicación Angular.

## Endpoint SSE

- **URL:** `GET /api/notifications/stream`
- **Autenticación:** Token JWT (en query parameter `?token=...`)

## Problema con EventSource API nativa

La API nativa `EventSource` de JavaScript **NO permite** agregar headers personalizados como `Authorization`. Por eso, el backend acepta el token JWT como query parameter:

```
GET /api/notifications/stream?token=TU_JWT_TOKEN
```

## Opción 1: Usando EventSource nativo (Recomendado para desarrollo)

```typescript
// notification.service.ts
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private eventSource: EventSource | null = null;
  private baseUrl = 'http://localhost:8080/api/notifications/stream';

  connect(token: string): Observable<any> {
    return new Observable(observer => {
      // Cerrar conexión anterior si existe
      this.disconnect();

      // Crear nueva conexión con token en query param
      const url = `${this.baseUrl}?token=${encodeURIComponent(token)}`;
      this.eventSource = new EventSource(url);

      // Evento inicial de conexión
      this.eventSource.addEventListener('init', (event: any) => {
        console.log('SSE conectado:', event.data);
        observer.next({ type: 'init', data: event.data });
      });

      // Heartbeat (pings para mantener conexión)
      this.eventSource.addEventListener('ping', (event: any) => {
        console.log('SSE ping recibido:', event.data);
        observer.next({ type: 'ping', data: event.data });
      });

      // Notificaciones de cambio de estado de aplicación
      this.eventSource.addEventListener('application-status', (event: any) => {
        try {
          const data = JSON.parse(event.data);
          console.log('Cambio de estado recibido:', data);
          observer.next({ type: 'application-status', data });
        } catch (e) {
          console.error('Error al parsear evento SSE:', e);
        }
      });

      // Manejo de errores
      this.eventSource.onerror = (error) => {
        console.error('Error en SSE:', error);
        observer.error(error);
        
        // Si el error es crítico, intentar reconectar después de un delay
        if (this.eventSource?.readyState === EventSource.CLOSED) {
          setTimeout(() => this.connect(token), 5000);
        }
      };
    });
  }

  disconnect(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }
}
```

**Uso en componente:**

```typescript
// app.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { NotificationService } from './notification.service';
import { AuthService } from './auth.service'; // Tu servicio de autenticación

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html'
})
export class AppComponent implements OnInit, OnDestroy {
  private sseSubscription?: Subscription;

  constructor(
    private notificationService: NotificationService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const token = this.authService.getToken(); // Obtener JWT del servicio de auth
    
    if (token) {
      this.sseSubscription = this.notificationService.connect(token).subscribe({
        next: (event) => {
          if (event.type === 'application-status') {
            this.handleStatusChange(event.data);
          } else if (event.type === 'init') {
            console.log('SSE conectado exitosamente');
          }
        },
        error: (error) => {
          console.error('Error en SSE:', error);
        }
      });
    }
  }

  private handleStatusChange(data: any): void {
    console.log('Estado de aplicación cambió:', data);
    // data contiene:
    // - type: "APPLICATION_STATUS_CHANGED"
    // - applicationId: string
    // - oldStatus: string
    // - newStatus: string
    // - timestamp: string
    
    // Aquí puedes:
    // - Actualizar la UI
    // - Mostrar una notificación
    // - Refrescar datos de la aplicación
  }

  ngOnDestroy(): void {
    if (this.sseSubscription) {
      this.sseSubscription.unsubscribe();
    }
    this.notificationService.disconnect();
  }
}
```

## Opción 2: Usando HttpClient con Observable (Alternativa)

Si necesitas más control o prefieres usar HttpClient de Angular:

```typescript
// notification.service.ts (alternativa)
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private baseUrl = 'http://localhost:8080/api/notifications/stream';
  private notificationSubject = new Subject<any>();

  constructor(private http: HttpClient) {}

  connect(token: string): Observable<any> {
    const url = `${this.baseUrl}?token=${encodeURIComponent(token)}`;
    
    // Nota: HttpClient no soporta SSE directamente, necesitarías una librería
    // o usar EventSource como en la Opción 1
    
    return this.notificationSubject.asObservable();
  }

  // Para usar HttpClient necesitarías una librería como 'rxjs-sse' o similar
}
```

## Eventos SSE

El stream SSE envía los siguientes eventos:

### 1. `init` (inicial)
```
event: init
data: connected
```
Indica que la conexión SSE se estableció correctamente.

### 2. `ping` (heartbeat)
```
event: ping
data: keep-alive
```
Enviado cada 10 segundos para mantener la conexión activa. No requiere acción del cliente.

### 3. `application-status` (notificaciones)
```
event: application-status
data: {"type":"APPLICATION_STATUS_CHANGED","applicationId":"...","oldStatus":"PENDING","newStatus":"SUBMITTED","timestamp":"2024-01-15T10:30:00Z"}
```
Se envía cuando el estado de una solicitud de crédito cambia.

## Estructura del payload `application-status`

```typescript
interface ApplicationStatusChange {
  type: "APPLICATION_STATUS_CHANGED";
  applicationId: string;
  oldStatus: string;
  newStatus: string;
  timestamp: string; // ISO 8601
}
```

## Mejores prácticas

1. **Reconexión automática:** Si la conexión se cierra, intenta reconectar después de un delay.
2. **Manejo de errores:** Implementa manejo robusto de errores y logs.
3. **Desconexión:** Siempre cierra la conexión SSE cuando el componente se destruye (`ngOnDestroy`).
4. **Seguridad:** En producción, asegúrate de usar HTTPS y validar el origen del token.
5. **Rate limiting:** El backend limita automáticamente, pero evita crear múltiples conexiones simultáneas.

## Ejemplo completo con reconexión automática

```typescript
// notification.service.ts (versión mejorada)
import { Injectable } from '@angular/core';
import { Observable, Subject, timer } from 'rxjs';
import { switchMap, retry, delay } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private eventSource: EventSource | null = null;
  private baseUrl = 'http://localhost:8080/api/notifications/stream';
  private reconnectDelay = 5000; // 5 segundos
  private maxReconnectAttempts = 5;
  private reconnectAttempts = 0;

  connect(token: string): Observable<any> {
    return new Observable(observer => {
      const url = `${this.baseUrl}?token=${encodeURIComponent(token)}`;
      this.eventSource = new EventSource(url);

      this.eventSource.addEventListener('init', (event: any) => {
        this.reconnectAttempts = 0; // Resetear intentos en conexión exitosa
        observer.next({ type: 'init', data: event.data });
      });

      this.eventSource.addEventListener('ping', (event: any) => {
        observer.next({ type: 'ping', data: event.data });
      });

      this.eventSource.addEventListener('application-status', (event: any) => {
        try {
          const data = JSON.parse(event.data);
          observer.next({ type: 'application-status', data });
        } catch (e) {
          console.error('Error al parsear evento SSE:', e);
        }
      });

      this.eventSource.onerror = (error) => {
        console.error('Error en SSE:', error);
        
        if (this.eventSource?.readyState === EventSource.CLOSED) {
          if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            console.log(`Reintentando conexión SSE (intento ${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
            
            this.disconnect();
            setTimeout(() => this.connect(token), this.reconnectDelay);
          } else {
            observer.error(new Error('No se pudo reconectar SSE después de múltiples intentos'));
          }
        }
      };
    });
  }

  disconnect(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }
}
```

## Troubleshooting

### El stream se cierra inmediatamente
- Verifica que el token JWT sea válido y no esté expirado
- Revisa los logs del backend para errores de autenticación
- Asegúrate de que CORS esté configurado correctamente

### No se reciben eventos `application-status`
- Verifica que el `userId` en el JWT coincida con el dueño de la solicitud
- Revisa los logs del backend para confirmar que los eventos se están enviando
- Asegúrate de que el cambio de estado se está realizando correctamente

### Error CORS
- Verifica que el origen de Angular (`http://localhost:4200`) esté en la lista de orígenes permitidos en `SecurityConfig`
- En producción, ajusta los headers CORS según tu configuración

## Notas de seguridad

⚠️ **IMPORTANTE:** Aceptar el token en query parameter es práctico para desarrollo, pero en producción considera:
- Usar un endpoint alternativo que solo acepte token en query param (no en headers)
- Validar que solo se use en desarrollo (`@Profile("dev")`)
- O implementar un polyfill de EventSource que permita headers (librería externa)

Para producción, se recomienda usar una librería como `@sse-controller/angular` o implementar WebSocket como alternativa más segura.

