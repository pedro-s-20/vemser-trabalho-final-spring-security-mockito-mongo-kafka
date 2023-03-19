package br.com.dbc.vemser.trabalhofinal.service;

import br.com.dbc.vemser.trabalhofinal.dto.AgendamentoClientePersonalizadoDTO;
import br.com.dbc.vemser.trabalhofinal.dto.AgendamentoCreateDTO;
import br.com.dbc.vemser.trabalhofinal.dto.AgendamentoDTO;
import br.com.dbc.vemser.trabalhofinal.dto.PageDTO;
import br.com.dbc.vemser.trabalhofinal.entity.AgendamentoEntity;
import br.com.dbc.vemser.trabalhofinal.entity.ClienteEntity;
import br.com.dbc.vemser.trabalhofinal.entity.MedicoEntity;
import br.com.dbc.vemser.trabalhofinal.exceptions.RegraDeNegocioException;
import br.com.dbc.vemser.trabalhofinal.repository.AgendamentoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final ClienteService clienteService;
    private final MedicoService medicoService;
    private final ObjectMapper objectMapper;
    private final EmailService emailService;
    private final UsuarioService usuarioService;


    public List<AgendamentoDTO> listar() throws RegraDeNegocioException {
        return agendamentoRepository.findAgendamentoWithMedicoNomeAndClienteNome();
/*        return agendamentoRepository.findAll().stream()
                .map(agendamentoEntity -> objectMapper.convertValue(agendamentoEntity, AgendamentoDTO.class))
                .toList();
*/
    }

    public AgendamentoDTO adicionar(AgendamentoCreateDTO agendamentoCreateDTO) throws RegraDeNegocioException {
        ClienteEntity clienteEntity = clienteService.getCliente(agendamentoCreateDTO.getIdCliente());
        MedicoEntity medicoEntity = medicoService.getMedico(agendamentoCreateDTO.getIdMedico());

        AgendamentoEntity agendamentoEntity = objectMapper.convertValue(agendamentoCreateDTO, AgendamentoEntity.class);

        agendamentoEntity.setClienteEntity(clienteEntity);
        agendamentoEntity.setMedicoEntity(medicoEntity);

        agendamentoRepository.save(agendamentoEntity);

        return objectMapper.convertValue(agendamentoEntity, AgendamentoDTO.class);
    }

    public AgendamentoDTO editar(Integer id, AgendamentoCreateDTO agendamentoCreateDTO) throws RegraDeNegocioException {
        AgendamentoEntity agendamentoEntity = getAgendamento(id);
        ClienteEntity clienteEntity = clienteService.getCliente(agendamentoCreateDTO.getIdCliente());
        MedicoEntity medicoEntity = medicoService.getMedico(agendamentoCreateDTO.getIdMedico());

        agendamentoEntity.setMedicoEntity(medicoEntity);
        agendamentoEntity.setClienteEntity(clienteEntity);
        agendamentoEntity.setExame(agendamentoCreateDTO.getExame());
        agendamentoEntity.setTratamento(agendamentoCreateDTO.getTratamento());
        agendamentoEntity.setDataHorario(agendamentoCreateDTO.getDataHorario());

        agendamentoRepository.save(agendamentoEntity);

        return objectMapper.convertValue(agendamentoEntity, AgendamentoDTO.class);
    }

    public void remover(Integer id) throws RegraDeNegocioException {
        agendamentoRepository.delete(getAgendamento(id));
    }

    public AgendamentoEntity getAgendamento(Integer id) throws RegraDeNegocioException {
        return agendamentoRepository.findById(id).orElseThrow(() -> new RegraDeNegocioException("Agendamento não encontrado."));
    }

    public AgendamentoDTO getById(Integer id) throws RegraDeNegocioException {
        return objectMapper.convertValue(getAgendamento(id), AgendamentoDTO.class);
    }


    public AgendamentoClientePersonalizadoDTO getRelatorioClienteById(Integer idCliente) throws RegraDeNegocioException {
        clienteService.getCliente(idCliente);
        AgendamentoClientePersonalizadoDTO agendamentoClientePersonalizadoDTO = agendamentoRepository.clientePersonalizado(idCliente);


        List<AgendamentoDTO> allByIdCliente = agendamentoRepository.findAllByIdCliente(idCliente).stream()
                .map(agendamentoEntity -> objectMapper.convertValue(agendamentoEntity, AgendamentoDTO.class))
                .toList();
        agendamentoClientePersonalizadoDTO.setAgendamentoDTOList(allByIdCliente);

        return agendamentoClientePersonalizadoDTO;
    }

//    public AgendamentoClientePersonalizadoDTO getRelatorioClienteById(Integer idCliente, TipoUsuario tipoUsuario) throws RegraDeNegocioException {
//        clienteService.getCliente(idCliente);
//        AgendamentoClientePersonalizadoDTO agendamentoClientePersonalizadoDTO = agendamentoRepository.clientePersonalizado(idCliente);
//
//        List<AgendamentoDTO> allByIdCliente = agendamentoRepository.findAllByIdClienteOrMedico(idCliente, "id_" + tipoUsuario.toString()).stream()
//                .map(agendamentoEntity -> objectMapper.convertValue(agendamentoEntity, AgendamentoDTO.class))
//                .toList();
//
//        agendamentoClientePersonalizadoDTO.setAgendamentoDTOList(allByIdCliente);
//
//        return agendamentoClientePersonalizadoDTO;
//    }
//
//    public List<AgendamentoDTO> getAllByIdClienteOrMedico(Integer id, TipoUsuario tipoUsuario) throws RegraDeNegocioException {
//        return agendamentoRepository.findAllByIdClienteOrMedico(id, tipoUsuario.toString()).stream()
//                .map(agendamentoEntity -> objectMapper.convertValue(agendamentoEntity, AgendamentoDTO.class))
//                .toList();
//    }

    public PageDTO<AgendamentoDTO> findAllPaginado(Integer pagina, Integer tamanho) {

        Pageable solicitacaoPagina = PageRequest.of(pagina, tamanho);
        Page<AgendamentoEntity> agendamento = agendamentoRepository.findAllPaginado(solicitacaoPagina);
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