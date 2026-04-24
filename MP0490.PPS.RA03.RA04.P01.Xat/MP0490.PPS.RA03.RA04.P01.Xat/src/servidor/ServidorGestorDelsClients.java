package servidor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Gestiona la comunicació d'un client concret dins d'un fil independent.
 */
public class ServidorGestorDelsClients implements Runnable {

    private final Socket socket;
    private final BufferedReader entrada;
    private final PrintWriter sortida;

    private String nomUsuari;
    private boolean tancat;

    public ServidorGestorDelsClients(Socket socket) throws IOException {
        this.socket = socket;
        this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.sortida = new PrintWriter(socket.getOutputStream(), true);
    }

    @Override
    public void run() {
        try {
            sortida.println("Benvingut/da al xat!");
            sortida.println("Escriu el teu nom i prem Connectar:");

            nomUsuari = entrada.readLine();
            if (nomUsuari == null || nomUsuari.isBlank()) {
                nomUsuari = "Anonim-" + socket.getPort();
            } else {
                nomUsuari = nomUsuari.trim();
            }

            ServidorXat.registrarEsdeveniment("L'usuari '" + nomUsuari + "' s'ha unit al xat.");
            ServidorXat.broadcast("*** " + nomUsuari + " s'ha unit al xat ***", this);
            sortida.println("Connectat/da! Ja pots enviar missatges.");

            String missatgeRebut;
            while ((missatgeRebut = entrada.readLine()) != null) {
                if (missatgeRebut.isBlank()) {
                    continue;
                }

                if ("sortir".equalsIgnoreCase(missatgeRebut.trim())) {
                    break;
                }

                String missatgeFormatat = "[" + nomUsuari + "]: " + missatgeRebut;
                ServidorXat.registrarEsdeveniment(missatgeFormatat);
                ServidorXat.broadcast(missatgeFormatat, this);
            }
        } catch (IOException e) {
            if (!tancat) {
                ServidorXat.registrarEsdeveniment("Connexió perduda amb '" + obtenirNomVisible() + "'.");
            }
        } finally {
            desconnectar();
        }
    }

    public void enviarMissatge(String missatge) {
        sortida.println(missatge);
    }

    public synchronized void tancarDesDeServidor() {
        if (tancat) {
            return;
        }

        sortida.println("*** El servidor s'ha aturat. ***");
        desconnectar();
    }

    private synchronized void desconnectar() {
        if (tancat) {
            return;
        }
        tancat = true;

        ServidorXat.eliminarClient(this);

        if (nomUsuari != null) {
            ServidorXat.broadcast("*** " + nomUsuari + " ha sortit del xat ***", this);
            ServidorXat.registrarEsdeveniment("L'usuari '" + nomUsuari + "' s'ha desconnectat.");
        }

        try {
            socket.close();
        } catch (IOException e) {
            ServidorXat.registrarEsdeveniment("Error tancant el socket d'un client: " + e.getMessage());
        }
    }

    private String obtenirNomVisible() {
        return nomUsuari != null ? nomUsuari : socket.getInetAddress().getHostAddress();
    }
}
