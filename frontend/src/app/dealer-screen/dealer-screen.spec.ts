import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { AnswerControllerService, AnswerView, CheckRecordView } from '../generated/api';
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

function setup(api: Partial<Record<keyof AnswerControllerService, unknown>>) {
  TestBed.configureTestingModule({
    providers: [{ provide: AnswerControllerService, useValue: api }],
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
    expect(text).toContain('Dear Marc, BP-EUR is available.');
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
    expect(el.textContent).toContain('Dear Marc');
    expect(el.querySelector('[role="alert"]')?.textContent).toContain(
      'Check Record id could not be loaded',
    );
  });

  it('does not call the backend for an empty email', async () => {
    const api = { answer: vi.fn(), checkRecords: vi.fn() };
    await paste(setup(api), '   ');
    expect(api.answer).not.toHaveBeenCalled();
  });
});
