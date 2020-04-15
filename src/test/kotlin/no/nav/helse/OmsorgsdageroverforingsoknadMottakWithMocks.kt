package no.nav.helse

import io.ktor.server.testing.withApplication
import no.nav.helse.dusseldorf.testsupport.asArguments
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OmsorgsdageroverforingsoknadMottakWithMocks {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(OmsorgsdageroverforingsoknadMottakWithMocks::class.java)


        @JvmStatic
        fun main(args: Array<String>) {

            val token = Azure.V2_0.generateJwt(clientId = "azure-client-2", audience = "omsorgsdageroverforingsoknad-mottak")
            logger.info("Token ---> {}", token)

            val wireMockServer = WireMockBuilder()
                .withPort(8141)
                .withAzureSupport()
                .build()
                .stubK9DokumentHealth()
                .stubLagreDokument()
                .stubAktoerRegisterGetAktoerId("02119970078", "1234561")

            val kafkaEnvironment = KafkaWrapper.bootstrap()

            val testArgs = TestConfiguration.asMap(
                wireMockServer = wireMockServer,
                kafkaEnvironment = kafkaEnvironment,
                port = 8142
            ).asArguments()

            Runtime.getRuntime().addShutdownHook(object : Thread() {
                override fun run() {
                    logger.info("Tearing down")
                    wireMockServer.stop()
                    kafkaEnvironment.tearDown()
                    logger.info("Tear down complete")
                }
            })

            withApplication { no.nav.helse.main(testArgs) }
        }
    }
}