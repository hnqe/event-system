package br.edu.ifg.event_system.service;

import br.edu.ifg.event_system.model.Departamento;
import br.edu.ifg.event_system.repository.DepartamentoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartamentoService {

    private final DepartamentoRepository departamentoRepository;

    public DepartamentoService(DepartamentoRepository departamentoRepository) {
        this.departamentoRepository = departamentoRepository;
    }

    public List<Departamento> listarTodos() {
        return departamentoRepository.findAll();
    }

    public List<Departamento> listarPorCampus(Long campusId) {
        return departamentoRepository.findByCampusId(campusId);
    }

    public Departamento criarOuAtualizar(Departamento departamento) {
        return departamentoRepository.save(departamento);
    }

    public Departamento buscarPorId(Long id) {
        return departamentoRepository.findById(id).orElse(null);
    }

    public void deletar(Long id) {
        departamentoRepository.deleteById(id);
    }

}