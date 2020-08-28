package no.nav.helse.kafka

import no.nav.helse.Metadata
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer

data class TopicEntry<V>(
    val metadata: Metadata,
    val data: V
)
internal data class TopicUse<V>(
    val name: String,
    val valueSerializer : Serializer<TopicEntry<V>>
) {
    internal fun keySerializer() = StringSerializer()
}

object Topics {
    const val MOTTATT_OVERFORE_DAGER = "privat-overfore-omsorgsdager-soknad-mottatt"
    const val DITT_NAV_BESKJED = "aapen-brukernotifikasjon-nyBeskjed-v1"
    const val MOTTATT_DELE_OMSORGSDAGER = "privat-dele-omsorgsdager-melding-mottatt" //TODO: Må lage topic
}