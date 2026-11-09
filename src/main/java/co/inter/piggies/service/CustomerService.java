package co.inter.piggies.service;

import co.inter.piggies.domain.customer.Customer;
import co.inter.piggies.domain.customer.CustomerRequestDTO;
import co.inter.piggies.domain.customer.CustomerResponseDTO;
import co.inter.piggies.domain.pix.MakePixRequestDTO;
import co.inter.piggies.domain.pix.MakePixResponseDTO;
import co.inter.piggies.domain.pix.Pix;
import co.inter.piggies.domain.pix.PixLimitRequestDTO;
import co.inter.piggies.kafka.events.PixRequestEventDTO;
import co.inter.piggies.kafka.producer.PixRequestProducer;
import co.inter.piggies.repository.CustomerRepository;
import co.inter.piggies.repository.PixRepository;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/*
 *-----------------------------------------------------------------------*
 * @Singleton registra a classe como um bean do Micronaut com uma única  *
 * instância compartilhada por toda a aplicação. É o que permite injetar *
 * este service no controller (via @Inject ou construtor). Por ser       *
 * compartilhado, não guarde estado de requisição em campos da classe.   *
 *-----------------------------------------------------------------------*
 */
@Singleton
public class CustomerService {

    /*
     *-------------------------------------------------------------------------*
     * @Inject: o Micronaut cria o repository e o injeta neste campo.          *
     * O campo não pode ser private nem final (o Micronaut evita reflection).  *
     * Alternativa: receber o repository pelo construtor (permite usar final). *
     *-------------------------------------------------------------------------*
     */
    @Inject
    CustomerRepository customerRepository;

    @Inject
    PixRepository pixRepository;

    @Inject
    PixRequestProducer pixRequestProducer;

    /*
     *--------------------------------------------------------------------*
     * Metodo: getCustomerById                                            *
     *--------------------------------------------------------------------*
     * Descrição: Função para buscar uma Customer pelo seu id se ele      *
     *            existir.                                                *
     *--------------------------------------------------------------------*
     * Params:  UUID id                                               *
     * Returns: CustomerResponseDTO                                       *
     *--------------------------------------------------------------------*
     */

    public CustomerResponseDTO getCustomerById(UUID id) {

        // Busca a entidade no banco de dados via id e caso a mesma não seja encontrada
        // retornará um Status 404 com a mensagem de dados nao encontrado para aquele id
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new HttpStatusException(HttpStatus.NOT_FOUND,
                        "Dado não encontrado para o id: " + id));

        //Retorna a conversão da Entity em um ResponseDTO
        return new CustomerResponseDTO(
                customer.getId(),
                customer.getName(),
                customer.getDailyLimit(),
                customer.getNightLimit()
        );
    }

    public PixLimitRequestDTO getCustomerPixLimit(UUID id) {

        // Busca a entidade no banco de dados via id e caso a mesma não seja encontrada
        // retornará um Status 404 com a mensagem de dados nao encontrado para aquele id
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new HttpStatusException(HttpStatus.NOT_FOUND,
                        "Dado não encontrado para o id: " + id));

        //Retorna a conversão da Entity em um ResponseDTO
        return new PixLimitRequestDTO(
                customer.getDailyLimit(),
                customer.getNightLimit()
        );
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo: getAllCustomer                                             *
     *--------------------------------------------------------------------*
     * Descrição: Função para buscar uma lista com todas as Customers     *
     *            presentes no banco de dados.                            *
     *--------------------------------------------------------------------*
     * Params:  None                                                      *
     * Returns: List<CustomerResponseDTO>                                 *
     *--------------------------------------------------------------------*
     */

    public List<CustomerResponseDTO> getAllCustomer() {

        //Busca todas as Entities presentes no banco
        List<Customer> customers = customerRepository.findAll();

        //Retorna a conversão da Lista de Entities em uma Lista de ResponseDTO
        return customers.stream().map(
                customer -> new CustomerResponseDTO(
                        customer.getId(),
                        customer.getName(),
                        customer.getDailyLimit(),
                        customer.getNightLimit()
                )).toList();
    }


    /*
     *--------------------------------------------------------------------*
     * Metodo: createCustomer                                             *
     *--------------------------------------------------------------------*
     * Descrição: Função para criar uma Customer com os dados passados    *
     *            via body da requisição                                  *
     *--------------------------------------------------------------------*
     * Params:  CustomerRequestDTO body                                   *
     * Returns: CustomerResponseDTO                                       *
     *--------------------------------------------------------------------*
     */

    public CustomerResponseDTO createCustomer(CustomerRequestDTO body) {

        //Converte o Body que é um RequestDTO em uma Entity
        Customer customer = new Customer();

        //Setando Entity...
        customer.setName(body.name());
        customer.setDailyLimit(BigDecimal.valueOf(3000.00));
        customer.setNightLimit(BigDecimal.valueOf(1000.00));


        // Salvando Entity no banco e recebendo os dados que foram salvos
        Customer customerSaved = customerRepository.save(customer);

        //Retorna a conversão da Entidade que foi salva em um ResponseDTO
        return new CustomerResponseDTO(
                customerSaved.getId(),
                customerSaved.getName(),
                customerSaved.getDailyLimit(),
                customerSaved.getNightLimit()
        );
    }

    public MakePixResponseDTO makePix(UUID customerID, MakePixRequestDTO body) {

        Pix pix = new Pix();

        pix.setId(UUID.randomUUID());
        pix.setCustomerID(customerID);
        pix.setStatus("PENDING");
        pix.setAmount(body.amount());
        pix.setRequestedAt(OffsetDateTime.now());

        Pix pixSaved = pixRepository.save(pix);

        PixRequestEventDTO event = new PixRequestEventDTO(
                pixSaved.getId(),
                pixSaved.getCustomerID(),
                pixSaved.getAmount(),
                pixSaved.getRequestedAt()
        );

        //Pus como key o costumerID por conta que os pix daquele customer serão feitos por ordenadem de realização
        pixRequestProducer.send(customerID, event);

        return new MakePixResponseDTO(
                pixSaved.getId(),
                pixSaved.getStatus()
                );

    }

    /*
     *--------------------------------------------------------------------*
     * Metodo: updateCustomer                                             *
     *--------------------------------------------------------------------*
     * Descrição: Função para buscar uma Customer pelo seu id se ele      *
     *            existir. E se ela existir editar com os dados passados  *
     *            via body da requisição                                  *
     *--------------------------------------------------------------------*
     * Params:  UUID id, CustomerRequestDTO body                      *
     * Returns: CustomerResponseDTO                                       *
     *--------------------------------------------------------------------*
     */

    public CustomerResponseDTO updateCustomer(UUID id, CustomerRequestDTO body) {

        // Busca a entidade no banco de dados via id e caso a mesma não seja encontrada
        // retornará um Status 404 com a mensagem de dados nao encontrado para aquele id
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new HttpStatusException(HttpStatus.NOT_FOUND,
                        "Dado não encontrado para o id: " + id));

        customer.setName(body.name());

        // Atualizando Entity referente ao id passado no banco
        Customer customerUpdated = customerRepository.update(customer);

        //Retorna a conversão da RequestDTO em um ResponseDTO
        return new CustomerResponseDTO(
                customerUpdated.getId(),
                customerUpdated.getName(),
                customerUpdated.getDailyLimit(),
                customerUpdated.getNightLimit()
        );
    }

    public PixLimitRequestDTO updatePixLimits(UUID id, PixLimitRequestDTO body) {

        // Busca a entidade no banco de dados via id e caso a mesma não seja encontrada
        // retornará um Status 404 com a mensagem de dados nao encontrado para aquele id
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new HttpStatusException(HttpStatus.NOT_FOUND,
                        "Dado não encontrado para o id: " + id));

        if ((body.dailyLimit().compareTo(BigDecimal.ZERO) <= 0)
                || (body.dailyLimit().scale() > 2)
                || (body.nightLimit().compareTo(BigDecimal.ZERO) <= 0)
                || (body.nightLimit().scale() > 2)) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST,
                    "Valor invalido para limites verifique se é maior que 0 ou se tem mais de 2 casas decimais");
        }

        customer.setDailyLimit(body.dailyLimit());
        customer.setNightLimit(body.nightLimit());

        // Atualizando Entity referente ao id passado no banco
        Customer customerUpdated = customerRepository.update(customer);

        //Retorna a conversão da RequestDTO em um ResponseDTO
        return new PixLimitRequestDTO(
                customerUpdated.getDailyLimit(),
                customerUpdated.getNightLimit()
        );
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo: deleteCustomer                                             *
     *--------------------------------------------------------------------*
     * Descrição: Função para buscar uma Customer pelo seu id se ele      *
     *            existir. E se ela existir ela será excluída             *
     *--------------------------------------------------------------------*
     * Params:  UUID id                                               *
     * Returns: None                                                      *
     *--------------------------------------------------------------------*
     */

    public void deleteCustomer(UUID id) {

        // Busca a entidade no banco de dados via id e caso a mesma não seja encontrada
        // retornará um Status 404 com a mensagem de dados nao encontrado para aquele id
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new HttpStatusException(HttpStatus.NOT_FOUND,
                        "Dado não encontrado para o id: " + id));

        // Excluindo Entity referente ao id passado no banco se ela existir
        customerRepository.delete(customer);
    }
}
