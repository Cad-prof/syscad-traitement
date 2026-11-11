package sn.syscad.traitement.api;

import java.util.List;

public record PageResponse<T>(int page, int taille, long totalElements, List<T> contenu) {}
