package com.financas.repository;

import com.financas.model.Lancamento;
import com.financas.model.TipoLancamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {

    // Listagens em ordem de data crescente (ordem de vencimento)

    List<Lancamento> findAllByOrderByDataAscIdAsc();

    List<Lancamento> findByTipoOrderByDataAscIdAsc(TipoLancamento tipo);

    List<Lancamento> findByDataBetweenOrderByDataAscIdAsc(LocalDate inicio, LocalDate fim);

    List<Lancamento> findByTipoAndDataBetweenOrderByDataAscIdAsc(TipoLancamento tipo, LocalDate inicio, LocalDate fim);

    // Agregações (período inclusivo nas duas pontas)

    List<Lancamento> findByTipo(TipoLancamento tipo);

    List<Lancamento> findByDataBetween(LocalDate inicio, LocalDate fim);

    List<Lancamento> findByTipoAndDataBetween(TipoLancamento tipo, LocalDate inicio, LocalDate fim);
}
