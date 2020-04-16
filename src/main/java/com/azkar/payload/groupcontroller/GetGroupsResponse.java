package com.azkar.payload.groupcontroller;

import com.azkar.entities.User.UserGroup;
import com.azkar.payload.ResponseBase;
import java.util.List;
import lombok.Data;

@Data
public class GetGroupsResponse extends ResponseBase<List<UserGroup>> {

}
