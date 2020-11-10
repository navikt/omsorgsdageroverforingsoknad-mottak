# Omsorgsdageroverføringsøknad-mottak
![CI / CD](https://github.com/navikt/omsorgsdageroverforingsoknad-mottak/workflows/CI%20/%20CD/badge.svg)

![NAIS Alerts](https://github.com/navikt/omsorgsdageroverforingsoknad-mottak/workflows/Alerts/badge.svg)

## Overføre-dager
Tjeneste som tar imot søknader om overføring av omsorgsdager fra api og legger de til til prosessering.
Mottar søknad som REST API-kall. Legges videre på en Kafka Topic som tjenesten [omsorgsdageroverforingsoknad-prosessering](https://github.com/navikt/omsorgsdageroverforingsoknad-prosessering) prosesserer.

## Versjon 1
### Path
/v1/soknad/overfore-dager

### Meldingsformat
- Gir 202 response med SøknadId som entity på formatet ```{"id":"b3106960-0a85-4e02-9221-6ae057c8e93f"}```
- Må være gyldig JSON
- Må inneholde soker.fodselsnummer som er et gyldig fødselsnummer/D-nummer
- Må inneholder soker.aktoer_id som er en gyldig aktør ID
- Utover dette validerer ikke tjenesten ytterligere felter som sendes om en del av meldingen.

### Format på søknad lagt på kafka
attributten "data" er tilsvarende søknaden som kommer inn i REST-API'et med noen unntak:
- "soknad_id" lagt til
- Alle andre felter som har vært en del av JSON-meldingen som kom inn i REST-API'et vil også være en del av "data"-attributten i Kafka-meldingen.

## Dele-dager
Tjeneste som tar imot meldinger om deling av omsorgsdager fra api og legger de til til prosessering.
Mottar søknad som REST API-kall. Legges videre på en Kafka Topic som tjenesten [omsorgsdageroverforingsoknad-prosessering](https://github.com/navikt/omsorgsdageroverforingsoknad-prosessering) prosesserer.

## Versjon 1
### Path
/v1/melding/dele-dager

### Format på melding lagt på kafka
attributten "data" er tilsvarende melding som kommer inn i REST-API'et med noen unntak:
- "soknad_id" lagt til
- Alle andre felter som har vært en del av JSON-meldingen som kom inn i REST-API'et vil også være en del av "data"-attributten i Kafka-meldingen.


### Metadata
#### Correlation ID vs Request ID
Correlation ID blir propagert videre, og har ikke nødvendigvis sitt opphav hos konsumenten.
Request ID blir ikke propagert videre, og skal ha sitt opphav hos konsumenten om den settes.

## Alarmer
Vi bruker [nais-alerts](https://doc.nais.io/observability/alerts) for å sette opp alarmer. Disse finner man konfigurert i [nais/alerts.yml](nais/alerterator.yml).

#### REST API
- Correlation ID må sendes som header 'X-Correlation-ID'
- Request ID kan sendes som heder 'X-Request-ID'

## Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

Interne henvendelser kan sendes via Slack i kanalen #team-düsseldorf.
