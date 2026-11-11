package sn.syscad.traitement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

class ImportControllerTest {

  private final ImportController controller = new ImportController(new ImportService());

  @Test
  void shouldAcceptZipShapefileImport() throws Exception {
    byte[] zipBytes = createZipArchive("parcelles.shp", "POLYGON ((0 0, 1 0, 1 1, 0 1, 0 0))");

    MockMultipartFile fichier =
        new MockMultipartFile(
            "fichier", "lot.zip", "application/octet-stream", zipBytes);

    ResponseEntity<ImportController.ImportCreationResponse> response =
        controller.createImport(
            "agent-1",
            "AGENT_CONTROLE",
            fichier,
            "Lot factice commune A",
            "Commune Test A",
            null,
            null,
            null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().etat()).isEqualTo("recu");
    assertThat(response.getBody().format()).isEqualTo("SHAPEFILE_ZIP");
    assertThat(response.getHeaders().getLocation()).isNotNull();
  }

  @Test
  void shouldRejectZipSlipAttempt() throws Exception {
    byte[] zipBytes = createZipArchive("../evil.shp", "bad");

    MockMultipartFile fichier =
        new MockMultipartFile(
            "fichier", "malicious.zip", "application/octet-stream", zipBytes);

    assertThatThrownBy(
            () ->
                controller.createImport(
                    "agent-1",
                    "AGENT_CONTROLE",
                    fichier,
                    "Lot rogue",
                    "Commune Test A",
                    null,
                    null,
                    null))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("zip-slip");
  }

  private byte[] createZipArchive(String entryName, String content) throws Exception {
    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
      zip.putNextEntry(new ZipEntry(entryName));
      zip.write(content.getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
    }
    return out.toByteArray();
  }
}
