package sn.syscad.traitement.api;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import sn.syscad.traitement.domain.Parcelle;

@Service
public class ParcelleService {

  private final List<Parcelle> parcelles;

  public ParcelleService(List<Parcelle> parcelles) {
    this.parcelles = List.copyOf(parcelles);
  }

  public List<Parcelle> rechercher(
      String commune, String statut, String reference, int page, int taille) {
    Stream<Parcelle> stream = parcelles.stream();

    if (commune != null && !commune.isBlank()) {
      stream =
          stream.filter(parcelle -> parcelle.commune().equalsIgnoreCase(commune.trim()));
    }
    if (statut != null && !statut.isBlank()) {
      stream =
          stream.filter(parcelle -> parcelle.statut().equalsIgnoreCase(statut.trim()));
    }
    if (reference != null && !reference.isBlank()) {
      stream =
          stream.filter(
              parcelle ->
                  parcelle.referenceCadastrale().equalsIgnoreCase(reference.trim()));
    }

    return stream
        .sorted(Comparator.comparing(Parcelle::referenceCadastrale))
        .skip((long) page * taille)
        .limit(taille)
        .toList();
  }

  public long total(String commune, String statut, String reference) {
    return rechercher(commune, statut, reference, 0, Integer.MAX_VALUE).size();
  }

  public Optional<Parcelle> trouverParId(UUID id) {
    return parcelles.stream().filter(parcelle -> parcelle.id().equals(id)).findFirst();
  }
}
