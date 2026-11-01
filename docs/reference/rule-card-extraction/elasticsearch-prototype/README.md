# Page selection by retrieval (PROTOTYPE, ticket #32)

Throwaway. Compares v2 keyword selection with BM25, kNN (multilingual-e5-small) and hybrid RRF in Elasticsearch 8.17
(Docker, trial licence), on the Java spike's answer keys (`../page-recall-key*.json`), same page budget. Ran on
azure-vm1 (D8s_v3 for the run): copy `../docs/*.pages.json` and both keys next to `rag_es.py`, then `./run.sh`
(resumable, result in `report.md`). Python because it is a measurement, not product code (ADR 0006).

Result (40-page budget, unseen set, 74 rules): v2 keywords 72, kNN 69, hybrid 63, BM25 58, hybrid+v2 72; demo set
35/35 for all. See `report.md` per document.
