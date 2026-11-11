package sn.syscad.traitement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ImportDonneesTransitionTest {

  @Test
  void shouldAdvanceThroughTheExpectedImportLifecycle() {
    ImportDonnees importDonnees = ImportDonnees.recu(UUID.randomUUID(), "Lot factice commune A");

    assertThat(importDonnees.getEtat()).isEqualTo(ImportEtat.RECU);

    importDonnees.transitionTo(ImportEtat.EN_PREPARATION);
    importDonnees.transitionTo(ImportEtat.NORMALISE);
    importDonnees.transitionTo(ImportEtat.CONTROLE);
    importDonnees.transitionTo(ImportEtat.EN_ATTENTE_VALIDATION);

    assertThat(importDonnees.getEtat()).isEqualTo(ImportEtat.EN_ATTENTE_VALIDATION);

    importDonnees.appliquerDecision(DecisionValidation.VALIDER, "Données conformes");
    assertThat(importDonnees.getEtat()).isEqualTo(ImportEtat.VALIDE);

    importDonnees.transitionTo(ImportEtat.PUBLIE);
    importDonnees.transitionTo(ImportEtat.ARCHIVE);

    assertThat(importDonnees.getEtat()).isEqualTo(ImportEtat.ARCHIVE);
  }

  @Test
  void shouldRejectInvalidTransitions() {
    ImportDonnees importDonnees = ImportDonnees.recu(UUID.randomUUID(), "Lot test");

    assertThatThrownBy(() -> importDonnees.transitionTo(ImportEtat.VALIDE))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("RECU");

    importDonnees.transitionTo(ImportEtat.EN_PREPARATION);
    importDonnees.transitionTo(ImportEtat.NORMALISE);
    importDonnees.transitionTo(ImportEtat.CONTROLE);
    importDonnees.transitionTo(ImportEtat.EN_ATTENTE_VALIDATION);

    assertThatThrownBy(() -> importDonnees.appliquerDecision(DecisionValidation.REJETER, ""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("motif");
  }
}
