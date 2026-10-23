# Entwicklungsnotizen

Notiere nach jedem abgeschlossenen Schritt, wobei Copilot geholfen hat und was du selbst korrigiert hast. So bleibt die Entstehungsgeschichte des Lernprojekts nachvollziehbar.

## Schritt 1 · 2026-10-22/23

### Was Claude umgesetzt hat

- `persistence.xml` mit JTA-Persistence-Unit `LvsPU` (zeigt auf `java:jboss/datasources/LvsDS`, kein Schema-Generation durch Hibernate)
- Flyway-Migration `V1__init.sql`: alle 6 Tabellen + 11 Testlagerplätze in 2 Gassen + 1 Kaltlager-Platz
- `FlywayInitializer.kt` als `@Singleton @Startup`-EJB
- JAX-RS Application (`@ApplicationPath("/api")`), DTOs als `data class`
- REST-Ressourcen als `@Stateless`-EJBs: `POST /api/asn`, `POST /api/goods-receipt`, `GET /api/locations`, `GET /api/storage-orders/{id}`
- `LvsExceptionMapper` mit HTTP 404 (`ResourceNotFound`) und HTTP 409 (Geschäftsfehler)

### Fehler und Lösungen beim Deployment

#### 1 · Maven läuft mit Java 17, Projekt kompiliert für Java 21
**Fehler:** `com/lvslab/lvs/domain/SlottingStrategyTest has been compiled by a more recent version of the Java Runtime (class file version 65.0), this version of the Java Runtime only recognizes class file versions up to 61.0`

**Ursache:** Homebrew-Maven nutzt standardmäßig Java 17 als JVM, kompiliert aber mit dem Kotlin-Plugin auf Java 21.

**Lösung:** Maven immer mit explizitem `JAVA_HOME` aufrufen:
```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.7/libexec/openjdk.jdk/Contents/Home mvn …
```
Dauerhaft in `~/.zshrc` setzen.

---

#### 2 · `@PostConstruct` fehlte in FlywayInitializer
**Fehler:** `relation "storage_location" does not exist` beim ersten REST-Aufruf.

**Ursache:** Die `init()`-Methode war nicht mit `@PostConstruct` annotiert, deshalb rief der EJB-Container sie beim Start nicht auf – Flyway lief nie.

**Lösung:** `@PostConstruct` über `init()` ergänzt.

---

#### 3 · Flyway kann Auto-Commit der JTA-Connection nicht einschalten
**Fehler:** `Unable to turn on auto-commit for the connection`

**Ursache:** WildFly gibt Flyway eine JTA-verwaltete Connection. JTA-Connections verbieten Auto-Commit, weil die Transaktion vom Container gesteuert wird. Flyways interne Schema-Prüfung setzt aber Auto-Commit voraus.

**Lösung:** Flyway bekommt eine eigene direkte JDBC-URL statt der JTA-Datasource. Verbindungsparameter kommen aus Umgebungsvariablen (`DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`), die in `docker-compose.yml` gesetzt werden.

---

#### 4 · PostgreSQL-JDBC-Treiber im WAR-Classpath fehlte
**Fehler:** `No database found to handle jdbc:postgresql://postgres:5432/lvslab`

**Ursache:** Der PostgreSQL-Treiber liegt als WildFly-Modul vor und ist für JTA-Verbindungen sichtbar – aber **nicht** für Bibliotheken innerhalb der WAR wie Flyway. Flyway findet daher keinen passenden Treiber für die direkte JDBC-URL.

**Lösung:** PostgreSQL-JDBC-Treiber (`org.postgresql:postgresql:42.7.5`) als reguläre Abhängigkeit (kein `provided`-Scope) in die `lvs-service/pom.xml` aufgenommen, damit er ins WAR eingepackt wird.

---

#### Hinweis: Port 5433 vs. 5432
PostgreSQL ist im Host auf Port **5433** gemappt (da 5432 bereits belegt war). Innerhalb des Docker-Netzwerks kommunizieren die Container aber direkt über Port **5432**. Flyway und WildFly nutzen deshalb intern `postgres:5432`.

---

## Schritt 0 · 2026-10-20

- Copilot hat Parent-POM, Kotlin- und Java-WAR, minimale Unit-Tests sowie WildFly-, PostgreSQL- und JMS-Konfiguration angelegt.
- Beim Build wurde die Kotlin-Plugin-Konfiguration so angepasst, dass auch der Kotlin-Test kompiliert und ausgeführt wird.
- Der Host-Port 5432 war bereits belegt; PostgreSQL ist deshalb lokal auf Port 5433 veröffentlicht.
- WildFly brauchte nach dem Image-Build passende Besitzrechte für seine Laufzeitverzeichnisse.
- Verifiziert: `mvn clean package` erfolgreich, beide Unit-Tests grün; Compose startet PostgreSQL und WildFly, Datasource/Queues vorhanden und beide WARs deployed.
