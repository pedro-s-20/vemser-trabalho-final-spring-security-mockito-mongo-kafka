package br.com.dbc.vemser.trabalhofinal.service;

import br.com.dbc.vemser.trabalhofinal.client.EnderecoClient;
import br.com.dbc.vemser.trabalhofinal.dto.RedefinicaoSenhaDTO;
import br.com.dbc.vemser.trabalhofinal.dto.TrocaSenhaDTO;
import br.com.dbc.vemser.trabalhofinal.dto.usuario.UsuarioCreateDTO;
import br.com.dbc.vemser.trabalhofinal.dto.usuario.UsuarioDTO;
import br.com.dbc.vemser.trabalhofinal.entity.TipoEmail;
import br.com.dbc.vemser.trabalhofinal.entity.UsuarioEntity;
import br.com.dbc.vemser.trabalhofinal.exceptions.RegraDeNegocioException;
import br.com.dbc.vemser.trabalhofinal.repository.UsuarioRepository;
import br.com.dbc.vemser.trabalhofinal.security.CodigoTrocaSenha;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final EnderecoClient enderecoClient;
    private final CodigoTrocaSenha codigoTrocaSenha;

    public UsuarioService(UsuarioRepository usuarioRepository, ObjectMapper objectMapper, PasswordEncoder passwordEncoder, @Lazy EmailService emailService, EnderecoClient enderecoClient, CodigoTrocaSenha codigoTrocaSenha) {
        this.usuarioRepository = usuarioRepository;
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.enderecoClient = enderecoClient;
        this.codigoTrocaSenha = codigoTrocaSenha;
    }

    public void adicionar(UsuarioEntity usuario) throws RegraDeNegocioException {
        usuario.setAtivo(1);
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        usuarioRepository.save(usuario);
    }

    public void remover(Integer id) throws RegraDeNegocioException {
        UsuarioEntity usuario = getUsuario(id);
        if (usuario.getAtivo().equals(0)){
            throw new RegraDeNegocioException("Este usuário já está desativado!");
        }
        usuario.setAtivo(0);
        usuarioRepository.save(usuario);
    }

    public void hardDelete(Integer id) throws RegraDeNegocioException {
        UsuarioEntity usuario = getUsuario(id);
        usuarioRepository.delete(usuario);
    }

    public void editar(UsuarioCreateDTO usuario, Integer id) throws RegraDeNegocioException {

        UsuarioEntity usuarioRecuperado = getUsuario(id);
        usuarioRecuperado.setCpf(usuario.getCpf());
        usuarioRecuperado.setNome(usuario.getNome());
        usuarioRecuperado.setCep(usuario.getCep());
        usuarioRecuperado.setNumero(usuario.getNumero());
        usuarioRecuperado.setContatos(usuario.getContatos());

        usuarioRepository.save(usuarioRecuperado);
    }

    public void validarUsuarioAdicionado(UsuarioEntity usuarioEntity) throws RegraDeNegocioException {

        List<UsuarioEntity> usuarioEntities = usuarioRepository.findAll();

        if (usuarioEntity.getIdCargo().equals(2)) {
            for (UsuarioEntity value : usuarioEntities) {
                if (value.getMedicoEntity() != null) {
                    if (value.getMedicoEntity().getCrm().equals(usuarioEntity.getMedicoEntity().getCrm())) {
                        throw new RegraDeNegocioException("Já existe usuário com esse CRM!");
                    }
                }
            }
        }

        for (UsuarioEntity value : usuarioEntities) {
            //Quando estivermos atualizando, devemos verifiar se email e cpf já existem em outro
            if (value.getCpf().equals(usuarioEntity.getCpf())) {
                throw new RegraDeNegocioException("Já existe usuário com esse CPF!");
            }
            if (value.getEmail().equals(usuarioEntity.getEmail())) {
                throw new RegraDeNegocioException("Já existe usuário com esse e-mail!");
            }
        }
    }

    public void validarUsuarioEditado(UsuarioCreateDTO usuarioCreateDTO, Integer id) throws RegraDeNegocioException {

        List<UsuarioEntity> usuarioEntities = usuarioRepository.findAll();

        for (UsuarioEntity value : usuarioEntities) {
            //Quando estivermos atualizando, devemos verifiar se email e cpf já existem em outro usuário ALÉM do que está sendo atualizado.
            if (value.getCpf().equals(usuarioCreateDTO.getCpf()) && !value.getIdUsuario().equals(id)) {
                throw new RegraDeNegocioException("Já existe usuário com esse CPF!");
            }
            if (value.getEmail().equals(usuarioCreateDTO.getEmail()) && !value.getIdUsuario().equals(id)) {
                throw new RegraDeNegocioException("Já existe usuário com esse e-mail!");
            }
        }
    }

    public UsuarioEntity getUsuario(Integer id) throws RegraDeNegocioException {
        return usuarioRepository.findById(id)
                .filter(usuarioEntity -> usuarioEntity.getAtivo().equals(1))
                .orElseThrow(() -> new RegraDeNegocioException("Usuário não encontrado!"));

    }

    public UsuarioDTO getById(Integer id) throws RegraDeNegocioException {
        UsuarioDTO usuarioDTO = objectMapper.convertValue(getUsuario(id), UsuarioDTO.class);
        usuarioDTO.setEnderecoDTO(enderecoClient.getEndereco(usuarioDTO.getCep()));
        return usuarioDTO;
    }

    public Optional<UsuarioEntity> findByEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .filter(usuarioEntity -> usuarioEntity.getAtivo().equals(1));
    }

    public UsuarioDTO reativarUsuario(Integer idUsuario) throws RegraDeNegocioException {
        UsuarioEntity usuarioEntity = getUsuario(idUsuario);
        if (usuarioEntity.getAtivo() == 1) {
            throw new RegraDeNegocioException("Este usuário já está ativo!");
        }
        usuarioEntity.setAtivo(1);
        usuarioRepository.save(usuarioEntity);

        return getById(usuarioEntity.getIdUsuario());
    }

    public Integer getIdLoggedUser() {
        return Integer.parseInt(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString());
    }

    public void trocarSenha(TrocaSenhaDTO trocaSenhaDTO) throws RegraDeNegocioException {

        UsuarioEntity usuarioAcessado = getUsuario(getIdLoggedUser());
        if(passwordEncoder.matches(trocaSenhaDTO.getSenhaAntiga(), usuarioAcessado.getSenha()) && !passwordEncoder.matches(trocaSenhaDTO.getSenhaNova(), usuarioAcessado.getSenha())){
            usuarioAcessado.setSenha(passwordEncoder.encode(trocaSenhaDTO.getSenhaNova()));
            usuarioRepository.save(usuarioAcessado);
        }else{
            throw new RegraDeNegocioException("Senha inválida.");
        }

    }

    public void solicitarRedefinirSenha(String email) throws RegraDeNegocioException, JsonProcessingException {

        UsuarioEntity usuario = findByEmail(email)
                .orElseThrow(() -> new RegraDeNegocioException("Usuário não encontrado."));

        Random random = new Random();
        Integer codigoGerado = random.nextInt(100000, 999999);

        Map<String, Integer> codigoSenha = new HashMap<>();
        codigoSenha.put(usuario.getEmail(), codigoGerado);

        codigoTrocaSenha.setTokenBD(codigoSenha);

        Timer timer = new Timer();
        TimerTask tarefa = new TimerTask() {
            @Override
            public void run() {
                codigoTrocaSenha.getTokenBD().remove(email);
            }
        };
        timer.schedule(tarefa, 900000); // 15 minutos

        emailService.producerUsuarioEmail(usuario, TipoEmail.USUARIO_REDEFINIR_SENHA, codigoGerado);

    }

    public void redefinirSenha(RedefinicaoSenhaDTO redefinicaoSenhaDTO) throws RegraDeNegocioException, JsonProcessingException {

        UsuarioEntity usuarioEntity = findByEmail(redefinicaoSenhaDTO.getEmail())
                .orElseThrow(() -> new RegraDeNegocioException("Usuário não encontrado."));


        Map<String, Integer> verificarCodigo = new HashMap();
        verificarCodigo.put(redefinicaoSenhaDTO.getEmail(), redefinicaoSenhaDTO.getCodigoConfirmacao());

        Integer codigo = codigoTrocaSenha.getTokenBD().get(redefinicaoSenhaDTO.getEmail());

        if (codigo == null || !codigo.equals(redefinicaoSenhaDTO.getCodigoConfirmacao())) {
            throw new RegraDeNegocioException("Código de alteração de senha inválido!");
        } else {
            usuarioEntity.setSenha(passwordEncoder.encode(redefinicaoSenhaDTO.getSenhaNova()));
            usuarioRepository.save(usuarioEntity);

            emailService.producerUsuarioEmail(usuarioEntity, TipoEmail.USUARIO_SENHA_REDEFINIDA, null);

            codigoTrocaSenha.getTokenBD().remove(redefinicaoSenhaDTO.getEmail());
        }

    }

}
