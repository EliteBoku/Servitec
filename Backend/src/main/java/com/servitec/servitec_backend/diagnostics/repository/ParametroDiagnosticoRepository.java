package com.servitec.servitec_backend.diagnostics.repository;

import com.servitec.servitec_backend.diagnostics.entity.ParametroDiagnostico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParametroDiagnosticoRepository extends JpaRepository<ParametroDiagnostico, Integer> {

    List<ParametroDiagnostico> findByDiagnostico_IdDiagnosticoOrderByIdParametroAsc(Integer idDiagnostico);
}