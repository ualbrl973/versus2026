import { Injectable, inject } from '@angular/core';
import { Client, IFrame, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { BehaviorSubject, Observable, filter, switchMap } from 'rxjs';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class WebSocketService {
  private readonly auth = inject(AuthService);
  private client?: Client;
  private readonly _connected$ = new BehaviorSubject<boolean>(false);
  readonly connected$ = this._connected$.asObservable();

  get isConnected(): boolean {
    return this._connected$.value;
  }

  connect(): void {
    if (this.client?.active) return;
    const token = this.auth.getAccessToken();
    if (!token) {
      console.warn('[WS] No auth token; skipping connect');
      return;
    }
    this.client = new Client({
      webSocketFactory: () => new SockJS(environment.wsUrl) as unknown as WebSocket,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      heartbeatIncoming: 10_000,
      heartbeatOutgoing: 10_000,
      onConnect: () => this._connected$.next(true),
      onDisconnect: () => this._connected$.next(false),
      onWebSocketClose: () => this._connected$.next(false),
      onStompError: (frame: IFrame) => {
        console.error('[WS] STOMP error', frame.headers['message'], frame.body);
        this._connected$.next(false);
      },
    });
    this.client.activate();
  }

  disconnect(): void {
    this.client?.deactivate();
    this.client = undefined;
    this._connected$.next(false);
  }

  subscribe<T>(destination: string): Observable<T> {
    return this._connected$.pipe(
      filter((c) => c),
      switchMap(
        () =>
          new Observable<T>((sub) => {
            const subscription = this.client!.subscribe(destination, (msg: IMessage) => {
              try {
                sub.next(JSON.parse(msg.body) as T);
              } catch {
                sub.next(msg.body as unknown as T);
              }
            });
            return () => subscription.unsubscribe();
          }),
      ),
    );
  }

  publish<T>(destination: string, body?: T): void {
    if (!this.client?.connected) {
      console.warn(`[WS] publish dropped (not connected): ${destination}`);
      return;
    }
    this.client.publish({
      destination,
      body: body == null ? '' : JSON.stringify(body),
    });
  }
}
