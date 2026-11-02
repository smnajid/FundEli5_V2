import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { ReviewView, RuleCardView, ValidationControllerService } from '../generated/api';
import { ValidationScreen } from './validation-screen';

const card: RuleCardView = {
  fundName: 'Nordea 1 GCE',
  shareClasses: ['LU1'],
  version: 1,
  status: 'draft',
  drafting: { draftedAt: '2026-06-30T10:00:00Z' },
  rules: [
    {
      scope: 'share_class',
      shareClass: 'LU1',
      condition: { kind: 'investor_category', allowed: ['institutional'], excluded: [] },
      citations: [
        {
          document: 'prospectus',
          page: 123,
          quote: 'institutional investors only',
          quoteFoundOnPage: true,
        },
      ],
      summary: 'Institutional only',
      contradiction: true,
      history: [{ action: 'drafted' }],
    },
    {
      scope: 'share_class',
      shareClass: 'LU1',
      condition: { kind: 'minimum_initial_subscription', amount: 75000, currency: 'EUR' },
      citations: [
        { document: 'prospectus', page: 5, quote: 'EUR 75,000 minimum', quoteFoundOnPage: false },
      ],
      summary: 'Minimum 75,000',
      contradiction: false,
      history: [{ action: 'drafted' }],
    },
    {
      scope: 'fund',
      condition: { kind: 'unmodelled_restriction', status: 'blocking' },
      citations: [],
      summary: 'Reject or close',
      contradiction: false,
      history: [{ action: 'drafted' }],
    },
  ],
};

const blocked: ReviewView = {
  card,
  contradictions: [
    {
      shareClass: 'LU1',
      kind: 'INVESTOR_CATEGORY',
      options: [
        { rule: 0, document: 'prospectus', summary: 'institutional only' },
        { rule: 3, document: 'kid', summary: 'all investors' },
      ],
    },
  ],
  unreviewedRules: [0, 1, 2],
  signals: [
    { rule: 1, type: 'QUOTE_NOT_FOUND_ON_PAGE' },
    { rule: 2, type: 'NO_SUPPORTING_QUOTE' },
  ],
  canValidate: false,
  blockers: ['contradiction on INVESTOR_CATEGORY of LU1: choose a document'],
};

function setup(api: Record<string, unknown>) {
  TestBed.configureTestingModule({
    providers: [
      {
        provide: ValidationControllerService,
        useValue: {
          drafts: () => of([{ fundName: 'Nordea 1 GCE', version: 1, shareClasses: ['LU1'] }]),
          review: () => of(blocked),
          ...api,
        },
      },
    ],
  });
  const fixture = TestBed.createComponent(ValidationScreen);
  fixture.detectChanges();
  return fixture;
}

async function open(fixture: ReturnType<typeof setup>) {
  const el: HTMLElement = fixture.nativeElement;
  const select = el.querySelector('select') as HTMLSelectElement;
  select.value = select.options[1].value;
  select.dispatchEvent(new Event('change'));
  await fixture.whenStable();
  fixture.detectChanges();
  return el;
}

const button = (el: HTMLElement, cls: string) =>
  el.querySelector(`button.${cls}`) as HTMLButtonElement;

describe('ValidationScreen', () => {
  it('leads with the contradiction banner and cannot validate yet', async () => {
    const el = await open(setup({}));
    expect(el.querySelector('.banner')?.textContent).toContain(
      'prospectus says "institutional only"',
    );
    expect(el.querySelector('.banner')?.textContent).toContain('kid says "all investors"');
    expect(button(el, 'validate').disabled).toBe(true);
    expect(el.querySelector('.ready')?.textContent).toContain('0 of 3 rules ready');
  });

  it('shows the cited quote highlighted and the checks the code ran', async () => {
    const fixture = setup({});
    const el = await open(fixture);
    expect(el.querySelector('.quote mark')?.textContent).toBe('institutional investors only');
    (el.querySelectorAll('button.rule')[1] as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(el.querySelector('.document')?.textContent).toContain('Quote not found on its page');
    expect(el.querySelector('.signals')?.textContent).toContain(
      'Quote not found on its cited page',
    );
    expect(el.querySelector('.verified')?.textContent).toContain('some quotes are not found');
  });

  it('warns of a core rule kind with no rule for a Share Class', async () => {
    const el = await open(setup({}));
    expect(el.querySelector('.missed')?.textContent).toContain(
      'No US Person exclusion found for LU1',
    );
  });

  it('settles a contradiction by following a document', async () => {
    const follow = vi.fn(() => of({ ...blocked, contradictions: [] }));
    const fixture = setup({ follow });
    const el = await open(fixture);
    (el.querySelectorAll('button.follow')[0] as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(follow).toHaveBeenCalledWith(
      expect.objectContaining({
        shareClass: 'LU1',
        kind: 'INVESTOR_CATEGORY',
        document: 'prospectus',
        validator: 'validator',
      }),
    );
    expect(el.querySelector('.banner')).toBeNull();
  });

  it('accepts an unchanged rule and jumps to the next open one', async () => {
    const accept = vi.fn(() => of({ ...blocked, unreviewedRules: [1, 2] }));
    const fixture = setup({ accept });
    const el = await open(fixture);
    button(el, 'accept').click();
    fixture.detectChanges();
    expect(accept).toHaveBeenCalledWith(
      expect.objectContaining({ rule: 0, fundName: 'Nordea 1 GCE', version: 1 }),
    );
    expect(el.querySelectorAll('button.rule')[1].getAttribute('aria-current')).toBe('true');
  });

  it('saves a correction once the value is edited', async () => {
    const correctRule = vi.fn(() => of({ ...blocked, unreviewedRules: [0, 2] }));
    const fixture = setup({ correctRule });
    const el = await open(fixture);
    (el.querySelectorAll('button.rule')[1] as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(button(el, 'correct')).toBeNull();
    const amount = el.querySelector('#f-amount') as HTMLInputElement;
    amount.value = '80000';
    amount.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    button(el, 'correct').click();
    expect(correctRule).toHaveBeenCalledWith(
      expect.objectContaining({
        rule: 1,
        condition: { kind: 'minimum_initial_subscription', amount: 80000, currency: 'EUR' },
      }),
    );
  });

  it('marks an Unmodelled Restriction Noted only with a reason', async () => {
    const mark = vi.fn(() => of(blocked));
    const fixture = setup({ mark });
    const el = await open(fixture);
    (el.querySelectorAll('button.rule')[2] as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(button(el, 'noted').disabled).toBe(true);
    const reason = el.querySelector('#reason') as HTMLInputElement;
    reason.value = 'no condition on the client';
    reason.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    button(el, 'noted').click();
    expect(mark).toHaveBeenCalledWith(
      expect.objectContaining({ rule: 2, status: 'noted', reason: 'no condition on the client' }),
    );
  });

  it('validates a ready card and shows who and when', async () => {
    const validate = vi.fn(() =>
      of({
        ...card,
        status: 'validated' as const,
        validation: { validator: 'Ana', validatedAt: '2026-07-01T09:00:00Z' },
      }),
    );
    const ready: ReviewView = {
      ...blocked,
      contradictions: [],
      unreviewedRules: [],
      canValidate: true,
      blockers: [],
    };
    const fixture = setup({ validate, review: () => of(ready) });
    const el = await open(fixture);
    button(el, 'validate').click();
    fixture.detectChanges();
    expect(el.querySelector('.validated-line')?.textContent).toContain('Validated v1 by Ana');
    expect(el.querySelector('.validated-line')?.textContent).toContain(
      'in force for Eligibility Checks',
    );
  });

  it('refuses a blank amount and shows a failed request', async () => {
    const correctRule = vi.fn();
    const fixture = setup({ correctRule });
    const el = await open(fixture);
    (el.querySelectorAll('button.rule')[1] as HTMLButtonElement).click();
    fixture.detectChanges();
    const amount = el.querySelector('#f-amount') as HTMLInputElement;
    amount.value = '';
    amount.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    button(el, 'correct').click();
    fixture.detectChanges();
    expect(correctRule).not.toHaveBeenCalled();
    expect(el.querySelector('[role=alert]')?.textContent).toContain('amount must be a number');
  });

  it('says when no Draft card waits', () => {
    const fixture = setup({ drafts: () => of([]) });
    expect((fixture.nativeElement as HTMLElement).querySelector('.no-drafts')).not.toBeNull();
  });
});
