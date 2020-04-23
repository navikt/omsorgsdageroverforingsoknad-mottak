package no.nav.helse.mottakOverføreDager.v1

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.request.ApplicationRequest
import io.ktor.request.header
import io.ktor.request.receiveStream
import io.ktor.response.ApplicationResponse
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import no.nav.helse.Metadata
import no.nav.helse.getSoknadId
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("no.nav.SoknadV1Api")

internal fun Route.SoknadV1Api(
    soknadOverforeDagerMottakService: SoknadOverforeDagerMottakService,
    dittNavV1Service: DittNavV1Service
) {

    post("v1/soknad/overfore-dager") {
        val soknadId = call.getSoknadId()
        val metadata = call.metadata()
        val soknad = call.soknadOverforeDager()

        soknadOverforeDagerMottakService.leggTilProsessering(
            soknadId = soknadId,
            metadata = metadata,
            soknad = soknad
        )

        sendBeskjedTilDittNav(
            dittNavV1Service = dittNavV1Service,
            dittNavTekst = "Melding om overføring av omsorgsdager er mottatt.",
            dittNavLink = "",
            sokerFodselsNummer = soknad.sokerFodselsNummer,
            soknadId = soknadId
        )

        call.respond(HttpStatusCode.Accepted, mapOf("id" to soknadId.id))
    }

}

private suspend fun ApplicationCall.soknadOverforeDager() : SoknadOverforeDagerIncoming {
    val json = receiveStream().use { String(it.readAllBytes(), Charsets.UTF_8) }
    val incoming = SoknadOverforeDagerIncoming(json)
    incoming.validate()
    return incoming
}

private fun ApplicationCall.metadata() = Metadata(
    version = 2,
    correlationId = request.getCorrelationId(),
    requestId = response.getRequestId()
)

private fun ApplicationRequest.getCorrelationId(): String {
    return header(HttpHeaders.XCorrelationId) ?: throw IllegalStateException("Correlation Id ikke satt")
}

private fun ApplicationResponse.getRequestId(): String {
    return headers[HttpHeaders.XRequestId] ?: throw IllegalStateException("Request Id ikke satt")
}