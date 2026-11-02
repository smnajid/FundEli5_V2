import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { catchError, forkJoin, map, of, switchMap } from 'rxjs';
import {
  AnswerControllerService,
  AnswerView,
  CheckRecordControllerService,
  CheckRecordView,
  CheckView,
  HandRequestBody,
} from '../generated/api';

type Verdict = NonNullable<CheckView['verdict']>;

const VERDICT_LABEL: Record<Verdict, string> = {
  ELIGIBLE: 'Eligible',
  NOT_ELIGIBLE: 'Not eligible',
  NEEDS_REVIEW: 'Needs review',
};

const VERDICT_PHRASE: Record<Verdict, string> = {
  ELIGIBLE: 'can buy',
  NOT_ELIGIBLE: 'cannot buy',
  NEEDS_REVIEW: 'needs review for',
};

const EDITABLE = ['shareClassIsin', 'amount', 'currency', 'clientInitiated'] as const;
type EditableField = (typeof EDITABLE)[number];

/** One row of the parsed request form. The Portfolio Reference is never editable. */
interface FormRow {
  portfolioReference: string;
  shareClassIsin: string;
  shareClassName: string;
  amount: number | null;
  currency: string;
  clientInitiated: boolean;
  /** What the AI parsed, kept to tell a dealer's correction from the AI's reading. */
  parsed: Record<EditableField, string> | null;
  /** Corrections already sent to the Check Record, by field, so a re-check does not repeat them. */
  sent: Partial<Record<EditableField, string>>;
}

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-dealer-screen',
  imports: [FormsModule, NgTemplateOutlet],
  styles: `
    :host {
      display: grid;
      gap: var(--s-3);
    }
  `,
  template: `
    <h1 class="sr-only">Dealer screen</h1>

    @if (folded()) {
      <details class="request card">
        <summary>
          Request from the pasted email ·
          <span class="privacy-note">✓ {{ result()?.screenNote }}</span>
        </summary>
        <pre class="email">{{ email() }}</pre>
        <ng-container *ngTemplateOutlet="form" />
        <button type="button" class="another link" (click)="another()">Answer another email</button>
      </details>
    } @else {
      <section class="card">
        <label for="email">Requester's email</label>
        <textarea
          id="email"
          rows="8"
          [ngModel]="email()"
          (ngModelChange)="email.set($event)"
        ></textarea>
        <button type="button" class="answer-it" [disabled]="busy()" (click)="answerIt()">
          Answer it
        </button>
      </section>
    }

    @if (error(); as message) {
      <p role="alert" class="card refusal">{{ message }}</p>
    }

    @if (result(); as r) {
      @if (r.refusal; as refusal) {
        <section class="refusal card">
          <h2>No AI used for this email</h2>
          <p>{{ refusal.reason }}</p>
          @if (refusal.namesFound?.length) {
            <p>Found: {{ refusal.namesFound?.join(', ') }}</p>
          }
        </section>
      } @else if (!folded()) {
        <p class="privacy-note card">✓ {{ r.screenNote }}</p>
      }
      @if (!folded() && rows().length) {
        <section class="manual card">
          <h2>Enter the Subscription Request by hand</h2>
          <ng-container *ngTemplateOutlet="form" />
        </section>
      }

      @for (check of r.checks ?? []; track $index) {
        <section class="check card" [attr.data-verdict]="check.verdict">
          <p>
            <code>{{ check.portfolioReference }}</code>
            <strong>{{ phrase(check) }}</strong>
            {{ check.shareClassName }}.
            <span class="reason">{{ decidingExplanation(check) }}</span>
          </p>
          <span class="verdict">{{ label(check) }}</span>
        </section>
      }

      @if (r.unclear?.length) {
        <section class="unclear card">
          <h2>Could not read</h2>
          <ul>
            @for (u of r.unclear; track $index) {
              <li>{{ u }}</li>
            }
          </ul>
        </section>
      }

      <div class="answer">
        @if (r.reply !== undefined && r.reply !== null) {
          <section class="reply card">
            <div class="reply-head">
              <h2 class="ai-label">
                {{
                  r.aiUsed
                    ? '✦ Drafted by AI, edit before sending'
                    : 'Reply template, edit before sending'
                }}
              </h2>
              <button type="button" class="link edit" (click)="replyBox.focus()">Edit</button>
              <button type="button" class="copy primary" (click)="copyReply()">Copy reply</button>
            </div>
            <textarea
              #replyBox
              aria-label="Reply"
              [ngModel]="reply()"
              (ngModelChange)="editReply($event)"
            ></textarea>
            @if (copied()) {
              <span role="status">Copied</span>
            }
          </section>
        }
        <aside class="rail" aria-label="Evidence">
          <!-- room for the evidence rail (ticket: Citations on outcomes) -->
          @if (records().length) {
            <button type="button" class="saw" (click)="drawer.set(true)">
              What the AI saw (Check Record)
            </button>
          }
        </aside>
      </div>

      @if (r.checks?.length) {
        <details class="justification card">
          <summary>
            Full Justification <span class="muted">· every rule of every check</span>
          </summary>
          @for (check of r.checks; track $index) {
            <h3>
              <code>{{ check.portfolioReference }}</code> · {{ check.shareClassName }} ·
              {{ label(check) }}
            </h3>
            <ul>
              @for (o of check.outcomes ?? []; track $index) {
                <li>
                  <strong>{{ o.kind }}</strong> {{ o.result }}: {{ o.explanation }}
                </li>
              }
            </ul>
          }
        </details>
      }

      @if (records().length) {
        <p class="check-records muted">
          Check Record:
          @for (rec of records(); track rec.id) {
            <code>{{ rec.id }}</code>
          }
        </p>
      }
    }

    @if (drawer()) {
      <aside class="drawer" aria-label="Check Record">
        <button type="button" class="close link" (click)="drawer.set(false)">Close</button>
        <h2>What the AI saw</h2>
        @for (rec of records(); track rec.id) {
          <article class="record">
            <h3>
              <code>{{ rec.id }}</code> · {{ rec.portfolioReference }} · {{ rec.shareClassIsin }}
            </h3>
            <p>
              Verdict {{ rec.verdict }} · Rule Card v{{ rec.ruleCardVersion }} · Scale Mapping
              {{ rec.scaleMappingVersion }} · engine {{ rec.engineVersion }}
            </p>
            @if (rec.clientFieldsRead?.length) {
              <p>Client fields read: {{ rec.clientFieldsRead?.join(', ') }}</p>
            }
            @for (c of rec.llmCalls ?? []; track $index) {
              <h4>LLM call · {{ c.model }}</h4>
              <p>Fields sent: {{ c.fieldsSent?.join(', ') || 'none' }}</p>
              <pre class="prompt">{{ c.gatedPrompt }}</pre>
              <pre class="response">{{ c.response }}</pre>
            } @empty {
              <p>No LLM call for this check.</p>
            }
            @for (c of rec.corrections ?? []; track $index) {
              <p class="correction">
                Dealer corrected {{ c.field }}: {{ c.parsedValue }} → {{ c.dealerValue }} ({{
                  c.by
                }}, {{ c.at }})
              </p>
            }
          </article>
        }
      </aside>
    }

    <ng-template #form>
      <table class="request">
        <thead>
          <tr>
            <th>Portfolio</th>
            <th>Share Class (ISIN)</th>
            <th>Amount</th>
            <th>Currency</th>
            <th>Client initiated</th>
          </tr>
        </thead>
        <tbody>
          @for (row of rows(); track $index; let i = $index) {
            <tr>
              <td>
                @if (row.parsed) {
                  <code>{{ row.portfolioReference }}</code>
                } @else {
                  <input
                    aria-label="Portfolio Reference"
                    [ngModel]="row.portfolioReference"
                    (ngModelChange)="edit(i, 'portfolioReference', $event)"
                  />
                }
              </td>
              <td>
                <input
                  aria-label="Share Class ISIN"
                  [ngModel]="row.shareClassIsin"
                  (ngModelChange)="edit(i, 'shareClassIsin', $event)"
                />
                <small>{{ row.shareClassName }}</small>
              </td>
              <td>
                <input
                  type="number"
                  aria-label="Amount"
                  [ngModel]="row.amount"
                  (ngModelChange)="edit(i, 'amount', $event)"
                />
              </td>
              <td>
                <input
                  aria-label="Currency"
                  maxlength="3"
                  [ngModel]="row.currency"
                  (ngModelChange)="edit(i, 'currency', $event)"
                />
              </td>
              <td>
                <input
                  type="checkbox"
                  aria-label="Client initiated"
                  [ngModel]="row.clientInitiated"
                  (ngModelChange)="edit(i, 'clientInitiated', $event)"
                />
              </td>
            </tr>
          }
        </tbody>
      </table>
      <button type="button" class="recheck" [disabled]="busy()" (click)="recheck()">
        Re-check
      </button>
    </ng-template>
  `,
})
export class DealerScreen {
  private readonly api = inject(AnswerControllerService);
  private readonly recordApi = inject(CheckRecordControllerService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly email = signal('');
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly result = signal<AnswerView | null>(null);
  protected readonly records = signal<CheckRecordView[]>([]);
  protected readonly rows = signal<FormRow[]>([]);
  protected readonly reply = signal('');
  protected readonly copied = signal(false);
  protected readonly drawer = signal(false);
  protected readonly folded = signal(false);

  protected label(check: CheckView): string {
    return check.verdict ? VERDICT_LABEL[check.verdict] : 'Unknown';
  }

  protected phrase(check: CheckView): string {
    return check.verdict ? VERDICT_PHRASE[check.verdict] : 'unknown for';
  }

  /** The sentence's reason: the first outcome that is not a pass. */
  protected decidingExplanation(check: CheckView): string {
    const o = (check.outcomes ?? []).find((x) => x.result !== 'PASS') ?? check.outcomes?.[0];
    return o?.explanation ?? '';
  }

  protected edit<K extends keyof FormRow>(index: number, field: K, value: FormRow[K]): void {
    this.rows.update((rows) => rows.map((r, i) => (i === index ? { ...r, [field]: value } : r)));
  }

  protected another(): void {
    this.folded.set(false);
    this.email.set('');
    this.result.set(null);
    this.rows.set([]);
    this.records.set([]);
    this.drawer.set(false);
    this.error.set(null);
  }

  protected editReply(text: string): void {
    this.reply.set(text);
    this.copied.set(false);
  }

  protected async copyReply(): Promise<void> {
    try {
      await navigator.clipboard.writeText(this.reply());
      this.copied.set(true);
    } catch {
      this.error.set('Could not copy the reply. Select the text and copy it by hand.');
    }
  }

  protected answerIt(): void {
    const text = this.email().trim();
    if (!text) return;
    this.busy.set(true);
    this.error.set(null);
    this.result.set(null);
    this.records.set([]);
    this.rows.set([]);
    this.drawer.set(false);
    // The answer carries no record id, so the records are the ones that appear because of this answer.
    this.api
      .checkRecords()
      .pipe(
        catchError(() => of(null)),
        switchMap((before) =>
          this.api.answer({ email: text }).pipe(
            switchMap((view) =>
              this.api.checkRecords().pipe(
                map((after) => ({ view, records: newRecords(before, after) })),
                catchError(() => of({ view, records: null })),
              ),
            ),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: ({ view, records }) => {
          this.show(view);
          this.rows.set(view.refusal ? [blankRow()] : (view.checks ?? []).map((c) => rowOf(c)));
          this.folded.set(!view.refusal && (view.checks?.length ?? 0) > 0);
          if (records === null)
            this.error.set('The answer is shown, but its Check Record id could not be loaded.');
          else this.records.set(records);
          this.busy.set(false);
        },
        error: () => {
          this.busy.set(false);
          this.error.set('Could not answer this email. Try again.');
        },
      });
  }

  /** Runs the (edited) form rows again through the by-hand route and records what the dealer changed. */
  protected recheck(): void {
    const rows = this.rows();
    const bodies = rows.map(toBody).filter((x): x is HandRequestBody => x !== null);
    if (!rows.length || bodies.length !== rows.length) {
      this.error.set('Fill in portfolio, Share Class, a positive amount and currency first.');
      return;
    }
    const corrections = this.corrections(rows);
    if (corrections === null) {
      this.error.set(
        'The Check Record of this answer is not loaded, so your corrections cannot be recorded. Answer the email again.',
      );
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    const sends = corrections.length
      ? forkJoin(corrections.map((c) => c.send))
      : of([] as boolean[]);
    forkJoin([forkJoin(bodies.map((b) => this.api.answerByHand(b))), sends])
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ([views, ok]) => {
          const untouched = this.reply() === (this.result()?.reply ?? '');
          const view: AnswerView = {
            aiUsed: false,
            screenNote: 'no AI was used for this email',
            checks: views.flatMap((v) => v.checks ?? []),
            unclear: views.flatMap((v) => v.unclear ?? []),
            reply: views.map((v) => v.reply ?? '').join('\n\n'),
          };
          const keep = this.reply();
          this.show(view);
          if (!untouched && keep) this.reply.set(keep);
          this.rows.update((rs) =>
            rs.map((r, i) => ({
              ...r,
              sent: {
                ...r.sent,
                ...Object.fromEntries(
                  corrections.filter((c, k) => c.row === i && ok[k]).map((c) => [c.field, c.value]),
                ),
              },
            })),
          );
          if (ok.some((x) => !x))
            this.error.set('Some corrections could not be recorded. Re-check again to retry them.');
          this.busy.set(false);
        },
        error: () => {
          this.busy.set(false);
          this.error.set('Could not re-check this request. Try again.');
        },
      });
  }

  /** Null when a correction is due but its Check Record is not loaded. */
  private corrections(rows: FormRow[]) {
    let missing = false;
    const list = rows.flatMap((row, i) => {
      const parsed = row.parsed;
      const record = this.records().find(
        (r) =>
          r.portfolioReference === row.portfolioReference &&
          r.shareClassIsin === parsed?.shareClassIsin,
      );
      if (!parsed) return [];
      return EDITABLE.flatMap((field) => {
        const value = String(row[field] ?? '');
        if (value === parsed[field] || row.sent[field] === value) return [];
        const id = record?.id;
        if (!id) {
          missing = true;
          return [];
        }
        return [
          {
            row: i,
            field,
            value,
            send: this.recordApi
              .correct(id, {
                field,
                parsedValue: parsed[field],
                dealerValue: value,
                by: 'dealer',
              })
              .pipe(
                map(() => true),
                catchError(() => of(false)),
              ),
          },
        ];
      });
    });
    return missing ? null : list;
  }

  private show(view: AnswerView): void {
    this.result.set(view);
    this.reply.set(view.reply ?? '');
    this.copied.set(false);
  }
}

function blankRow(): FormRow {
  return {
    portfolioReference: '',
    shareClassIsin: '',
    shareClassName: '',
    amount: null,
    currency: 'EUR',
    clientInitiated: false,
    parsed: null,
    sent: {},
  };
}

function rowOf(check: CheckView): FormRow {
  const row: FormRow = {
    portfolioReference: check.portfolioReference ?? '',
    shareClassIsin: check.shareClassIsin ?? '',
    shareClassName: check.shareClassName ?? '',
    amount: check.amount ?? null,
    currency: check.currency ?? '',
    clientInitiated: check.clientInitiated ?? false,
    parsed: null,
    sent: {},
  };
  row.parsed = {
    shareClassIsin: row.shareClassIsin,
    amount: String(row.amount ?? ''),
    currency: row.currency,
    clientInitiated: String(row.clientInitiated),
  };
  return row;
}

function toBody(row: FormRow): HandRequestBody | null {
  if (!row.portfolioReference || !row.shareClassIsin || !row.currency) return null;
  if (row.amount === null || !(Number(row.amount) > 0)) return null;
  return {
    portfolioReference: row.portfolioReference,
    shareClassIsin: row.shareClassIsin.trim(),
    amount: Number(row.amount),
    currency: row.currency.trim().toUpperCase(),
    clientInitiated: row.clientInitiated,
  };
}

function newRecords(before: CheckRecordView[] | null, after: CheckRecordView[]): CheckRecordView[] {
  const known = new Set((before ?? []).map((r) => r.id));
  return after.filter((r) => r.id && !known.has(r.id));
}
