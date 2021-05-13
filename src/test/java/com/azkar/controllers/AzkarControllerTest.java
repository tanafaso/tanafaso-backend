package com.azkar.controllers;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azkar.TestBase;
import com.azkar.configs.AzkarCacher;
import com.azkar.controllers.utils.JsonHandler;
import com.azkar.entities.User;
import com.azkar.entities.Zekr;
import com.azkar.factories.entities.UserFactory;
import com.azkar.payload.azkarcontroller.responses.GetAzkarResponse;
import com.google.common.collect.ImmutableList;
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

    String zekr1 = "اللهم أنت ربي، لا إله إلا أنت، خلَقتني وأنا عبدك، وأنا على عهدك ووعدك ما "
        + "استطعت، أعوذ بك من شرِّ ما صنعت، أبُوء لك بنعمتك عليّ وأبوء بذنبي، فاغفر لي؛ فإنه لا يغفر الذنوب إلا أنت'، قال: 'مَن قالها من النهار موقنًا بها، فمات من يومه قبل أن يُمسي، فهو من أهل الجنة، ومن قالها من الليل وهو مُوقن بها، فمات قبل أن يُصبح، فهو من أهل الجنة";
    String zekr2 = "اللهم بك أصبحنا، وبك أمسينا، وبك نحيا، وبك نموت، وإليك النشور";
    List<Zekr> expectedAzkar = ImmutableList.of(
        Zekr.builder().id(0).zekr(zekr1).build(),
        Zekr.builder().id(1).zekr(zekr2).build());

    GetAzkarResponse expectedResponse = new GetAzkarResponse();
    expectedResponse.setData(expectedAzkar);

    performGetRequest(user, "/azkar")
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(JsonHandler.toJson(expectedResponse)));
  }
}
