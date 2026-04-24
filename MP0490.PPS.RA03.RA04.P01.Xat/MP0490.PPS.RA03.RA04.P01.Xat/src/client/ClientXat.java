package client;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Client de xat amb Swing.
 * Cada execució obre una finestra independent que actua com un client.
 */
public class ClientXat extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final String ADRECA_SERVIDOR = "localhost";
    private static final int PORT = 12345;

    private final JTextField campHost = new JTextField(ADRECA_SERVIDOR, 12);
    private final JTextField campPort = new JTextField(String.valueOf(PORT), 6);
    private final JTextField campNom = new JTextField(12);
    private final JButton botoConnectar = new JButton("Connectar");
    private final JButton botoDesconnectar = new JButton("Desconnectar");

    private final JTextArea areaChat = new JTextArea();
    private final JTextField campMissatge = new JTextField();
    private final JButton botoEnviar = new JButton("Enviar");

    private Socket socket;
    private BufferedReader entradaServidor;
    private PrintWriter sortidaServidor;
    private volatile boolean connectat;

    public ClientXat() {
        super("Client Xat");
        configurarFinestra();
        configurarEsdeveniments();
    }

    private void configurarFinestra() {
        JPanel panellConnexio = new JPanel(new GridLayout(2, 3, 8, 8));
        panellConnexio.setBorder(BorderFactory.createTitledBorder("Connexió"));
        panellConnexio.add(new JLabel("Servidor"));
        panellConnexio.add(new JLabel("Port"));
        panellConnexio.add(new JLabel("Nom d'usuari"));
        panellConnexio.add(campHost);
        panellConnexio.add(campPort);
        panellConnexio.add(campNom);

        JPanel panellBotons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botoDesconnectar.setEnabled(false);
        panellBotons.add(botoConnectar);
        panellBotons.add(botoDesconnectar);

        areaChat.setEditable(false);
        areaChat.setLineWrap(true);
        areaChat.setWrapStyleWord(true);
        areaChat.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

        JScrollPane scrollChat = new JScrollPane(areaChat);
        scrollChat.setBorder(BorderFactory.createTitledBorder("Conversa"));

        JPanel panellMissatge = new JPanel(new BorderLayout(8, 8));
        panellMissatge.setBorder(BorderFactory.createTitledBorder("Nou missatge"));
        campMissatge.setEnabled(false);
        botoEnviar.setEnabled(false);
        panellMissatge.add(campMissatge, BorderLayout.CENTER);
        panellMissatge.add(botoEnviar, BorderLayout.EAST);

        JPanel capcalera = new JPanel(new BorderLayout(8, 8));
        capcalera.add(panellConnexio, BorderLayout.CENTER);
        capcalera.add(panellBotons, BorderLayout.SOUTH);

        setLayout(new BorderLayout(10, 10));
        add(capcalera, BorderLayout.NORTH);
        add(scrollChat, BorderLayout.CENTER);
        add(panellMissatge, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(botoEnviar);
        setSize(700, 500);
        setLocationByPlatform(true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    }

    private void configurarEsdeveniments() {
        botoConnectar.addActionListener(e -> connectar());
        botoDesconnectar.addActionListener(e -> desconnectar(true));
        botoEnviar.addActionListener(e -> enviarMissatge());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                desconnectar(false);
                dispose();
            }
        });
    }

    private void connectar() {
        if (connectat) {
            afegirMissatgeSistema("Ja estàs connectat/da.");
            return;
        }

        String host = campHost.getText().trim();
        String portText = campPort.getText().trim();
        String nomUsuari = campNom.getText().trim();

        if (host.isEmpty()) {
            mostrarError("Indica l'adreça del servidor.");
            return;
        }

        if (nomUsuari.isEmpty()) {
            mostrarError("Escriu un nom d'usuari abans de connectar.");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            mostrarError("El port ha de ser un número vàlid.");
            return;
        }

        botoConnectar.setEnabled(false);
        afegirMissatgeSistema("Connectant a " + host + ":" + port + "...");

        Thread filConnexio = new Thread(() -> {
            try {
                socket = new Socket(host, port);
                entradaServidor = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                sortidaServidor = new PrintWriter(socket.getOutputStream(), true);
                connectat = true;

                actualitzarControlsConnexio(true);
                afegirMissatgeSistema("Connexió establerta.");
                iniciarFilReceptor();
                sortidaServidor.println(nomUsuari);
            } catch (IOException e) {
                connectat = false;
                actualitzarControlsConnexio(false);
                afegirMissatgeSistema("No s'ha pogut connectar: " + e.getMessage());
                tancarRecursos();
            }
        }, "Connexio-Client");

        filConnexio.start();
    }

    private void iniciarFilReceptor() {
        Thread receptor = new Thread(() -> {
            try {
                String missatgeServidor;
                while ((missatgeServidor = entradaServidor.readLine()) != null) {
                    afegirMissatge(missatgeServidor);
                }
                if (connectat) {
                    afegirMissatgeSistema("El servidor ha tancat la connexió.");
                }
            } catch (IOException e) {
                if (connectat) {
                    afegirMissatgeSistema("Connexió interrompuda: " + e.getMessage());
                }
            } finally {
                desconnectar(false);
            }
        }, "Receptor-Missatges");

        receptor.setDaemon(true);
        receptor.start();
    }

    private void enviarMissatge() {
        if (!connectat || sortidaServidor == null) {
            afegirMissatgeSistema("No hi ha cap connexió activa.");
            return;
        }

        String missatge = campMissatge.getText().trim();
        if (missatge.isEmpty()) {
            return;
        }

        sortidaServidor.println(missatge);

        if ("sortir".equalsIgnoreCase(missatge)) {
            campMissatge.setText("");
            desconnectar(false);
            return;
        }

        afegirMissatge("[Tu]: " + missatge);
        campMissatge.setText("");
        campMissatge.requestFocusInWindow();
    }

    private synchronized void desconnectar(boolean avisarServidor) {
        boolean hiHaviaConnexio = connectat || socket != null;
        PrintWriter sortida = sortidaServidor;

        connectat = false;

        if (avisarServidor && sortida != null) {
            sortida.println("sortir");
        }

        tancarRecursos();
        actualitzarControlsConnexio(false);

        if (hiHaviaConnexio && avisarServidor) {
            afegirMissatgeSistema("T'has desconnectat del xat.");
        }
    }

    private void tancarRecursos() {
        try {
            if (entradaServidor != null) {
                entradaServidor.close();
            }
        } catch (IOException e) {
            afegirMissatgeSistema("Error tancant l'entrada: " + e.getMessage());
        }

        if (sortidaServidor != null) {
            sortidaServidor.close();
        }

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            afegirMissatgeSistema("Error tancant el socket: " + e.getMessage());
        }

        entradaServidor = null;
        sortidaServidor = null;
        socket = null;
    }

    private void actualitzarControlsConnexio(boolean estaConnectat) {
        SwingUtilities.invokeLater(() -> {
            botoConnectar.setEnabled(!estaConnectat);
            botoDesconnectar.setEnabled(estaConnectat);
            campHost.setEnabled(!estaConnectat);
            campPort.setEnabled(!estaConnectat);
            campNom.setEnabled(!estaConnectat);
            campMissatge.setEnabled(estaConnectat);
            botoEnviar.setEnabled(estaConnectat);

            if (estaConnectat) {
                campMissatge.requestFocusInWindow();
            }
        });
    }

    private void afegirMissatge(String missatge) {
        SwingUtilities.invokeLater(() -> {
            areaChat.append(missatge + System.lineSeparator());
            areaChat.setCaretPosition(areaChat.getDocument().getLength());
        });
    }

    private void afegirMissatgeSistema(String missatge) {
        afegirMissatge("[INFO] " + missatge);
    }

    private void mostrarError(String missatge) {
        JOptionPane.showMessageDialog(this, missatge, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientXat finestra = new ClientXat();
            finestra.setVisible(true);
        });
    }
}
