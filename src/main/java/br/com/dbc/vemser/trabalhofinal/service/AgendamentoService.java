package br.com.dbc.vemser.trabalhofinal.service;

import br.com.dbc.vemser.trabalhofinal.dto.PageDTO;
import br.com.dbc.vemser.trabalhofinal.dto.agendamento.AgendamentoClienteRelatorioDTO;
import br.com.dbc.vemser.trabalhofinal.dto.agendamento.AgendamentoCreateDTO;
import br.com.dbc.vemser.trabalhofinal.dto.agendamento.AgendamentoDTO;
import br.com.dbc.vemser.trabalhofinal.dto.agendamento.AgendamentoMedicoRelatorioDTO;
import br.com.dbc.vemser.trabalhofinal.dto.log.LogCreateDTO;
import br.com.dbc.vemser.trabalhofinal.entity.*;
import br.com.dbc.vemser.trabalhofinal.exceptions.RegraDeNegocioException;
import br.com.dbc.vemser.trabalhofinal.repository.AgendamentoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final ClienteService clienteService;
    private final MedicoService medicoService;
    private final ObjectMapper objectMapper;
    private final EmailService emailService;
    private final SolicitacaoService solicitacaoService;
    private final UsuarioService usuarioService;
    private final LogService logService;
    private final ProducerService producerService;

    public AgendamentoDTO adicionar(String idSolicitacao, AprovarReprovarSolicitacao aprovarReprovarSolicitacao) throws RegraDeNegocioException, JsonProcessingException {
        SolicitacaoEntity solicitacaoEntity = solicitacaoService.getSolicitacao(idSolicitacao);

        // Soliticação não é pendente
        if (!solicitacaoEntity.getStatusSolicitacao().equals(StatusSolicitacao.PENDENTE)) {
            throw new RegraDeNegocioException("Insira uma solicitação com status PENDENTE.");
        }

        //solicita reprovada
        if (aprovarReprovarSolicitacao.equals(AprovarReprovarSolicitacao.REPROVADA)) {
            solicitacaoEntity.setStatusSolicitacao(StatusSolicitacao.RECUSADA);
            solicitacaoService.reprovarSolicitacao(solicitacaoEntity);

            emailService.producerSolicitacaoEmail(solicitacaoEntity, TipoEmail.SOLICITACAO_RECUSADA);

            return null;
        }

        ClienteEntity clienteEntity = clienteService.getCliente(solicitacaoEntity.getIdCliente());
        MedicoEntity medicoEntity = medicoService.getMedico(solicitacaoEntity.getIdMedico());

        AgendamentoEntity agendamentoEntity = objectMapper.convertValue(solicitacaoEntity, AgendamentoEntity.class);

        agendamentoEntity.setClienteEntity(clienteEntity);
        agendamentoEntity.setMedicoEntity(medicoEntity);
        agendamentoEntity.setValorAgendamento((medicoEntity.getEspecialidadeEntity().getValor()) -
                medicoEntity.getEspecialidadeEntity().getValor() * (clienteEntity.getConvenioEntity().getTaxaAbatimento()/100));

        solicitacaoEntity.setStatusSolicitacao(StatusSolicitacao.APROVADA);
        agendamentoRepository.save(agendamentoEntity);
        solicitacaoService.aprovarSolicitacao(solicitacaoEntity);

        LogCreateDTO logCreateDTO = new LogCreateDTO();
        logCreateDTO.setIdSolicitacao(solicitacaoEntity.getIdSoliciatacao());
        logCreateDTO.setIdAgendamento(agendamentoEntity.getIdAgendamento());
        logCreateDTO.setIdUsuario(usuarioService.getIdLoggedUser());
        logCreateDTO.setDataHora(LocalDateTime.now());
        logCreateDTO.setTipoLog(TipoLog.APROVACAO_SOLICITACAO);

        logService.salvarLog(logCreateDTO);

        emailService.producerAgendamentoEmail(agendamentoEntity, TipoEmail.AGENDAMENTO_CRIADO_MEDICO);
        emailService.producerAgendamentoEmail(agendamentoEntity, TipoEmail.AGENDAMENTO_CRIADO_CLIENTE);

        return objectMapper.convertValue(agendamentoEntity, AgendamentoDTO.class);
    }

    public AgendamentoDTO editar(Integer id, AgendamentoCreateDTO agendamentoCreateDTO) throws RegraDeNegocioException, JsonProcessingException {
        AgendamentoEntity agendamentoEntity = getAgendamento(id);
        ClienteEntity clienteEntity = clienteService.getCliente(agendamentoCreateDTO.getIdCliente());
        MedicoEntity medicoEntity = medicoService.getMedico(agendamentoCreateDTO.getIdMedico());

        agendamentoEntity.setMedicoEntity(medicoEntity);
        agendamentoEntity.setClienteEntity(clienteEntity);
        agendamentoEntity.setExame(agendamentoCreateDTO.getExame());
        agendamentoEntity.setTratamento(agendamentoCreateDTO.getTratamento());
        agendamentoEntity.setDataHorario(agendamentoCreateDTO.getDataHorario());
        agendamentoEntity.setValorAgendamento((medicoEntity.getEspecialidadeEntity().getValor()) -
                medicoEntity.getEspecialidadeEntity().getValor() * (clienteEntity.getConvenioEntity().getTaxaAbatimento() / 100));

        agendamentoRepository.save(agendamentoEntity);

        emailService.producerAgendamentoEmail(agendamentoEntity, TipoEmail.AGENDAMENTO_EDITADO_MEDICO);
        emailService.producerAgendamentoEmail(agendamentoEntity, TipoEmail.AGENDAMENTO_EDITADO_CLIENTE);

        LogCreateDTO logCreateDTO = new LogCreateDTO();
        logCreateDTO.setIdSolicitacao(null);
        logCreateDTO.setIdAgendamento(id);
        logCreateDTO.setIdUsuario(usuarioService.getIdLoggedUser());
        logCreateDTO.setDataHora(LocalDateTime.now());
        logCreateDTO.setTipoLog(TipoLog.EDICAO_AGENDAMENTO);

        logService.salvarLog(logCreateDTO);

        return objectMapper.convertValue(agendamentoEntity, AgendamentoDTO.class);
    }

    public void remover(Integer id) throws RegraDeNegocioException, JsonProcessingException {
        AgendamentoEntity agendamentoEntity = getAgendamento(id);
        AgendamentoEntity agendamentoEmail = agendamentoEntity;

        agendamentoRepository.delete(agendamentoEntity);

        LogCreateDTO logCreateDTO = new LogCreateDTO();
        logCreateDTO.setIdSolicitacao(null);
        logCreateDTO.setIdAgendamento(id);
        logCreateDTO.setIdUsuario(usuarioService.getIdLoggedUser());
        logCreateDTO.setDataHora(LocalDateTime.now());
        logCreateDTO.setTipoLog(TipoLog.EXCLUSAO_AGENDAMENTO);
        logService.salvarLog(logCreateDTO);

        emailService.producerAgendamentoEmail(agendamentoEmail, TipoEmail.AGENDAMENTO_CANCELADO_MEDICO);
        emailService.producerAgendamentoEmail(agendamentoEmail, TipoEmail.AGENDAMENTO_CANCELADO_CLIENTE);

    }

    public void removerPorMedicoDesativado(MedicoEntity medicoEntity) throws RegraDeNegocioException {
        agendamentoRepository.deleteByMedicoEntity(medicoEntity);
    }

    public void removerPorClienteDesativado(ClienteEntity clienteEntity) throws RegraDeNegocioException {
        agendamentoRepository.deleteByClienteEntity(clienteEntity);
    }

    public AgendamentoEntity getAgendamento(Integer id) throws RegraDeNegocioException {
        return agendamentoRepository.findById(id).orElseThrow(() -> new RegraDeNegocioException("Agendamento não encontrado."));
    }

    public AgendamentoDTO getById(Integer id) throws RegraDeNegocioException {
        return objectMapper.convertValue(getAgendamento(id), AgendamentoDTO.class);
    }


    public AgendamentoClienteRelatorioDTO getRelatorioClienteById(Integer idCliente) throws RegraDeNegocioException {
        AgendamentoClienteRelatorioDTO agendamentoRelatorio = objectMapper.convertValue(clienteService.getById(idCliente), AgendamentoClienteRelatorioDTO.class);

        List<AgendamentoDTO> allByIdCliente = agendamentoRepository.findAllByIdCliente(idCliente).stream()
                .map(agendamentoEntity -> objectMapper.convertValue(agendamentoEntity, AgendamentoDTO.class))
                .toList();
        agendamentoRelatorio.setAgendamentoDTOList(allByIdCliente);

        if (allByIdCliente.isEmpty()) {
            throw new RegraDeNegocioException("Esse cliente não possui agendamento");
        }

        return agendamentoRelatorio;
    }

    public AgendamentoMedicoRelatorioDTO getRelatorioMedicoById(Integer idMedico) throws RegraDeNegocioException {
        AgendamentoMedicoRelatorioDTO agendamentoRelatorio = objectMapper.convertValue(medicoService.getById(idMedico), AgendamentoMedicoRelatorioDTO.class);

        List<AgendamentoDTO> allByIdMedico = agendamentoRepository.findAllByIdMedico(idMedico).stream()
                .map(agendamentoEntity -> objectMapper.convertValue(agendamentoEntity, AgendamentoDTO.class))
                .toList();
        agendamentoRelatorio.setAgendamentoDTOList(allByIdMedico);

        if (allByIdMedico.isEmpty()) {
            throw new RegraDeNegocioException("Esse médico não possui agendamento");
        }

        return agendamentoRelatorio;
    }

    public PageDTO<AgendamentoDTO> findAllPaginado(Integer pagina, Integer tamanho) {

        Pageable solicitacaoPagina = PageRequest.of(pagina, tamanho);
        Page<AgendamentoEntity> agendamento = agendamentoRepository.findAll(solicitacaoPagina);
        List<AgendamentoDTO> agendamentoDTO = agendamento.getContent().stream()
                .map(x -> objectMapper.convertValue(x, AgendamentoDTO.class))
                .toList();

        return new PageDTO<>(agendamento.getTotalElements(),
                agendamento.getTotalPages(),
                pagina,
                tamanho,
                agendamentoDTO);
    }

}