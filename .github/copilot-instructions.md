# Projektregeln für GitHub Copilot – LVS/MFR Lab

Diese Datei gehört nach `.github/copilot-instructions.md`. Copilot liest sie bei jeder Anfrage in diesem Repo mit.

## Worum es geht

LVS/MFR Lab ist ein **Lernprojekt**. Es bildet im Kleinen ein Lagerverwaltungssystem (LVS) mit Materialflussrechner (MFR) für ein automatisches Lager nach:
Avis vom Host-System → Wareneingang → Lagerplatz vergeben → Transportauftrag an die Fördertechnik → Quittierung → Bestand buchen.

Das Projekt ist bewusst **fiktiv**. Verwende keine Namen realer Firmen, Produkte oder Kunden und keine Details aus echten Kundenprojekten.

Ich nutze das Projekt, um **Jakarta EE** auf einem Applikationsserver zu üben, und zwar mit **Kotlin und Java** nebeneinander. Spring Boot kenne ich bereits. Deshalb gilt:
- **Erkläre jedes Jakarta-EE-Konzept im Vergleich zu Spring** (z. B. CDI `@Inject` gegenüber `@Autowired`, `ExceptionMapper` gegenüber `@RestControllerAdvice`).
- **Erkläre im Kotlin-Code, wo sich Kotlin und Jakarta EE reiben** und wie wir das lösen (finale Klassen, Konstruktoren ohne Parameter, Nullability bei JPA).

## Arbeitsweise

- Arbeite immer nur an **einem Schritt** aus `docs/PLAN.md`. Beginne keinen neuen Schritt ohne meine Zustimmung.
- Zeig mir vor jedem Schritt kurz, welche Dateien du anlegst oder änderst. Warte auf mein OK.
- Jeder Schritt endet mit grünen Tests. Ein Schritt ohne Tests ist nicht fertig.
- Wenn ich sage, dass ich ein Konzept verstanden habe, stell mir zwei bis drei kurze Kontrollfragen dazu.
- Kleine Commits mit Conventional Commits, z. B. `feat(lvs-service): add slotting strategy`.
- Führe **niemals** `terraform apply` oder `terraform destroy` aus. `terraform fmt`, `validate` und `plan` sind erlaubt.
- Keine Secrets oder Passwörter im Code. Nutze Umgebungsvariablen oder `.env` (in `.gitignore`).

## Stack und Konventionen

### Sprachen pro Modul
| Modul | Sprache | Grund |
| --- | --- | --- |
| `services/lvs-service` | **Kotlin** | Kotlin auf Jakarta EE üben |
| `services/mfr-service` | **Java 21** | Klassisches Java EE, wie in den meisten bestehenden JEE-Systemen |
| `legacy/javax-inventory` | **Java** | Migrationsübung `javax` → `jakarta` |

Halte dich strikt an diese Zuordnung. Gemeinsame Nachrichtenformate (JMS-JSON) werden in beiden Sprachen getrennt definiert, kein gemeinsames Code-Modul.

### Backend (services/*)
- **Jakarta EE 10** (Web- bzw. Full-Profile), Deployment als **WAR** auf **WildFly** mit dem Profil `standalone-full` (enthält Jakarta Messaging mit eingebettetem ActiveMQ Artemis). JVM-Ziel Java 21.
- Build mit **Maven**, Multi-Modul-Projekt mit Parent-POM im Root. Abhängigkeit `jakarta.platform:jakarta.jakartaee-api` als `provided`.
- **Kein Spring**, kein Spring-Ersatz-Framework, kein Lombok.

### Kotlin auf Jakarta EE (lvs-service)
- Build mit dem `kotlin-maven-plugin`, Kotlin-Standardbibliothek wird **mit ins WAR** gepackt (nicht `provided`).
- Kotlin-Klassen sind standardmäßig `final`, CDI und EJB brauchen aber überschreibbare Klassen für Proxies und Interceptoren (z. B. `@Transactional`). Lösung: Compiler-Plugin **all-open** für die Annotationen `@ApplicationScoped`, `@RequestScoped`, `@Dependent`, `@Stateless`, `@Singleton`, `@MessageDriven`, `@Path`, `@Transactional` und `@Entity`.
- JPA-Entitäten und CDI-Proxies brauchen einen Konstruktor ohne Parameter. Lösung: Compiler-Plugin **no-arg** mit dem Preset `jpa` und zusätzlich für die CDI-Scope-Annotationen.
- JPA-Entitäten als normale `class`, **keine `data class`** (wegen `equals`/`hashCode` mit Lazy-Proxies). IDs nullable (`Long?`) bis zur Persistierung.
- DTOs als `data class`. Für JSON-B entweder ein Konstruktor mit `@JsonbCreator` und `@JsonbProperty` oder no-arg-Unterstützung – erkläre die Wahl.
- Konstruktor-Injection mit `@Inject constructor(...)`. Erkläre, warum das mit dem no-arg-Plugin zusammen funktioniert.
- Tests mit JUnit 5 und **MockK**.

### Java (mfr-service)
- Java 21 mit `record` für DTOs, CDI-Feld- oder Konstruktor-Injection, Tests mit JUnit 5 und **Mockito**.
- Paketstruktur pro Service: `com.lvslab.<service>.domain`, `.api`, `.persistence`, `.messaging`.
- REST mit **Jakarta REST** (`@Path`, `@GET`, `@POST`, `Response`), Fehlerbehandlung mit `ExceptionMapper`.
- Dependency Injection mit **CDI** (`@ApplicationScoped`, `@RequestScoped`, `@Inject`). EJB (`@Stateless`, `@Singleton`, `@MessageDriven`) nur dort, wo es fachlich passt, und mit Begründung.
- Transaktionen mit `jakarta.transaction.Transactional` oder container-verwalteten EJB-Transaktionen. Erkläre, welche Variante du wählst und warum.
- Persistenz mit **Jakarta Persistence (JPA)** und Hibernate, `EntityManager` per `@PersistenceContext`. PostgreSQL als Datenbank, Datasource in WildFly konfiguriert (JNDI `java:jboss/datasources/LvsDS`).
- Schema mit **Flyway**, ausgeführt beim Start über eine `@Startup @Singleton`-EJB.
- JSON über **Jakarta JSON Binding**.
- Messaging mit **Jakarta Messaging (JMS)**: Senden über `JMSContext`, Empfangen über Message-Driven Beans.
- Integrationstests mit **Testcontainers** und **REST Assured** gegen das WildFly-Docker-Image.

### Server-Konfiguration (deploy/wildfly)
- Eigenes `Dockerfile` auf Basis des offiziellen WildFly-Images.
- Datasource, JDBC-Treiber und JMS-Queues per **jboss-cli-Skript** (`configure.cli`), nicht von Hand in `standalone-full.xml`.

### Infrastruktur (infra/terraform) – erst in der Vollversion
- Provider `azurerm`, Remote State, Module unter `modules/`, Umgebung unter `envs/dev/`. Kleinste sinnvolle Größen.

### Frontend (frontend/dashboard) – erst in der Vollversion
- Angular oder Vue 3 mit TypeScript. Zeigt Lagerplätze, Ladungsträger und Transportaufträge.

### Simulator (tools/plc-simulator) – erst in der Vollversion
- Python 3, `requests`, Tests mit pytest. Simuliert die SPS der Fördertechnik.

## Fachbegriffe

| Deutsch | Im Code | Bedeutung |
| --- | --- | --- |
| Host-System | – | Übergeordnetes System (z. B. ERP), schickt Avise und erwartet Rückmeldungen |
| Avis | `AdvanceShippingNotice` | Angekündigte Lieferung mit Positionen (Artikel, Menge) |
| Artikel | `Article` | Ware mit Artikelnummer |
| Ladungsträger (LHM) | `LoadCarrier` | Behälter oder Palette mit eindeutiger Nummer, trägt Artikel und Menge |
| Lagerplatz | `StorageLocation` | Fach im Lager, adressiert als Gasse-Ebene-Fach, Status FREI, BELEGT, RESERVIERT, GESPERRT |
| Platzvergabe | `SlottingStrategy` | Wählt einen freien, passenden Lagerplatz |
| Einlagerauftrag | `StorageOrder` | Fachlicher Auftrag des LVS: Ladungsträger X auf Platz Y einlagern |
| Transportauftrag (TA) | `TransportOrder` | Auftrag des MFR an die Fördertechnik: von Quelle nach Ziel. Status ANGELEGT → GESENDET → QUITTIERT oder STÖRUNG |
| Quittierung | `TransportConfirmation` | Rückmeldung, dass ein Transport ausgeführt wurde |
| Störung | `TransportFault` | Transport fehlgeschlagen, z. B. Platz nicht erreichbar |
| Bestandsbuchung | `StockMovement` | Historie jeder Bestandsänderung: wer, was, wann, von wo nach wo |

Aufgabenteilung: Das **LVS** weiß, *was* wo liegt und wohin etwas soll. Der **MFR** weiß, *wie* es dorthin kommt, und spricht mit der Fördertechnik.
