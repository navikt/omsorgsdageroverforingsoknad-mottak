package no.nav.helse.mottakDeleOmsorgsdager.v1

import no.nav.helse.CorrelationId
import no.nav.helse.Metadata
import no.nav.helse.SoknadId
import no.nav.helse.mottakOverføreDager.v1.SoknadOverforeDagerKafkaProducer
import org.slf4j.LoggerFactory

internal class MeldingDeleOmsorgsdagerMottakService(
    private val kafkaProducer: SoknadOverforeDagerKafkaProducer
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(MeldingDeleOmsorgsdagerMottakService::class.java)
    }

    internal suspend fun leggTilProsessering(
        soknadId: SoknadId,
        metadata: Metadata,
        soknad: MeldingDeleOmsorgsdagerIncoming
    ): SoknadId {
        val correlationId = CorrelationId(metadata.correlationId)

        val outgoing = soknad.medSoknadId(soknadId).somOutgoing()

        logger.info("Legger melding for deling av omsorgsdager på kø")
        kafkaProducer.produceDeleOmsorgsdager(
            metadata = metadata,
            melding = outgoing
        )
        return soknadId
    }
}
