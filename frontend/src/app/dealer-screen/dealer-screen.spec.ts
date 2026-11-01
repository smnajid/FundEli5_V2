import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import {
  AnswerControllerService,
  AnswerView,
  CheckRecordControllerService,
  CheckRecordView,
} from '../generated/api';
import { DealerScreen } from './dealer-screen';

const answer: AnswerView = {
  aiUsed: true,
  screenNote: 'the LLM saw Portfolio Ids only',
  checks: [
    {
      portfolioReference: 'LOC90000101',
      shareClassName: 'Nordea 1 Global Climate BI-EUR',
      verdict: 'NOT_ELIGIBLE',
      outcomes: [{ kind: 'INVESTOR_CATEGORY', result: 'FAIL', explanation: 'Not institutional.' }],
    },
    {
      portfolioReference: 'LOC90000101',
      shareClassName: 'Nordea 1 Global Climate BP-EUR',
      verdict: 'ELIGIBLE',
      outcomes: [],
    },
  ],
  reply: 'Dear Marc, BP-EUR is available.',
};

const records: CheckRecordView[] = [{ id: 'old-1' }, { id: 'rec-1' }, { id: 'rec-2' }];

function setup(
  api: Partial<Record<keyof AnswerControllerService, unknown>>,
  recordApi: Partial<Record<keyof CheckRecordControllerService, unknown>> = {},
) {
  TestBed.configureTestingModule({
    providers: [
      { provide: AnswerControllerService, useValue: api },
      { provide: CheckRecordControllerService, useValue: recordApi },
    ],
  });
  const fixture = TestBed.createComponent(DealerScreen);
  fixture.detectChanges();
  return fixture;
}

async function paste(fixture: ReturnType<typeof setup>, text: string) {
  const el: HTMLElement = fixture.nativeElement;
  const area = el.querySelector('textarea') as HTMLTextAreaElement;
  area.value = text;
  area.dispatchEvent(new Event('input'));
  (el.querySelector('button') as HTMLButtonElement).click();
  await fixture.whenStable();
  fixture.detectChanges();
  return el;
}

function replyBox(el: HTMLElement): HTMLTextAreaElement {
  return el.querySelector('textarea[aria-label="Reply"]') as HTMLTextAreaElement;
}

function button(el: HTMLElement, text: string): HTMLButtonElement {
  return Array.from(el.querySelectorAll('button')).find((b) =>
    b.textContent?.includes(text),
  ) as HTMLButtonElement;
}

const answered: AnswerView = {
  ...answer,
  checks: [
    {
      portfolioReference: 'LOC90000101',
      shareClassIsin: 'LU0000000001',
      shareClassName: 'Nordea 1 Global Climate BI-EUR',
      amount: 80000,
      currency: 'EUR',
      clientInitiated: false,
      verdict: 'NOT_ELIGIBLE',
      outcomes: [{ kind: 'INVESTOR_CATEGORY', result: 'FAIL', explanation: 'Not institutional.' }],
    },
  ],
};

describe('DealerScreen', () => {
  it('shows verdicts, reply and Check Record ids for a pasted email', async () => {
    const api = {
      answer: vi.fn(() => of(answer)),
      checkRecords: vi
        .fn()
        .mockReturnValueOnce(of([records[0]]))
        .mockReturnValueOnce(of(records)),
    };
    const el = await paste(setup(api), 'Please buy');

    expect(api.answer).toHaveBeenCalledWith({ email: 'Please buy' });
    const text = el.textContent ?? '';
    expect(text).toContain('Nordea 1 Global Climate BI-EUR');
    expect(text).toContain('Not eligible');
    expect(text).toContain('Eligible');
    expect(text).toContain('Not institutional.');
    expect(replyBox(el).value).toBe('Dear Marc, BP-EUR is available.');
    expect(text).toContain('cannot buy');
    expect(text).toContain('can buy');
    expect(text).toContain('the LLM saw Portfolio Ids only');
    expect(text).toContain('rec-1');
    expect(text).toContain('rec-2');
    expect(text).not.toContain('old-1');
  });

  it('shows the refusal panel when the Privacy Gate refuses', async () => {
    const refused: AnswerView = {
      aiUsed: false,
      screenNote: 'no AI was used for this email',
      refusal: { reason: 'A client name remains', namesFound: ['Hans Muster'] },
      checks: [],
    };
    const api = { answer: vi.fn(() => of(refused)), checkRecords: vi.fn(() => of([])) };
    const el = await paste(setup(api), 'Hans Muster wants funds');
    const text = el.textContent ?? '';
    expect(text).toContain('No AI used for this email');
    expect(text).toContain('A client name remains');
    expect(text).toContain('Hans Muster');
  });

  it('shows an error when the backend fails', async () => {
    const api = {
      answer: vi.fn(() => throwError(() => new Error('boom'))),
      checkRecords: vi.fn(() => of([])),
    };
    const el = await paste(setup(api), 'x');
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Could not answer');
  });

  it('still shows the answer when the Check Record ids cannot load', async () => {
    const api = {
      answer: vi.fn(() => of(answer)),
      checkRecords: vi
        .fn()
        .mockReturnValueOnce(of([]))
        .mockReturnValueOnce(throwError(() => new Error('x'))),
    };
    const el = await paste(setup(api), 'Please buy');
    expect(replyBox(el).value).toContain('Dear Marc');
    expect(el.querySelector('[role="alert"]')?.textContent).toContain(
      'Check Record id could not be loaded',
    );
  });

  it('does not call the backend for an empty email', async () => {
    const api = { answer: vi.fn(), checkRecords: vi.fn() };
    await paste(setup(api), '   ');
    expect(api.answer).not.toHaveBeenCalled();
  });

  describe('after an answer', () => {
    const full: CheckRecordView[] = [
      {
        id: 'rec-1',
        portfolioReference: 'LOC90000101',
        shareClassIsin: 'LU0000000001',
        verdict: 'NOT_ELIGIBLE',
        llmCalls: [
          { model: 'm1', gatedPrompt: 'GATED PROMPT', fieldsSent: ['a'], response: 'RESP' },
        ],
      },
    ];
    const handled: AnswerView = { ...answered, aiUsed: false, reply: 'Template reply' };

    function apis() {
      const api = {
        answer: vi.fn(() => of(answered)),
        answerByHand: vi.fn(() => of(handled)),
        checkRecords: vi.fn().mockReturnValueOnce(of([])).mockReturnValue(of(full)),
      };
      const recordApi = { correct: vi.fn(() => of(undefined)) };
      return { api, recordApi };
    }

    function type(el: HTMLElement, label: string, value: string) {
      const input = el.querySelector(`input[aria-label="${label}"]`) as HTMLInputElement;
      input.value = value;
      input.dispatchEvent(new Event('input'));
    }

    it('folds the email, keeps the Portfolio Reference fixed and the reply editable', async () => {
      const { api, recordApi } = apis();
      const el = await paste(setup(api, recordApi), 'Please buy');
      expect(el.querySelector('details.request')).not.toBeNull();
      expect(el.querySelector('input[aria-label="Portfolio Reference"]')).toBeNull();
      expect(el.querySelector('input[aria-label="Currency"]')).not.toBeNull();
      expect(replyBox(el).disabled).toBe(false);
    });

    it('copies the edited reply', async () => {
      const { api, recordApi } = apis();
      const writeText = vi.fn(() => Promise.resolve());
      Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true });
      const fixture = setup(api, recordApi);
      const el = await paste(fixture, 'Please buy');
      const box = replyBox(el);
      box.value = 'Edited reply';
      box.dispatchEvent(new Event('input'));
      button(el, 'Copy reply').click();
      await fixture.whenStable();
      fixture.detectChanges();
      expect(writeText).toHaveBeenCalledWith('Edited reply');
      expect(el.textContent).toContain('Copied');
    });

    it('re-checks an edited currency and records parsed and dealer values', async () => {
      const { api, recordApi } = apis();
      const fixture = setup(api, recordApi);
      const el = await paste(fixture, 'Please buy');
      type(el, 'Currency', 'chf');
      fixture.detectChanges();
      button(el, 'Re-check').click();
      await fixture.whenStable();
      fixture.detectChanges();
      expect(api.answerByHand).toHaveBeenCalledWith(
        expect.objectContaining({
          portfolioReference: 'LOC90000101',
          currency: 'CHF',
          amount: 80000,
        }),
      );
      expect(recordApi.correct).toHaveBeenCalledWith('rec-1', {
        field: 'currency',
        parsedValue: 'EUR',
        dealerValue: 'chf',
        by: 'dealer',
      });
      expect(replyBox(el).value).toBe('Template reply');
    });

    it('opens What the AI saw with the gated prompt and response', async () => {
      const { api, recordApi } = apis();
      const fixture = setup(api, recordApi);
      const el = await paste(fixture, 'Please buy');
      button(el, 'What the AI saw').click();
      fixture.detectChanges();
      const drawer = el.querySelector('aside.drawer')?.textContent ?? '';
      expect(drawer).toContain('GATED PROMPT');
      expect(drawer).toContain('RESP');
      expect(drawer).toContain('m1');
    });

    it('lists every rule in the folded Full Justification', async () => {
      const { api, recordApi } = apis();
      const el = await paste(setup(api, recordApi), 'Please buy');
      expect(el.querySelector('details.justification')?.textContent).toContain('INVESTOR_CATEGORY');
    });
  });

  it('offers the manual form after a refusal and checks by hand', async () => {
    const refused: AnswerView = {
      aiUsed: false,
      refusal: { reason: 'A client name remains', namesFound: ['Hans Muster'] },
      checks: [],
    };
    const api = {
      answer: vi.fn(() => of(refused)),
      answerByHand: vi.fn(() => of(answered)),
      checkRecords: vi.fn(() => of([])),
    };
    const fixture = setup(api);
    const el = await paste(fixture, 'Hans Muster');
    const set = (label: string, value: string) => {
      const i = el.querySelector(`input[aria-label="${label}"]`) as HTMLInputElement;
      i.value = value;
      i.dispatchEvent(new Event('input'));
    };
    set('Portfolio Reference', 'LOC90000101');
    set('Share Class ISIN', 'LU0000000001');
    set('Amount', '80000');
    fixture.detectChanges();
    button(el, 'Re-check').click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(api.answerByHand).toHaveBeenCalledWith({
      portfolioReference: 'LOC90000101',
      shareClassIsin: 'LU0000000001',
      amount: 80000,
      currency: 'EUR',
      clientInitiated: false,
    });
    expect(el.textContent).toContain('cannot buy');
    expect(el.querySelector('input[aria-label="Portfolio Reference"]')).not.toBeNull();
  });

  it('lets the dealer answer another email after folding', async () => {
    const api = {
      answer: vi.fn(() => of(answered)),
      checkRecords: vi.fn(() => of([])),
    };
    const fixture = setup(api, {});
    const el = await paste(fixture, 'Please buy');
    button(el, 'Answer another email').click();
    fixture.detectChanges();
    expect(button(el, 'Answer it')).toBeTruthy();
  });
});
