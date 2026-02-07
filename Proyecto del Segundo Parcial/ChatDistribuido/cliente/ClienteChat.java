package cliente;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class ClienteChat extends JFrame {

    private JTextArea areaMensajes;
    private JTextField campoMensaje;
    private JButton botonEnviar;
    private JList<String> listaUsuarios;
    private DefaultListModel<String> modeloUsuarios;

    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;
    private String nombreUsuario;

    public ClienteChat() {
        solicitarNombre();
        configurarVentana();
        inicializarInterfaz();
        conectarServidor();
    }

    private void solicitarNombre() {
        nombreUsuario = JOptionPane.showInputDialog(
                null,
                "Ingrese su nombre:",
                "Identificación de Usuario",
                JOptionPane.QUESTION_MESSAGE
        );

        if (nombreUsuario == null || nombreUsuario.trim().isEmpty()) {
            nombreUsuario = "Usuario";
        }
    }

    private void configurarVentana() {
        setTitle("Cliente de Chat - " + nombreUsuario);
        setSize(720, 420);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void inicializarInterfaz() {
        Color fondo = new Color(245, 247, 250);
        Color azul = new Color(52, 152, 219);

        setLayout(new BorderLayout());
        getContentPane().setBackground(fondo);

        // Usuarios
        modeloUsuarios = new DefaultListModel<>();
        listaUsuarios = new JList<>(modeloUsuarios);
        listaUsuarios.setBackground(new Color(230, 235, 240));
        JScrollPane scrollUsuarios = new JScrollPane(listaUsuarios);
        scrollUsuarios.setBorder(
                BorderFactory.createTitledBorder("Usuarios conectados")
        );
        scrollUsuarios.setPreferredSize(new Dimension(200, 0));

        // Mensajes
        areaMensajes = new JTextArea();
        areaMensajes.setEditable(false);
        areaMensajes.setLineWrap(true);
        areaMensajes.setWrapStyleWord(true);
        areaMensajes.setBackground(Color.WHITE);
        JScrollPane scrollMensajes = new JScrollPane(areaMensajes);
        scrollMensajes.setBorder(
                BorderFactory.createTitledBorder("Mensajes")
        );

        // Panel inferior
        JPanel panelInferior = new JPanel(new BorderLayout(5, 5));
        panelInferior.setBackground(fondo);

        campoMensaje = new JTextField();
        botonEnviar = new JButton("Enviar");
        botonEnviar.setBackground(azul);
        botonEnviar.setForeground(Color.WHITE);
        botonEnviar.setFocusPainted(false);

        panelInferior.add(new JLabel("Mensaje:"), BorderLayout.WEST);
        panelInferior.add(campoMensaje, BorderLayout.CENTER);
        panelInferior.add(botonEnviar, BorderLayout.EAST);

        add(scrollUsuarios, BorderLayout.WEST);
        add(scrollMensajes, BorderLayout.CENTER);
        add(panelInferior, BorderLayout.SOUTH);

        botonEnviar.addActionListener(e -> enviarMensaje());
        campoMensaje.addActionListener(e -> enviarMensaje());
    }

    private void conectarServidor() {
        try {
            socket = new Socket("localhost", 5000);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida = new PrintWriter(socket.getOutputStream(), true);

            // Enviar nombre al servidor
            salida.println(nombreUsuario);

            new Thread(this::escucharMensajes).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo conectar al servidor",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void enviarMensaje() {
        String mensaje = campoMensaje.getText().trim();
        if (!mensaje.isEmpty()) {
            salida.println(mensaje);
            campoMensaje.setText("");
        }
    }

    private void escucharMensajes() {
        try {
            String linea;
            while ((linea = entrada.readLine()) != null) {
                if (linea.startsWith("USUARIOS:")) {
                    actualizarUsuarios(linea.substring(9));
                } else {
                    areaMensajes.append(linea + "\n");
                }
            }
        } catch (IOException e) {
            areaMensajes.append("Conexión cerrada.\n");
        }
    }

    private void actualizarUsuarios(String datos) {
        SwingUtilities.invokeLater(() -> {
            modeloUsuarios.clear();
            for (String usuario : datos.split(",")) {
                modeloUsuarios.addElement(usuario);
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
                new ClienteChat().setVisible(true)
        );
    }
}
