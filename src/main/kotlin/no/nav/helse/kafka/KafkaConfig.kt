package no.nav.helse.kafka

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer

private val logger: Logger = LoggerFactory.getLogger(KafkaConfig::class.java)
private const val ID_PREFIX = "srvomsover-mtk-"

internal class KafkaConfig(
    bootstrapServers: String,
    val credentials: Pair<String, String>,
    trustStore: Pair<String, String>?
) {
    private val producer = Properties().apply {
        put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
        medCredentials(credentials)
        medTrustStore(trustStore)
    }

    internal fun producer(name: String) = producer.apply {
        put(ProducerConfig.CLIENT_ID_CONFIG, "$ID_PREFIX$name")
    }

    internal fun producerDittNavMelding(name: String) = producer.apply {
        put(ProducerConfig.CLIENT_ID_CONFIG, "$ID_PREFIX$name")
        put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer::class.java)
        put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer::class.java)
        put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, getEnvVar("KAFKA_SCHEMAREGISTRY_SERVERS", "http://localhost:8141"))
    }
}

private fun Properties.medTrustStore(trustStore: Pair<String, String>?) {
    trustStore?.let {
        try {
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")
            put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, File(it.first).absolutePath)
            put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, it.second)
            logger.info("Truststore på '${it.first}' konfigurert.")
        } catch (cause: Throwable) {
            logger.error(
                "Feilet for konfigurering av truststore på '${it.first}'",
                cause
            )
        }
    }
}

fun getEnvVar(varName: String, defaultValue: String? = null): String {
    val varValue = System.getenv(varName) ?: defaultValue
    return varValue ?: throw IllegalArgumentException("Variable $varName cannot be empty")
}

private fun Properties.medCredentials(credentials: Pair<String, String>) {
    put(SaslConfigs.SASL_MECHANISM, "PLAIN")
    put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
    put(
        SaslConfigs.SASL_JAAS_CONFIG,
        "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${credentials.first}\" password=\"${credentials.second}\";"
    )
}
