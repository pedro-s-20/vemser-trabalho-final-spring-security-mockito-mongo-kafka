package br.com.dbc.vemser.trabalhofinal.controller;


import br.com.dbc.vemser.trabalhofinal.controller.documentacao.DocumentacaoEspecialidadeConvenio;
import br.com.dbc.vemser.trabalhofinal.dto.PageDTO;
import br.com.dbc.vemser.trabalhofinal.dto.convenio.ConvenioCreateDTO;
import br.com.dbc.vemser.trabalhofinal.dto.convenio.ConvenioDTO;
import br.com.dbc.vemser.trabalhofinal.exceptions.RegraDeNegocioException;
import br.com.dbc.vemser.trabalhofinal.service.ConvenioService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@Tag(name="Convênio")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/convenio")
public class ConvenioController implements DocumentacaoEspecialidadeConvenio<ConvenioDTO, ConvenioCreateDTO, Integer, Integer> {

    private final ConvenioService convenioService;


    @Override
    public ResponseEntity<PageDTO<ConvenioDTO>> list(Integer pagina, Integer tamanho) {
        return new ResponseEntity<>(convenioService.list(pagina, tamanho), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ConvenioDTO> getById(Integer id) throws RegraDeNegocioException {
        return new ResponseEntity<>(convenioService.getById(id), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ConvenioDTO> create(ConvenioCreateDTO convenio) throws RegraDeNegocioException {
        return new ResponseEntity<>(convenioService.adicionar(convenio), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ConvenioDTO> update(Integer id, ConvenioCreateDTO convenio) throws RegraDeNegocioException {
        return new ResponseEntity<>(convenioService.editar(id, convenio), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> delete(Integer id) throws RegraDeNegocioException {
        convenioService.remover(id);
        return ResponseEntity.ok().build();
    }
}

