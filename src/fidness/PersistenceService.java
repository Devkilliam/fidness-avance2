package fidness;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class PersistenceService {

    private final File dataDir = new File("fidness_data");

    public PersistenceService() {
        if (!dataDir.exists()) dataDir.mkdirs();
    }

    private File fileForUser(String username) {
        String safe = username.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        return new File(dataDir, "rutinas_" + safe + ".ser");
    }

    public List<Rutina> cargarRutinas(String username) {
        File f = fileForUser(username);
        if (!f.exists()) return new ArrayList<>();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            Object obj = ois.readObject();
            if (obj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Rutina> list = (List<Rutina>) obj;
                return list != null ? list : new ArrayList<>();
            }
            return new ArrayList<>();
        } catch (Exception e) {
            // Si el archivo se dañó, devolvemos vacío para no romper el prototipo
            return new ArrayList<>();
        }
    }

    public void guardarRutinas(String username, List<Rutina> rutinas) throws IOException {
        File f = fileForUser(username);
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f))) {
            oos.writeObject(rutinas);
        }
    }
}