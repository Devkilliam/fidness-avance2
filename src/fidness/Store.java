package fidness;

import fidness.Exceptions.AuthException;
import fidness.Exceptions.ValidationException;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Store {

    private final AtomicInteger userId = new AtomicInteger(1);
    private final AtomicInteger ejercicioId = new AtomicInteger(1);
    private final AtomicInteger rutinaId = new AtomicInteger(1);

    private final List<Usuario> usuarios = new ArrayList<>();
    private final List<EjercicioBase> ejercicios = new ArrayList<>();

    // rutinas por usuarioId (en memoria), pero también se cargan/guardan por serialización
    private final Map<Integer, List<Rutina>> rutinasPorUsuario = new HashMap<>();

    private final PersistenceService persistence = new PersistenceService();

    public Store() {
        seed();
    }

    private void seed() {
        usuarios.add(new Usuario(userId.getAndIncrement(), "admin", "admin123", "Administrador", RolUsuario.ADMIN));
        usuarios.add(new Usuario(userId.getAndIncrement(), "cliente", "1234", "Cliente Fidness", RolUsuario.CLIENTE));

        // Fuerza
        ejercicios.add(new EjercicioFuerza(ejercicioId.getAndIncrement(), "Sentadillas", TipoEjercicio.PIERNA,
                "Ejercicio base para piernas y glúteos.",
                List.of("Pies al ancho de hombros.",
                        "Baja controlado manteniendo espalda recta.",
                        "Sube empujando con los talones."),
                12));

        ejercicios.add(new EjercicioFuerza(ejercicioId.getAndIncrement(), "Remo con mancuerna", TipoEjercicio.ESPALDA,
                "Fortalece dorsales y espalda media.",
                List.of("Inclina el torso con espalda neutra.",
                        "Lleva la mancuerna hacia la cadera.",
                        "Baja controlado."),
                10));

        // Cardio
        ejercicios.add(new EjercicioCardio(ejercicioId.getAndIncrement(), "Caminata rápida", TipoEjercicio.CARDIO,
                "Cardio suave para resistencia.",
                List.of("Ritmo sostenido.",
                        "Postura erguida.",
                        "Hidratación adecuada."),
                20));
    }

    public Usuario login(String username, String passwordPlain) throws AuthException {
        if (username == null || username.trim().isEmpty()) throw new AuthException("Usuario requerido.");
        if (passwordPlain == null || passwordPlain.trim().isEmpty()) throw new AuthException("Contraseña requerida.");

        String u = username.trim().toLowerCase();
        Usuario user = usuarios.stream()
                .filter(x -> x.validarCredenciales(u, passwordPlain))
                .findFirst()
                .orElse(null);

        if (user == null) throw new AuthException("Credenciales inválidas.");

        // Cargar rutinas del usuario (serialización opcional)
        List<Rutina> cargadas = persistence.cargarRutinas(user.getUsername());
        rutinasPorUsuario.put(user.getId(), new ArrayList<>(cargadas));

        return user;
    }

    public List<EjercicioBase> listarEjercicios(Optional<TipoEjercicio> filtroTipo, String buscarPorNombre) {
        String q = buscarPorNombre == null ? "" : buscarPorNombre.trim().toLowerCase();
        return ejercicios.stream()
                .filter(e -> filtroTipo.map(t -> e.getTipo() == t).orElse(true))
                .filter(e -> q.isEmpty() || e.getNombre().toLowerCase().contains(q))
                .sorted(Comparator.comparing(EjercicioBase::getTipo).thenComparing(EjercicioBase::getNombre))
                .collect(Collectors.toList());
    }

    public EjercicioBase registrarEjercicio(
            String nombre,
            TipoEjercicio tipo,
            String descripcion,
            List<String> pasos,
            Integer repsFuerza,
            Integer duracionCardio
    ) throws ValidationException {

        if (nombre == null || nombre.trim().isEmpty()) throw new ValidationException("Nombre requerido.");
        if (tipo == null) throw new ValidationException("Tipo requerido.");
        if (descripcion == null || descripcion.trim().isEmpty()) throw new ValidationException("Descripción requerida.");
        if (pasos == null || pasos.isEmpty()) throw new ValidationException("Debe ingresar al menos 1 paso.");

        int id = ejercicioId.getAndIncrement();

        EjercicioBase nuevo;
        if (tipo == TipoEjercicio.CARDIO) {
            if (duracionCardio == null || duracionCardio <= 0) throw new ValidationException("Duración cardio debe ser > 0.");
            nuevo = new EjercicioCardio(id, nombre, tipo, descripcion, pasos, duracionCardio);
        } else {
            if (repsFuerza == null || repsFuerza <= 0) throw new ValidationException("Repeticiones deben ser > 0.");
            nuevo = new EjercicioFuerza(id, nombre, tipo, descripcion, pasos, repsFuerza);
        }

        ejercicios.add(nuevo);
        return nuevo;
    }

    public Rutina crearRutina(int usuarioId, String nombreRutina, List<EjercicioBase> seleccionados, String usernameForSave)
            throws ValidationException {

        if (nombreRutina == null || nombreRutina.trim().isEmpty()) throw new ValidationException("Nombre de rutina requerido.");
        if (seleccionados == null || seleccionados.isEmpty()) throw new ValidationException("Debe seleccionar al menos 1 ejercicio.");

        Rutina r = new Rutina(rutinaId.getAndIncrement(), usuarioId, nombreRutina.trim());
        r.setEjercicios(seleccionados);

        rutinasPorUsuario.putIfAbsent(usuarioId, new ArrayList<>());
        rutinasPorUsuario.get(usuarioId).add(r);

        // Guardar rutinas por serialización (opcional)
        try {
            persistence.guardarRutinas(usernameForSave, rutinasPorUsuario.get(usuarioId));
        } catch (Exception ignored) {}

        return r;
    }

    public List<Rutina> listarRutinas(int usuarioId) {
        return new ArrayList<>(rutinasPorUsuario.getOrDefault(usuarioId, new ArrayList<>()));
    }
}