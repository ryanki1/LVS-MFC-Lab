# LVS-MFC-Lab

Fiktives Lernprojekt für ein Lagerverwaltungssystem (LVS) und einen Materialflussrechner (MFR) mit Jakarta EE 10 auf WildFly.

## Voraussetzungen

- JDK 21
- Maven 3.9+
- Docker mit Compose

## Build

```sh
mvn clean package
```

## Laufzeitumgebung

Lege zunächst eine lokale `.env` aus der Vorlage an und setze dort eigene Zugangsdaten:

```sh
cp .env.example .env
```

Nach dem Build startet `docker compose up --build` PostgreSQL und WildFly. Die beiden WAR-Dateien werden aus den Modulverzeichnissen `target/` in WildFly deployt. PostgreSQL ist am Host-Port `5433` erreichbar, WildFly unter `http://localhost:8080`.

Die Umsetzung erfolgt schrittweise gemäß [docs/PLAN.md](docs/PLAN.md). Terraform `apply` und `destroy` werden nicht durch Copilot ausgeführt.
