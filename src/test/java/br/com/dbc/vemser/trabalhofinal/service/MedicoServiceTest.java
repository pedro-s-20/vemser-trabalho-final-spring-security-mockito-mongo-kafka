package br.com.dbc.vemser.trabalhofinal.service;


import br.com.dbc.vemser.trabalhofinal.client.EnderecoClient;
import br.com.dbc.vemser.trabalhofinal.dto.AgendamentoMedicoEditarCreateDTO;
import br.com.dbc.vemser.trabalhofinal.dto.EnderecoDTO;
import br.com.dbc.vemser.trabalhofinal.dto.agendamento.AgendamentoCreateDTO;
import br.com.dbc.vemser.trabalhofinal.dto.agendamento.AgendamentoDTO;
import br.com.dbc.vemser.trabalhofinal.dto.agendamento.AgendamentoListaDTO;
import br.com.dbc.vemser.trabalhofinal.dto.agendamento.AgendamentoMedicoRelatorioDTO;
import br.com.dbc.vemser.trabalhofinal.dto.medico.MedicoCompletoDTO;
import br.com.dbc.vemser.trabalhofinal.dto.medico.MedicoCreateDTO;
import br.com.dbc.vemser.trabalhofinal.dto.medico.MedicoUpdateDTO;
import br.com.dbc.vemser.trabalhofinal.entity.*;
import br.com.dbc.vemser.trabalhofinal.exceptions.RegraDeNegocioException;
import br.com.dbc.vemser.trabalhofinal.repository.MedicoRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import freemarker.template.TemplateException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import javax.mail.MessagingException;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static br.com.dbc.vemser.trabalhofinal.service.ClienteServiceTest.getClienteEntityMock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MedicoServiceTest {
    @Spy
    @InjectMocks
    private MedicoService medicoService;
    private ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private MedicoRepository medicoRepository;
    @Mock
    private EspecialidadeService especialidadeService;
    @Mock
    private  UsuarioService usuarioService;
    @Mock
    private EmailService emailService;
    @Mock
    private EnderecoClient enderecoClient;
    @Mock
    private AgendamentoService agendamentoService;

    @Before
    public void init() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        ReflectionTestUtils.setField(medicoService, "objectMapper", objectMapper);
    }

    @Test
    public void deveRecuperarMedicoLogadoPorId() throws RegraDeNegocioException {
        //SETUP
        MedicoEntity medicoEntityMock = getMedicoEntityMock();
        MedicoCompletoDTO medicoCompletoDTOMock = getMedicoCompletoDTOMock();

        when(usuarioService.getIdLoggedUser()).thenReturn(1);
        when(medicoRepository.getMedicoEntityByIdUsuario(any())).thenReturn(medicoEntityMock);
        when(medicoRepository.getByIdPersonalizado(any())).thenReturn(Optional.of(medicoCompletoDTOMock));

        //ACT
        MedicoCompletoDTO medicoRecuperado = medicoService.recuperarMedico();

        //ASSERT
        assertNotNull(medicoRecuperado);
        assertEquals(medicoRecuperado, medicoCompletoDTOMock);
    }

    @Test
    public void testarGetById() throws RegraDeNegocioException {
        //setup
        Mockito.when(medicoRepository.getByIdPersonalizado(Mockito.anyInt())).thenReturn(Optional.of(getMedicoCompletoDTOMock()));
        Mockito.when(enderecoClient.getEndereco(Mockito.anyString())).thenReturn(new EnderecoDTO());
        //act
        MedicoCompletoDTO medicoCompletoDTO = medicoService.getById(1);
        //assert
        assertNotNull(medicoCompletoDTO);
        assertNotNull(medicoCompletoDTO.getEnderecoDTO());
    }

    @Test(expected = RegraDeNegocioException.class)
    public void testarGetByIdFalha() throws RegraDeNegocioException {
        //setup
        //act
        MedicoCompletoDTO medicoCompletoDTO = medicoService.getById(1);
        //assert
    }

    @Test
    public void testarGetMedico() throws RegraDeNegocioException{
        //setup
        Mockito.when(medicoRepository.findById(anyInt())).thenReturn(Optional.of(getMedicoEntityMock()));
        //act
        MedicoEntity medicoEntity = medicoService.getMedico(1);
        //assert
        assertNotNull(medicoEntity);
    }

    @Test
    public void testarGetMedicoAgendamentos() throws RegraDeNegocioException{
        //setup
        Mockito.doReturn(getMedicoCompletoDTOMock()).when(medicoService).recuperarMedico();
        Mockito.when(agendamentoService.getRelatorioMedicoById(any())).thenReturn(new AgendamentoMedicoRelatorioDTO());
        //act
        AgendamentoListaDTO agendamentoListaDTO = medicoService.getMedicoAgentamentos();
        //assert
        assertNotNull(agendamentoListaDTO);
    }
    @Test // deveCriarComSucesso
    public void deveCriarComSucesso() throws RegraDeNegocioException {
        // declaração de variaveis (SETUP)
        MedicoCreateDTO medicoCreateDTOMock = new MedicoCreateDTO();
        medicoCreateDTOMock.setCep("12345678");
        medicoCreateDTOMock.setCrm("123456");
        medicoCreateDTOMock.setNome("Alan");
        medicoCreateDTOMock.setCpf("12345678910");
        medicoCreateDTOMock.setEmail("Alan@gmail.com");
        medicoCreateDTOMock.setContatos("12345678");
        medicoCreateDTOMock.setNumero(145);
        medicoCreateDTOMock.setSenha("123");
        medicoCreateDTOMock.setIdEspecialidade(1);

        MedicoCompletoDTO medicoCompletoDTOMock = getMedicoCompletoDTOMock();
        doReturn(medicoCompletoDTOMock).when(medicoService).getById(any());
        // ação (ACT)
        MedicoCompletoDTO medicoAdicionado = medicoService.adicionar(medicoCreateDTOMock);

        // verificar se deu certo / afirmativa  (ASSERT)
        assertNotNull(medicoAdicionado);
        assertEquals(medicoCompletoDTOMock,medicoAdicionado);
    }

    @Test(expected = RegraDeNegocioException.class)
    public void deveCriarComFalha() throws RegraDeNegocioException, MessagingException, TemplateException, IOException {
        // declaração de variaveis (SETUP)
        MedicoCreateDTO medicoCreateDTOMock = new MedicoCreateDTO();
        medicoCreateDTOMock.setCep("12345678");
        medicoCreateDTOMock.setCrm("123456");
        medicoCreateDTOMock.setNome("Alan");
        medicoCreateDTOMock.setCpf("12345678910");
        medicoCreateDTOMock.setEmail("Alan@gmail.com");
        medicoCreateDTOMock.setContatos("12345678");
        medicoCreateDTOMock.setNumero(145);
        medicoCreateDTOMock.setSenha("123");
        medicoCreateDTOMock.setIdEspecialidade(1);

        Mockito.doThrow(new MessagingException("Erro ao enviar o e-mail. Cadastro não realizado.")).when(emailService).sendEmailUsuario(any(),any(),any());
        // ação (ACT)
        medicoService.adicionar(medicoCreateDTOMock);
    }
    @Test
    public void deveEditarMedico() throws RegraDeNegocioException {
        //SETUP
        MedicoUpdateDTO medicoUpdateDTO = getMedicoUpdate();
        MedicoEntity medicoEntityMock = getMedicoEntityMock();
        medicoEntityMock.setCrm("123");
        MedicoCompletoDTO medicoCompletoDTOMock = getMedicoCompletoDTOMock();
        List<MedicoEntity> medicoEntityList = List.of(medicoEntityMock);

        doReturn(medicoCompletoDTOMock).when(medicoService).recuperarMedico();
        doReturn(medicoCompletoDTOMock).when(medicoService).getById(any());
        when(medicoRepository.save(any())).thenReturn(medicoEntityMock);
        doReturn(medicoEntityList).when(medicoRepository).findAll();

        // ACT
        MedicoCompletoDTO medicoEditado = medicoService.editar(medicoUpdateDTO);

        //ASSERT
        assertNotNull(medicoEditado);
        assertEquals(medicoCompletoDTOMock, medicoEditado);
    }

    @Test
    public void deveEditarMedicoFalha() throws RegraDeNegocioException {
        //SETUP

        MedicoUpdateDTO medicoUpdateDTO = getMedicoUpdate();
        MedicoEntity medicoEntityMock = getMedicoEntityMock();
        MedicoCompletoDTO medicoCompletoDTOMock = getMedicoCompletoDTOMock();
        List<MedicoEntity> medicoEntityList = List.of(medicoEntityMock);

        doReturn(medicoCompletoDTOMock).when(medicoService).recuperarMedico();
        doReturn(medicoCompletoDTOMock).when(medicoService).getById(any());
        doReturn(medicoEntityList).when(medicoRepository).findAll();
        when(medicoRepository.save(any())).thenReturn(medicoEntityMock);
        // ACT
        medicoService.editar(medicoUpdateDTO);
    }

    @Test
    public void deveRemover() throws RegraDeNegocioException {
        //SETUP
        MedicoEntity medicoEntityMock = getMedicoEntityMock();
        doReturn(medicoEntityMock).when(medicoService).getMedico(any());
        //ACT
        medicoService.remover(medicoEntityMock.getIdMedico());
        //ASSERT
        verify(usuarioService, times(1)).remover(medicoEntityMock.getIdUsuario());
        verify(agendamentoService, times(1)).removerPorMedicoDesativado(medicoEntityMock);
    }

    @Test(expected = RegraDeNegocioException.class)
    public void testChecarSeTemNumero() throws RegraDeNegocioException {
        //act
        medicoService.checarSeTemNumero("thassio123");
    }

    @Test
    public void testarEditarAgendamentoMedico() throws RegraDeNegocioException {
        //setup
        AgendamentoMedicoEditarCreateDTO agendamentoMedicoEditarCreateDTO = getAgendamentoMedicoEditarCreateDTO();
        AgendamentoEntity agendamento = getAgendamentoEntityMock();
        MedicoEntity medico = getMedicoEntityMock();

        when(agendamentoService.editar(anyInt(),any())).thenReturn(getAgendamentoDTOMock());
        when(agendamentoService.getAgendamento(any())).thenReturn(agendamento);
        when(medicoRepository.getMedicoEntityByIdUsuario(any())).thenReturn(medico);
        //act
        AgendamentoDTO agendamentoDTO = medicoService.editarAgendamentoMedico(1,agendamentoMedicoEditarCreateDTO);
        //assert
        assertEquals(agendamentoDTO.getIdMedico(),medico.getIdMedico());
    }

    @Test(expected = RegraDeNegocioException.class)
    public void testarIfEditarAgendamentoMedico() throws RegraDeNegocioException {
        //setup
        AgendamentoMedicoEditarCreateDTO agendamentoMedicoEditarCreateDTO = getAgendamentoMedicoEditarCreateDTO();
        AgendamentoEntity agendamento = getAgendamentoEntityMock();
        MedicoEntity medico = getMedicoEntityMock();
        agendamento.setIdMedico(1);
        medico.setIdMedico(2);

        when(agendamentoService.getAgendamento(any())).thenReturn(agendamento);
        when(medicoRepository.getMedicoEntityByIdUsuario(any())).thenReturn(medico);
        //act
        AgendamentoDTO agendamentoDTO = medicoService.editarAgendamentoMedico(1,agendamentoMedicoEditarCreateDTO);
    }


    @NotNull
    static MedicoEntity getMedicoEntityMock() {
        MedicoEntity medicoMockadaDoBanco = new MedicoEntity();
        medicoMockadaDoBanco.setIdMedico(1);
        medicoMockadaDoBanco.setCrm("123456");
        medicoMockadaDoBanco.setUsuarioEntity(getUsuarioEntityMock());
        medicoMockadaDoBanco.setEspecialidadeEntity(getEspecialidadeEntityMock());
        return medicoMockadaDoBanco;
    }

    private static MedicoUpdateDTO getMedicoUpdate(){
        MedicoUpdateDTO medicoUpdateDTO = new MedicoUpdateDTO();
        medicoUpdateDTO.setCep("12345678");
        medicoUpdateDTO.setNome("Carlos");
        medicoUpdateDTO.setCpf("12345678910");
        medicoUpdateDTO.setNumero(123);
        medicoUpdateDTO.setContatos("12345678");
        medicoUpdateDTO.setIdUsuario(1);
        medicoUpdateDTO.setIdEspecialidade(1);
        medicoUpdateDTO.setCrm("123456");
        return medicoUpdateDTO;
    }

    @NotNull
    static MedicoCompletoDTO getMedicoCompletoDTOMock() {
        MedicoCompletoDTO medicoMockadaDoBancoDTO = new MedicoCompletoDTO();
        medicoMockadaDoBancoDTO.setCep("12345678");
        medicoMockadaDoBancoDTO.setCrm("123456");
        medicoMockadaDoBancoDTO.setNome("Alan");
        medicoMockadaDoBancoDTO.setCpf("12345678910");
        medicoMockadaDoBancoDTO.setEmail("Alan@gmail.com");
        medicoMockadaDoBancoDTO.setContatos("12345678");
        medicoMockadaDoBancoDTO.setNumero(145);
        medicoMockadaDoBancoDTO.setIdEspecialidade(1);
        medicoMockadaDoBancoDTO.setIdMedico(1);
        return medicoMockadaDoBancoDTO;
    }

    public static AgendamentoDTO getAgendamentoDTOMock() {
        AgendamentoDTO agendamentoDTO = new AgendamentoDTO();
        agendamentoDTO.setTratamento("Um tratamento");
        agendamentoDTO.setExame("Um exame");
        agendamentoDTO.setDataHorario(LocalDateTime.of(2023, 05, 12, 20, 15));
        agendamentoDTO.setIdMedico(1);
        agendamentoDTO.setIdCliente(1);
        agendamentoDTO.setIdAgendamento(1);
        agendamentoDTO.setValorAgendamento(450.0);
        return agendamentoDTO;
    }

    private static UsuarioEntity getUsuarioEntityMock() {
        UsuarioEntity usuarioEntity = new UsuarioEntity();
        usuarioEntity.setSenha("123");
        usuarioEntity.setCargoEntity(getCargoEntityMock());
        usuarioEntity.setAtivo(1);
        usuarioEntity.setNome("Carlos");
        usuarioEntity.setCpf("12345678910");
        usuarioEntity.setCep("12345678");
        usuarioEntity.setContatos("12345678");
        usuarioEntity.setIdUsuario(1);
        usuarioEntity.setEmail("Carlos@gmail.com");
        usuarioEntity.setNumero(123);
        return usuarioEntity;
    }

    public static AgendamentoCreateDTO getAgendamentoCreateDTOMock() {
        AgendamentoCreateDTO agendamentoCreateDTO = new AgendamentoCreateDTO();
        agendamentoCreateDTO.setTratamento("Um tratamento");
        agendamentoCreateDTO.setExame("Um exame");
        agendamentoCreateDTO.setDataHorario(LocalDateTime.of(2023, 05, 12, 20, 15));
        agendamentoCreateDTO.setIdMedico(1);
        agendamentoCreateDTO.setIdCliente(1);
        return agendamentoCreateDTO;
    }

    private static CargoEntity getCargoEntityMock() {
        CargoEntity cargoEntity = new CargoEntity();
        cargoEntity.setIdCargo(1);
        cargoEntity.setNomeCargo("ROLE_CLIENTE");
        return cargoEntity;
    }

    private static EspecialidadeEntity getEspecialidadeEntityMock() {
        EspecialidadeEntity especialidadeEntity = new EspecialidadeEntity();
        especialidadeEntity.setIdEspecialidade(1);
        especialidadeEntity.setNomeEspecialidade("Cardiologista");
        especialidadeEntity.setValor(500);
        return especialidadeEntity;
    }

    private static AgendamentoMedicoEditarCreateDTO getAgendamentoMedicoEditarCreateDTO() {
        AgendamentoMedicoEditarCreateDTO agendamentoMedicoEditarCreateDTO = new AgendamentoMedicoEditarCreateDTO();
        agendamentoMedicoEditarCreateDTO.setTratamento("Tratamento a ser seguido pelo cliente");
        agendamentoMedicoEditarCreateDTO.setExame("Exame(s) pedidos pelo médico");
        return agendamentoMedicoEditarCreateDTO;
    }

    private AgendamentoEntity getAgendamentoEntityMock() {
        AgendamentoEntity agendamentoEntity = new AgendamentoEntity();
        agendamentoEntity.setValorAgendamento(450.0);
        agendamentoEntity.setIdAgendamento(1);
        agendamentoEntity.setIdCliente(1);
        agendamentoEntity.setIdMedico(1);
        agendamentoEntity.setMedicoEntity(getMedicoEntityMock());
        agendamentoEntity.setClienteEntity(getClienteEntityMock());
        agendamentoEntity.setExame("Um exame");
        agendamentoEntity.setTratamento("Um tratamento");
        agendamentoEntity.setDataHorario(LocalDateTime.of(2023, 05, 12, 20, 15));
        return agendamentoEntity;
    }

}
