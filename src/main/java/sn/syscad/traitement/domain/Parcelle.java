package sn.syscad.traitement.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Parcelle(
    UUID id,
    String referenceCadastrale,
    String commune,
    String section,
    String numero,
    String statut,
    BigDecimal superficieDeclaree,
    BigDecimal superficieCalculee,
    String source,
    Instant dateImport) {

  public Parcelle {
    if (id == null) {
      throw new IllegalArgumentException("L'identifiant de la parcelle est obligatoire.");
    }
    if (referenceCadastrale == null || referenceCadastrale.isBlank()) {
      throw new IllegalArgumentException("La référence cadastrale est obligatoire.");
    }
    if (commune == null || commune.isBlank()) {
      throw new IllegalArgumentException("La commune est obligatoire.");
    }
    if (source == null || source.isBlank()) {
      throw new IllegalArgumentException("La source est obligatoire.");
    }
    if (dateImport == null) {
      dateImport = Instant.now();
    }
  }
}
