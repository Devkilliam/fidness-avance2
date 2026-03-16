package fidness;

import fidness.Exceptions.AuthException;
import fidness.Exceptions.ValidationException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AppFrame extends JFrame {

    private final Store store = new Store();
    private final ExportService exportService = new ExportService();

    private Usuario sessionUser;

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    private final LoginPanel loginPanel = new LoginPanel();
    private final MainPanel mainPanel = new MainPanel();

    public AppFrame() {
        super("Fidness (Avance 2 - Simple)");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(980, 640);
        setLocationRelativeTo(null);

        root.add(loginPanel, "LOGIN");
        root.add(mainPanel, "MAIN");
        setContentPane(root);

        showLogin();
    }

    private void showLogin() {
        sessionUser = null;
        loginPanel.reset();
        cards.show(root, "LOGIN");
    }

    private void showMain() {
        mainPanel.refreshAll();
        cards.show(root, "MAIN");
    }

    // ===================== UI: LOGIN =====================
    private class LoginPanel extends JPanel {
        private final JTextField txtUser = new JTextField(18);
        private final JPasswordField txtPass = new JPasswordField(18);
        private final JLabel lblMsg = new JLabel(" ");
        private final JButton btn = new JButton("Ingresar");

        LoginPanel() {
            setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(8,8,8,8);
            c.anchor = GridBagConstraints.WEST;

            JLabel title = new JLabel("FIDNESS - Login");
            title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));

            c.gridx=0; c.gridy=0; c.gridwidth=2;
            add(title, c);

            c.gridwidth=1;
            c.gridx=0; c.gridy=1; add(new JLabel("Usuario:"), c);
            c.gridx=1; add(txtUser, c);

            c.gridx=0; c.gridy=2; add(new JLabel("Contraseña:"), c);
            c.gridx=1; add(txtPass, c);

            c.gridx=1; c.gridy=3; c.anchor=GridBagConstraints.EAST;
            add(btn, c);

            c.gridx=0; c.gridy=4; c.gridwidth=2; c.anchor=GridBagConstraints.CENTER;
            lblMsg.setForeground(new Color(160,0,0));
            add(lblMsg, c);

            // demo
            txtUser.setText("cliente");
            txtPass.setText("1234");

            btn.addActionListener(e -> doLogin());
            txtPass.addActionListener(e -> doLogin());
        }

        void reset() {
            lblMsg.setText(" ");
            txtPass.setText("");
        }

        private void doLogin() {
            btn.setEnabled(false);
            try {
                String u = txtUser.getText();
                String p = new String(txtPass.getPassword());

                sessionUser = store.login(u, p); // lanza AuthException
                lblMsg.setText(" ");
                showMain();

            } catch (AuthException ex) {
                lblMsg.setText(ex.getMessage());
            } finally {
                btn.setEnabled(true);
            }
        }
    }

    // ===================== UI: MAIN =====================
    private class MainPanel extends JPanel {

        private final JLabel lblSesion = new JLabel();
        private final JButton btnLogout = new JButton("Cerrar sesión");
        private final JTabbedPane tabs = new JTabbedPane();

        // ----- tab ejercicios
        private final JComboBox<Object> cboTipo = new JComboBox<>();
        private final JTextField txtBuscar = new JTextField(16);
        private final JButton btnBuscar = new JButton("Buscar");
        private final JButton btnRegistrar = new JButton("Registrar ejercicio");

        private final DefaultTableModel exModel = new DefaultTableModel(new Object[]{"ID","Nombre","Tipo"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        private final JTable exTable = new JTable(exModel);

        private final JTextArea exDetail = new JTextArea();
        private final JButton btnCrearRutina = new JButton("Crear rutina con seleccionados");

        // ----- tab rutinas
        private final DefaultListModel<Rutina> rutModel = new DefaultListModel<>();
        private final JList<Rutina> rutList = new JList<>(rutModel);
        private final JTextArea rutDetail = new JTextArea();
        private final JButton btnExportar = new JButton("Exportar rutina (TXT)");

        MainPanel() {
            setLayout(new BorderLayout(10,10));
            setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

            JPanel top = new JPanel(new BorderLayout());
            lblSesion.setFont(lblSesion.getFont().deriveFont(Font.BOLD, 14f));
            top.add(lblSesion, BorderLayout.WEST);
            top.add(btnLogout, BorderLayout.EAST);
            add(top, BorderLayout.NORTH);

            tabs.add("Ejercicios", buildTabEjercicios());
            tabs.add("Mis rutinas", buildTabRutinas());
            add(tabs, BorderLayout.CENTER);

            btnLogout.addActionListener(e -> showLogin());
        }

        JPanel buildTabEjercicios() {
            JPanel panel = new JPanel(new BorderLayout(10,10));

            JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
            cboTipo.addItem("Todos");
            for (TipoEjercicio t : TipoEjercicio.values()) cboTipo.addItem(t);

            filters.add(new JLabel("Tipo:"));
            filters.add(cboTipo);
            filters.add(new JLabel("Buscar:"));
            filters.add(txtBuscar);
            filters.add(btnBuscar);
            filters.add(btnRegistrar);

            panel.add(filters, BorderLayout.NORTH);

            exTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            JScrollPane left = new JScrollPane(exTable);

            exDetail.setEditable(false);
            exDetail.setLineWrap(true);
            exDetail.setWrapStyleWord(true);
            JScrollPane right = new JScrollPane(exDetail);

            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
            split.setResizeWeight(0.55);
            panel.add(split, BorderLayout.CENTER);

            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            bottom.add(btnCrearRutina);
            panel.add(bottom, BorderLayout.SOUTH);

            btnBuscar.addActionListener(e -> refreshEjercicios());
            txtBuscar.addActionListener(e -> refreshEjercicios());
            cboTipo.addActionListener(e -> refreshEjercicios());

            exTable.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) showSelectedExerciseDetail();
            });

            btnCrearRutina.addActionListener(e -> crearRutinaDesdeSeleccion());
            btnRegistrar.addActionListener(e -> registrarEjercicio());

            return panel;
        }

        JPanel buildTabRutinas() {
            JPanel panel = new JPanel(new BorderLayout(10,10));

            rutList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane left = new JScrollPane(rutList);

            rutDetail.setEditable(false);
            rutDetail.setLineWrap(true);
            rutDetail.setWrapStyleWord(true);
            JScrollPane right = new JScrollPane(rutDetail);

            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
            split.setResizeWeight(0.35);
            panel.add(split, BorderLayout.CENTER);

            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            bottom.add(btnExportar);
            panel.add(bottom, BorderLayout.SOUTH);

            rutList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) showSelectedRoutineDetail();
            });

            btnExportar.addActionListener(e -> exportarRutinaSeleccionada());

            return panel;
        }

        void refreshAll() {
            lblSesion.setText("Sesión: " + sessionUser.getNombreCompleto() + " — " + sessionUser.getRol());
            btnRegistrar.setVisible(sessionUser.esAdmin());
            refreshEjercicios();
            refreshRutinas();
        }

        void refreshEjercicios() {
            exModel.setRowCount(0);
            Optional<TipoEjercicio> filtro = Optional.empty();
            Object sel = cboTipo.getSelectedItem();
            if (sel instanceof TipoEjercicio) filtro = Optional.of((TipoEjercicio) sel);

            List<EjercicioBase> list = store.listarEjercicios(filtro, txtBuscar.getText());
            for (EjercicioBase e : list) {
                exModel.addRow(new Object[]{e.getId(), e.getNombre(), e.getTipo()});
            }
            exDetail.setText("");
        }

        void refreshRutinas() {
            rutModel.clear();
            List<Rutina> list = store.listarRutinas(sessionUser.getId());
            for (Rutina r : list) rutModel.addElement(r);
            rutDetail.setText("");
        }

        void showSelectedExerciseDetail() {
            int row = exTable.getSelectedRow();
            if (row < 0) { exDetail.setText(""); return; }

            int id = (int) exModel.getValueAt(row, 0);

            // Buscamos por listado actual (simple)
            Optional<TipoEjercicio> filtro = Optional.empty();
            Object sel = cboTipo.getSelectedItem();
            if (sel instanceof TipoEjercicio) filtro = Optional.of((TipoEjercicio) sel);

            EjercicioBase e = store.listarEjercicios(filtro, txtBuscar.getText())
                    .stream().filter(x -> x.getId() == id).findFirst().orElse(null);

            if (e == null) { exDetail.setText(""); return; }

            StringBuilder sb = new StringBuilder();
            sb.append(e.getNombre()).append(" (").append(e.getTipo()).append(")\n");
            sb.append(e.detalleExtra()).append("\n\n"); // POLIMORFISMO
            sb.append("Descripción:\n").append(e.getDescripcion()).append("\n\n");
            sb.append("Cómo se ejecuta (pasos):\n");
            int i = 1;
            for (String paso : e.getPasos()) sb.append(i++).append(") ").append(paso).append("\n");

            exDetail.setText(sb.toString());
            exDetail.setCaretPosition(0);
        }

        void crearRutinaDesdeSeleccion() {
            int[] rows = exTable.getSelectedRows();
            if (rows == null || rows.length == 0) {
                JOptionPane.showMessageDialog(this, "Seleccione uno o más ejercicios.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String nombre = JOptionPane.showInputDialog(this, "Nombre de la rutina:", "Crear rutina", JOptionPane.QUESTION_MESSAGE);
            if (nombre == null) return;

            try {
                List<EjercicioBase> seleccionados = new ArrayList<>();
                List<EjercicioBase> actuales = store.listarEjercicios(Optional.empty(), txtBuscar.getText());

                for (int r : rows) {
                    int id = (int) exModel.getValueAt(r, 0);
                    for (EjercicioBase e : store.listarEjercicios(Optional.empty(), "")) {
                        if (e.getId() == id) { seleccionados.add(e); break; }
                    }
                }

                store.crearRutina(sessionUser.getId(), nombre, seleccionados, sessionUser.getUsername());
                JOptionPane.showMessageDialog(this, "Rutina guardada.", "OK", JOptionPane.INFORMATION_MESSAGE);
                refreshRutinas();
                tabs.setSelectedIndex(1);

            } catch (ValidationException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        void showSelectedRoutineDetail() {
            Rutina r = rutList.getSelectedValue();
            if (r == null) { rutDetail.setText(""); return; }

            StringBuilder sb = new StringBuilder();
            sb.append("Rutina: ").append(r.getNombre()).append("\n");
            sb.append("Ejercicios: ").append(r.cantidadEjercicios()).append("\n\n");

            for (RutinaItem item : r.getItems()) {
                EjercicioBase e = item.getEjercicio();
                sb.append(item.getOrden()).append(". ").append(e.getNombre()).append(" (").append(e.getTipo()).append(") - ")
                        .append(e.detalleExtra()).append("\n");
            }

            rutDetail.setText(sb.toString());
            rutDetail.setCaretPosition(0);
        }

        void exportarRutinaSeleccionada() {
            Rutina r = rutList.getSelectedValue();
            if (r == null) {
                JOptionPane.showMessageDialog(this, "Seleccione una rutina.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Guardar rutina como TXT");
            fc.setSelectedFile(new File("rutina_" + r.getNombre().replaceAll("\\s+", "_") + ".txt"));
            int ok = fc.showSaveDialog(this);
            if (ok != JFileChooser.APPROVE_OPTION) return;

            File file = fc.getSelectedFile();
            btnExportar.setEnabled(false);

            // MULTIHILO (opcional): exportar en thread
            new Thread(() -> {
                try {
                    exportService.exportarRutinaTxt(r, file);
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(this, "Exportada a:\n" + file.getAbsolutePath(), "OK", JOptionPane.INFORMATION_MESSAGE)
                    );
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(this, "No se pudo exportar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
                    );
                } finally {
                    SwingUtilities.invokeLater(() -> btnExportar.setEnabled(true));
                }
            }).start();
        }

        void registrarEjercicio() {
            if (!sessionUser.esAdmin()) return;

            JTextField nombre = new JTextField();
            JComboBox<TipoEjercicio> tipo = new JComboBox<>(TipoEjercicio.values());
            JTextArea desc = new JTextArea(3, 22);
            JTextArea pasos = new JTextArea(6, 22);
            JTextField reps = new JTextField("12");       // para fuerza
            JTextField dur = new JTextField("20");        // para cardio

            pasos.setText("Paso 1\nPaso 2\nPaso 3");

            JPanel p = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(6,6,6,6);
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;

            c.gridx=0; c.gridy=0; p.add(new JLabel("Nombre:"), c);
            c.gridx=1; p.add(nombre, c);

            c.gridx=0; c.gridy=1; p.add(new JLabel("Tipo:"), c);
            c.gridx=1; p.add(tipo, c);

            c.gridx=0; c.gridy=2; p.add(new JLabel("Descripción:"), c);
            c.gridx=1; p.add(new JScrollPane(desc), c);

            c.gridx=0; c.gridy=3; p.add(new JLabel("Pasos (1 por línea):"), c);
            c.gridx=1; p.add(new JScrollPane(pasos), c);

            c.gridx=0; c.gridy=4; p.add(new JLabel("Reps (fuerza):"), c);
            c.gridx=1; p.add(reps, c);

            c.gridx=0; c.gridy=5; p.add(new JLabel("Duración min (cardio):"), c);
            c.gridx=1; p.add(dur, c);

            int res = JOptionPane.showConfirmDialog(this, p, "Registrar ejercicio", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res != JOptionPane.OK_OPTION) return;

            try {
                String n = nombre.getText();
                TipoEjercicio t = (TipoEjercicio) tipo.getSelectedItem();
                String d = desc.getText();

                List<String> listaPasos = new ArrayList<>();
                for (String line : (pasos.getText() == null ? "" : pasos.getText()).split("\\R")) {
                    String s = line.trim();
                    if (!s.isEmpty()) listaPasos.add(s);
                }

                Integer repsInt = null;
                Integer durInt = null;
                try { repsInt = Integer.parseInt(reps.getText().trim()); } catch (Exception ignored) {}
                try { durInt = Integer.parseInt(dur.getText().trim()); } catch (Exception ignored) {}

                store.registrarEjercicio(n, t, d, listaPasos, repsInt, durInt);
                JOptionPane.showMessageDialog(this, "Ejercicio registrado.", "OK", JOptionPane.INFORMATION_MESSAGE);
                refreshEjercicios();

            } catch (ValidationException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}