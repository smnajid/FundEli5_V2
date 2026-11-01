# Validator may mark an Unmodelled Restriction as Noted

Amends the Rule Card model (an Unmodelled Restriction always gives Review). Nearly every prospectus carries clauses that set no condition on the client, such as the board's right to reject applications or to close a Share Class (Nordea 1, prospectus p.138). If each gave Review, no check could ever be Eligible. So when validating a Rule Card, the validator marks each Unmodelled Restriction **Blocking** (Review on every check) or **Noted** (Pass, still shown in the Justification with its Citation). The extractor proposes Blocking, so an unreviewed clause never passes silently, and each marking is kept in the card's history with who made it and when. A "legal only where registered" clause is not an Unmodelled Restriction at all: it is filed under Fund Registration.

## Considered Options

- **Add rule kinds such as "issuer discretion"**: rejected because each new prospectus can bring another clause that sets no condition on the client, so the set of kinds would keep growing. One human call covers them all.
- **Keep "always Review"**: rejected because Eligible would become unreachable for every real fund.
