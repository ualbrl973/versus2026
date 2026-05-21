import { Component, EventEmitter, Input, Output, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'vs-numeric-input',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './numeric-input.html',
  styleUrl: './numeric-input.scss',
})
export class NumericInputComponent {
  @Input() unit: string | null = null;
  @Input() placeholder = 'Tu estimación';
  @Input() disabled = false;
  /**
   * Hint visual de ruido (sabotaje OBFUSCATION en modo duelo numérico).
   * Si está presente, el campo muestra una etiqueta `±N%`. NO altera el valor enviado.
   */
  @Input() noiseHint: number | null = null;
  @Input() submitLabel = 'ENVIAR';

  @Output() submitted = new EventEmitter<number>();

  readonly value = signal<string>('');
  readonly canSubmit = computed(() => {
    if (this.disabled) return false;
    const raw = this.value().replace(',', '.').trim();
    if (!raw) return false;
    const n = Number(raw);
    return Number.isFinite(n);
  });

  submit(): void {
    if (!this.canSubmit()) return;
    const raw = this.value().replace(',', '.').trim();
    const n = Number(raw);
    this.submitted.emit(n);
  }

  reset(): void {
    this.value.set('');
  }
}
