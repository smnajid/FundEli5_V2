import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, map, of, switchMap } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { AnswerControllerService, AnswerView, CheckRecordView, CheckView } from '../generated/api';

const VERDICT_LABEL: Record<NonNullable<CheckView['verdict']>, string> = {
  ELIGIBLE: 'Eligible',
  NOT_ELIGIBLE: 'Not eligible',
  NEEDS_REVIEW: 'Needs review',
};

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-dealer-screen',
  imports: [FormsModule],
  template: `
    <h1>Dealer screen</h1>

    <section>
      <label for="email">Requester's email</label>
      <textarea
        id="email"
        rows="8"
        [ngModel]="email()"
        (ngModelChange)="email.set($event)"
      ></textarea>
      <button type="button" [disabled]="busy()" (click)="answerIt()">Answer it</button>
    </section>

    @if (error(); as message) {
      <p role="alert">{{ message }}</p>
    }

    @if (result(); as r) {
      @if (r.refusal; as refusal) {
        <section class="refusal">
          <h2>No AI used for this email</h2>
          <p>{{ refusal.reason }}</p>
          @if (refusal.namesFound?.length) {
            <p>Found: {{ refusal.namesFound?.join(', ') }}</p>
          }
        </section>
      } @else {
        <p class="privacy-note">{{ r.screenNote }}</p>
      }

      @for (check of r.checks ?? []; track $index) {
        <section class="check" [attr.data-verdict]="check.verdict">
          <p>
            <code>{{ check.portfolioReference }}</code>
            <strong>{{ label(check) }}</strong>
            {{ check.shareClassName }}
          </p>
          <ul>
            @for (o of check.outcomes ?? []; track $index) {
              <li>{{ o.explanation }}</li>
            }
          </ul>
        </section>
      }

      @if (r.unclear?.length) {
        <section class="unclear">
          <h2>Could not read</h2>
          <ul>
            @for (u of r.unclear; track $index) {
              <li>{{ u }}</li>
            }
          </ul>
        </section>
      }

      @if (r.reply) {
        <section class="reply">
          <h2>Reply (drafted by AI, review before sending)</h2>
          <pre>{{ r.reply }}</pre>
        </section>
      }

      @if (recordIds().length) {
        <p class="check-records">
          Check Record:
          @for (id of recordIds(); track id) {
            <code>{{ id }}</code>
          }
        </p>
      }
    }
  `,
})
export class DealerScreen {
  private readonly api = inject(AnswerControllerService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly email = signal('');
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly result = signal<AnswerView | null>(null);
  protected readonly recordIds = signal<string[]>([]);

  protected label(check: CheckView): string {
    return check.verdict ? VERDICT_LABEL[check.verdict] : 'Unknown';
  }

  protected answerIt(): void {
    const text = this.email().trim();
    if (!text) return;
    this.busy.set(true);
    this.error.set(null);
    this.result.set(null);
    this.recordIds.set([]);
    // The answer carries no record id, so the ids are the records that appear because of this answer.
    this.api
      .checkRecords()
      .pipe(
        catchError(() => of(null)),
        switchMap((before) =>
          this.api.answer({ email: text }).pipe(
            switchMap((view) =>
              this.api.checkRecords().pipe(
                map((after) => ({ view, ids: newIds(before, after) })),
                catchError(() => of({ view, ids: null })),
              ),
            ),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: ({ view, ids }) => {
          this.result.set(view);
          if (ids === null)
            this.error.set('The answer is shown, but its Check Record id could not be loaded.');
          else this.recordIds.set(ids);
          this.busy.set(false);
        },
        error: () => {
          this.busy.set(false);
          this.error.set('Could not answer this email. Try again.');
        },
      });
  }
}

function newIds(before: CheckRecordView[] | null, after: CheckRecordView[]): string[] {
  const known = new Set((before ?? []).map((r) => r.id));
  return after.flatMap((r) => (r.id && !known.has(r.id) ? [r.id] : []));
}
