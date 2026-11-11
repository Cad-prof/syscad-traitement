package sn.syscad.traitement.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportService {

  private static final long MAX_SIZE_BYTES = 100L * 1024L * 1024L;
  private static final int MAX_FILES = 100;

  public String detectFormat(MultipartFile fichier) {
    if (fichier == null || fichier.isEmpty()) {
      throw new IllegalArgumentException("Le fichier est obligatoire.");
    }

    if (isZipArchive(fichier)) {
      return "SHAPEFILE_ZIP";
    }
    if (isGpkg(fichier)) {
      return "GPKG";
    }
    return "UNKNOWN";
  }

  public boolean isArchiveSafe(MultipartFile fichier) {
    if (fichier == null || fichier.isEmpty()) {
      return false;
    }
    if (!isZipArchive(fichier)) {
      return true;
    }

    try (InputStream input = fichier.getInputStream();
        ZipInputStream zip = new ZipInputStream(input, StandardCharsets.UTF_8)) {
      Set<String> seen = new HashSet<>();
      int count = 0;
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        count++;
        if (count > MAX_FILES) {
          return false;
        }
        String name = entry.getName();
        if (name == null || name.isBlank() || name.contains("..") || name.startsWith("/")) {
          return false;
        }
        if (seen.contains(name)) {
          return false;
        }
        seen.add(name);
      }
      return true;
    } catch (IOException ex) {
      return false;
    }
  }

  private boolean isZipArchive(MultipartFile fichier) {
    try (InputStream input = fichier.getInputStream()) {
      byte[] signature = new byte[4];
      int read = input.read(signature);
      if (read < 4) {
        return false;
      }
      return signature[0] == 0x50 && signature[1] == 0x4B && signature[2] == 0x03 && signature[3] == 0x04;
    } catch (IOException ex) {
      return false;
    }
  }

  private boolean isGpkg(MultipartFile fichier) {
    try (InputStream input = fichier.getInputStream()) {
      byte[] signature = new byte[16];
      int read = input.read(signature);
      if (read < 16) {
        return false;
      }
      return new String(signature, 0, 15, StandardCharsets.US_ASCII).contains("SQLite format 3\0");
    } catch (IOException ex) {
      return false;
    }
  }
}
