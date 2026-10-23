package com.lvslab.lvs

import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.images.builder.ImageFromDockerfile
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.nio.file.Path
import java.sql.DriverManager
import java.time.Duration

@Testcontainers
class GoodsReceiptIT {

    companion object {
        private val network: Network = Network.newNetwork()

        // services/lvs-service → services → Projekt-Root
        private val projectRoot: Path = Path.of("").toAbsolutePath().parent.parent

        @Container
        @JvmField
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16")
            .withNetwork(network)
            .withNetworkAliases("postgres")
            .withDatabaseName("lvslab")
            .withUsername("lvs_test")
            .withPassword("lvs_test")

        @Container
        @JvmField
        val wildfly: GenericContainer<*> = GenericContainer(
            ImageFromDockerfile()
                .withFileFromPath("deploy/wildfly/Dockerfile",         projectRoot.resolve("deploy/wildfly/Dockerfile"))
                .withFileFromPath("deploy/wildfly/configure.cli",      projectRoot.resolve("deploy/wildfly/configure.cli"))
                .withFileFromPath("deploy/wildfly/postgresql-module.xml", projectRoot.resolve("deploy/wildfly/postgresql-module.xml"))
                .withFileFromPath(
                    "services/lvs-service/target/lvs-service-1.0.0-SNAPSHOT.war",
                    Path.of("target/lvs-service-1.0.0-SNAPSHOT.war").toAbsolutePath()
                )
                .withFileFromPath(
                    "services/mfr-service/target/mfr-service-1.0.0-SNAPSHOT.war",
                    projectRoot.resolve("services/mfr-service/target/mfr-service-1.0.0-SNAPSHOT.war")
                )
                .withDockerfilePath("deploy/wildfly/Dockerfile")
        )
            .withNetwork(network)
            .withEnv("DB_USER", "lvs_test")
            .withEnv("DB_PASSWORD", "lvs_test")
            .withEnv("DB_HOST", "postgres")
            .withEnv("DB_NAME", "lvslab")
            .withExposedPorts(8080)
            .waitingFor(
                Wait.forHttp("/lvs-service/api/locations")
                    .forPort(8080)
                    .withStartupTimeout(Duration.ofMinutes(3))
            )
            .dependsOn(postgres)

        @BeforeAll
        @JvmStatic
        fun setup() {
            RestAssured.baseURI = "http://${wildfly.host}:${wildfly.getMappedPort(8080)}/lvs-service/api"

            DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
                conn.createStatement().execute(
                    """INSERT INTO article (article_number, description, storage_type)
                       VALUES ('ART-IT-001', 'IT-Testartikel Schrauben M8', 'STANDARD')
                       ON CONFLICT DO NOTHING"""
                )
            }
        }
    }

    @Test
    fun `Avis anlegen und Wareneingang erfassen erzeugt Einlagerauftrag mit reserviertem Platz`() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("""{"noticeNumber":"ASN-IT-001","positions":[{"articleNumber":"ART-IT-001","expectedQuantity":30}]}""")
            .post("/asn")
            .then()
            .statusCode(201)
            .body("id", notNullValue())

        val storageOrderId = RestAssured.given()
            .contentType(ContentType.JSON)
            .body("""{"noticeNumber":"ASN-IT-001","articleNumber":"ART-IT-001","carrierCode":"LT-IT-001","quantity":10}""")
            .post("/goods-receipt")
            .then()
            .statusCode(201)
            .body("storageOrderId", notNullValue())
            .body("targetLocationId", notNullValue())
            .extract().path<Int>("storageOrderId")

        RestAssured.given()
            .get("/storage-orders/$storageOrderId")
            .then()
            .statusCode(200)
            .body("status", equalTo("CREATED"))
            .body("loadCarrierCode", equalTo("LT-IT-001"))

        RestAssured.given()
            .get("/locations?status=RESERVED")
            .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(1))
    }

    @Test
    fun `Überlieferung liefert HTTP 409 mit Fehlermeldung`() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("""{"noticeNumber":"ASN-IT-002","positions":[{"articleNumber":"ART-IT-001","expectedQuantity":5}]}""")
            .post("/asn")
            .then()
            .statusCode(201)

        // Erst 5 Stück – genau die erlaubte Menge
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("""{"noticeNumber":"ASN-IT-002","articleNumber":"ART-IT-001","carrierCode":"LT-IT-002","quantity":5}""")
            .post("/goods-receipt")
            .then()
            .statusCode(201)

        // 1 weiterer Ladungsträger → Überlieferung → 409
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("""{"noticeNumber":"ASN-IT-002","articleNumber":"ART-IT-001","carrierCode":"LT-IT-003","quantity":1}""")
            .post("/goods-receipt")
            .then()
            .statusCode(409)
            .body("message", containsString("Überlieferung"))
    }
}
