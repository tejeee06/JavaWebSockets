package client;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Finestra senzilla per comprovar si el servidor està disponible.
 */
public class ClientVerificadorDisponibilitatDelServidor extends JFrame {

    private static final long serialVersionUID = 1L;

    static final String ADRECA = "localhost";
    static final int PORT = 12345;
    static final int TIMEOUT_MS = 2000;

    private final JTextField campHost = new JTextField(ADRECA, 12);
    private final JTextField campPort = new JTextField(String.valueOf(PORT), 6);
    private final JLabel etiquetaResultat = new JLabel("Prem el botó per verificar el servei.");
    private final JButton botoComprovar = new JButton("Comprovar");

    public ClientVerificadorDisponibilitatDelServidor() {
        super("Verificador del Servidor");
        configurarFinestra();
        configurarEsdeveniments();
    }

    private void configurarFinestra() {
        JPanel panellDades = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panellDades.setBorder(BorderFactory.createTitledBorder("Connexió"));
        panellDades.add(new JLabel("Servidor"));
        panellDades.add(campHost);
        panellDades.add(new JLabel("Port"));
        panellDades.add(campPort);
        panellDades.add(botoComprovar);

        JPanel panellResultat = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panellResultat.setBorder(BorderFactory.createTitledBorder("Resultat"));
        panellResultat.add(etiquetaResultat);

        setLayout(new BorderLayout(10, 10));
        add(panellDades, BorderLayout.NORTH);
        add(panellResultat, BorderLayout.CENTER);

        setSize(520, 180);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    private void configurarEsdeveniments() {
        botoComprovar.addActionListener(e -> comprovarDisponibilitat());
    }

    private void comprovarDisponibilitat() {
        String host = campHost.getText().trim();
        int port;

        try {
            port = Integer.parseInt(campPort.getText().trim());
        } catch (NumberFormatException e) {
            etiquetaResultat.setText("El port ha de ser un número vàlid.");
            return;
        }

        etiquetaResultat.setText("Comprovant " + host + ":" + port + "...");
        botoComprovar.setEnabled(false);

        Thread filVerificacio = new Thread(() -> {
            boolean disponible = estaDisponible(host, port);
            SwingUtilities.invokeLater(() -> {
                if (disponible) {
                    etiquetaResultat.setText("Servei disponible: el servidor està en marxa.");
                } else {
                    etiquetaResultat.setText("Servei no disponible: el servidor no respon.");
                }
                botoComprovar.setEnabled(true);
            });
        }, "Verificador-Servidor");

        filVerificacio.start();
    }

    public static boolean estaDisponible(String adreca, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(adreca, port), TIMEOUT_MS);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientVerificadorDisponibilitatDelServidor finestra =
                    new ClientVerificadorDisponibilitatDelServidor();
            finestra.setVisible(true);
        });
    }
}
