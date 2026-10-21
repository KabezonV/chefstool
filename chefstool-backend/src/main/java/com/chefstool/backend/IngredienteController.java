package com.chefstool.backend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Arrays;

@RestController
@RequestMapping("/api/ingredientes")
public class IngredienteController {

    // Por ahora, devolvemos una lista falsa (mock) para probar la conexión
    @GetMapping
    public List<Ingrediente> obtenerIngredientes() {
        Ingrediente ing1 = new Ingrediente();
        ing1.setId(1L);
        ing1.setNombre("Tomate");
        ing1.setPrecio(2.50);
        ing1.setUnidadMedida("kg");

        Ingrediente ing2 = new Ingrediente();
        ing2.setId(2L);
        ing2.setNombre("Cebolla");
        ing2.setPrecio(1.20);
        ing2.setUnidadMedida("kg");

        return Arrays.asList(ing1, ing2);
    }
}