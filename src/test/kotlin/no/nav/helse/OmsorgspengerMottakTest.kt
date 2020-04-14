package no.nav.helse

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.typesafe.config.ConfigFactory
import io.ktor.config.ApplicationConfig
import io.ktor.config.HoconApplicationConfig
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.createTestEnvironment
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.util.KtorExperimentalAPI
import no.nav.common.KafkaEnvironment
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.helse.kafka.Topics
import no.nav.helse.mottakOverføreDager.v1.SoknadOverforeDagerIncoming
import no.nav.helse.mottakOverføreDager.v1.SoknadOverforeDagerOutgoing
import org.json.JSONObject
import org.junit.AfterClass
import org.junit.BeforeClass
import org.skyscreamer.jsonassert.JSONAssert
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@KtorExperimentalAPI
class OmsorgspengerMottakTest {

    @KtorExperimentalAPI
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(OmsorgspengerMottakTest::class.java)

        // Se https://github.com/navikt/dusseldorf-ktor#f%C3%B8dselsnummer
        private val gyldigFodselsnummerA = "02119970078"
        private val gyldigFodselsnummerB = "19066672169"
        private val gyldigFodselsnummerC = "20037473937"
        private val dNummerA = "55125314561"

        private val wireMockServer: WireMockServer = WireMockBuilder()
            .withAzureSupport()
            .build()
            .stubK9DokumentHealth()
            .stubLagreDokument()
            .stubAktoerRegisterGetAktoerId(gyldigFodselsnummerA, "1234561")
            .stubAktoerRegisterGetAktoerId(gyldigFodselsnummerB, "1234562")
            .stubAktoerRegisterGetAktoerId(gyldigFodselsnummerC, "1234563")
            .stubAktoerRegisterGetAktoerId(dNummerA, "1234564")


        private val kafkaEnvironment = KafkaWrapper.bootstrap()
        private val kafkaTestConsumer = kafkaEnvironment.testConsumer()
        private val objectMapper = jacksonObjectMapper().dusseldorfConfigured()

        private val authorizedAccessToken = Azure.V1_0.generateJwt(clientId = "omsorgspenger-api", audience = "omsorgsdageroverforingsoknad-mottak")
        private val unAauthorizedAccessToken = Azure.V2_0.generateJwt(clientId = "ikke-authorized-client", audience = "omsorgsdageroverforingsoknad-mottak")

        private var engine = newEngine(kafkaEnvironment)

        private fun getConfig(kafkaEnvironment: KafkaEnvironment) : ApplicationConfig {
            val fileConfig = ConfigFactory.load()
            val testConfig = ConfigFactory.parseMap(TestConfiguration.asMap(
                wireMockServer = wireMockServer,
                kafkaEnvironment = kafkaEnvironment,
                omsorgspengerMottakAzureClientId = "omsorgsdageroverforingsoknad-mottak",
                azureAuthorizedClients = setOf("omsorgspenger-api")
            ))
            val mergedConfig = testConfig.withFallback(fileConfig)
            return HoconApplicationConfig(mergedConfig)
        }

        private fun newEngine(kafkaEnvironment: KafkaEnvironment) = TestApplicationEngine(createTestEnvironment {
            config = getConfig(kafkaEnvironment)
        })

        @BeforeClass
        @JvmStatic
        fun buildUp() {
            logger.info("Building up")
            engine.start(wait = true)
            logger.info("Buildup complete")
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            logger.info("Tearing down")
            wireMockServer.stop()
            engine.stop(5, 60, TimeUnit.SECONDS)
            kafkaEnvironment.tearDown()
            logger.info("Tear down complete")
        }
    }

    @Test
    fun `test isready, isalive, health og metrics`() {
        with(engine) {
            handleRequest(HttpMethod.Get, "/isready") {}.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                handleRequest(HttpMethod.Get, "/isalive") {}.apply {
                    assertEquals(HttpStatusCode.OK, response.status())
                    handleRequest(HttpMethod.Get, "/metrics") {}.apply {
                        assertEquals(HttpStatusCode.OK, response.status())
                        handleRequest(HttpMethod.Get, "/health") {}.apply {
                            assertEquals(HttpStatusCode.OK, response.status())
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `Gyldig søknad for overføring av dager blir lagt til prosessering`(){
        gyldigSoknadOverforeDagerBlirLagtTilProsessering(Azure.V1_0.generateJwt(clientId = "omsorgspenger-api", audience = "omsorgsdageroverforingsoknad-mottak"))
        gyldigSoknadOverforeDagerBlirLagtTilProsessering(Azure.V2_0.generateJwt(clientId = "omsorgspenger-api", audience = "omsorgsdageroverforingsoknad-mottak"))

    }

    private fun gyldigSoknadOverforeDagerBlirLagtTilProsessering(accessToken: String) {
        val soknad = gyldigSoknadOverforeDager(
            fodselsnummerSoker = gyldigFodselsnummerA
        )

        val soknadId = requestAndAssert(
            soknad = soknad,
            expectedCode = HttpStatusCode.Accepted,
            expectedResponse = null,
            accessToken = accessToken,
            path = "/v1/soknad/overfore-dager"
        )

        val sendtTilProsessering = hentSoknadOverforeDagerSendtTilProsessering(soknadId)
        verifiserSoknadOverforeDagerLagtTilProsessering(
            incomingJsonString = soknad,
            outgoingJsonObject = sendtTilProsessering
        )
    }

    @Test
    fun `Gyldig søknad for overføring av dager fra D-nummer blir lagt til prosessering`() {
        val soknad = gyldigSoknadOverforeDager(
            fodselsnummerSoker = dNummerA
        )

        val soknadId = requestAndAssert(
            soknad = soknad,
            expectedCode = HttpStatusCode.Accepted,
            expectedResponse = null,
            path = "/v1/soknad/overfore-dager"
        )

        val sendtTilProsessering  = hentSoknadOverforeDagerSendtTilProsessering(soknadId)
        verifiserSoknadOverforeDagerLagtTilProsessering(
            incomingJsonString = soknad,
            outgoingJsonObject = sendtTilProsessering
        )
    }

    @Test
    fun `Request fra ikke autorisert system feiler, søknad for overføring av dager`() {
        val soknad = gyldigSoknadOverforeDager(
            fodselsnummerSoker = gyldigFodselsnummerA
        )

        requestAndAssert(
            soknad = soknad,
            expectedCode = HttpStatusCode.Forbidden,
            expectedResponse = """
            {
                "type": "/problem-details/unauthorized",
                "title": "unauthorized",
                "status": 403,
                "detail": "Requesten inneholder ikke tilstrekkelige tilganger.",
                "instance": "about:blank"
            }
            """.trimIndent(),
            accessToken = unAauthorizedAccessToken,
            path = "/v1/soknad/overfore-dager"
        )
    }

    @Test
    fun `Request uten corelation id feiler, søknad for overføring av dager`() {
        val soknad = gyldigSoknadOverforeDager(
            fodselsnummerSoker = gyldigFodselsnummerA
        )

        requestAndAssert(
            soknad = soknad,
            expectedCode = HttpStatusCode.BadRequest,
            expectedResponse = """
                {
                    "type": "/problem-details/invalid-request-parameters",
                    "title": "invalid-request-parameters",
                    "detail": "Requesten inneholder ugyldige paramtere.",
                    "status": 400,
                    "instance": "about:blank",
                    "invalid_parameters" : [
                        {
                            "name" : "X-Correlation-ID",
                            "reason" : "Correlation ID må settes.",
                            "type": "header",
                            "invalid_value": null
                        }
                    ]
                }
            """.trimIndent(),
            leggTilCorrelationId = false,
            path = "/v1/soknad/overfore-dager"
        )
    }

    @Test
    fun `En ugyldig melding for overføring av dager gir valideringsfeil`() {
        val soknad = """
        {
            "søker": {
                "aktørId": "ABC"
            }
        }
        """.trimIndent()

        requestAndAssert(
            soknad = soknad,
            expectedCode = HttpStatusCode.BadRequest,
            expectedResponse = """
                {
                    "type": "/problem-details/invalid-request-parameters",
                    "title": "invalid-request-parameters",
                    "status": 400,
                    "detail": "Requesten inneholder ugyldige paramtere.",
                    "instance": "about:blank",
                    "invalid_parameters": [{
                        "type": "entity",
                        "name": "søker.aktørId",
                        "reason": "Ikke gyldig Aktør ID.",
                        "invalid_value": "ABC"
                    }]
                }
            """.trimIndent(),
            path = "/v1/soknad/overfore-dager"
        )
    }

    @Test
    fun `Gyldig melding om overføring av dager fra D-nummer blir lagt til prosessering`() {
        val soknad = gyldigSoknadOverforeDager(
            fodselsnummerSoker = dNummerA
        )

        val soknadId = requestAndAssert(
            soknad = soknad,
            expectedCode = HttpStatusCode.Accepted,
            expectedResponse = null,
            path = "/v1/soknad/overfore-dager"
        )

        val sendtTilProsessering  = hentSoknadOverforeDagerSendtTilProsessering(soknadId)
        verifiserSoknadOverforeDagerLagtTilProsessering(
            incomingJsonString = soknad,
            outgoingJsonObject = sendtTilProsessering
        )
    }

    // Utils
    private fun verifiserSoknadOverforeDagerLagtTilProsessering(
        incomingJsonString: String,
        outgoingJsonObject: JSONObject
    ) {
        val outgoing =
            SoknadOverforeDagerOutgoing(outgoingJsonObject)

        val outgoingFromIncoming = SoknadOverforeDagerIncoming(
            incomingJsonString
        )
            .medSoknadId(outgoing.soknadId)
            .somOutgoing()

        JSONAssert.assertEquals(outgoingFromIncoming.jsonObject.toString(), outgoing.jsonObject.toString(), true)
    }

    private fun requestAndAssert(soknad : String,
                                 expectedResponse : String?,
                                 expectedCode : HttpStatusCode,
                                 leggTilCorrelationId : Boolean = true,
                                 leggTilAuthorization : Boolean = true,
                                 accessToken : String = authorizedAccessToken,
                                 path:String) : String? {
        with(engine) {
            handleRequest(HttpMethod.Post, "$path") {
                if (leggTilAuthorization) {
                    addHeader(HttpHeaders.Authorization, "Bearer $accessToken")
                }
                if (leggTilCorrelationId) {
                    addHeader(HttpHeaders.XCorrelationId, "123156")
                }
                addHeader(HttpHeaders.ContentType, "application/json")
                val requestEntity = objectMapper.writeValueAsString(soknad)
                logger.info("Request Entity = $requestEntity")
                setBody(soknad)
            }.apply {
                logger.info("Response Entity = ${response.content}")
                logger.info("Expected Entity = $expectedResponse")
                assertEquals(expectedCode, response.status())
                when {
                    expectedResponse != null -> JSONAssert.assertEquals(expectedResponse, response.content!!, true)
                    HttpStatusCode.Accepted == response.status() -> {
                        val json = JSONObject(response.content!!)
                        assertEquals(1, json.keySet().size)
                        val soknadId = json.getString("id")
                        assertNotNull(soknadId)
                        return soknadId
                    }
                    else -> assertEquals(expectedResponse, response.content)
                }

            }
        }
        return null
    }

    private fun gyldigSoknadOverforeDager(
        fodselsnummerSoker : String
    ) : String =
        """
        {
            "søker": {
                "fødselsnummer": "$fodselsnummerSoker",
                "aktørId": "123456"
            },
            "hvilke_som_helst_andre_atributter": {
                  "språk": "nb",
                  "arbeidssituasjon": ["arbeidstaker"],
                  "medlemskap": {
                    "harBoddIUtlandetSiste12Mnd": false,
                    "utenlandsoppholdSiste12Mnd": [],
                    "skalBoIUtlandetNeste12Mnd": false,
                    "utenlandsoppholdNeste12Mnd": []
                  },
                  "harForståttRettigheterOgPlikter": true,
                  "harBekreftetOpplysninger": true,
                  "antallDager": 5,
                  "mottakerAvDagerNorskIdentifikator": "$gyldigFodselsnummerB",
                  "harSamfunnskritiskJobb": true
                }
            }
        """.trimIndent()

    private fun hentSoknadOverforeDagerSendtTilProsessering(soknadId: String?) : JSONObject {
        assertNotNull(soknadId)
        return kafkaTestConsumer.hentSoknad(soknadId, topic = Topics.MOTTATT_OVERFORE_DAGER).data
    }

}