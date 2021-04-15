package com.azkar.controllers;

import com.azkar.entities.Zekr;
import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.azkarcontroller.responses.GetAzkarResponse;
import com.opencsv.CSVReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AzkarController extends BaseController {

  private static final String AZKAR_FILE = "azkar.csv";
  private static final Logger logger = LoggerFactory.getLogger(ChallengeController.class);

  @Autowired
  ResourceLoader resourceLoader;

  @GetMapping(path = "/azkar", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<GetAzkarResponse> getAzkar() {
    GetAzkarResponse response = new GetAzkarResponse();
    List<Zekr> azkar = new ArrayList<>();
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
    } catch (Exception e) {
      logger.error("Can't retrieve azkar: " + e.getMessage());
      response.setError(new Error(Error.DEFAULT_ERROR));
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    response.setData(azkar);
    return ResponseEntity.ok(response);
  }

}
