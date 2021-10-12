package com.azkar.configs;

import com.opencsv.CSVReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;

@Configuration
@Getter
@Setter
public class QuranMetadataCacher {

  private static final Logger logger = LoggerFactory.getLogger(QuranMetadataCacher.class);
  @Value("${files.quran-metadata}")
  public String quranMetadataFile;

  List<AyahMetadata> ayahsMetadata;
  List<SurahMetadata> surahsMetadata;
  List<JuzMetadata> juzsMetadata;
  List<RubMetadata> rubsMetadata;

  @Bean
  @Primary
  public QuranMetadataCacher parseQuranMetadataFromFile() {
    QuranMetadataCacher quranMetadataCacher = new QuranMetadataCacher();

    List<LineInQuranMetadataFile> linesInQuranMetadataFile = parseQuranMetadataFile();
    quranMetadataCacher.setAyahsMetadata(buildAyahsMetadata(linesInQuranMetadataFile));
    quranMetadataCacher.setSurahsMetadata(buildSurahsMetadata(linesInQuranMetadataFile));
    quranMetadataCacher.setJuzsMetadata(buildJuzsMetadata(linesInQuranMetadataFile));
    quranMetadataCacher.setRubsMetadata(buildRubsMetadata(linesInQuranMetadataFile));

    return quranMetadataCacher;
  }


  private List<LineInQuranMetadataFile> parseQuranMetadataFile() {
    List<LineInQuranMetadataFile> linesInQuranMetadataFile = new ArrayList<>();
    try {
      CSVReader csvReader =
          new CSVReader(
              new InputStreamReader(new ClassPathResource(quranMetadataFile).getInputStream()));

      boolean readFirstLine = false;
      String[] values;
      while ((values = csvReader.readNext()) != null) {
        if (values.length != 6) {
          throw new IOException(
              "Didn't find exactly 6 columns per row in CSV file: " + quranMetadataFile);
        }
        if (!readFirstLine) {
          readFirstLine = true;
          continue;
        }

        LineInQuranMetadataFile lineInQuranMetadataFile =
            LineInQuranMetadataFile.builder()
                .ayah(Integer.parseInt(values[0]))
                .surah(Integer.parseInt(values[1]))
                .rub(Integer.parseInt(values[2]))
                .juz(Integer.parseInt(values[3]))
                .firstAyahInRub(Integer.parseInt(values[4]))
                .firstAyahInJuz(Integer.parseInt(values[5]))
                .build();
        linesInQuranMetadataFile.add(lineInQuranMetadataFile);
      }

      if (linesInQuranMetadataFile.size() == 0) {
        throw new IOException("Error while parsing file: " + quranMetadataFile);
      }
    } catch (Exception e) {
      logger.error("Can't retrieve Quran: " + e.getMessage());
    }
    return linesInQuranMetadataFile;
  }


  private List<RubMetadata> buildRubsMetadata(
      List<LineInQuranMetadataFile> linesInQuranMetadataFile) {
    List<RubMetadata> rubsMetadata = new ArrayList<>();

    int currentRub = 1;
    rubsMetadata.add(RubMetadata.builder()
        .rub(1)
        .firstAyahInRub(1)
        .build());

    for (LineInQuranMetadataFile lineInQuranMetadataFile : linesInQuranMetadataFile) {
      if (lineInQuranMetadataFile.rub != currentRub) {
        currentRub = lineInQuranMetadataFile.rub;
        rubsMetadata.add(RubMetadata.builder()
            .rub(currentRub)
            .firstAyahInRub(lineInQuranMetadataFile.ayah)
            .build());
      }
    }

    return rubsMetadata;
  }

  private List<JuzMetadata> buildJuzsMetadata(
      List<LineInQuranMetadataFile> linesInQuranMetadataFile) {
    List<JuzMetadata> juzsMetadata = new ArrayList<>();

    int currentJuz = 1;
    juzsMetadata.add(JuzMetadata.builder()
        .juz(1)
        .firstAyahInJuz(1)
        .build());

    for (LineInQuranMetadataFile lineInQuranMetadataFile : linesInQuranMetadataFile) {
      if (lineInQuranMetadataFile.juz != currentJuz) {
        currentJuz = lineInQuranMetadataFile.juz;
        juzsMetadata.add(JuzMetadata.builder()
            .juz(currentJuz)
            .firstAyahInJuz(lineInQuranMetadataFile.ayah)
            .build());
      }
    }

    return juzsMetadata;
  }

  private List<SurahMetadata> buildSurahsMetadata(
      List<LineInQuranMetadataFile> linesInQuranMetadataFile) {
    List<SurahMetadata> surahsMetadata = new ArrayList<>();

    int currentSurah = 1;
    surahsMetadata.add(SurahMetadata.builder()
        .surah(1)
        .firstAyahInSurah(1)
        .build());

    for (LineInQuranMetadataFile lineInQuranMetadataFile : linesInQuranMetadataFile) {
      if (lineInQuranMetadataFile.surah != currentSurah) {
        currentSurah = lineInQuranMetadataFile.surah;
        surahsMetadata.add(SurahMetadata.builder()
            .surah(currentSurah)
            .firstAyahInSurah(lineInQuranMetadataFile.ayah)
            .build());
      }
    }

    return surahsMetadata;
  }

  private List<AyahMetadata> buildAyahsMetadata(
      List<LineInQuranMetadataFile> linesInQuranMetadataFile) {
    return linesInQuranMetadataFile.stream().map(lineInQuranMetadataFile ->
        AyahMetadata.builder()
            .ayah(lineInQuranMetadataFile.getAyah())
            .surah(lineInQuranMetadataFile.getSurah())
            .juz(lineInQuranMetadataFile.getJuz())
            .rub(lineInQuranMetadataFile.getRub())
            .build()
    ).collect(Collectors.toList());
  }

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Setter
  @Builder
  public class JuzMetadata {

    int juz;
    int firstAyahInJuz;
  }

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Setter
  @Builder
  public class RubMetadata {

    int rub;
    int firstAyahInRub;
  }

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Setter
  @Builder
  public class SurahMetadata {

    int surah;
    int firstAyahInSurah;
  }

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Setter
  @Builder
  public class AyahMetadata {

    int ayah;
    int surah;
    int rub;
    int juz;
  }

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Setter
  @Builder
  public class LineInQuranMetadataFile {

    int ayah;
    int surah;
    int rub;
    int juz;
    int firstAyahInRub;
    int firstAyahInJuz;
  }
}
