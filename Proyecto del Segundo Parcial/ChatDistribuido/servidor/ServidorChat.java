package servidor;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ServidorChat extends JFrame {

    private JTextArea areaLog;
    private DefaultListModel<String> modeloClientes;
    private ArrayList<PrintWriter> salidasClientes;
    private int puerto = 5000;

    public ServidorChat() {
        setTitle("Servidor de Chat Distribuido");
        setSize(750, 420);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        salidasClientes = new ArrayList<>();

        inicializarInterfaz();
        iniciarServidor();
    }

    private void inicializarInterfaz() {
        setLayout(new BorderLayout());

        // Barra superior
        JPanel panelSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelSuperior.setBackground(new Color(245, 247, 250));

        JLabel lblEstado = new JLabel("Estado: ACTIVO");
        JLabel lblPuerto = new JLabel("Puerto: " + puerto);

        panelSuperior.add(lblEstado);
        panelSuperior.add(Box.createHorizontalStrut(20));
        panelSuperior.add(lblPuerto);

        // Lista de clientes
        modeloClientes = new DefaultListModel<>();
        JList<String> listaClientes = new JList<>(modeloClientes);
        JScrollPane scrollClientes = new JScrollPane(listaClientes);
        scrollClientes.setPreferredSize(new Dimension(220, 0));
        scrollClientes.setBorder(
                BorderFactory.createTitledBorder("Clientes conectados")
        );

        // Área de log
        areaLog = new JTextArea();
        areaLog.setEditable(false);
        areaLog.setLineWrap(true);
        areaLog.setWrapStyleWord(true);
        JScrollPane scrollLog = new JScrollPane(areaLog);
        scrollLog.setBorder(
                BorderFactory.createTitledBorder("Registro del servidor")
        );

        add(panelSuperior, BorderLayout.NORTH);
        add(scrollClientes, BorderLayout.WEST);
        add(scrollLog, BorderLayout.CENTER);
    }

    private void iniciarServidor() {
        new Thread(() -> {
            try (ServerSocket servidor = new ServerSocket(puerto)) {
                log("Servidor iniciado en el puerto " + puerto);
                log("Esperando conexiones...");

                while (true) {
                    Socket socketCliente = servidor.accept();
                    new Thread(new ManejadorCliente(socketCliente)).start();
                }

            } catch (IOException e) {
                log("Error del servidor: " + e.getMessage());
            }
        }).start();
    }

    private void enviarListaClientes() {
        StringBuilder lista = new StringBuilder("USUARIOS:");
        for (int i = 0; i < modeloClientes.size(); i++) {
            lista.append(modeloClientes.get(i));
            if (i < modeloClientes.size() - 1) {
                lista.append(",");
            }
        }

        for (PrintWriter salida : salidasClientes) {
            salida.println(lista.toString());
        }
    }

    private void log(String mensaje) {
        SwingUtilities.invokeLater(() ->
                areaLog.append(mensaje + "\n")
        );
    }

    private class ManejadorCliente implements Runnable {

        private Socket socket;
        private PrintWriter salida;
        private BufferedReader entrada;
        private String nombreCliente;

        public ManejadorCliente(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                entrada = new BufferedReader(
                        new InputStreamReader(socket.getInputStream())
                );
                salida = new PrintWriter(
                        socket.getOutputStream(), true
                );

                // Leer nombre del cliente
                nombreCliente = entrada.readLine();
                salidasClientes.add(salida);

                SwingUtilities.invokeLater(() ->
                        modeloClientes.addElement(nombreCliente)
                );
                enviarListaClientes();

                log("Cliente conectado: " + nombreCliente);

                // Leer mensajes
                String mensaje;
                while ((mensaje = entrada.readLine()) != null) {
                    String mensajeFinal = nombreCliente + ": " + mensaje;
                    log(mensajeFinal);

                    for (PrintWriter cliente : salidasClientes) {
                        cliente.println(mensajeFinal);
                    }
                }

            } catch (IOException e) {
                log("Cliente desconectado: " + nombreCliente);
            } finally {
                cerrarConexion();
            }
        }

        private void cerrarConexion() {
            try {
                if (nombreCliente != null) {
                    SwingUtilities.invokeLater(() ->
                            modeloClientes.removeElement(nombreCliente)
                    );
                    enviarListaClientes();
                }

                if (salida != null) salidasClientes.remove(salida);
                if (socket != null) socket.close();

            } catch (IOException e) {
                log("Error al cerrar conexión");
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
                new ServidorChat().setVisible(true)
        );
    }
}
