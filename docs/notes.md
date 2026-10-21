# Entwicklungsnotizen

Notiere nach jedem abgeschlossenen Schritt, wobei Copilot geholfen hat und was du selbst korrigiert hast. So bleibt die Entstehungsgeschichte des Lernprojekts nachvollziehbar.

## Schritt 0 · 2026-10-20

- Copilot hat Parent-POM, Kotlin- und Java-WAR, minimale Unit-Tests sowie WildFly-, PostgreSQL- und JMS-Konfiguration angelegt.
- Beim Build wurde die Kotlin-Plugin-Konfiguration so angepasst, dass auch der Kotlin-Test kompiliert und ausgeführt wird.
- Der Host-Port 5432 war bereits belegt; PostgreSQL ist deshalb lokal auf Port 5433 veröffentlicht.
- WildFly brauchte nach dem Image-Build passende Besitzrechte für seine Laufzeitverzeichnisse.
- Verifiziert: `mvn clean package` erfolgreich, beide Unit-Tests grün; Compose startet PostgreSQL und WildFly, Datasource/Queues vorhanden und beide WARs deployed.
