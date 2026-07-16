package com.exp.medic.cics.service.historial;

import com.exp.medic.cics.dto.historial.request.PatientIntakeFormDTO;
import com.exp.medic.cics.dto.historial.response.PatientIntakeFormDTOResponse;

public interface IHistorialMedicaService {

    PatientIntakeFormDTOResponse createdHistorialMedic(PatientIntakeFormDTO patientIntakeFormDTO);
}
