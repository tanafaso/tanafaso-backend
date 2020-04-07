package com.azkar.requestbodies.usercontroller;

import com.azkar.requestbodies.RequestBody;
import lombok.Data;

@Data
public class ResolveFriendRequestBody implements RequestBody {

  private boolean accept;

  @Override
  public boolean validate() {
    return true;
  }
}
