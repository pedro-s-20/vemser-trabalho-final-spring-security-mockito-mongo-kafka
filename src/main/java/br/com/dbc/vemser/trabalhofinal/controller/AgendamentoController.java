package br.com.dbc.vemser.trabalhofinal.controller;

import br.com.dbc.vemser.trabalhofinal.dto.*;
import br.com.dbc.vemser.trabalhofinal.exceptions.RegraDeNegocioException;
import br.com.dbc.vemser.trabalhofinal.service.AgendamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/agendamento")
@RequiredArgsConstructor
public class AgendamentoController implements InterfaceDocumentacao<AgendamentoDTO, AgendamentoCreateDTO, Integer> {

    private final AgendamentoService agendamentoService;

    @Override
    public ResponseEntity<List<AgendamentoDTO>> listAll() throws RegraDeNegocioException {
        return new ResponseEntity<>(agendamentoService.listar(), HttpStatus.OK);
    }

    @GetMapping("/paginado")
    public PageDTO<AgendamentoDTO> listAll(Integer pagina, Integer tamanho) {
        return agendamentoService.findAllPaginado(pagina, tamanho);
    }

    @Override
    public ResponseEntity<AgendamentoDTO> getById(Integer id) throws RegraDeNegocioException {
        return new ResponseEntity<>(agendamentoService.getById(id), HttpStatus.OK);
    }

    @GetMapping("/{idCliente}/relatorio-cliente")
    public ResponseEntity<AgendamentoClienteRelatorioDTO> getClienteByIdPersonalizado(@PathVariable("idCliente") Integer idCliente) throws RegraDeNegocioException {
        return new ResponseEntity<>(agendamentoService.getRelatorioClienteById(idCliente), HttpStatus.OK);
    }

    @GetMapping("/{idMedico}/relatorio-medico")
    public ResponseEntity<AgendamentoMedicoRelatorioDTO> getMedicoByIdPersonalizado(@PathVariable("idMedico") Integer idMedico) throws RegraDeNegocioException {
        return new ResponseEntity<>(agendamentoService.getRelatorioMedicoById(idMedico), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<AgendamentoDTO> create(AgendamentoCreateDTO agendamento) throws RegraDeNegocioException {
        return new ResponseEntity<>(agendamentoService.adicionar(agendamento), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<AgendamentoDTO> update(Integer id, AgendamentoCreateDTO agendamento) throws RegraDeNegocioException {
        return new ResponseEntity<>(agendamentoService.editar(id, agendamento), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> delete(Integer id) throws RegraDeNegocioException {
        agendamentoService.remover(id);
        return ResponseEntity.ok().build();
    }



}
