package ar.edu.cafe.web;

import java.util.Map;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ErroresController {
    private static final Logger LOG = LoggerFactory.getLogger(ErroresController.class);
    private ResponseEntity<Map<String,String>> respuesta(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of("error", mensaje));
    }
    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<Map<String,String>> inexistente(NoSuchElementException error) {
        return respuesta(HttpStatus.NOT_FOUND, error.getMessage());
    }
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String,String>> invalido(IllegalArgumentException error) {
        return respuesta(HttpStatus.BAD_REQUEST, error.getMessage());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String,String>> validacion(MethodArgumentNotValidException error) {
        var campo = error.getBindingResult().getFieldError();
        return respuesta(HttpStatus.BAD_REQUEST, campo == null ? "Datos invalidos" : "Revise el campo " + campo.getField());
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String,String>> formato() { return respuesta(HttpStatus.BAD_REQUEST, "Solicitud o valores invalidos"); }
    @ExceptionHandler(DuplicateKeyException.class)
    ResponseEntity<Map<String,String>> duplicado() { return respuesta(HttpStatus.CONFLICT, "El registro ya existe"); }
    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<Map<String,String>> baseDatos(DataAccessException error) {
        LOG.error("Fallo de persistencia", error);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo completar la operacion en la base de datos");
    }
}
