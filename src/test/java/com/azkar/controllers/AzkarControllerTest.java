package com.azkar.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.TestBase;
import com.azkar.controllers.utils.JsonHandler;
import com.azkar.entities.User;
import com.azkar.factories.entities.UserFactory;
import com.azkar.payload.azkarcontroller.responses.GetAzkarResponse;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;

public class AzkarControllerTest extends TestBase {

  @Autowired
  ResourceLoader resourceLoader;

  @Test
  public void getAzkar_shouldSucceed() throws Exception {
    User user = UserFactory.getNewUser();
    addNewUser(user);

    List<String> expectedAzkar = new ArrayList<>();
    // Read the CSV file lines using a different way than the controller to validate the response.
    // Note: CSV cells with strings are saved using quotations which is read by the
    // BufferedReader which is different from CSVReader which is used in the controller under test.
    BufferedReader bufferedReader =
        new BufferedReader(
            new FileReader(resourceLoader.getClassLoader().getResource("azkar.csv").getFile()));
    while (bufferedReader.ready()) {
      String line = bufferedReader.readLine();
      assertThat(line.length(), greaterThanOrEqualTo(2));
      assertThat(line.charAt(0), is('"'));
      assertThat(line.charAt(line.length() - 1), is('"'));
      expectedAzkar.add(line.substring(1, line.length() - 1));
    }

    GetAzkarResponse expectedResponse = new GetAzkarResponse();
    expectedResponse.setData(expectedAzkar);

    performGetRequest(user, "/azkar")
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }
}
