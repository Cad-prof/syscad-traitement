package sn.syscad.traitement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class ExportControllerTest {

  private final ExportController controller = new ExportController();

  @Test
  void shouldAllowGeojsonExportForAuthorizedRole() {
    ResponseEntity<java.util.Map<String, Object>> response =
        controller.exportParcelles("user-1", "UTILISATEUR_AUTORISE", "Commune Test A", "geojson");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsEntry("format", "geojson");
    assertThat(response.getBody()).containsEntry("zone", "Commune Test A");
  }

  @Test
  void shouldRejectUnknownExportFormat() {
    assertThatThrownBy(
            () -> controller.exportParcelles("user-1", "UTILISATEUR_AUTORISE", "Commune Test A", "pdf"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Format inconnu");
  }
}
