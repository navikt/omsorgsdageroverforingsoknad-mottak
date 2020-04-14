package no.nav.helse

data class Metadata(
    val version : Int,
    val correlationId : String,
    val requestId : String
)