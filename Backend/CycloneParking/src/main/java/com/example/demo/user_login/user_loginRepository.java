package com.example.demo.user_login;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

public interface user_loginRepository extends JpaRepository<user_login, String> {

}