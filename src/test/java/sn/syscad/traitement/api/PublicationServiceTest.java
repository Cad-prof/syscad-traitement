package sn.syscad.traitement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.syscad.traitement.domain.ImportDonnees;
import sn.syscad.traitement.domain.ImportEtat;

class PublicationServiceTest {

  private final PublicationService service = new PublicationService();

  @Test
  void shouldValidateAndPublishImport() {
    ImportDonnees importDonnees = ImportDonnees.recu(UUID.randomUUID(), "Lot factice");

    service.validerImport(importDonnees, "auth-1", "Données conformes");
    service.publierLot(importDonnees);

    assertThat(importDonnees.getEtat()).isEqualTo(ImportEtat.ARCHIVE);
  }

  @Test
  void shouldRejectInvalidPublicationBeforeValidation() {
    ImportDonnees importDonnees = ImportDonnees.recu(UUID.randomUUID(), "Lot factice");

    assertThatThrownBy(() -> service.publierLot(importDonnees))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("validé");
  }

  @Test
  void shouldRejectMissingReasonOnRejection() {
    ImportDonnees importDonnees = ImportDonnees.recu(UUID.randomUUID(), "Lot factice");

    assertThatThrownBy(() -> service.rejeterImport(importDonnees, "auth-1", "court"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("motif");
  }
}
