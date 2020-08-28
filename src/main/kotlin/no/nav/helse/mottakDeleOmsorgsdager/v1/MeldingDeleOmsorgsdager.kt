package no.nav.helse.mottakDeleOmsorgsdager.v1

import no.nav.helse.SoknadId
import no.nav.helse.AktoerId
import org.json.JSONObject

private object JsonKeys {
    internal const val søker = "søker"
    internal const val aktørId = "aktørId"
    internal const val søknadId = "søknadId"
    internal const val fødselsnummer = "fødselsnummer"
}

internal class MeldingDeleOmsorgsdagerIncoming(json: String) {
    private val jsonObject = JSONObject(json)

    internal val søkerAktørId = AktoerId(jsonObject.getJSONObject(JsonKeys.søker).getString(
        JsonKeys.aktørId
    ))

    internal val sokerFodselsNummer = jsonObject.getJSONObject(JsonKeys.søker).getString(
        JsonKeys.fødselsnummer
    )

    internal fun medSoknadId(soknadId: SoknadId): MeldingDeleOmsorgsdagerIncoming {
        jsonObject.put(JsonKeys.søknadId, soknadId.id)
        return this
    }

    internal fun somOutgoing() =
        MeldingDeleOmsorgsdagerOutgoing(jsonObject)
}

internal class MeldingDeleOmsorgsdagerOutgoing(internal val jsonObject: JSONObject) {
    internal val soknadId = SoknadId(jsonObject.getString(JsonKeys.søknadId))
}
