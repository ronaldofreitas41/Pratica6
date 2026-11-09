package co.inter.piggies.controller;

import co.inter.piggies.domain.customer.CustomerRequestDTO;
import co.inter.piggies.domain.customer.CustomerResponseDTO;
import co.inter.piggies.domain.pix.MakePixRequestDTO;
import co.inter.piggies.domain.pix.MakePixResponseDTO;
import co.inter.piggies.domain.pix.PixLimitRequestDTO;
import co.inter.piggies.service.CustomerService;
import io.micronaut.http.HttpStatus;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;

import java.util.List;
import java.util.UUID;

/*
 *-------------------------------------------------------------------------*
 * @Controller marca a classe como um controller HTTP (bean do Micronaut). *
 * O valor passado (ex.: "/api/produtos") é o caminho base de todos os     *
 * endpoints da classe. O path de cada método (ex.: @Get("/{id}")) é       *
 * somado a ele, resultando em /api/produtos/{id}.                         *
 *-------------------------------------------------------------------------*
 * @ExecuteOn(TaskExecutors.BLOCKING) faz os métodos deste controller      *
 * rodarem no pool de threads para tarefas bloqueantes, e não no event     *
 * loop do Netty (padrão do Micronaut).                                    *
 *-------------------------------------------------------------------------*
 * Importância: o event loop usa poucas threads, que atendem todas as      *
 * requisições. Se uma delas ficar travada esperando banco (JDBC/JPA),     *
 * arquivo ou outra API, o servidor inteiro fica lento ou para.            *
 * Com o BLOCKING, essas chamadas rodam em outro pool e o event loop       *
 * continua livre para receber novas requisições.                          *
 *-------------------------------------------------------------------------*
 * Usar sempre que: O controller fizer chamadas bloqueantes (o caso comum  *
 * com repositories). Não é necessário em métodos reativos/assíncronos.    *
 *-------------------------------------------------------------------------*
 */

@Controller("/api/customer")
@ExecuteOn(TaskExecutors.BLOCKING)
public class CustomerController {

    /*
     *-----------------------------------------------------------------------------*
     * @Inject: o Micronaut cria o service e o injeta neste campo automaticamente. *
     * O campo não pode ser private nem final (o Micronaut evita usar reflection). *
     * Alternativa: receber o service pelo construtor (permite usar final).        *
     *-----------------------------------------------------------------------------*
     */

    @Inject
    CustomerService customerService;

    /*
     *----------------------------------------------------------------------*
     * Metodo: GET                                                          *
     * Path: /api/customer                                            *
     *----------------------------------------------------------------------*
     * Descrição: Função para buscar todos os Customers presentes no banco. *
     *----------------------------------------------------------------------*
     * Params:  None                                                        *
     * Returns: List<CustomerResponseDTO>                                   *
     *----------------------------------------------------------------------*
     */

    @Get
    public HttpResponse<List<CustomerResponseDTO>> getAllCustomer() {
        return HttpResponse.ok(customerService.getAllCustomer());
    }

    /*
     *-----------------------------------------------------------------------------------*
     * Metodo: GET                                                                       *
     * Path: /api/customer/{id}                                                    *
     *-----------------------------------------------------------------------------------*
     * Descrição: Função para buscar no banco o registro que tenha o id passado no Path. *
     *-----------------------------------------------------------------------------------*
     * Params:  UUID id                                                              *
     * Returns: CustomerResponseDTO                                                      *
     *-----------------------------------------------------------------------------------*
     */

    @Get("/{id}")
    public HttpResponse<CustomerResponseDTO> getCustomerById(@PathVariable UUID id) {
        return HttpResponse.ok(customerService.getCustomerById(id));
    }
    @Get("/{id}/pix-limits")
    public HttpResponse<PixLimitRequestDTO> getCustomerPixLimits(@PathVariable UUID id) {
        return HttpResponse.ok(customerService.getCustomerPixLimit(id));
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo: POST                                                       *
     * Path: /api/customer                                          *
     *--------------------------------------------------------------------*
     * Descrição: Criar um registro novo referente a Entidade Customer.   *
     *--------------------------------------------------------------------*
     * Params:  CustomerRequestDTO body                                   *
     * Returns: CustomerResponseDTO                                       *
     *--------------------------------------------------------------------*
     */

    @Post
    public HttpResponse<CustomerResponseDTO> createCustomer(@Valid @Body CustomerRequestDTO body) {
        return HttpResponse.created(customerService.createCustomer(body));
    }

    @Post("/{id}/pix")
    public HttpResponse<MakePixResponseDTO> createCustomer(@PathVariable UUID id, @Valid @Body MakePixRequestDTO body) {
        return HttpResponse.status(HttpStatus.ACCEPTED).body(customerService.makePix(id,body));
    }

    /*
     *-----------------------------------------------------------------------------------*
     * Metodo: PUT                                                                       *
     * Path: /api/customer/{id}                                                    *
     *-----------------------------------------------------------------------------------*
     * Descrição: Função para buscar no banco o registro que tenha o id passado no Path, *
     *            e editá-lo conforme o body passado.                                    *
     *-----------------------------------------------------------------------------------*
     * Params:  UUID id, CustomerRequestDTO body                                     *
     * Returns: CustomerResponseDTO                                                      *
     *-----------------------------------------------------------------------------------*
     */
    @Put("/{id}")
    public HttpResponse<CustomerResponseDTO> updateCustomer(@PathVariable UUID id, @Valid @Body CustomerRequestDTO body) {
        return HttpResponse.ok(customerService.updateCustomer(id, body));
    }
    @Put("/{id}/pix-limits")
    public HttpResponse<PixLimitRequestDTO> updateCustomerLimits(@PathVariable UUID id, @Valid @Body PixLimitRequestDTO body) {
        return HttpResponse.ok(customerService.updatePixLimits(id, body));
    }

    /*
     *-----------------------------------------------------------------------------------*
     * Metodo: DELETE                                                                    *
     * Path: /api/customer/{id}                                                    *
     *-----------------------------------------------------------------------------------*
     * Descrição: Função para buscar no banco o registro que tenha o id passado no Path, *
     *            e excluir o mesmo                                                      *
     *-----------------------------------------------------------------------------------*
     * Params:  UUID id                                                              *
     * Returns: apenas o status                                                          *
     *-----------------------------------------------------------------------------------*
     */
    @Delete("/{id}")
    public HttpResponse<Void> deleteCustomer(@PathVariable UUID id) {
        customerService.deleteCustomer(id);
        return HttpResponse.noContent();
    }
}
