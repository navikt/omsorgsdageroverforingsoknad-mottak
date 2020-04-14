package no.nav.helse.mottakOverføreDager.v1

import no.nav.helse.CorrelationId
import no.nav.helse.Metadata
import no.nav.helse.SoknadId
import org.slf4j.LoggerFactory

internal class SoknadOverforeDagerMottakService(
    private val soknadOverforeDagerKafkaProducer: SoknadOverforeDagerKafkaProducer
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(SoknadOverforeDagerMottakService::class.java)
    }

    internal suspend fun leggTilProsessering(
        soknadId: SoknadId,
        metadata: Metadata,
        soknad: SoknadOverforeDagerIncoming
    ): SoknadId {
        val correlationId = CorrelationId(metadata.correlationId)

        val outgoing = soknad.medSoknadId(soknadId).somOutgoing()

        logger.info("Legger søknad for overføring av dager på kø")
        soknadOverforeDagerKafkaProducer.produce(
            metadata = metadata,
            soknad = outgoing
        )
        return soknadId
    }
}
