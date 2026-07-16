package com.exp.medic.cics.dto.receta.request;

public record SignosVitalesDTO(
        String peso,
        String talla,
        String ta,      // Tensión arterial
        String fc,       // Frecuencia cardiaca
        String fr,        // Frecuencia respiratoria
        String temp,
        String sato2,
        String imc,
        String alergias
) {}
