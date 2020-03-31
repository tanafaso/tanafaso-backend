package com.omar.azkar.repos;

import com.omar.azkar.entities.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepo extends CrudRepository<User, Integer> {

}
