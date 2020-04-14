package no.nav.helse.mottakOverføreDager.v1

import no.nav.helse.dusseldorf.ktor.core.*

internal fun SoknadOverforeDagerIncoming.validate() {
    val violations = mutableSetOf<Violation>()

    if (!søkerAktørId.id.erKunSiffer()) {
        violations.add(
            Violation(
                parameterName = "søker.aktørId",
                parameterType = ParameterType.ENTITY,
                reason = "Ikke gyldig Aktør ID.",
                invalidValue = søkerAktørId.id
            )
        )
    }

    if (violations.isNotEmpty()) {
        throw Throwblem(ValidationProblemDetails(violations))
    }
}