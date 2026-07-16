package com.exp.medic.cics.service.receta;


import com.exp.medic.cics.dto.receta.request.RecetaDTO;
import com.exp.medic.cics.dto.receta.response.RecetaDTOResponse;

public interface IRecetaMedicaService {

    RecetaDTOResponse createdRecetaMedic(RecetaDTO recetaDTO);

}
