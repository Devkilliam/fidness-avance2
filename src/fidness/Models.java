package fidness;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

enum RolUsuario { CLIENTE, ADMIN }
enum TipoEjercicio { PIERNA, ESPALDA, BRAZO, PECHO, HOMBRO, CORE, CARDIO }

class Usuario implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final String username;
    private final String passwordHash;
    private final String nombreCompleto;
    private final RolUsuario rol;

    Usuario(int id, String username, String passwordPlain, String nombreCompleto, RolUsuario rol) {
        this.id = id;
        this.username = Objects.requireNonNull(username).trim().toLowerCase();
        this.passwordHash = hashPassword(Objects.requireNonNull(passwordPlain));
        this.nombreCompleto = Objects.requireNonNull(nombreCompleto);
        this.rol = Objects.requireNonNull(rol);
    }

    int getId() { return id; }
    String getUsername() { return username; }
    String getNombreCompleto() { return nombreCompleto; }
    RolUsuario getRol() { return rol; }
    boolean esAdmin() { return rol == RolUsuario.ADMIN; }

    boolean validarCredenciales(String username, String passwordPlain) {
        if (username == null || passwordPlain == null) return false;
        if (!this.username.equals(username.trim().toLowerCase())) return false;
        return this.passwordHash.equals(hashPassword(passwordPlain));
    }

    static String hashPassword(String plain) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(plain.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : out) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo hashear la contraseña", e);
        }
    }
}

/**
 * HERENCIA: EjercicioBase (abstract) es la clase padre.
 * POLIMORFISMO: RutinaItem guarda EjercicioBase, que puede ser EjercicioFuerza o EjercicioCardio.
 */
abstract class EjercicioBase implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final String nombre;
    private final TipoEjercicio tipo;
    private final String descripcion;
    private final List<String> pasos;

    EjercicioBase(int id, String nombre, TipoEjercicio tipo, String descripcion, List<String> pasos) {
        this.id = id;
        this.nombre = Objects.requireNonNull(nombre).trim();
        this.tipo = Objects.requireNonNull(tipo);
        this.descripcion = Objects.requireNonNull(descripcion).trim();
        this.pasos = new ArrayList<>(Objects.requireNonNull(pasos));
    }

    int getId() { return id; }
    String getNombre() { return nombre; }
    TipoEjercicio getTipo() { return tipo; }
    String getDescripcion() { return descripcion; }
    List<String> getPasos() { return new ArrayList<>(pasos); }

    String resumen() { return nombre + " (" + tipo + ")"; }

    // Polimorfismo: cada hijo implementa su extra
    abstract String detalleExtra();

    @Override public String toString() {
        return resumen();
    }
}

class EjercicioFuerza extends EjercicioBase {
    private static final long serialVersionUID = 1L;

    private final int repeticionesRecomendadas;

    EjercicioFuerza(int id, String nombre, TipoEjercicio tipo, String descripcion, List<String> pasos, int reps) {
        super(id, nombre, tipo, descripcion, pasos);
        this.repeticionesRecomendadas = reps;
    }

    int getRepeticionesRecomendadas() { return repeticionesRecomendadas; }

    @Override
    String detalleExtra() {
        return "Repeticiones recomendadas: " + repeticionesRecomendadas;
    }
}

class EjercicioCardio extends EjercicioBase {
    private static final long serialVersionUID = 1L;

    private final int duracionMinRecomendada;

    EjercicioCardio(int id, String nombre, TipoEjercicio tipo, String descripcion, List<String> pasos, int duracionMin) {
        super(id, nombre, tipo, descripcion, pasos);
        this.duracionMinRecomendada = duracionMin;
    }

    int getDuracionMinRecomendada() { return duracionMinRecomendada; }

    @Override
    String detalleExtra() {
        return "Duración recomendada (min): " + duracionMinRecomendada;
    }
}

class RutinaItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int orden;
    private final EjercicioBase ejercicio;

    RutinaItem(int orden, EjercicioBase ejercicio) {
        this.orden = orden;
        this.ejercicio = Objects.requireNonNull(ejercicio);
    }

    int getOrden() { return orden; }
    EjercicioBase getEjercicio() { return ejercicio; }

    @Override public String toString() {
        return orden + ". " + ejercicio.resumen();
    }
}

class Rutina implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final int usuarioId;
    private final String nombre;
    private final LocalDateTime fechaCreacion;
    private final List<RutinaItem> items = new ArrayList<>();

    Rutina(int id, int usuarioId, String nombre) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.nombre = Objects.requireNonNull(nombre).trim();
        this.fechaCreacion = LocalDateTime.now();
    }

    int getId() { return id; }
    int getUsuarioId() { return usuarioId; }
    String getNombre() { return nombre; }
    LocalDateTime getFechaCreacion() { return fechaCreacion; }
    List<RutinaItem> getItems() { return new ArrayList<>(items); }

    void setEjercicios(List<EjercicioBase> ejercicios) {
        items.clear();
        int o = 1;
        for (EjercicioBase e : ejercicios) items.add(new RutinaItem(o++, e));
    }

    int cantidadEjercicios() { return items.size(); }

    @Override public String toString() {
        return nombre + " (" + cantidadEjercicios() + " ejercicios)";
    }
}