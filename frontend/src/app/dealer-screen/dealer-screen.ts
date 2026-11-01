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
    .check[data-verdict='ELIGIBLE'] strong {
      color: #1a7f37;
    }
    .check[data-verdict='NOT_ELIGIBLE'] strong {
      color: #cf222e;
    }
    .check[data-verdict='NEEDS_REVIEW'] strong {
      color: #9a6700;
    }
    .refusal {
      border: 2px solid #cf222e;
      background: #ffebe9;
      padding: 0.5rem 1rem;
    }
    .reply textarea {
      width: 100%;
      min-height: 16rem;
      font-size: 1.1rem;
    }
    .drawer {
      position: fixed;
      top: 0;
      right: 0;
      bottom: 0;
      width: min(40rem, 90vw);
      overflow: auto;
      background: #fff;
      border-left: 1px solid #999;
      padding: 1rem;
      box-shadow: -4px 0 12px #0003;
    }
    .drawer pre {
      white-space: pre-wrap;
      background: #f6f8fa;
      padding: 0.5rem;
    }
    table.request td,
    table.request th {
      padding: 0.15rem 0.5rem;
      text-align: left;
    }
  `,
  template: `
    <h1>Dealer screen</h1>

    @if (folded()) {
      <details class="request">
        <summary>
          Request from the pasted email ·
          <span class="privacy-note">{{ result()?.screenNote }}</span>
        </summary>
        <pre class="email">{{ email() }}</pre>
        <ng-container *ngTemplateOutlet="form" />
      </details>
    } @else {
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
    }

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
        <section class="manual">
          <h2>Enter the Subscription Request by hand</h2>
          <ng-container *ngTemplateOutlet="form" />
        </section>
      } @else if (!folded()) {
        <p class="privacy-note">{{ r.screenNote }}</p>
      }

      @for (check of r.checks ?? []; track $index) {
        <section class="check" [attr.data-verdict]="check.verdict">
          <p>
            <code>{{ check.portfolioReference }}</code>
            <strong>{{ phrase(check) }}</strong>
            {{ check.shareClassName }}.
            {{ decidingExplanation(check) }}
          </p>
          <span class="verdict">{{ label(check) }}</span>
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

      @if (r.reply !== undefined && r.reply !== null) {
        <section class="reply">
          <h2>
            {{ r.aiUsed ? 'Reply (drafted by AI, review before sending)' : 'Reply (template)' }}
          </h2>
          <textarea
            aria-label="Reply"
            [ngModel]="reply()"
            (ngModelChange)="reply.set($event)"
          ></textarea>
          <button type="button" class="copy" (click)="copyReply()">Copy reply</button>
          @if (copied()) {
            <span role="status">Copied</span>
          }
        </section>
      }

      @if (r.checks?.length) {
        <details class="justification">
          <summary>Full Justification</summary>
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

      <p class="check-records">
        @if (records().length) {
          Check Record:
          @for (rec of records(); track rec.id) {
            <code>{{ rec.id }}</code>
          }
        }
        @if (records().length) {
          <button type="button" class="saw" (click)="drawer.set(true)">What the AI saw</button>
        }
      </p>
    }

    @if (drawer()) {
      <aside class="drawer" aria-label="Check Record">
        <button type="button" class="close" (click)="drawer.set(false)">Close</button>
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
          this.rows.set(
            view.refusal ? [blankRow()] : (view.checks ?? []).map((c) => rowOf(c, true)),
          );
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
    const bodies = rows.map(toBody);
    if (!rows.length || bodies.some((b) => b === null)) {
      this.error.set('Fill in portfolio, Share Class, a positive amount and currency first.');
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    const corrections = this.corrections(rows);
    forkJoin([
      ...bodies.map((b) => this.api.answerByHand(b as HandRequestBody)),
      ...corrections.map((c) => c.send),
    ])
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (all) => {
          const views = all.slice(0, bodies.length) as AnswerView[];
          this.show({
            aiUsed: false,
            screenNote: 'no AI was used for this email',
            checks: views.flatMap((v) => v.checks ?? []),
            unclear: views.flatMap((v) => v.unclear ?? []),
            reply: views.map((v) => v.reply ?? '').join('\n\n'),
          });
          this.rows.update((rs) =>
            rs.map((r, i) => ({
              ...r,
              sent: {
                ...r.sent,
                ...Object.fromEntries(
                  corrections.filter((c) => c.row === i).map((c) => [c.field, c.value]),
                ),
              },
            })),
          );
          this.busy.set(false);
        },
        error: () => {
          this.busy.set(false);
          this.error.set('Could not re-check this request. Try again.');
        },
      });
  }

  private corrections(rows: FormRow[]) {
    return rows.flatMap((row, i) => {
      const parsed = row.parsed;
      const record = this.records().find(
        (r) =>
          r.portfolioReference === row.portfolioReference &&
          r.shareClassIsin === parsed?.shareClassIsin,
      );
      if (!parsed || !record?.id) return [];
      return EDITABLE.flatMap((field) => {
        const value = String(row[field] ?? '');
        if (value === parsed[field] || row.sent[field] === value) return [];
        return [
          {
            row: i,
            field,
            value,
            send: this.recordApi
              .correct(record.id as string, {
                field,
                parsedValue: parsed[field],
                dealerValue: value,
                by: 'dealer',
              })
              .pipe(catchError(() => of(null))),
          },
        ];
      });
    });
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

function rowOf(check: CheckView, fromAi: boolean): FormRow {
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
  if (fromAi) {
    row.parsed = {
      shareClassIsin: row.shareClassIsin,
      amount: String(row.amount ?? ''),
      currency: row.currency,
      clientInitiated: String(row.clientInitiated),
    };
  }
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
