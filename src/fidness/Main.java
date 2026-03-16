//cliente / 1234
//admin / admin123 (solo para habilitar “Registrar ejercicio”,alcance incluye ingreso de ejercicios)
package fidness;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AppFrame().setVisible(true));
    }
}