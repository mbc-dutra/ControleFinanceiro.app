package com.financas.controller;

import com.financas.dto.CategoriaResumoResponse;
import com.financas.dto.LancamentoRequest;
import com.financas.dto.LancamentoResponse;
import com.financas.dto.ResumoResponse;
import com.financas.model.TipoLancamento;
import com.financas.service.LancamentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/lancamentos")
public class LancamentoController {

    private final LancamentoService service;

    public LancamentoController(LancamentoService service) {
        this.service = service;
    }

    @GetMapping
    public List<LancamentoResponse> listar(@RequestParam(required = false) TipoLancamento tipo,
                                           @RequestParam(required = false) YearMonth mes) {
        return service.listar(tipo, mes);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LancamentoResponse criar(@Valid @RequestBody LancamentoRequest request) {
        return service.criar(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }

    @GetMapping("/resumo")
    public ResumoResponse resumo(@RequestParam(required = false) YearMonth mes) {
        return service.resumo(mes);
    }

    @GetMapping("/categorias")
    public List<CategoriaResumoResponse> gastosPorCategoria(@RequestParam(required = false) YearMonth mes) {
        return service.gastosPorCategoria(mes);
    }
}
