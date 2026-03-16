package fidness;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

public class ExportService {

    public void exportarRutinaTxt(Rutina rutina, File destino) throws Exception {
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(destino), StandardCharsets.UTF_8))) {

            bw.write("FIDNESS - RUTINA");
            bw.newLine();
            bw.write("Nombre: " + rutina.getNombre());
            bw.newLine();
            bw.write("Fecha: " + rutina.getFechaCreacion().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            bw.newLine();
            bw.newLine();

            for (RutinaItem item : rutina.getItems()) {
                EjercicioBase e = item.getEjercicio();
                bw.write(item.getOrden() + ". " + e.getNombre() + " (" + e.getTipo() + ")");
                bw.newLine();
                bw.write("   " + e.detalleExtra()); // POLIMORFISMO (sobrescrito según el tipo)
                bw.newLine();
                bw.write("   Descripción: " + e.getDescripcion());
                bw.newLine();
                bw.write("   Pasos:");
                bw.newLine();
                int i = 1;
                for (String paso : e.getPasos()) {
                    bw.write("     " + (i++) + ") " + paso);
                    bw.newLine();
                }
                bw.newLine();
            }
        }
    }
}