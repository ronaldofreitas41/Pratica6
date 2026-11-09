package co.inter.piggies.service;


import co.inter.piggies.domain.customer.Customer;
import co.inter.piggies.domain.pix.Pix;
import co.inter.piggies.domain.pix.PixRequestDTO;
import co.inter.piggies.domain.pix.PixResponseDTO;
import co.inter.piggies.kafka.consumer.PixRequestConsumer;
import co.inter.piggies.kafka.events.PixProcessedEventDTO;
import co.inter.piggies.kafka.events.PixRequestEventDTO;
import co.inter.piggies.kafka.producer.PixProcessedProducer;
import co.inter.piggies.repository.CustomerRepository;
import co.inter.piggies.repository.PixRepository;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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
public class PixService {

    /*
     *-------------------------------------------------------------------------*
     * @Inject: o Micronaut cria o repository e o injeta neste campo.          *
     * O campo não pode ser private nem final (o Micronaut evita reflection).  *
     * Alternativa: receber o repository pelo construtor (permite usar final). *
     *-------------------------------------------------------------------------*
     */
    @Inject
    PixRepository pixRepository;

    @Inject
    CustomerRepository customerRepository;

    @Inject
    PixProcessedProducer producer;

    @Inject
    private static Logger LOG = LoggerFactory.getLogger(PixRequestConsumer.class);

    /*
     *--------------------------------------------------------------------*
     * Metodo: getPixById                                            *
     *--------------------------------------------------------------------*
     * Descrição: Função para buscar uma Pix pelo seu id se ele      *
     *            existir.                                                *
     *--------------------------------------------------------------------*
     * Params:  UUID id                                               *
     * Returns: PixResponseDTO                                       *
     *--------------------------------------------------------------------*
     */

    public PixResponseDTO getPixById(UUID id) {

        // Busca a entidade no banco de dados via id e caso a mesma não seja encontrada
        // retornará um Status 404 com a mensagem de dados nao encontrado para aquele id
        Pix pix = pixRepository.findById(id)
                .orElseThrow(() -> new HttpStatusException(HttpStatus.NOT_FOUND,
                        "Dado não encontrado para o id: " + id));

        //Retorna a conversão da Entity em um ResponseDTO
        return new PixResponseDTO(
                pix.getId(),
                pix.getStatus(),
                pix.getAmount(),
                pix.getRequestedAt()
        );
    }

    /*
     *--------------------------------------------------------------------*
     * Metodo: getAllPix                                             *
     *--------------------------------------------------------------------*
     * Descrição: Função para buscar uma lista com todas as Pixs     *
     *            presentes no banco de dados.                            *
     *--------------------------------------------------------------------*
     * Params:  None                                                      *
     * Returns: List<PixResponseDTO>                                 *
     *--------------------------------------------------------------------*
     */

    public List<PixResponseDTO> getAllPix() {

        //Busca todas as Entities presentes no banco
        List<Pix> pixs = pixRepository.findAll();

        //Retorna a conversão da Lista de Entities em uma Lista de ResponseDTO
        return pixs.stream().map(
                pix -> new PixResponseDTO(
                        pix.getId(),
                        pix.getStatus(),
                        pix.getAmount(),
                        pix.getRequestedAt()
                )).toList();
    }


    /*
     *--------------------------------------------------------------------*
     * Metodo: deletePix                                             *
     *--------------------------------------------------------------------*
     * Descrição: Função para buscar uma Pix pelo seu id se ele      *
     *            existir. E se ela existir ela será excluída             *
     *--------------------------------------------------------------------*
     * Params:  UUID id                                               *
     * Returns: None                                                      *
     *--------------------------------------------------------------------*
     */

    public void deletePix(UUID id) {

        // Busca a entidade no banco de dados via id e caso a mesma não seja encontrada
        // retornará um Status 404 com a mensagem de dados nao encontrado para aquele id
        Pix pix = pixRepository.findById(id)
                .orElseThrow(() -> new HttpStatusException(HttpStatus.NOT_FOUND,
                        "Dado não encontrado para o id: " + id));

        // Excluindo Entity referente ao id passado no banco se ela existir
        pixRepository.delete(pix);
    }

    @Transactional
    public void validadePix(PixRequestEventDTO event) {

        // 1 -> Buscando Pix
        Pix pix = pixRepository.findById(event.pixId()).orElse(null);

        // 2 -> Verificando se Pix Existe
        if (pix == null) {
            LOG.warn("O pix de id {} não foi encontrado", event.pixId());
            return;
        }

        // 3 -> Idempotência
        if (!"PENDING".equals(pix.getStatus())) {
            LOG.warn("O pix de id {} já foi processado", pix.getId());
            return;
        }

        // 4 -> Buscando Customer
        Customer customer = customerRepository.findById(pix.getCustomerID())
                .orElse(null);

        if (customer == null) {
            LOG.warn("Customer não encontrado para o Pix {}", pix.getId());

            pix.setStatus("REJECTED");
            pixRepository.update(pix);
            return;
        }

        // 5 -> Definindo fuso horário e data da solicitação
        ZoneId zone = ZoneId.of("America/Sao_Paulo");

        OffsetDateTime requestedAt = event.requestedAt()
                .atZoneSameInstant(zone)
                .toOffsetDateTime();

        LocalDate today = requestedAt.toLocalDate();

        OffsetDateTime startOfDay = today.atStartOfDay(zone)
                .toOffsetDateTime();

        OffsetDateTime endOfDay = today.plusDays(1)
                .atStartOfDay(zone)
                .toOffsetDateTime();

        // 6 -> Identificando período DAY ou NIGHT
        int hour = requestedAt.getHour();

        if (hour >= 20 || hour < 6) {
            pix.setPeriod("NIGHT");
        } else {
            pix.setPeriod("DAY");
        }

        // 7 -> Buscando Pix aprovados no mesmo dia
        List<Pix> dailyPixList = pixRepository
                .findByCustomerIdAndStatusAndRequestedAtGreaterThanEqualsAndRequestedAtLessThan(
                        customer.getId(),
                        "APPROVED",
                        startOfDay,
                        endOfDay
                );

        // Somando valores dos Pix aprovados
        BigDecimal dailyTotal = dailyPixList.stream()
                .map(Pix::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Acrescentando o valor do Pix atual
        BigDecimal newDailyTotal = dailyTotal.add(pix.getAmount());

        // 8 -> Validando limite diário
        if (newDailyTotal.compareTo(customer.getDailyLimit()) > 0) {

            pix.setStatus("REJECTED");
            pix.setReason("DAILY_LIMIT_EXCEEDED");
            Pix pixUpdated = pixRepository.update(pix);

            producer.send(pixUpdated.getCustomerID() ,new PixProcessedEventDTO(
                    pixUpdated.getId(),
                    pixUpdated.getCustomerID(),
                    pixUpdated.getStatus(),
                    pixUpdated.getReason(),
                    pixUpdated.getPeriod()
            ));

            LOG.warn("Pix {} rejeitado: limite diário excedido", pix.getId());
            return;
        }

        // 9 -> Validando limite noturno
        if (pix.getPeriod().equals("NIGHT")) {

            // Identificando a data de início da janela noturna
            LocalDate nightDate = hour < 6
                    ? today.minusDays(1)
                    : today;

            OffsetDateTime startOfNight = nightDate
                    .atTime(20, 0)
                    .atZone(zone)
                    .toOffsetDateTime();

            OffsetDateTime endOfNight = nightDate.plusDays(1)
                    .atTime(6, 0)
                    .atZone(zone)
                    .toOffsetDateTime();

            // Buscando Pix aprovados na mesma janela noturna
            List<Pix> nightPixList = pixRepository
                    .findByCustomerIdAndStatusAndRequestedAtGreaterThanEqualsAndRequestedAtLessThan(
                            customer.getId(),
                            "APPROVED",
                            startOfNight,
                            endOfNight
                    );

            // Somando valores dos Pix aprovados
            BigDecimal nightTotal = nightPixList.stream()
                    .map(Pix::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Acrescentando o valor do Pix atual
            BigDecimal newNightTotal = nightTotal.add(pix.getAmount());

            // Comparando com limite noturno
            if (newNightTotal.compareTo(customer.getNightLimit()) > 0) {

                pix.setStatus("REJECTED");
                pix.setReason("NIGHT_LIMIT_EXCEEDED");

                Pix pixUpdated = pixRepository.update(pix);

                producer.send(pixUpdated.getCustomerID() ,new PixProcessedEventDTO(
                        pixUpdated.getId(),
                        pixUpdated.getCustomerID(),
                        pixUpdated.getStatus(),
                        pixUpdated.getReason(),
                        pixUpdated.getPeriod()
                ));

                LOG.warn("Pix {} rejeitado: limite noturno excedido", pix.getId());
                return;
            }
        }

        // 10 -> Aprovando Pix
        pix.setStatus("APPROVED");
        Pix pixUpdated = pixRepository.update(pix);

        //Publicando resultado do pix
        producer.send(pixUpdated.getCustomerID() ,new PixProcessedEventDTO(
                pixUpdated.getId(),
                pixUpdated.getCustomerID(),
                pixUpdated.getStatus(),
                null,
                pixUpdated.getPeriod()
        ));

        LOG.info("Pix {} aprovado com sucesso", pix.getId());
    }
}
