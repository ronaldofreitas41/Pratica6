package co.inter.piggies.controller;

import co.inter.piggies.domain.pix.PixRequestDTO;
import co.inter.piggies.domain.pix.PixResponseDTO;
import co.inter.piggies.service.PixService;
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

@Controller("/api/Pix")
@ExecuteOn(TaskExecutors.BLOCKING)
public class PixController {

    /*
     *-----------------------------------------------------------------------------*
     * @Inject: o Micronaut cria o service e o injeta neste campo automaticamente. *
     * O campo não pode ser private nem final (o Micronaut evita usar reflection). *
     * Alternativa: receber o service pelo construtor (permite usar final).        *
     *-----------------------------------------------------------------------------*
     */

    @Inject
    PixService pixService;

    /*
     *----------------------------------------------------------------------*
     * Metodo: GET                                                          *
     * Path: /api/Pix                                            *
     *----------------------------------------------------------------------*
     * Descrição: Função para buscar todos os Pixs presentes no banco. *
     *----------------------------------------------------------------------*
     * Params:  None                                                        *
     * Returns: List<PixResponseDTO>                                   *
     *----------------------------------------------------------------------*
     */

    @Get
    public HttpResponse<List<PixResponseDTO>> getAllPix() {
        return HttpResponse.ok(pixService.getAllPix());
    }

    /*
     *-----------------------------------------------------------------------------------*
     * Metodo: GET                                                                       *
     * Path: /api/Pix/{id}                                                    *
     *-----------------------------------------------------------------------------------*
     * Descrição: Função para buscar no banco o registro que tenha o id passado no Path. *
     *-----------------------------------------------------------------------------------*
     * Params:  UUID id                                                              *
     * Returns: PixResponseDTO                                                      *
     *-----------------------------------------------------------------------------------*
     */

    @Get("/{id}")
    public HttpResponse<PixResponseDTO> getPixById(@PathVariable UUID id) {
        return HttpResponse.ok(pixService.getPixById(id));
    }

    /*
     *-----------------------------------------------------------------------------------*
     * Metodo: DELETE                                                                    *
     * Path: /api/Pix/{id}                                                    *
     *-----------------------------------------------------------------------------------*
     * Descrição: Função para buscar no banco o registro que tenha o id passado no Path, *
     *            e excluir o mesmo                                                      *
     *-----------------------------------------------------------------------------------*
     * Params:  UUID id                                                              *
     * Returns: apenas o status                                                          *
     *-----------------------------------------------------------------------------------*
     */
    @Delete("/{id}")
    public HttpResponse<Void> deletePix(@PathVariable UUID id) {
        pixService.deletePix(id);
        return HttpResponse.noContent();
    }
}

