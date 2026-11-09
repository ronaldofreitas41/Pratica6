package co.inter.piggies.domain.pix;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.DecimalMax;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/*
 *-------------------------------------------------------------------------*
 * @Serdeable: o Micronaut Serialization gera, em tempo de compilação, o   *
 * código que converte JSON em objeto (e vice-versa). Sem ele, o Micronaut *
 * não consegue ler o body da requisição nem devolver este DTO como JSON.  *
 *-------------------------------------------------------------------------*
 */

@Serdeable
public record PixRequestDTO(
        String period,
        String reason,
        BigDecimal amount
) {
}
