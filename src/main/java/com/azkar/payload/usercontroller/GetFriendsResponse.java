package com.azkar.payload.usercontroller;

import com.azkar.entities.Friendship;
import com.azkar.payload.ResponseBase;
import java.util.List;
import lombok.Data;


@Data
public class GetFriendsResponse extends ResponseBase<List<Friendship>> {

}
