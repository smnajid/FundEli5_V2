import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, Subscription } from 'rxjs';
import {
  ConditionView,
  DraftSummary,
  RuleCardView,
  RuleView,
  ReviewView,
  ValidationControllerService,
} from '../generated/api';

type Kind = ConditionView['kind'];
type Field =
  | 'allowed'
  | 'excluded'
  | 'definition'
  | 'excludesUsCitizens'
  | 'amount'
  | 'currency'
  | 'manager'
  | 'kidAvailable'
  | 'dimension'
  | 'value';

/** The editable value fields of each rule kind (an Unmodelled Restriction is marked, not edited). */
const FIELDS: Record<Kind, Field[]> = {
  investor_category: ['allowed', 'excluded'],
  us_person_exclusion: ['definition', 'excludesUsCitizens'],
  minimum_initial_subscription: ['amount', 'currency'],
  mandate_only_class: ['manager'],
  kid_required_for_retail: ['kidAvailable'],
  target_market: ['dimension', 'value'],
  unmodelled_restriction: [],
};

const KIND_LABEL: Record<Kind, string> = {
  investor_category: 'Investor category',
  us_person_exclusion: 'US Person exclusion',
  minimum_initial_subscription: 'Minimum initial subscription',
  mandate_only_class: 'Mandate-only class',
  kid_required_for_retail: 'KID required for retail',
  target_market: 'Target Market',
  unmodelled_restriction: 'Unmodelled Restriction',
};

const LIST_FIELDS: Field[] = ['allowed', 'excluded'];
const BOOLEAN_FIELDS: Field[] = ['excludesUsCitizens', 'kidAvailable'];

/** Core rule kinds a Share Class should have a rule for; a missing one is "possibly missed". */
const CORE: { kind: Kind; label: string }[] = [
  { kind: 'investor_category', label: 'investor category' },
  { kind: 'us_person_exclusion', label: 'US Person exclusion' },
  { kind: 'minimum_initial_subscription', label: 'minimum' },
];

type Status = 'open' | 'accepted' | 'corrected' | 'contradiction';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-validation-screen',
  imports: [FormsModule],
  styles: `
    .banner {
      border: 2px solid #cf222e;
      background: #ffebe9;
      padding: 0.5rem 1rem;
    }
    .missed {
      border: 2px solid #9a6700;
      background: #fff8c5;
      padding: 0.5rem 1rem;
      margin-top: 0.5rem;
    }
    .columns {
      display: grid;
      grid-template-columns: 1fr 1.4fr 1.2fr;
      gap: 1rem;
      margin-top: 1rem;
    }
    .rules button.rule {
      display: block;
      width: 100%;
      text-align: left;
      margin-bottom: 0.25rem;
    }
    .rules button.rule[aria-current='true'] {
      outline: 2px solid #0969da;
    }
    .status-open {
      color: #9a6700;
    }
    .status-accepted,
    .status-corrected {
      color: #1a7f37;
    }
    .status-contradiction {
      color: #cf222e;
      font-weight: bold;
    }
    .quote {
      white-space: pre-wrap;
      background: #f6f8fa;
      padding: 0.5rem;
    }
    .quote mark {
      background: #ffe066;
    }
    .signal {
      color: #cf222e;
    }
    .header.validated {
      color: #1a7f37;
    }
  `,
  template: `
    <h1>Rule Card validation screen</h1>

    @if (error(); as message) {
      <p role="alert">{{ message }}</p>
    }

    <label for="validator">Validator</label>
    <input id="validator" [ngModel]="validator()" (ngModelChange)="validator.set($event)" />

    <label for="draft">Draft card</label>
    <select id="draft" [ngModel]="selectedDraft()" (ngModelChange)="pick($event)">
      <option value="">Pick a Draft card…</option>
      @for (d of drafts(); track $index) {
        <option [value]="d.fundName + '|' + d.version">
          {{ d.fundName }} · Draft v{{ d.version }}
        </option>
      }
    </select>
    @if (drafts().length === 0 && !card()) {
      <p class="no-drafts">No Draft card is waiting for validation.</p>
    }

    @if (card(); as c) {
      <section class="header" [class.validated]="c.status === 'validated'">
        <h2>{{ c.fundName }}</h2>
        <p>
          {{ c.shareClasses?.join(', ') }}
          @if (c.status === 'validated') {
            ·
            <strong class="validated-line"
              >Validated v{{ c.version }} by {{ c.validation?.validator }} at
              {{ c.validation?.validatedAt }} · in force for Eligibility Checks</strong
            >
          } @else {
            · Draft v{{ c.version }} · drafted {{ c.drafting?.draftedAt }} ·
            <span class="verified">{{
              quotesVerified()
                ? 'every quote verified on its page'
                : 'some quotes are not found on their page'
            }}</span>
            · <span class="ready">{{ ready() }} of {{ rules().length }} rules ready</span>
          }
        </p>
        @if (c.status !== 'validated') {
          <button
            type="button"
            class="validate"
            [disabled]="!review()?.canValidate || busy()"
            (click)="validate()"
          >
            Validate card
          </button>
          @if (!review()?.canValidate) {
            <ul class="blockers">
              @for (b of review()?.blockers ?? []; track b) {
                <li>{{ b }}</li>
              }
            </ul>
          }
        }
      </section>

      @if ((review()?.contradictions ?? []).length > 0) {
        <section class="banner" role="alert">
          <strong>{{ review()?.contradictions?.length }} contradiction(s) need a decision.</strong>
          @for (k of review()?.contradictions ?? []; track $index) {
            <div class="contradiction">
              <p>
                {{ kindLabel(k.kind) }} of {{ k.shareClass ?? 'the fund' }}:
                @for (o of k.options ?? []; track o.rule) {
                  the {{ o.document }} says "{{ o.summary }}";
                }
              </p>
              @for (o of k.options ?? []; track o.rule) {
                <button
                  type="button"
                  class="follow"
                  [disabled]="busy()"
                  (click)="follow(k.shareClass, k.kind, o.document)"
                >
                  Follow {{ o.document }}
                </button>
              }
            </div>
          }
        </section>
      }

      @for (m of missed(); track m) {
        <section class="missed">Possibly missed: {{ m }}</section>
      }

      <div class="columns">
        <nav class="rules" aria-label="Rules">
          <h3>Rules</h3>
          @for (r of rules(); track $index) {
            <button
              type="button"
              class="rule"
              [attr.aria-current]="$index === index()"
              (click)="select($index)"
            >
              <span class="kind">{{ kindLabel(r.condition?.kind) }}</span>
              <span class="value">{{ valueOf(r) }}</span>
              <span class="scope">{{
                r.scope === 'share_class' ? r.shareClass : 'all classes'
              }}</span>
              <span [class]="'status status-' + statusOf($index)">{{ statusLabel($index) }}</span>
            </button>
          }
        </nav>

        <section class="document">
          <h3>Document</h3>
          @if (rule(); as r) {
            @if ((r.citations ?? []).length === 0) {
              <p class="signal">No supporting quote for this value.</p>
            }
            <div role="tablist">
              @for (cit of r.citations ?? []; track $index) {
                <button
                  type="button"
                  role="tab"
                  [attr.aria-selected]="$index === tab()"
                  (click)="tab.set($index)"
                >
                  {{ cit.document }} p.{{ cit.page }}
                </button>
              }
            </div>
            @if (citation(); as cit) {
              <p>{{ cit.document }}, page {{ cit.page }}</p>
              <pre class="quote"><mark>{{ cit.quote }}</mark></pre>
              @if (!cit.quoteFoundOnPage) {
                <p class="signal">Quote not found on its page.</p>
              }
            }
          }
        </section>

        <section class="decision">
          <h3>Decision</h3>
          @if (rule(); as r) {
            <p class="summary">{{ r.summary }}</p>
            <ul class="signals">
              @for (s of ruleSignals(); track s) {
                <li class="signal">{{ signalLabel(s) }}</li>
              }
              @if (r.contradiction) {
                <li class="signal">
                  Contradiction between documents: choose the governing source in the banner.
                </li>
              }
            </ul>
            @if (c.status !== 'validated') {
              @if (r.condition?.kind === 'unmodelled_restriction') {
                <p>
                  Restriction is {{ r.condition?.status
                  }}{{ r.condition?.notedReason ? ': ' + r.condition?.notedReason : '' }}
                </p>
                <button
                  type="button"
                  class="blocking"
                  [disabled]="busy()"
                  (click)="mark('blocking')"
                >
                  Blocking
                </button>
                <label for="reason">Reason it sets no condition on the client</label>
                <input id="reason" [ngModel]="reason()" (ngModelChange)="reason.set($event)" />
                <button
                  type="button"
                  class="noted"
                  [disabled]="busy() || !reason().trim()"
                  (click)="mark('noted')"
                >
                  Noted
                </button>
              } @else {
                @for (f of fields(); track f) {
                  <label [for]="'f-' + f">{{ f }}</label>
                  @if (isBoolean(f)) {
                    <input
                      type="checkbox"
                      [id]="'f-' + f"
                      [ngModel]="edit()[f]"
                      (ngModelChange)="setField(f, $event)"
                    />
                  } @else {
                    <input
                      [id]="'f-' + f"
                      [ngModel]="edit()[f]"
                      (ngModelChange)="setField(f, $event)"
                    />
                  }
                }
                <div>
                  @if (changed()) {
                    <button type="button" class="correct" [disabled]="busy()" (click)="correct()">
                      Save correction
                    </button>
                  } @else {
                    <button type="button" class="accept" [disabled]="busy()" (click)="accept()">
                      Accept
                    </button>
                  }
                </div>
              }
            }
          }
        </section>
      </div>
    }
  `,
})
export class ValidationScreen {
  private readonly api = inject(ValidationControllerService);
  private readonly destroyRef = inject(DestroyRef);
  /** The one request in flight: a new call cancels it, so a late answer never lands on another card. */
  private inFlight?: Subscription;

  protected readonly drafts = signal<DraftSummary[]>([]);
  protected readonly selectedDraft = signal('');
  protected readonly review = signal<ReviewView | null>(null);
  /** A card just validated, shown in place of the review. */
  private readonly validatedCard = signal<RuleCardView | null>(null);
  protected readonly index = signal(0);
  protected readonly tab = signal(0);
  protected readonly validator = signal('validator');
  protected readonly reason = signal('');
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly edit = signal<Partial<Record<Field, string | boolean>>>({});

  protected readonly card = computed(() => this.validatedCard() ?? this.review()?.card ?? null);
  protected readonly rules = computed<RuleView[]>(() => this.card()?.rules ?? []);
  protected readonly rule = computed(() => this.rules()[this.index()] ?? null);
  protected readonly citation = computed(() => this.rule()?.citations?.[this.tab()] ?? null);
  protected readonly fields = computed(() => {
    const kind = this.rule()?.condition?.kind;
    return kind ? FIELDS[kind] : [];
  });
  protected readonly ready = computed(
    () =>
      this.rules().filter(
        (_, i) => this.statusOf(i) !== 'open' && this.statusOf(i) !== 'contradiction',
      ).length,
  );
  protected readonly quotesVerified = computed(() =>
    this.rules().every((r) => (r.citations ?? []).every((c) => c.quoteFoundOnPage)),
  );
  protected readonly ruleSignals = computed(() =>
    (this.review()?.signals ?? []).filter((s) => s.rule === this.index()).map((s) => s.type ?? ''),
  );
  protected readonly changed = computed(() => {
    const original = this.originalFields();
    const now = this.edit();
    return this.fields().some((f) => original[f] !== now[f]);
  });
  /** Core rule kinds with no rule for a Share Class (a fund-level rule counts for every class). */
  protected readonly missed = computed(() => {
    const c = this.card();
    if (!c || c.status === 'validated') return [];
    const out: string[] = [];
    for (const sc of c.shareClasses ?? []) {
      for (const core of CORE) {
        const has = this.rules().some(
          (r) => r.condition?.kind === core.kind && (r.scope === 'fund' || r.shareClass === sc),
        );
        if (!has)
          out.push(`No ${core.label} found for ${sc}: add the rule or confirm there is none.`);
      }
    }
    return out;
  });

  constructor() {
    this.api
      .drafts()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (d) => this.drafts.set(d), error: (e) => this.fail(e) });
  }

  protected pick(value: string): void {
    this.selectedDraft.set(value);
    this.validatedCard.set(null);
    this.review.set(null);
    this.inFlight?.unsubscribe();
    if (!value) {
      this.busy.set(false);
      return;
    }
    const at = value.lastIndexOf('|');
    this.run(this.api.review(value.slice(0, at), Number(value.slice(at + 1))), true);
  }

  protected select(i: number): void {
    this.index.set(i);
    this.tab.set(0);
    this.loadEdit();
  }

  protected accept(): void {
    this.act(
      this.api.accept({ ...this.target(), rule: this.index(), validator: this.validator() }),
    );
  }

  protected correct(): void {
    const kind = this.rule()?.condition?.kind;
    if (!kind) return;
    const e = this.edit();
    if (
      this.fields().includes('amount') &&
      (String(e.amount ?? '').trim() === '' || Number.isNaN(Number(e.amount)))
    ) {
      this.error.set('The amount must be a number.');
      return;
    }
    const condition: ConditionView = { kind };
    for (const f of this.fields()) {
      const v = e[f];
      if (LIST_FIELDS.includes(f)) {
        (condition as Record<string, unknown>)[f] = String(v ?? '')
          .split(',')
          .map((s) => s.trim())
          .filter(Boolean);
      } else if (f === 'amount') {
        condition.amount = Number(v);
      } else {
        (condition as Record<string, unknown>)[f] = v;
      }
    }
    this.act(
      this.api.correctRule({
        ...this.target(),
        rule: this.index(),
        condition,
        validator: this.validator(),
      }),
    );
  }

  protected mark(status: 'blocking' | 'noted'): void {
    this.act(
      this.api.mark({
        ...this.target(),
        rule: this.index(),
        status,
        reason: status === 'noted' ? this.reason() : undefined,
        validator: this.validator(),
      }),
    );
  }

  protected follow(
    shareClass: string | undefined,
    kind: string | undefined,
    document: string | undefined,
  ): void {
    this.act(
      this.api.follow({
        ...this.target(),
        shareClass,
        kind,
        document,
        validator: this.validator(),
      }),
    );
  }

  protected validate(): void {
    this.busy.set(true);
    this.error.set(null);
    this.api
      .validate({ ...this.target(), validator: this.validator() })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (c) => {
          this.busy.set(false);
          this.validatedCard.set(c);
          this.drafts.update((d) =>
            d.filter((x) => x.fundName !== c.fundName || x.version !== c.version),
          );
        },
        error: (e) => this.fail(e),
      });
  }

  /** After a decision: jump to the next open rule. */
  private act(call: Observable<ReviewView>): void {
    this.run(call, false);
  }

  private run(call: Observable<ReviewView>, first: boolean): void {
    this.busy.set(true);
    this.error.set(null);
    const previous = this.index();
    this.inFlight?.unsubscribe();
    this.inFlight = call.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (r) => {
        this.busy.set(false);
        this.review.set(r);
        this.reason.set('');
        this.index.set(first ? 0 : this.nextOpen(r, previous));
        this.tab.set(0);
        this.loadEdit();
      },
      error: (e) => this.fail(e),
    });
  }

  private nextOpen(r: ReviewView, after: number): number {
    const open = r.unreviewedRules ?? [];
    return open.find((i) => i > after) ?? open[0] ?? after;
  }

  private target(): { fundName: string; version: number } {
    const c = this.card();
    return { fundName: c?.fundName ?? '', version: c?.version ?? 0 };
  }

  protected statusOf(i: number): Status {
    const r = this.rules()[i];
    if (r?.contradiction) return 'contradiction';
    if (!(this.review()?.unreviewedRules ?? []).includes(i)) {
      const last = r?.history?.[r.history.length - 1]?.action ?? '';
      return last.startsWith('corrected') || last.startsWith('marked') ? 'corrected' : 'accepted';
    }
    return 'open';
  }

  protected statusLabel(i: number): string {
    const s = this.statusOf(i);
    return s === 'contradiction' ? '!' : s;
  }

  protected kindLabel(kind: string | undefined): string {
    const k = (kind ?? '').toLowerCase() as Kind;
    return KIND_LABEL[k] ?? kind ?? '';
  }

  protected valueOf(r: RuleView): string {
    const c = r.condition;
    if (!c) return '';
    const parts = FIELDS[c.kind].map((f) => c[f]).filter((v) => v !== undefined && v !== null);
    if (c.kind === 'unmodelled_restriction') return c.status ?? '';
    return parts.map((v) => (Array.isArray(v) ? v.join(', ') : String(v))).join(' ');
  }

  protected signalLabel(type: string): string {
    return type === 'QUOTE_NOT_FOUND_ON_PAGE'
      ? 'Quote not found on its cited page'
      : 'Value with no supporting quote';
  }

  protected isBoolean(f: Field): boolean {
    return BOOLEAN_FIELDS.includes(f);
  }

  protected setField(f: Field, value: string | boolean): void {
    this.edit.update((e) => ({ ...e, [f]: value }));
  }

  private originalFields(): Partial<Record<Field, string | boolean>> {
    const c = this.rule()?.condition;
    const out: Partial<Record<Field, string | boolean>> = {};
    if (!c) return out;
    for (const f of FIELDS[c.kind]) {
      const v = c[f];
      out[f] = Array.isArray(v)
        ? v.join(', ')
        : typeof v === 'boolean'
          ? v
          : v === undefined
            ? ''
            : String(v);
    }
    return out;
  }

  private loadEdit(): void {
    this.edit.set(this.originalFields());
  }

  private fail(e: unknown): void {
    this.busy.set(false);
    const body = e instanceof HttpErrorResponse ? e.error : null;
    this.error.set(
      Array.isArray(body)
        ? body.join('; ')
        : typeof body === 'string' && body
          ? body
          : 'The request failed.',
    );
  }
}
