package com.financas.repository;

import com.financas.model.Lancamento;
import com.financas.model.TipoLancamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {

    List<Lancamento> findAllByOrderByDataDescIdDesc();

    List<Lancamento> findByTipoOrderByDataDescIdDesc(TipoLancamento tipo);

    List<Lancamento> findByTipo(TipoLancamento tipo);

    // Variantes filtradas por período (inclusivo nas duas pontas)

    List<Lancamento> findByDataBetweenOrderByDataDescIdDesc(LocalDate inicio, LocalDate fim);

    List<Lancamento> findByTipoAndDataBetweenOrderByDataDescIdDesc(TipoLancamento tipo, LocalDate inicio, LocalDate fim);

    List<Lancamento> findByDataBetween(LocalDate inicio, LocalDate fim);

    List<Lancamento> findByTipoAndDataBetween(TipoLancamento tipo, LocalDate inicio, LocalDate fim);
}
