package com.financas.controller;

import com.financas.dto.DividaRequest;
import com.financas.dto.DividaResponse;
import com.financas.dto.PessoaDividasResponse;
import com.financas.service.DividaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dividas")
public class DividaController {

    private final DividaService service;

    public DividaController(DividaService service) {
        this.service = service;
    }

    @GetMapping
    public List<PessoaDividasResponse> listarPorPessoa() {
        return service.listarPorPessoa();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<DividaResponse> criar(@Valid @RequestBody DividaRequest request) {
        return service.criar(request);
    }

    @PutMapping("/{id}")
    public DividaResponse atualizar(@PathVariable Long id, @Valid @RequestBody DividaRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }

    /** Débito: marca como paga e cria a entrada correspondente no controle do usuário. */
    @PostMapping("/{id}/quitar")
    public DividaResponse quitar(@PathVariable Long id) {
        return service.quitar(id);
    }

    @PostMapping("/{id}/reabrir")
    public DividaResponse reabrir(@PathVariable Long id) {
        return service.reabrir(id);
    }

    /** Crédito: a pessoa pagou o usuário (não mexe no saldo). */
    @PostMapping("/{id}/receber")
    public DividaResponse receber(@PathVariable Long id) {
        return service.receber(id);
    }

    @PostMapping("/{id}/desfazer-recebimento")
    public DividaResponse desfazerRecebimento(@PathVariable Long id) {
        return service.desfazerRecebimento(id);
    }

    /** Crédito: o usuário pagou a instituição (não mexe no saldo). */
    @PostMapping("/{id}/pagar")
    public DividaResponse pagar(@PathVariable Long id) {
        return service.pagar(id);
    }

    @PostMapping("/{id}/desfazer-pagamento")
    public DividaResponse desfazerPagamento(@PathVariable Long id) {
        return service.desfazerPagamento(id);
    }
}
