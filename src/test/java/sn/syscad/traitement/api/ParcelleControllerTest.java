package sn.syscad.traitement.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import sn.syscad.traitement.domain.Parcelle;

class ParcelleControllerTest {

  private final ParcelleService service =
      new ParcelleService(
          List.of(
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
                  Instant.parse("2026-11-02T00:00:00Z"))));

  private final ParcelleController controller = new ParcelleController(service);

  @Test
  void shouldReturnPublishedParcelleListForAuthorizedUser() {
    var response =
        controller.rechercher(
            "user-1", "UTILISATEUR_AUTORISE", "Commune Test A", null, null, 0, 10);

    assertThat(response.contenu()).hasSize(2);
    assertThat(response.contenu().get(0).referenceCadastrale()).isEqualTo("A-001");
    assertThat(response.contenu().get(0).commune()).isEqualTo("Commune Test A");
  }

  @Test
  void shouldReturnPublishedParcelleDetailsById() {
    var details =
        controller.details(
            "user-1",
            "UTILISATEUR_AUTORISE",
            UUID.fromString("7f4bb2cb-08f8-4f49-a9cf-4952f4b4bba0"));

    assertThat(details.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(details.getBody()).isNotNull();
    assertThat(details.getBody().referenceCadastrale()).isEqualTo("A-001");
    assertThat(details.getBody().geometrie()).containsEntry("type", "MultiPolygon");
  }
}
