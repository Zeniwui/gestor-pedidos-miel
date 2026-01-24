package com.mielpanera.gestor_pedidos_miel.model;

import java.time.LocalDate;

public record CorreosInfo (
        String estado,
        LocalDate fechaEvento
) {}
