package sn.syscad.traitement.demo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import sn.syscad.traitement.domain.Parcelle;

@Configuration
@Profile("demo")
public class DemoDataGenerator {

  @Bean
  List<Parcelle> demoParcelles() {
    return List.of(
        new Parcelle(
            UUID.fromString("7f4bb2cb-08f8-4f49-a9cf-4952f4b4bba0"),
            "A-001",
            "Commune Test A",
            "A",
            "001",
            "actif",
            new BigDecimal("1200.00"),
            new BigDecimal("1188.50"),
            "FACTICE",
            Instant.parse("2026-11-01T00:00:00Z")),
        new Parcelle(
            UUID.fromString("3790c9d7-6a2d-4c48-9f74-f9fa0d1b42ff"),
            "A-002",
            "Commune Test A",
            "A",
            "002",
            "actif",
            new BigDecimal("980.00"),
            new BigDecimal("972.00"),
            "FACTICE",
            Instant.parse("2026-11-02T00:00:00Z")),
        new Parcelle(
            UUID.fromString("d8f8c0ee-ef4f-4ed4-b0ff-4b5bffee7115"),
            "B-010",
            "Commune Test B",
            "B",
            "010",
            "actif",
            new BigDecimal("1570.50"),
            new BigDecimal("1601.25"),
            "FACTICE",
            Instant.parse("2026-11-03T00:00:00Z")));
  }
}
