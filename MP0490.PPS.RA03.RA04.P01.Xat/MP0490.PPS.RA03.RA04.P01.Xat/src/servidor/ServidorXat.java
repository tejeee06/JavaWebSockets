package servidor;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * Servidor de chat amb interfície Swing.
 * Mostra un log de l'activitat i permet iniciar o aturar el servei.
 */
public class ServidorXat extends JFrame {

    static final int PORT = 12345;
    static final List<ServidorGestorDelsClients> clientsConnectats = new CopyOnWriteArrayList<>();

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMAT_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static ServidorXat instancia;

    private final JTextArea areaLog = new JTextArea();
    private final JLabel etiquetaEstat = new JLabel("Servidor aturat");
    private final JLabel etiquetaClients = new JLabel("Clients connectats: 0");
    private final JButton botoIniciar = new JButton("Iniciar servidor");
    private final JButton botoAturar = new JButton("Aturar servidor");

    private volatile boolean servidorActiu;
    private ServerSocket serverSocket;
    private Thread filAcceptacio;

    public ServidorXat() {
        super("Servidor Xat");
        instancia = this;
        configurarFinestra();
        configurarEsdeveniments();
    }

    private void configurarFinestra() {
        areaLog.setEditable(false);
        areaLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        JScrollPane scrollLog = new JScrollPane(areaLog);
        scrollLog.setBorder(BorderFactory.createTitledBorder("Registre del servidor"));

        JPanel panellEstat = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panellEstat.add(etiquetaEstat);
        panellEstat.add(new JLabel(" | "));
        panellEstat.add(etiquetaClients);

        JPanel panellBotons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botoAturar.setEnabled(false);
        panellBotons.add(botoIniciar);
        panellBotons.add(botoAturar);

        setLayout(new BorderLayout(10, 10));
        add(panellEstat, BorderLayout.NORTH);
        add(scrollLog, BorderLayout.CENTER);
        add(panellBotons, BorderLayout.SOUTH);

        setSize(760, 480);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    }

    private void configurarEsdeveniments() {
        botoIniciar.addActionListener(e -> iniciarServidor());
        botoAturar.addActionListener(e -> aturarServidor());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                tancarAplicacio();
            }
        });
    }

    private void iniciarServidor() {
        if (servidorActiu) {
            registrarEsdeveniment("El servidor ja està en marxa.");
            return;
        }

        botoIniciar.setEnabled(false);
        registrarEsdeveniment("Intentant iniciar el servidor al port " + PORT + "...");

        filAcceptacio = new Thread(() -> {
            boolean iniciatCorrectament = false;
            try {
                serverSocket = new ServerSocket(PORT);
                servidorActiu = true;
                iniciatCorrectament = true;

                actualitzarEstat("Servidor escoltant al port " + PORT, true);
                registrarEsdeveniment("Servidor iniciat correctament.");
                registrarEsdeveniment("Esperant connexions de clients...");

                while (servidorActiu) {
                    Socket socketClient = serverSocket.accept();
                    String ipClient = socketClient.getInetAddress().getHostAddress();
                    registrarEsdeveniment("Nou client connectat des de " + ipClient + ".");

                    try {
                        ServidorGestorDelsClients gestor = new ServidorGestorDelsClients(socketClient);
                        afegirClient(gestor);
                        Thread filClient = new Thread(gestor, "Client-" + socketClient.getPort());
                        filClient.start();
                    } catch (IOException e) {
                        registrarEsdeveniment("No s'ha pogut preparar el client: " + e.getMessage());
                        socketClient.close();
                    }
                }
            } catch (IOException e) {
                if (servidorActiu || !iniciatCorrectament) {
                    registrarEsdeveniment("Error iniciant o acceptant connexions: " + e.getMessage());
                }
            } finally {
                servidorActiu = false;
                tancarSocketServidor();
                desconnectarTotsElsClients();
                actualitzarEstat("Servidor aturat", false);
                registrarEsdeveniment("Servidor aturat.");
            }
        }, "Acceptacio-Clients");

        filAcceptacio.start();
    }

    private void aturarServidor() {
        if (!servidorActiu && serverSocket == null) {
            registrarEsdeveniment("El servidor ja està aturat.");
            return;
        }

        registrarEsdeveniment("Aturant servidor...");
        servidorActiu = false;
        tancarSocketServidor();
        desconnectarTotsElsClients();
    }

    private void tancarSocketServidor() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                registrarEsdeveniment("Error tancant el ServerSocket: " + e.getMessage());
            }
        }
        serverSocket = null;
    }

    private void desconnectarTotsElsClients() {
        for (ServidorGestorDelsClients client : clientsConnectats) {
            client.tancarDesDeServidor();
        }
    }

    private void tancarAplicacio() {
        aturarServidor();
        dispose();
    }

    private void afegirLog(String missatge) {
        SwingUtilities.invokeLater(() -> {
            String hora = LocalTime.now().format(FORMAT_HORA);
            areaLog.append("[" + hora + "] " + missatge + System.lineSeparator());
            areaLog.setCaretPosition(areaLog.getDocument().getLength());
        });
    }

    private void actualitzarEstat(String textEstat, boolean actiu) {
        SwingUtilities.invokeLater(() -> {
            etiquetaEstat.setText(textEstat);
            botoIniciar.setEnabled(!actiu);
            botoAturar.setEnabled(actiu);
            actualitzarComptadorClients();
        });
    }

    private void actualitzarComptadorClients() {
        SwingUtilities.invokeLater(() -> {
            etiquetaClients.setText("Clients connectats: " + clientsConnectats.size());
        });
    }

    static void registrarEsdeveniment(String missatge) {
        if (instancia != null) {
            instancia.afegirLog(missatge);
        }
    }

    static void afegirClient(ServidorGestorDelsClients gestor) {
        clientsConnectats.add(gestor);
        if (instancia != null) {
            instancia.actualitzarComptadorClients();
        }
    }

    static void broadcast(String missatge, ServidorGestorDelsClients origen) {
        for (ServidorGestorDelsClients client : clientsConnectats) {
            if (client != origen) {
                client.enviarMissatge(missatge);
            }
        }
    }

    static void eliminarClient(ServidorGestorDelsClients gestor) {
        clientsConnectats.remove(gestor);
        if (instancia != null) {
            instancia.actualitzarComptadorClients();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServidorXat finestra = new ServidorXat();
            finestra.setVisible(true);
            finestra.iniciarServidor();
        });
    }
}
