package co.inter.piggies.domain.pix;

import java.util.UUID;

import io.micronaut.serde.annotation.Serdeable;

/*
 *-------------------------------------------------------------------------*
 * @Serdeable: o Micronaut Serialization gera, em tempo de compilação, o   *
 * código que converte JSON em objeto (e vice-versa). Sem ele, o Micronaut *
 * não consegue ler o body da requisição nem devolver este DTO como JSON.  *
 *-------------------------------------------------------------------------*
 */
@Serdeable
public record MakePixResponseDTO(
        UUID pixid,
        String satus
) {
}
