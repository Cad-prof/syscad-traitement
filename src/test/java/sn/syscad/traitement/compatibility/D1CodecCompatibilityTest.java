package sn.syscad.traitement.compatibility;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.MemoryFeatureSet;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.Geometries;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

class D1CodecCompatibilityTest {

  @TempDir File temporaryDirectory;

  @Test
  void readsBothFormatsIntoSisAndReprojectsToCrs84() throws Exception {
    SimpleFeatureType schema = createSchema();
    SimpleFeature feature = createFeature(schema);
    ListFeatureCollection features = new ListFeatureCollection(schema, List.of(feature));
    File shapefile = new File(temporaryDirectory, "parcelles.shp");
    File geopackage = new File(temporaryDirectory, "parcelles.gpkg");

    writeShapefile(shapefile, schema, features);
    writeGeoPackage(geopackage, schema, features);

    assertReadableAndTransformable(Map.of("url", shapefile.toURI().toURL()));
    assertReadableAndTransformable(
        Map.of("dbtype", "geopkg", "database", geopackage.getAbsolutePath()));
  }

  private SimpleFeatureType createSchema() throws Exception {
    SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
    builder.setName("parcelles");
    builder.setCRS(
      org.geotools.referencing.CRS.parseWKT(
        "PROJCS[\"WGS 84 / UTM zone 28N\","
          + "GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563]],"
          + "PRIMEM[\"Greenwich\",0],UNIT[\"degree\",0.0174532925199433]],"
          + "PROJECTION[\"Transverse_Mercator\"],"
          + "PARAMETER[\"latitude_of_origin\",0],PARAMETER[\"central_meridian\",-15],"
          + "PARAMETER[\"scale_factor\",0.9996],PARAMETER[\"false_easting\",500000],"
          + "PARAMETER[\"false_northing\",0],UNIT[\"metre\",1],AUTHORITY[\"EPSG\",\"32628\"]]"));
    builder.add("the_geom", MultiPolygon.class);
    builder.add("reference", String.class);
    return builder.buildFeatureType();
  }

  private SimpleFeature createFeature(SimpleFeatureType schema) {
    GeometryFactory geometryFactory = new GeometryFactory();
    Polygon polygon =
        geometryFactory.createPolygon(
            new Coordinate[] {
              new Coordinate(500000, 1500000),
              new Coordinate(500100, 1500000),
              new Coordinate(500100, 1500100),
              new Coordinate(500000, 1500100),
              new Coordinate(500000, 1500000)
            });
    MultiPolygon geometry = geometryFactory.createMultiPolygon(new Polygon[] {polygon});

    SimpleFeatureBuilder builder = new SimpleFeatureBuilder(schema);
    builder.add(geometry);
    builder.add("FACTICE-001");
    return builder.buildFeature("parcelle-1");
  }

  private void writeShapefile(
      File file, SimpleFeatureType schema, ListFeatureCollection features) throws IOException {
    Map<String, Object> parameters = Map.of("url", file.toURI().toURL());
    DataStore dataStore = new ShapefileDataStoreFactory().createNewDataStore(parameters);
    try {
      dataStore.createSchema(schema);
      SimpleFeatureStore featureStore =
          (SimpleFeatureStore) dataStore.getFeatureSource(dataStore.getTypeNames()[0]);
      featureStore.setTransaction(Transaction.AUTO_COMMIT);
      featureStore.addFeatures(features);
    } finally {
      dataStore.dispose();
    }
  }

  private void writeGeoPackage(
      File file, SimpleFeatureType schema, ListFeatureCollection features) throws IOException {
    try (GeoPackage geopackage = new GeoPackage(file)) {
      geopackage.init();
      FeatureEntry entry = new FeatureEntry();
      entry.setTableName("parcelles");
      entry.setGeometryColumn("the_geom");
      entry.setGeometryType(Geometries.MULTIPOLYGON);
      entry.setSrid(32628);
        entry.setBounds(
          new ReferencedEnvelope(500000, 500100, 1500000, 1500100, schema.getCoordinateReferenceSystem()));
      geopackage.add(entry, features);
    }
  }

  private void assertReadableAndTransformable(Map<String, ?> parameters) throws Exception {
    DataStore dataStore = DataStoreFinder.getDataStore(parameters);
    assertThat(dataStore).isNotNull();
    try {
      SimpleFeature feature;
      try (var iterator =
          dataStore.getFeatureSource(dataStore.getTypeNames()[0]).getFeatures().features()) {
        assertThat(iterator.hasNext()).isTrue();
        feature = iterator.next();
      }

      FeatureSet featureSet = toSisFeatureSet(feature);
      assertThat(featureSet.features(false).count()).isEqualTo(1);

      CoordinateReferenceSystem source = CRS.forCode("EPSG:32628");
      CoordinateReferenceSystem target = CRS.forCode("CRS:84");
      MathTransform transform = CRS.findOperation(source, target, null).getMathTransform();
      double[] coordinates = {500000, 1500000};
      transform.transform(coordinates, 0, coordinates, 0, 1);
      assertThat(coordinates[0]).isBetween(-15.1, -14.9);
      assertThat(coordinates[1]).isBetween(13.0, 14.0);
    } finally {
      dataStore.dispose();
    }
  }

  private FeatureSet toSisFeatureSet(SimpleFeature source) throws Exception {
    CoordinateReferenceSystem crs = CRS.forCode("EPSG:32628");
    FeatureTypeBuilder builder = new FeatureTypeBuilder();
    builder.setName("Parcelle");
    builder.addAttribute(String.class).setName("reference");
    builder.addAttribute(MultiPolygon.class).setName("geometrie").setCRS(crs);
    DefaultFeatureType type = builder.build();
    AbstractFeature feature = type.newInstance();
    feature.setPropertyValue("reference", source.getAttribute("reference"));
    feature.setPropertyValue("geometrie", source.getDefaultGeometry());
    return new MemoryFeatureSet(List.of(feature));
  }
}