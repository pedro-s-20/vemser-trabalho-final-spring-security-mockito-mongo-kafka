package br.com.dbc.vemser.trabalhofinal.security;

import br.com.dbc.vemser.trabalhofinal.dto.LoginDTO;
import br.com.dbc.vemser.trabalhofinal.entity.UsuarioEntity;
import br.com.dbc.vemser.trabalhofinal.exceptions.RegraDeNegocioException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Service
public class TokenService {
    private AuthenticationManager authenticationManager;

    private TokenService(@Lazy AuthenticationManager authenticationManager) {
        authenticationManager = this.authenticationManager;
    }

    // FIXME RECUPERAR O TEMPO DE EXPIRAÇÃO DOS PROPERTIES
    @Value("${jwt.expiration}")
    private String expiration;

    // FIXME RECUPERAR A CHAVE SECRETA DOS PROPERTIES
    @Value("${jwt.secret}")
    private String secret;

    private final String CHAVE_LOGIN = "CHAVE_LOGIN";
    private static final String TOKEN_PREFIX = "Bearer";
    private static final String CARGOS_CHAVE = "cargos";


    public String generateToken(UsuarioEntity usuarioEntity) {
        java.util.Date now = new java.util.Date();
        java.util.Date exp = new java.util.Date(now.getTime() + Long.parseLong(expiration));

        List<String> cargos = usuarioEntity.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .toList();

        String token = Jwts.builder()
                .claim(CHAVE_LOGIN, usuarioEntity.getEmail())
                .claim(Claims.ID, usuarioEntity.getIdUsuario())
                .claim(CARGOS_CHAVE, cargos)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
        return token;
    }

    public UsernamePasswordAuthenticationToken isValid(String token) {
        if (token != null) {
            Claims body = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token.replace(TOKEN_PREFIX, ""))
                    .getBody();
            String user = body.get(Claims.ID, String.class);
            if (user != null) {
                List<String> cargos = body.get(CARGOS_CHAVE, List.class);
                List<SimpleGrantedAuthority> cargosDoMeuUsuario = cargos.stream()
                        .map(authority -> new SimpleGrantedAuthority(authority))
                        .toList();

                return new UsernamePasswordAuthenticationToken(user, null, cargosDoMeuUsuario);
            }
        }
        return null;
    }

    public String autenticar(LoginDTO loginDTO) throws RegraDeNegocioException {
        try {
            // 1 criar dto do spring
            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            loginDTO.getEmail(),
                            loginDTO.getSenha()
                    );

            // 1 autenticar-se
            Authentication authentication = authenticationManager.authenticate(usernamePasswordAuthenticationToken);

            // user details...
            Object principal = authentication.getPrincipal();
            UsuarioEntity usuarioEntity = (UsuarioEntity) principal;

            return generateToken(usuarioEntity);
        } catch (BadCredentialsException ex) {
            ex.printStackTrace();
            throw new RegraDeNegocioException("Credenciais inválidas.");
        }

    }
}