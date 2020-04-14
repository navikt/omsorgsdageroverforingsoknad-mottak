package no.nav.helse.mottakOverføreDager.v1

import no.nav.helse.SoknadId
import no.nav.helse.AktoerId
import org.json.JSONObject

private object JsonKeys {
    internal const val søker = "søker"
    internal const val aktørId = "aktørId"
    internal const val søknadId = "søknadId"
    internal const val fødselsnummer = "fødselsnummer"
    internal const val content = "content"
    internal const val contentType = "contentType"
    internal const val title = "title"
}

internal class SoknadOverforeDagerIncoming(json: String) {
    private val jsonObject = JSONObject(json)

    internal val søkerAktørId = AktoerId(jsonObject.getJSONObject(JsonKeys.søker).getString(
        JsonKeys.aktørId
    ))

    internal fun medSoknadId(soknadId: SoknadId): SoknadOverforeDagerIncoming {
        jsonObject.put(JsonKeys.søknadId, soknadId.id)
        return this
    }

    internal fun somOutgoing() =
        SoknadOverforeDagerOutgoing(jsonObject)
}

internal class SoknadOverforeDagerOutgoing(internal val jsonObject: JSONObject) {
    internal val soknadId = SoknadId(jsonObject.getString(JsonKeys.søknadId))
}
