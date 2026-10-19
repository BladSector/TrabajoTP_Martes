package ar.edu.cafe.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PaginaController {
    @GetMapping({"/mozo", "/cocina", "/entregas", "/gestion-menu", "/historial-tickets", "/historial-pedidos", "/resumen-diario"})
    public String pagina() { return "forward:/index.html"; }
}
