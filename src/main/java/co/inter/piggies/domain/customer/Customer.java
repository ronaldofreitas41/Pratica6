package co.inter.piggies.domain.customer;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/*
 *------------------------------------------------------------------------*
 * @Entity: marca a classe como uma entidade JPA, mapeada para uma tabela *
 * no banco e gerenciada pelo Hibernate.                                  *
 *------------------------------------------------------------------------*
 * @Table(name = ...): define o nome da tabela no banco. Sem ela, o nome  *
 * da tabela seria o nome da classe.                                      *
 *------------------------------------------------------------------------*
 * @Getter: o Lombok gera os getters de todos os campos em tempo de       *
 * compilação.                                                            *
 *------------------------------------------------------------------------*
 * @Setter: o Lombok gera os setters de todos os campos (usados no update *
 * para alterar a entidade).                                              *
 *------------------------------------------------------------------------*
 * @NoArgsConstructor: gera o construtor vazio, obrigatório para o JPA    *
 * conseguir instanciar a entidade ao ler do banco.                       *
 *------------------------------------------------------------------------*
 * @AllArgsConstructor: gera um construtor com todos os campos, útil para *
 * criar a entidade de uma vez a partir do RequestDTO.                    *
 *------------------------------------------------------------------------*
 */
@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    /*
     *------------------------------------------------------*
     * @Id: simboliza que aquela colina é o id da entidade  *
     *------------------------------------------------------*
     * @GeneratedValue: Define um valor aleatorio para o id *
     *------------------------------------------------------*
     */
    @Id
    @GeneratedValue
    private UUID id;
    private String name;

    @Column(precision = 20, scale = 2)
    private BigDecimal dailyLimit;
    @Column(precision = 20, scale = 2)
    private BigDecimal nightLimit;

    //Coloque aqui o restante das propriedades da entidade ...
}
