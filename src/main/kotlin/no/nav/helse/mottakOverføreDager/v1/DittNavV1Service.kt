package no.nav.helse.mottakOverføreDager.v1

import no.nav.helse.SoknadId
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("no.nav.DittNavV1Service")

internal class DittNavV1Service(
    private val soknadOverforeDagerKafkaProducer: SoknadOverforeDagerKafkaProducer
) {
    fun sendSoknadMottattMeldingTilDittNav(
        dto: ProduceBeskjedDto,
        søkersNorskeIdent: String,
        soknadId: SoknadId
    ) {

        soknadOverforeDagerKafkaProducer.produceDittnavMelding(
            dto = dto,
            søkersNorskeIdent = søkersNorskeIdent,
            soknadId = soknadId
        )
    }
}

internal fun sendBeskjedTilDittNav(
    dittNavV1Service: DittNavV1Service,
    dittNavTekst: String,
    dittNavLink: String,
    sokerFodselsNummer: String,
    soknadId: SoknadId
) {
    try {
        dittNavV1Service.sendSoknadMottattMeldingTilDittNav(
            dto = ProduceBeskjedDto(
                tekst = dittNavTekst,
                link = dittNavLink
            ),
            søkersNorskeIdent = sokerFodselsNummer,
            soknadId = soknadId
        )
    } catch (e: Exception) {
        logger.error("Kunne ikke sende melding til Ditt NAV om innsendt søknad: $e")
    }
}