# Presidio analyzer with English, French and German name detection (ADR 0009).
#   docker build -t fundeli5-presidio docker/presidio
#   docker run -p 5002:3000 fundeli5-presidio
# The stock image knows English only. Languages come from nlp.yaml and analyzer.yaml; the app asks for each of
# `fundeli5.privacygate.presidio.languages` (default en,fr,de) and refuses the email if one is not supported.
FROM mcr.microsoft.com/presidio-analyzer:latest

RUN python -m spacy download fr_core_news_md \
 && python -m spacy download de_core_news_md

COPY nlp.yaml /app/conf/nlp.yaml
COPY analyzer.yaml /app/conf/analyzer.yaml
COPY recognizers.yaml /app/conf/recognizers.yaml

ENV NLP_CONF_FILE=/app/conf/nlp.yaml \
    ANALYZER_CONF_FILE=/app/conf/analyzer.yaml \
    RECOGNIZER_REGISTRY_CONF_FILE=/app/conf/recognizers.yaml
