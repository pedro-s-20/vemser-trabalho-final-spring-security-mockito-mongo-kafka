package br.com.dbc.vemser.trabalhofinal.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity(name = "Agendamento")
public class AgendamentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_AGENDAMENTO")
    @SequenceGenerator(name = "SEQ_AGENDAMENTO", sequenceName = "SEQ_AGENDAMENTO", allocationSize = 1)
    @Column(name = "id_convenio")
    private Integer idAgendamento;
    @Column(name = "id_cliente", insertable = false, updatable = false)
    private Integer idCliente;
    @Column(name = "id_medico", insertable = false, updatable = false)
    private Integer idMedico;
    @Column(name = "tratamento")
    private String tratamento;
    @Column(name = "exame")
    private String exame;
    @Column(name = "data_horario")
    private LocalDateTime dataHorario;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "id_cliente", referencedColumnName = "id_cliente")
    private ClienteEntity clienteEntity;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "id_medico", referencedColumnName = "id_medico")
    private MedicoEntity medicoEntity;

}
