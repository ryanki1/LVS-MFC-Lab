# LVS/MFR Lab – Umsetzungsplan (Mini-Version)

Diese Datei gehört nach `docs/PLAN.md`. Jeder Schritt ist so geschrieben, dass du ihn einzeln an GitHub Copilot im Agent-Modus übergeben kannst.

**Ziel der Mini-Version:** In drei Abenden plus einem Abend Wiederholung Jakarta EE auf WildFly aus eigener Praxis erklären können, **mit Kotlin und Java**: Jakarta REST, CDI, JPA, Transaktionen, JMS mit Message-Driven Beans und die Migration von `javax` auf `jakarta`. Dazu die Fachsprache der Intralogistik (LVS, MFR, Transportauftrag, Quittierung).

**Sprachaufteilung:** `lvs-service` in **Kotlin**, `mfr-service` und `legacy/javax-inventory` in **Java**. Beide Services laufen im selben WildFly und sprechen über JMS miteinander. So kannst du Kotlin- und Java-Projekte gleichermaßen bedienen.

---

## So arbeitest du mit Copilot

1. `.github/copilot-instructions.md` liegt im Repo. Copilot liest die Projektregeln dann automatisch mit.
2. Öffne pro Schritt einen **neuen** Chat im Agent-Modus und hänge diese Datei an (`#file:docs/PLAN.md`).
3. Kopiere den Prompt des Schritts. Lass dir zuerst den Plan zeigen, dann erst umsetzen.
4. Lies jeden Diff, bevor du ihn annimmst. Frag nach, wenn du eine Zeile nicht erklären könntest – genau das wird im Interview gefragt.
5. Führe die Tests selbst aus. Erst wenn „Fertig, wenn“ erfüllt ist: committen, Haken setzen, nächster Schritt.

Notiere dir nach jedem Schritt in `docs/notes.md`, wo Copilot geholfen hat und wo du korrigieren musstest. Das ist dein Beispiel für KI-gestützte Entwicklung im Gespräch.

**IDE-Hinweis:** In VS Code brauchst du das „Extension Pack for Java“ und „Kotlin by JetBrains“. Öffne den Ordner mit der Parent-`pom.xml` als Arbeitsbereich, sonst findet die Kotlin-Erweiterung das Projekt nicht (wie beim OTA-Projekt). Bei Kotlin und Java gemischt ist IntelliJ IDEA die robustere Wahl.

---

## Zielbild: Datenfluss

1. Das **Host-System** schickt ein **Avis** per REST an das LVS.
2. Beim **Wareneingang** wird ein Ladungsträger erfasst und gegen das Avis geprüft.
3. Das **LVS** vergibt einen freien Lagerplatz, reserviert ihn und legt einen **Einlagerauftrag** an.
4. Das LVS sendet `TransportRequested` über die JMS-Queue `TransportRequests` an den **MFR**.
5. Der **MFR** legt einen **Transportauftrag** an und gibt ihn an die Fördertechnik (in der Mini-Version simuliert, später der Python-SPS-Simulator).
6. Die Fördertechnik meldet **Quittierung** oder **Störung**. Der MFR schickt das Ergebnis über die Queue `TransportResults` zurück.
7. Das LVS **bucht den Bestand** auf den Zielplatz. Bei einer Störung wird der Platz gesperrt und ein neuer Platz vergeben.

```
Host ──REST──▶ lvs-service ──JMS: TransportRequests──▶ mfr-service ──▶ Fördertechnik (simuliert)
                    ▲                                        │
                    └──────────JMS: TransportResults─────────┘
```

Beide Services laufen als getrennte WARs **im selben WildFly**. Die JMS-Queues liegen im eingebetteten ActiveMQ Artemis des Profils `standalone-full`.

---

## Repo-Struktur

```
lvs-mfr-lab/
├── .github/
│   └── copilot-instructions.md
├── pom.xml                       # Parent-POM: Java 21, Jakarta EE 10, Testabhängigkeiten
├── services/
│   ├── lvs-service/              # Kotlin · WAR: Avis, Wareneingang, Lagerplätze, Bestand
│   │   ├── pom.xml               # kotlin-maven-plugin mit all-open und no-arg
│   │   ├── src/main/kotlin/com/lvslab/lvs/   # domain/, api/, persistence/, messaging/
│   │   ├── src/main/resources/db/migration/
│   │   └── src/test/kotlin/
│   └── mfr-service/              # Java · WAR: Transportaufträge, Message-Driven Bean
│       ├── src/main/java/com/lvslab/mfr/
│       └── src/test/java/
├── legacy/
│   └── javax-inventory/          # bewusst altes Java-EE-8-Modul für die Migrationsübung
├── deploy/
│   └── wildfly/
│       ├── Dockerfile
│       └── configure.cli         # Datasource, JDBC-Treiber, JMS-Queues
├── tools/
│   └── plc-simulator/            # Python (Vollversion)
├── frontend/
│   └── dashboard/                # Vollversion
├── docs/
│   ├── PLAN.md
│   ├── notes.md
│   └── adr/
├── docker-compose.yml            # PostgreSQL + WildFly mit beiden WARs
├── .gitignore
└── README.md
```

---

## Schritt 0 · Repo-Gerüst und WildFly (vor Abend 1, ca. 45 Min.)

**Ziel:** Ein WildFly mit PostgreSQL-Datasource und zwei JMS-Queues startet per Docker Compose.

**Prompt für Copilot:**
> Lege das Repo-Gerüst gemäß „Repo-Struktur“ in docs/PLAN.md an: Parent-`pom.xml` (Java 21, `jakarta.platform:jakarta.jakartaee-api` 10 als provided, JUnit 5, Mockito, MockK, Testcontainers, REST Assured im dependencyManagement), leere Module `services/lvs-service` (Kotlin) und `services/mfr-service` (Java) mit Packaging `war`, eine `.gitignore` für Maven, Node, Python, Terraform und IDEs.
> Konfiguriere im lvs-service das `kotlin-maven-plugin` mit den Compiler-Plugins **all-open** und **no-arg** (Preset `jpa` plus die CDI- und EJB-Annotationen aus copilot-instructions.md) und packe die Kotlin-Standardbibliothek ins WAR. Lege je eine minimale Kotlin-Klasse und einen Test an, damit der Build die Konfiguration prüft. Erkläre mir, welches Problem all-open und no-arg auf einem Applikationsserver lösen.
> Erstelle `deploy/wildfly/Dockerfile` auf Basis des offiziellen WildFly-Images mit dem Profil `standalone-full` und ein `configure.cli`, das den PostgreSQL-JDBC-Treiber als Modul installiert, die Datasource `java:jboss/datasources/LvsDS` anlegt und die JMS-Queues `TransportRequests` und `TransportResults` mit den JNDI-Namen `java:/jms/queue/TransportRequests` und `java:/jms/queue/TransportResults` definiert.
> Erstelle eine `docker-compose.yml` mit PostgreSQL 16 und diesem WildFly, die WARs aus `target/` werden in das Deployment-Verzeichnis kopiert.
> Erkläre mir den Unterschied zwischen `standalone.xml` und `standalone-full.xml` und warum wir die Konfiguration per CLI-Skript machen. Zeig mir zuerst die geplanten Dateien.

**Fertig, wenn:**
- [ ] `mvn clean package` baut beide (noch leeren) WARs, der Kotlin-Test läuft grün
- [ ] `docker compose up` startet PostgreSQL und WildFly ohne Fehler
- [ ] Die WildFly-Admin-Konsole oder `jboss-cli` zeigt Datasource und beide Queues
- [ ] Erster Commit auf `main`

---

## Schritt 1 · lvs-service in Kotlin mit Jakarta REST, CDI und JPA (Abend 1)

**Ziel:** Das LVS nimmt Avise an, erfasst Ladungsträger und vergibt Lagerplätze – in Kotlin auf WildFly.

**Prompt für Copilot:**
> Implementiere im lvs-service **in Kotlin** (Regeln für Kotlin auf Jakarta EE aus copilot-instructions.md beachten):
> - JPA-Entitäten `Article`, `StorageLocation` (aisle, level, slot, type, status FREE/OCCUPIED/RESERVED/BLOCKED), `LoadCarrier`, `AdvanceShippingNotice` mit Positionen, `StorageOrder` und `StockMovement`. Schema per Flyway (`V1__init.sql`) mit einigen Testplätzen in zwei Gassen; Flyway läuft beim Start über eine `@Startup @Singleton`-EJB mit der Datasource aus JNDI.
> - Jakarta-REST-Ressourcen: `POST /api/asn` (Avis anlegen), `POST /api/goods-receipt` (Ladungsträger zu einer Avis-Position erfassen), `GET /api/locations?status=FREE`, `GET /api/storage-orders/{id}`.
> - Eine CDI-Bean `SlottingStrategy`: wählt den ersten freien Platz passenden Typs in der Gasse mit den meisten freien Plätzen und setzt ihn auf RESERVED. Beim Wareneingang entsteht ein `StorageOrder`.
> - Regeln: Ein Ladungsträger darf nur zu einer existierenden Avis-Position erfasst werden, die Menge darf die erwartete Menge nicht überschreiten, und ohne freien Platz gibt es einen fachlichen Fehler.
> - Fachliche Fehler über eigene Exceptions und einen `ExceptionMapper`, der HTTP 409 mit einer verständlichen JSON-Fehlermeldung liefert.
> - Unit-Tests mit JUnit 5 und MockK für `SlottingStrategy` und die Regeln, ein Integrationstest mit Testcontainers und REST Assured gegen den WildFly-Container für den Ablauf Avis → Wareneingang → Einlagerauftrag.
> - Entitäten als normale Klassen, DTOs als `data class`, Status als `enum class`, fachliche Fehler als `sealed class`-Hierarchie.
> Erkläre mir dabei jeweils die Entsprechung in Spring: `@Path` gegenüber `@RestController`, CDI-Scopes gegenüber Spring-Beans, `@PersistenceContext` gegenüber Spring Data, `ExceptionMapper` gegenüber `@RestControllerAdvice`. Erkläre außerdem, warum CDI-Beans mit normalem Scope einen Konstruktor ohne Parameter brauchen und nicht final sein dürfen, und wie all-open und no-arg das in Kotlin lösen.

**Fertig, wenn:**
- [ ] `mvn verify` ist grün, inklusive Integrationstest
- [ ] Avis → Wareneingang erzeugt einen Einlagerauftrag und reserviert einen Platz
- [ ] Überlieferung oder ein volles Lager führen zu HTTP 409
- [ ] Du kannst erklären: CDI-Scopes und Proxies, Persistence Context, wann eine EJB statt einer CDI-Bean sinnvoll ist
- [ ] Du kannst erklären, warum eine JPA-Entität in Kotlin keine `data class` sein sollte

**Interview-Satz:** „Spring Boot habe ich produktiv eingesetzt, Jakarta EE auf WildFly habe ich mir in einem eigenen Projekt nah an einem LVS erarbeitet, einen Service in Kotlin, einen in Java. Die Konzepte sind dieselben, aber Laufzeitumgebung, Konfiguration und Transaktionen liegen beim Applikationsserver.“

---

## Schritt 2 · Transportaufträge über JMS (Abend 2)

**Ziel:** LVS und MFR sprechen asynchron über JMS-Queues, mit sauberen Transaktionen.

**Prompt für Copilot:**
> Ergänze den Ablauf zwischen lvs-service (Kotlin) und mfr-service (**Java 21**):
> - Das LVS sendet nach dem Anlegen eines Einlagerauftrags eine Nachricht `TransportRequested` (storageOrderId, loadCarrierId, source, target) als JSON über `JMSContext` an `java:/jms/queue/TransportRequests` – **in derselben Transaktion** wie das Speichern des Auftrags.
> - Der mfr-service empfängt sie in einer Message-Driven Bean in Java (DTOs als `record`), legt einen `TransportOrder` mit Status CREATED → SENT an und simuliert die Fördertechnik: mit einer konfigurierbaren Wahrscheinlichkeit Quittierung, sonst Störung. Das Ergebnis geht als `TransportConfirmed` oder `TransportFaulted` an `java:/jms/queue/TransportResults`.
> - Das LVS empfängt das Ergebnis in einer eigenen Message-Driven Bean: Bei Quittierung wird der Platz OCCUPIED, der Ladungsträger auf den Platz gebucht und eine `StockMovement` geschrieben. Bei Störung wird der Platz BLOCKED, ein neuer Platz vergeben und ein neuer Transport angefordert.
> - Die Verarbeitung muss **idempotent** sein: Kommt dieselbe Quittierung zweimal, darf nicht doppelt gebucht werden.
> - Tests: Unit-Tests für die Buchungs- und Störungslogik, ein Integrationstest, der den kompletten Durchlauf bis zur Bestandsbuchung prüft.
> Zeig mir an der Message-Driven Bean im LVS, wie dasselbe in Kotlin und in Java aussieht, und welche Unterschiede bei Null-Safety und JSON-Mapping auffallen.
> Erkläre mir an diesem Beispiel: JTA-Transaktionen über Datenbank und JMS hinweg, was passiert, wenn die Datenbank nach dem Senden scheitert, Rollback und Redelivery bei MDBs, Dead Letter Queue, und warum Idempotenz trotzdem nötig ist.

**Fertig, wenn:**
- [ ] Ein Wareneingang endet automatisch mit einer Bestandsbuchung auf dem Zielplatz
- [ ] Eine simulierte Störung sperrt den Platz und führt zu einer neuen Platzvergabe
- [ ] Eine doppelte Quittierung bucht nicht doppelt
- [ ] `docs/adr/0001-jms-statt-kafka.md` geschrieben – **von dir selbst**, nicht von Copilot

**Inhalt des ADR (deine Worte, Stichpunkte):**
- Kontext: Aufträge zwischen LVS und MFR müssen zuverlässig genau einmal fachlich wirksam werden.
- Entscheidung: JMS-Queues im Applikationsserver.
- Vergleich: Queue mit Punkt-zu-Punkt-Zustellung und JTA-Transaktion (JMS, wie RabbitMQ bei TGW) gegenüber Kafka als Ereignis-Log (aus dem OTA-Lernprojekt).
- Konsequenz: Redelivery bei Fehlern, deshalb idempotente Verarbeitung; Dead Letter Queue für Nachrichten, die immer wieder scheitern.

**Interview-Satz:** „Bei TGW habe ich die Kommunikation zur Fördertechnik über Messaging gebaut. Im Lernprojekt habe ich dasselbe Muster mit JMS und Message-Driven Beans auf WildFly umgesetzt, inklusive Transaktionen und idempotenter Quittierung.“

---

## Schritt 3 · Migration von `javax` auf `jakarta` (Abend 3)

**Ziel:** Verstehen, was bei der Umstellung bestehender Java-EE-Anwendungen passiert – ein häufiges Thema in gewachsenen Systemen.

**Prompt für Copilot (Teil 1):**
> Lege unter `legacy/javax-inventory` ein kleines, bewusst altes Java-EE-8-Modul an: eine JAX-RS-Ressource mit `javax.ws.rs`, eine JPA-Entität mit `javax.persistence`, eine CDI-Bean mit `javax.inject`, eine `persistence.xml` und eine `beans.xml` mit den alten Namespaces, Abhängigkeit `javax:javaee-api:8.0`. Es soll für sich bauen. Erkläre mir, warum es überhaupt zwei Namensräume gibt.

**Danach selbst ausführen (mit Hilfe von Copilot beim Befehl):**
- [ ] Migration mit **OpenRewrite** über das Maven-Plugin und das Rezept zur Umstellung von `javax` auf `jakarta`
- [ ] Den Diff selbst durchgehen: Imports, Abhängigkeiten, XML-Namespaces in `persistence.xml` und `beans.xml`

**Prompt für Copilot (Teil 2):**
> Prüfe die migrierte Version von `legacy/javax-inventory`: Was hat OpenRewrite automatisch umgestellt, was nicht? Welche Stolpersteine gibt es typischerweise bei echten Projekten (Bibliotheken, die noch `javax` verwenden, Reflection, Konfigurationsdateien, Applikationsserver-Version)? Bring das Modul auf WildFly mit Jakarta EE 10 zum Laufen.

**Fertig, wenn:**
- [ ] Das migrierte Modul baut und läuft auf WildFly
- [ ] Du kannst in zwei Sätzen erklären, warum der Namensraum gewechselt hat und was bei einer Migration bricht

---

## Abend 4 · Fachsprache und Generalprobe (ohne Copilot)

**Fachbegriffe sicher erklären können:**
- [ ] Unterschied **ERP / Host – LVS – MFR – SPS**: wer entscheidet was, wer spricht mit wem
- [ ] Avis, Wareneingang, Einlagerung, Auslagerung, Kommissionierung, Inventur
- [ ] Transportauftrag, Quittierung, Störung, Platzsperre, Umplanung
- [ ] Platzvergabestrategien: erster freier Platz, Gleichverteilung über Gassen, ABC-Zonen
- [ ] FIFO und Chargen in der Auslagerung
- [ ] Telegramm-Kommunikation zwischen MFR und SPS in Grundzügen

**Spickzettel Jakarta EE:**

| Wenn sie fragen … | Kern deiner Antwort |
| --- | --- |
| Jakarta EE oder Spring? | Spring produktiv (TGW, MBTI Cockpit). Jakarta EE auf WildFly im Lernprojekt: Container verwaltet Transaktionen, Ressourcen und Messaging; die Konzepte (DI, REST, JPA) sind dieselben. |
| Kotlin oder Java? | Java produktiv, zuletzt Java 17 bei TGW. Kotlin in zwei Lernprojekten, mit Spring Boot und auf Jakarta EE. Auf einem Applikationsserver braucht Kotlin die Compiler-Plugins all-open und no-arg, weil Klassen sonst final sind und Proxies scheitern. Entitäten keine `data class`. |
| CDI oder EJB? | CDI für normale Beans und Injection. EJB, wenn ich Container-Dienste brauche: `@MessageDriven` für JMS, `@Singleton @Startup` beim Start, früher auch für Transaktionen und Pooling. |
| Wie laufen Transaktionen? | JTA über den Container, `@Transactional` oder container-verwaltete EJB-Transaktionen; Datenbank und JMS in einer Transaktion; bei Fehlern Rollback und Redelivery. |
| Was ist der Persistence Context? | Der Cache verwalteter Entitäten pro Transaktion; Änderungen werden beim Commit geschrieben. Typische Fallen: Lazy Loading außerhalb der Transaktion, N+1-Abfragen. |
| `javax` gegen `jakarta`? | Mit der Übergabe an die Eclipse Foundation musste der Namensraum wechseln; ab Jakarta EE 9 heißt alles `jakarta.*`. Migration mit OpenRewrite, Stolpersteine sind alte Bibliotheken und XML-Konfiguration. |
| Messaging zur Fördertechnik? | TGW: RabbitMQ zu SPS und Ladungsträgergeräten, REST zum Host, Keycloak, .NET-Service-Layer daneben. Im Lernprojekt dasselbe Muster mit JMS. |
| KI-gestützte Entwicklung? | Bei TGW Copilot mit Claude genutzt, DSGVO-konform; Features im Sprint inklusive Tests und Doku fertig. Im Lernprojekt dokumentiert, wo die KI geholfen hat und wo ich korrigieren musste. |

- [ ] Projekt in fünf Minuten zeigen: Avis per REST, Einlagerauftrag, JMS-Durchlauf, Bestandsbuchung, Störung mit Umplanung
- [ ] TGW-Geschichte in zwei Minuten

---

## Danach: Vollversion

- **plc-simulator** in Python: der MFR schickt Transportaufträge per REST, der Simulator quittiert zeitverzögert oder meldet Störungen
- **Auslagerung und Kommissionierung** mit FIFO
- **Dashboard** mit Lagerplatzübersicht pro Gasse und offenen Transportaufträgen
- **Keycloak** über WildFly Elytron OIDC
- **CI** mit GitHub Actions oder Azure DevOps, Image in einer Registry
- **Observability** mit MicroProfile Health und Metrics
- Optional: Betrieb auf Kubernetes, Infrastruktur mit Terraform
