# Page selection by retrieval (ticket #32, PROTOTYPE)

Elasticsearch 8.17.0, embeddings `intfloat/multilingual-e5-small`, passages 1000 chars (stride 800), page = best passage, front matter pages 1-5 always sent, round-robin over 7 rule-kind queries. v1 (Nordea-tuned keywords, measured in Java): unseen 40/74, demo 35/35.


## Budget 40 pages

| set | v2 | bm25 | knn | hybrid | hybrid+v2 |
|---|---|---|---|---|---|
| demo | **35/35**, 119 pages | **35/35**, 200 pages | **35/35**, 202 pages | **35/35**, 202 pages | **35/35**, 202 pages |
| unseen | **72/74**, 545 pages | **58/74**, 723 pages | **69/74**, 723 pages | **63/74**, 723 pages | **72/74**, 723 pages |

| document | pages | v2 | bm25 | knn | hybrid | hybrid+v2 |
|---|---|---|---|---|---|---|
| prospectus | 944 | 5/5 (40p) | 5/5 (40p) | 5/5 (40p) | 5/5 (40p) | 5/5 (40p) |
| kid-BI-EUR | 3 | 1/1 (3p) | 1/1 (3p) | 1/1 (3p) | 1/1 (3p) | 1/1 (3p) |
| sif-prospectus | 63 | 6/6 (20p) | 6/6 (40p) | 6/6 (40p) | 6/6 (40p) | 6/6 (40p) |
| sif-kid-BW-SEK | 3 | 1/1 (3p) | 1/1 (3p) | 1/1 (3p) | 1/1 (3p) | 1/1 (3p) |
| bnp-prospectus | 34 | 6/6 (20p) | 6/6 (34p) | 6/6 (34p) | 6/6 (34p) | 6/6 (34p) |
| bnp-prospectus-fr | 36 | 6/6 (17p) | 6/6 (34p) | 6/6 (36p) | 6/6 (36p) | 6/6 (36p) |
| bnp-kid-A | 3 | 1/1 (3p) | 1/1 (3p) | 1/1 (3p) | 1/1 (3p) | 1/1 (3p) |
| bnp-kid-I | 3 | 2/2 (3p) | 2/2 (3p) | 2/2 (3p) | 2/2 (3p) | 2/2 (3p) |
| sl-contract | 47 | 7/7 (10p) | 7/7 (40p) | 7/7 (40p) | 7/7 (40p) | 7/7 (40p) |
| x-agif-en | 293 | 3/4 (40p) ✗ minimum | 2/4 (40p) ✗ description, minimum | 3/4 (40p) ✗ minimum | 3/4 (40p) ✗ minimum | 3/4 (40p) ✗ minimum |
| x-amundi-en | 112 | 4/4 (22p) | 4/4 (40p) | 4/4 (40p) | 4/4 (40p) | 4/4 (40p) |
| x-blk-bgf-en | 724 | 3/4 (40p) ✗ description | 3/4 (40p) ✗ description | 3/4 (40p) ✗ description | 3/4 (40p) ✗ description | 3/4 (40p) ✗ description |
| x-blk-bgif-en | 80 | 4/4 (40p) | 4/4 (40p) | 4/4 (40p) | 4/4 (40p) | 4/4 (40p) |
| x-candriam-en | 371 | 4/4 (20p) | 4/4 (40p) | 4/4 (40p) | 4/4 (40p) | 4/4 (40p) |
| x-candriam-fr | 420 | 4/4 (22p) | 2/4 (40p) ✗ description, share-class eligibility | 4/4 (40p) | 3/4 (40p) ✗ share-class eligibility | 4/4 (40p) |
| x-ch-200141-en | 137 | 4/4 (39p) | 4/4 (40p) | 4/4 (40p) | 4/4 (40p) | 4/4 (40p) |
| x-ch-5475-de | 81 | 4/4 (12p) | 3/4 (40p) ✗ minimum | 4/4 (40p) | 4/4 (40p) | 4/4 (40p) |
| x-dws-funds-de | 135 | 4/4 (18p) | 2/4 (40p) ✗ share-class eligibility, minimum | 4/4 (40p) | 4/4 (40p) | 4/4 (40p) |
| x-dws-strat-de | 253 | 4/4 (27p) | 2/4 (40p) ✗ share-class eligibility, minimum | 3/4 (40p) ✗ description | 2/4 (40p) ✗ share-class eligibility, minimum | 4/4 (40p) |
| x-jpm-liq-fr | 179 | 4/4 (40p) | 3/4 (40p) ✗ share-class eligibility | 4/4 (40p) | 3/4 (40p) ✗ description | 4/4 (40p) |
| x-jpmf-hk-en | 567 | 4/4 (40p) | 4/4 (40p) | 4/4 (40p) | 4/4 (40p) | 4/4 (40p) |
| x-lafrancaise-fr | 76 | 4/4 (19p) | 4/4 (40p) | 4/4 (40p) | 4/4 (40p) | 4/4 (40p) |
| x-mg-lux-en | 204 | 4/4 (40p) | 4/4 (40p) | 4/4 (40p) | 4/4 (40p) | 4/4 (40p) |
| x-pictet-en | 685 | 4/4 (25p) | 2/4 (40p) ✗ share-class eligibility, minimum | 3/4 (40p) ✗ minimum | 2/4 (40p) ✗ share-class eligibility, minimum | 4/4 (40p) |
| x-robeco-cgf-de | 545 | 4/4 (30p) | 2/4 (40p) ✗ share-class eligibility, minimum | 4/4 (40p) | 2/4 (40p) ✗ share-class eligibility, minimum | 4/4 (40p) |
| x-vontobel-ch-en | 72 | 5/5 (28p) | 5/5 (40p) | 5/5 (40p) | 5/5 (40p) | 5/5 (40p) |
| x-vontobel-lu-en | 448 | 4/4 (40p) | 3/4 (40p) ✗ minimum | 3/4 (40p) ✗ minimum | 3/4 (40p) ✗ minimum | 4/4 (40p) |
| x-swisscanto-kid-de | 3 | 1/1 (3p) | 1/1 (3p) | 1/1 (3p) | 1/1 (3p) | 1/1 (3p) |

## Budget 25 pages

| set | v2 | bm25 | knn | hybrid | hybrid+v2 |
|---|---|---|---|---|---|
| demo | **35/35**, 119 pages | **34/35**, 137 pages | **35/35**, 137 pages | **34/35**, 137 pages | **35/35**, 152 pages |
| unseen | **72/74**, 545 pages | **53/74**, 453 pages | **66/74**, 453 pages | **60/74**, 453 pages | **72/74**, 582 pages |

| document | pages | v2 | bm25 | knn | hybrid | hybrid+v2 |
|---|---|---|---|---|---|---|
| prospectus | 944 | 5/5 (40p) | 5/5 (25p) | 5/5 (25p) | 5/5 (25p) | 5/5 (40p) |
| kid-BI-EUR | 3 | 1/1 (3p) | 1/1 (3p) | 1/1 (3p) | 1/1 (3p) | 1/1 (3p) |
| sif-prospectus | 63 | 6/6 (20p) | 6/6 (25p) | 6/6 (25p) | 6/6 (25p) | 6/6 (25p) |
| sif-kid-BW-SEK | 3 | 1/1 (3p) | 1/1 (3p) | 1/1 (3p) | 1/1 (3p) | 1/1 (3p) |
| bnp-prospectus | 34 | 6/6 (20p) | 6/6 (25p) | 6/6 (25p) | 6/6 (25p) | 6/6 (25p) |
| bnp-prospectus-fr | 36 | 6/6 (17p) | 6/6 (25p) | 6/6 (25p) | 5/6 (25p) ✗ distribution | 6/6 (25p) |
| bnp-kid-A | 3 | 1/1 (3p) | 1/1 (3p) | 1/1 (3p) | 1/1 (3p) | 1/1 (3p) |
| bnp-kid-I | 3 | 2/2 (3p) | 2/2 (3p) | 2/2 (3p) | 2/2 (3p) | 2/2 (3p) |
| sl-contract | 47 | 7/7 (10p) | 6/7 (25p) ✗ enforced conversion | 7/7 (25p) | 7/7 (25p) | 7/7 (25p) |
| x-agif-en | 293 | 3/4 (40p) ✗ minimum | 2/4 (25p) ✗ description, minimum | 3/4 (25p) ✗ minimum | 3/4 (25p) ✗ minimum | 3/4 (40p) ✗ minimum |
| x-amundi-en | 112 | 4/4 (22p) | 4/4 (25p) | 4/4 (25p) | 4/4 (25p) | 4/4 (25p) |
| x-blk-bgf-en | 724 | 3/4 (40p) ✗ description | 3/4 (25p) ✗ description | 3/4 (25p) ✗ description | 3/4 (25p) ✗ description | 3/4 (40p) ✗ description |
| x-blk-bgif-en | 80 | 4/4 (40p) | 3/4 (25p) ✗ description | 3/4 (25p) ✗ share-class eligibility | 4/4 (25p) | 4/4 (40p) |
| x-candriam-en | 371 | 4/4 (20p) | 4/4 (25p) | 4/4 (25p) | 4/4 (25p) | 4/4 (25p) |
| x-candriam-fr | 420 | 4/4 (22p) | 1/4 (25p) ✗ description, share-class eligibility, minimum | 4/4 (25p) | 3/4 (25p) ✗ share-class eligibility | 4/4 (25p) |
| x-ch-200141-en | 137 | 4/4 (39p) | 4/4 (25p) | 4/4 (25p) | 4/4 (25p) | 4/4 (39p) |
| x-ch-5475-de | 81 | 4/4 (12p) | 3/4 (25p) ✗ minimum | 4/4 (25p) | 3/4 (25p) ✗ minimum | 4/4 (25p) |
| x-dws-funds-de | 135 | 4/4 (18p) | 2/4 (25p) ✗ share-class eligibility, minimum | 4/4 (25p) | 3/4 (25p) ✗ minimum | 4/4 (25p) |
| x-dws-strat-de | 253 | 4/4 (27p) | 2/4 (25p) ✗ share-class eligibility, minimum | 2/4 (25p) ✗ description, share-class eligibility | 1/4 (25p) ✗ description, share-class eligibility, minimum | 4/4 (27p) |
| x-jpm-liq-fr | 179 | 4/4 (40p) | 3/4 (25p) ✗ share-class eligibility | 4/4 (25p) | 3/4 (25p) ✗ description | 4/4 (40p) |
| x-jpmf-hk-en | 567 | 4/4 (40p) | 2/4 (25p) ✗ share-class eligibility, minimum | 4/4 (25p) | 4/4 (25p) | 4/4 (40p) |
| x-lafrancaise-fr | 76 | 4/4 (19p) | 4/4 (25p) | 4/4 (25p) | 4/4 (25p) | 4/4 (25p) |
| x-mg-lux-en | 204 | 4/4 (40p) | 4/4 (25p) | 4/4 (25p) | 4/4 (25p) | 4/4 (40p) |
| x-pictet-en | 685 | 4/4 (25p) | 2/4 (25p) ✗ share-class eligibility, minimum | 2/4 (25p) ✗ description, minimum | 2/4 (25p) ✗ share-class eligibility, minimum | 4/4 (25p) |
| x-robeco-cgf-de | 545 | 4/4 (30p) | 1/4 (25p) ✗ description, share-class eligibility, minimum | 4/4 (25p) | 2/4 (25p) ✗ share-class eligibility, minimum | 4/4 (30p) |
| x-vontobel-ch-en | 72 | 5/5 (28p) | 5/5 (25p) | 5/5 (25p) | 5/5 (25p) | 5/5 (28p) |
| x-vontobel-lu-en | 448 | 4/4 (40p) | 3/4 (25p) ✗ minimum | 3/4 (25p) ✗ minimum | 3/4 (25p) ✗ minimum | 4/4 (40p) |
| x-swisscanto-kid-de | 3 | 1/1 (3p) | 1/1 (3p) | 1/1 (3p) | 1/1 (3p) | 1/1 (3p) |
