package co.inter.piggies.domain.pix;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.DecimalMax;

import java.math.BigDecimal;

/*
 *-------------------------------------------------------------------------*
 * @Serdeable: o Micronaut Serialization gera, em tempo de compilação, o   *
 * código que converte JSON em objeto (e vice-versa). Sem ele, o Micronaut *
 * não consegue ler o body da requisição nem devolver este DTO como JSON.  *
 *-------------------------------------------------------------------------*
 */

@Serdeable
public record MakePixRequestDTO(
        @DecimalMax(value = "2",message = "valor pode ter no maximo 2 casas decimais")
        BigDecimal amount
) {
}
