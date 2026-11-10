package com.financas.repository;

import com.financas.model.Divida;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DividaRepository extends JpaRepository<Divida, Long> {

    List<Divida> findAllByOrderByDataDescIdDesc();
}
