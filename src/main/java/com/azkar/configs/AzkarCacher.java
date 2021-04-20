package com.azkar.configs;

import com.azkar.entities.Zekr;
import com.opencsv.CSVReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ResourceLoader;

@Configuration
@Getter
public class AzkarCacher {

  private static final String AZKAR_FILE = "azkar.csv";
  private static final Logger logger = LoggerFactory.getLogger(AzkarCacher.class);

  ArrayList<Zekr> azkar = new ArrayList<>();

  @Autowired
  ResourceLoader resourceLoader;

  @Bean
  @Primary
  public AzkarCacher parseFromCsv() {
    AzkarCacher cacher = new AzkarCacher();
    try {
      CSVReader csvReader =
          new CSVReader(new FileReader(
              resourceLoader.getClassLoader().getResource(AZKAR_FILE).getFile()));
      String[] values;
      while ((values = csvReader.readNext()) != null) {
        if (values.length != 2) {
          throw new IOException("Didn't find exactly 2 columns per row in CSV file.");
        }

        Zekr zekr = Zekr.builder().id(Integer.parseInt(values[0])).zekr(values[1]).build();
        azkar.add(zekr);
      }

      if (azkar.size() == 0) {
        throw new IOException("Error while parsing file: " + AZKAR_FILE);
      }
      cacher.azkar = azkar;
    } catch (Exception e) {
      logger.error("Can't retrieve azkar: " + e.getMessage());
    }
    return cacher;
  }
}
