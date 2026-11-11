package sn.syscad.traitement.domain;

import java.time.Instant;
import java.util.UUID;

public class ImportDonnees {
  private final UUID id;
  private final String source;
  private final String perimetre;
  private final Instant creeLe;
  private ImportEtat etat;

  private ImportDonnees(UUID id, String source, String perimetre, Instant creeLe, ImportEtat etat) {
    this.id = id;
    this.source = source;
    this.perimetre = perimetre;
    this.creeLe = creeLe;
    this.etat = etat;
  }

  public static ImportDonnees recu(UUID id, String source) {
    return new ImportDonnees(id, source, "COMMUNE_INCONNUE", Instant.now(), ImportEtat.RECU);
  }

  public static ImportDonnees recu(UUID id, String source, String perimetre) {
    return new ImportDonnees(id, source, perimetre, Instant.now(), ImportEtat.RECU);
  }

  public UUID getId() {
    return id;
  }

  public String getSource() {
    return source;
  }

  public String getPerimetre() {
    return perimetre;
  }

  public Instant getCreeLe() {
    return creeLe;
  }

  public ImportEtat getEtat() {
    return etat;
  }

  public void transitionTo(ImportEtat etatCible) {
    if (etatCible == null) {
      throw new IllegalArgumentException("L'état cible est obligatoire.");
    }
    if (!etat.peutTransitionVers(etatCible)) {
      throw new IllegalStateException(
          "Transition impossible de l'état " + etat + " vers " + etatCible + ".");
    }
    this.etat = etatCible;
  }

  public void appliquerDecision(DecisionValidation decision, String motif) {
    if (decision == null) {
      throw new IllegalArgumentException("La décision est obligatoire.");
    }
    if (etat != ImportEtat.EN_ATTENTE_VALIDATION) {
      throw new IllegalStateException(
          "La décision ne peut être appliquée qu'à l'état EN_ATTENTE_VALIDATION. État actuel : "
              + etat
              + ".");
    }

    if (decision == DecisionValidation.VALIDER) {
      this.etat = ImportEtat.VALIDE;
      return;
    }

    if (motif == null || motif.isBlank() || motif.trim().length() < 10) {
      throw new IllegalArgumentException("Le motif du rejet est obligatoire et doit contenir au moins 10 caractères.");
    }
    this.etat = ImportEtat.REJETE;
  }
}
